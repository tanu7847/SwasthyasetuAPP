import os
import re
import uuid
import math
from pathlib import Path
from datetime import datetime, timezone
from typing import Any

import requests
from bson import ObjectId
from fastapi import (
    FastAPI, HTTPException, Depends, UploadFile, File, Query
)
from fastapi.middleware.cors import CORSMiddleware

from .db import db, client, ensure_indexes
from .security import (
    hash_password, verify_password, create_token,
    current_user, require_roles, is_assigned,
    require_patient_or_assigned_doctor,
)
from .schemas import (
    RegisterIn, LoginIn, SymptomIn, ConsultationIn,
    TranscriptIn, ClinicalAssistIn, PrescriptionIn,
    ReferralIn, ReferralUpdateIn,
)

JWT_SECRET = os.getenv("JWT_SECRET", "")
GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")
MAX_UPLOAD_BYTES = 20 * 1024 * 1024
UPLOAD_DIR = Path(os.getenv("UPLOAD_DIR", "./private_uploads")).resolve()
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

app = FastAPI(
    title="SwasthyaSetu API",
    description="Development prototype for healthcare coordination.",
    version="3.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        x.strip() for x in os.getenv(
            "CORS_ORIGINS", "http://localhost:5173"
        ).split(",") if x.strip()
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
def startup():
    client.admin.command("ping")
    ensure_indexes()


def now():
    return datetime.now(timezone.utc)


def oid(value: str):
    if not ObjectId.is_valid(str(value)):
        raise HTTPException(400, "Invalid record ID.")
    return ObjectId(str(value))


def clean(doc):
    if doc is None:
        return None
    out = dict(doc)
    out["id"] = str(out.pop("_id", ""))
    for key, value in list(out.items()):
        if isinstance(value, (datetime, ObjectId)):
            out[key] = value.isoformat() if isinstance(value, datetime) else str(value)
    return out


def groq_chat(messages, model="llama-3.3-70b-versatile"):
    if not GROQ_API_KEY:
        raise HTTPException(503, "AI is not configured. Set GROQ_API_KEY.")
    try:
        response = requests.post(
            "https://api.groq.com/openai/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {GROQ_API_KEY}",
                "Content-Type": "application/json",
            },
            json={
                "model": model,
                "messages": messages,
                "temperature": 0.2,
            },
            timeout=45,
        )
        response.raise_for_status()
        return response.json()["choices"][0]["message"]["content"]
    except requests.RequestException:
        raise HTTPException(502, "AI service is temporarily unavailable.")


def get_consultation(consultation_id: str, user):
    record = db.consultations.find_one({"_id": oid(consultation_id)})
    if not record:
        raise HTTPException(404, "Consultation not found.")
    if user["role"] == "admin":
        return record
    if user["role"] == "patient" and record["patient_id"] == str(user["_id"]):
        return record
    if user["role"] == "doctor" and record["doctor_id"] == str(user["_id"]):
        return record
    raise HTTPException(403, "You cannot access this consultation.")


def haversine_km(lat1, lon1, lat2, lon2):
    r = 6371
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


@app.get("/health")
def health():
    return {"status": "ok", "service": "SwasthyaSetu"}


# -------------------- Authentication --------------------

@app.post("/auth/register")
def register(data: RegisterIn):
    email = str(data.email).lower().strip()
    if db.users.find_one({"email": email}):
        raise HTTPException(409, "An account with this email already exists.")

    user = {
        "display_name": data.display_name.strip(),
        "email": email,
        "password_hash": hash_password(data.password),
        "phone": data.phone,
        "language": data.language,
        "role": data.role,
        "age": data.age,
        "gender": data.gender,
        "registration_number": data.registration_number,
        "qualifications": data.qualifications,
        "approved": data.role == "patient",
        "active": True,
        "assigned_patient_ids": [],
        "created_at": now(),
    }
    result = db.users.insert_one(user)
    return {
        "user_id": str(result.inserted_id),
        "message": (
            "Account created. Doctor access requires administrator approval."
            if data.role == "doctor" else "Account created."
        ),
    }


@app.post("/auth/login")
def login(data: LoginIn):
    user = db.users.find_one({"email": str(data.email).lower().strip()})
    if not user or not verify_password(data.password, user.get("password_hash", "")):
        raise HTTPException(401, "Incorrect email or password.")
    if not user.get("active", True):
        raise HTTPException(403, "This account is inactive.")
    if user["role"] == "doctor" and not user.get("approved", False):
        raise HTTPException(403, "Doctor account is awaiting administrator approval.")

    return {
        "access_token": create_token(str(user["_id"]), user["role"]),
        "token_type": "bearer",
        "role": user["role"],
        "display_name": user["display_name"],
        "language": user.get("language", "en"),
        "user_id": str(user["_id"]),
    }


@app.get("/auth/me")
def me(user=Depends(current_user)):
    result = clean(user)
    result.pop("password_hash", None)
    result.pop("assigned_patient_ids", None)
    return result


# -------------------- Patient symptom intake and triage --------------------

RED_FLAGS = [
    "chest pain", "difficulty breathing", "cannot breathe",
    "severe bleeding", "unconscious", "not responding",
    "face drooping", "weakness on one side", "seizure",
    "blue lips", "overdose", "suicidal", "anaphylaxis",
    "severe allergic reaction", "sudden severe headache",
]

YELLOW_FLAGS = [
    "high fever", "persistent vomiting", "dehydration",
    "worsening pain", "blood in stool", "fainting",
]


@app.post("/patients/me/symptoms")
def submit_symptoms(data: SymptomIn, user=Depends(require_roles("patient"))):
    text = data.symptoms.lower()
    red = [x for x in RED_FLAGS if x in text]
    yellow = [x for x in YELLOW_FLAGS if x in text]

    if red:
        signal = "RED"
        urgency = "Potential emergency — get emergency help now."
        guidance = [
            "Call 112 in India or your local emergency number now.",
            "Do not delay emergency help to finish this form or wait for AI.",
            "Do not drive yourself if severely unwell; ask someone to stay with you.",
            "Follow emergency dispatcher instructions.",
        ]
    elif yellow or data.severity == "severe":
        signal = "YELLOW"
        urgency = "Prompt professional assessment recommended."
        guidance = [
            "Contact a qualified healthcare professional promptly.",
            "Seek emergency help if symptoms worsen or feel dangerous.",
            "Avoid starting medicines or remedies without appropriate medical advice.",
        ]
    else:
        signal = "GREEN"
        urgency = "No configured warning phrase detected."
        guidance = [
            "This screen cannot prove symptoms are minor or rule out an emergency.",
            "Arrange clinician review if symptoms persist or concern you.",
            "Seek urgent care for new, severe, or worsening symptoms.",
        ]

    record = {
        "patient_id": str(user["_id"]),
        "symptoms": data.symptoms,
        "severity": data.severity,
        "duration": data.duration,
        "allergies": data.allergies,
        "current_medications": data.current_medications,
        "language": data.language,
        "latitude": data.latitude,
        "longitude": data.longitude,
        "signal": signal,
        "urgency": urgency,
        "warning_signs": red + yellow,
        "guidance": guidance,
        "created_at": now(),
    }
    inserted = db.symptoms.insert_one(record)
    record_id = str(inserted.inserted_id)

    db.audit_logs.insert_one({
        "event": "symptom_screening",
        "patient_id": str(user["_id"]),
        "signal": signal,
        "created_at": now(),
    })

    return {
        "record_id": record_id,
        "signal": signal,
        "urgency": urgency,
        "warning_signs": red + yellow,
        "guidance": guidance,
        "emergency_number": "112",
        "disclaimer": (
            "Prototype keyword screening only. It is not a diagnosis or validated "
            "triage tool. If you think this is an emergency, call 112 in India."
        ),
    }


@app.get("/patients/me/symptoms")
def my_symptoms(user=Depends(require_roles("patient"))):
    records = list(
        db.symptoms.find({"patient_id": str(user["_id"])})
        .sort("created_at", -1).limit(100)
    )
    return {"records": [clean(x) for x in records]}


# -------------------- Nearby hospitals --------------------

@app.get("/hospitals/nearby")
def nearby_hospitals(
    latitude: float = Query(..., ge=-90, le=90),
    longitude: float = Query(..., ge=-180, le=180),
    radius_m: int = Query(10000, ge=500, le=50000),
    user=Depends(current_user),
):
    query = f"""
    [out:json][timeout:15];
    (
      node["amenity"="hospital"](around:{radius_m},{latitude},{longitude});
      way["amenity"="hospital"](around:{radius_m},{latitude},{longitude});
      relation["amenity"="hospital"](around:{radius_m},{latitude},{longitude});
      node["healthcare"="hospital"](around:{radius_m},{latitude},{longitude});
    );
    out center tags;
    """
    try:
        response = requests.post(
            "https://overpass-api.de/api/interpreter",
            data=query,
            headers={"User-Agent": "SwasthyaSetu-Prototype/1.0"},
            timeout=20,
        )
        response.raise_for_status()
        elements = response.json().get("elements", [])
    except Exception:
        raise HTTPException(
            502,
            "Nearby hospital search is unavailable. Call emergency services if urgent.",
        )

    results = []
    for item in elements:
        tags = item.get("tags", {})
        lat = item.get("lat", item.get("center", {}).get("lat"))
        lon = item.get("lon", item.get("center", {}).get("lon"))
        if lat is None or lon is None:
            continue
        results.append({
            "name": tags.get("name", "Hospital name not listed"),
            "phone": tags.get("phone") or tags.get("contact:phone") or "Not listed",
            "address": tags.get("addr:full") or ", ".join(
                str(tags[k]) for k in ("addr:street", "addr:city") if tags.get(k)
            ) or "Address not listed",
            "latitude": lat,
            "longitude": lon,
            "distance_km": round(
                haversine_km(latitude, longitude, float(lat), float(lon)), 2
            ),
            "directions_url": (
                f"https://www.google.com/maps/dir/?api=1&destination={lat},{lon}"
            ),
            "source": "OpenStreetMap; contact details may be missing or outdated",
        })

    results.sort(key=lambda x: x["distance_km"])
    return {"hospitals": results[:20]}


# -------------------- Doctors and consultations --------------------

@app.get("/doctors/available")
def available_doctors(user=Depends(require_roles("patient"))):
    doctors = db.users.find({
        "role": "doctor", "approved": True, "active": True
    })
    return {"doctors": [{
        "doctor_id": str(d["_id"]),
        "name": d.get("display_name", "Doctor"),
        "qualifications": d.get("qualifications", ""),
        "language": d.get("language", "en"),
    } for d in doctors]}


@app.post("/consultations")
def create_consultation(
    data: ConsultationIn,
    user=Depends(require_roles("patient")),
):
    doctor = db.users.find_one({
        "_id": oid(data.doctor_id),
        "role": "doctor",
        "approved": True,
        "active": True,
    })
    if not doctor:
        raise HTTPException(404, "Approved doctor not found.")

    record = {
        "patient_id": str(user["_id"]),
        "doctor_id": str(doctor["_id"]),
        "reason": data.reason,
        "language": data.language,
        "status": "requested",
        "room_id": "swasthya-" + uuid.uuid4().hex,
        "created_at": now(),
        "updated_at": now(),
    }
    result = db.consultations.insert_one(record)
    return {
        "consultation_id": str(result.inserted_id),
        "status": "requested",
        "message": "Consultation request sent.",
    }


@app.get("/consultations/mine")
def consultations_mine(user=Depends(current_user)):
    if user["role"] == "patient":
        query = {"patient_id": str(user["_id"])}
    elif user["role"] == "doctor":
        query = {"doctor_id": str(user["_id"])}
    elif user["role"] == "admin":
        query = {}
    else:
        raise HTTPException(403, "Not authorised.")
    items = list(db.consultations.find(query).sort("created_at", -1).limit(100))
    return {"consultations": [clean(x) for x in items]}


@app.post("/consultations/{consultation_id}/accept")
def accept_consultation(
    consultation_id: str,
    user=Depends(require_roles("doctor")),
):
    record = db.consultations.find_one({
        "_id": oid(consultation_id),
        "doctor_id": str(user["_id"]),
    })
    if not record:
        raise HTTPException(404, "Consultation request not found.")
    if record["status"] not in ("requested", "accepted"):
        raise HTTPException(409, "This consultation cannot be accepted.")

    db.consultations.update_one(
        {"_id": record["_id"]},
        {"$set": {"status": "accepted", "updated_at": now()}},
    )
    db.users.update_one(
        {"_id": user["_id"]},
        {"$addToSet": {"assigned_patient_ids": record["patient_id"]}},
    )
    return {
        "status": "accepted",
        "room_id": record["room_id"],
        "patient_id": record["patient_id"],
    }


# -------------------- Audio, image, transcript --------------------

@app.post("/consultations/{consultation_id}/audio")
async def upload_audio(
    consultation_id: str,
    file: UploadFile = File(...),
    user=Depends(current_user),
):
    consultation = get_consultation(consultation_id, user)
    allowed = {
        "audio/webm", "audio/wav", "audio/x-wav", "audio/mpeg",
        "audio/mp4", "audio/ogg", "audio/mp3", "application/octet-stream",
    }
    if file.content_type not in allowed:
        raise HTTPException(415, "Unsupported audio format.")

    content = await file.read()
    if not content or len(content) > MAX_UPLOAD_BYTES:
        raise HTTPException(413, "Audio is empty or exceeds 20 MB.")

    filename = re.sub(r"[^a-zA-Z0-9._-]", "_", file.filename or "audio.webm")
    path = UPLOAD_DIR / f"{uuid.uuid4().hex}_{filename}"
    path.write_bytes(content)

    transcript = ""
    source_language = "unknown"
    if GROQ_API_KEY:
        try:
            with path.open("rb") as audio:
                response = requests.post(
                    "https://api.groq.com/openai/v1/audio/transcriptions",
                    headers={"Authorization": f"Bearer {GROQ_API_KEY}"},
                    files={"file": (filename, audio, file.content_type)},
                    data={"model": "whisper-large-v3", "response_format": "verbose_json"},
                    timeout=120,
                )
            response.raise_for_status()
            payload = response.json()
            transcript = payload.get("text", "")
            source_language = payload.get("language", "unknown")
        except Exception:
            path.unlink(missing_ok=True)
            raise HTTPException(502, "Transcription failed. Please try again.")

    db.uploads.insert_one({
        "consultation_id": consultation_id,
        "patient_id": consultation["patient_id"],
        "uploaded_by": str(user["_id"]),
        "filename": filename,
        "private_path": str(path),
        "kind": "audio",
        "created_at": now(),
    })

    english = transcript
    if transcript and source_language not in ("en", "english", "unknown"):
        try:
            english = groq_chat([
                {"role": "system", "content":
                    "Translate the conversation into English. Preserve uncertainty, "
                    "negation, medicine names and quantities. Do not add facts."},
                {"role": "user", "content": transcript},
            ])
        except HTTPException:
            english = "[English translation unavailable; original transcript retained.]"

    if transcript:
        db.transcripts.insert_one({
            "consultation_id": consultation_id,
            "patient_id": consultation["patient_id"],
            "doctor_id": consultation["doctor_id"],
            "original_text": transcript,
            "english_text": english,
            "source_language": source_language,
            "created_at": now(),
        })

    return {
        "message": "Audio uploaded.",
        "transcript": transcript,
        "english_text": english,
        "source_language": source_language,
        "transcription_configured": bool(GROQ_API_KEY),
    }


@app.post("/consultations/{consultation_id}/image")
async def upload_image(
    consultation_id: str,
    file: UploadFile = File(...),
    user=Depends(current_user),
):
    consultation = get_consultation(consultation_id, user)
    if file.content_type not in ("image/jpeg", "image/png", "image/webp"):
        raise HTTPException(415, "Upload a JPEG, PNG or WebP image.")

    content = await file.read()
    if not content or len(content) > MAX_UPLOAD_BYTES:
        raise HTTPException(413, "Image is empty or exceeds 20 MB.")

    filename = re.sub(r"[^a-zA-Z0-9._-]", "_", file.filename or "image.png")
    path = UPLOAD_DIR / f"{uuid.uuid4().hex}_{filename}"
    path.write_bytes(content)

    db.uploads.insert_one({
        "consultation_id": consultation_id,
        "patient_id": consultation["patient_id"],
        "uploaded_by": str(user["_id"]),
        "filename": filename,
        "private_path": str(path),
        "kind": "image",
        "created_at": now(),
    })

    # Basic OpenCV processing can produce a visual contrast preview only.
    # It is NOT infection detection and must not be presented as diagnosis.
    processed = False
    try:
        import cv2
        image = cv2.imread(str(path))
        if image is not None:
            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
            _ = cv2.GaussianBlur(gray, (5, 5), 0)
            processed = True
    except ImportError:
        pass

    return {
        "message": "Image stored for clinician review.",
        "opencv_available": processed,
        "finding": None,
        "note": (
            "No infection detection is performed. A trained and clinically validated "
            "model is required before highlighting suspected lesions."
        ),
    }


@app.post("/consultations/{consultation_id}/transcript")
def save_transcript(
    consultation_id: str,
    data: TranscriptIn,
    user=Depends(current_user),
):
    consultation = get_consultation(consultation_id, user)
    original = data.text.strip()
    english = original

    if data.source_language.lower() not in ("en", "english", "unknown"):
        try:
            english = groq_chat([
                {"role": "system", "content":
                    "Translate this medical conversation into clear English. "
                    "Preserve uncertainty, negation, medicine names and quantities. "
                    "Do not add medical facts."},
                {"role": "user", "content": original},
            ])
        except HTTPException:
            english = "[Translation unavailable; original retained.]"

    record = {
        "consultation_id": consultation_id,
        "patient_id": consultation["patient_id"],
        "doctor_id": consultation["doctor_id"],
        "original_text": original,
        "english_text": english,
        "source_language": data.source_language,
        "created_at": now(),
    }
    result = db.transcripts.insert_one(record)
    return {"transcript_id": str(result.inserted_id), **clean(record)}


@app.get("/consultations/{consultation_id}/transcripts")
def read_transcripts(
    consultation_id: str,
    user=Depends(current_user),
):
    get_consultation(consultation_id, user)
    records = list(
        db.transcripts.find({"consultation_id": consultation_id})
        .sort("created_at", 1)
    )
    return {"transcripts": [clean(x) for x in records]}


@app.post("/consultations/{consultation_id}/clinical-assist")
def clinical_assist(
    consultation_id: str,
    data: ClinicalAssistIn,
    user=Depends(require_roles("doctor")),
):
    consultation = get_consultation(consultation_id, user)
    patient = db.users.find_one({"_id": oid(consultation["patient_id"])})
    latest = db.symptoms.find_one(
        {"patient_id": consultation["patient_id"]},
        sort=[("created_at", -1)],
    )

    prompt = f"""
Assist a qualified clinician. Do not diagnose autonomously or prescribe medicines.
Return JSON keys: symptoms, allergies, duration, medicines_mentioned,
important_negations, uncertainties, questions_for_doctor.
Questions should be clarifying questions. Use "not stated" for missing information.
Patient name: {patient.get('display_name', 'not stated') if patient else 'not stated'}
Recent symptoms: {latest.get('symptoms', 'not stated') if latest else 'not stated'}
Allergies: {latest.get('allergies', 'not stated') if latest else 'not stated'}
Patient conversation:
{data.transcript}
"""
    result = groq_chat([
        {"role": "system", "content": "Return valid JSON only. No markdown."},
        {"role": "user", "content": prompt},
    ])
    return {
        "clinical_assistance": result,
        "notice": "AI-generated draft. Doctor must verify all details.",
    }


# -------------------- Prescriptions --------------------

@app.post("/consultations/{consultation_id}/prescription")
def create_prescription(
    consultation_id: str,
    data: PrescriptionIn,
    user=Depends(require_roles("doctor")),
):
    consultation = get_consultation(consultation_id, user)
    if consultation["doctor_id"] != str(user["_id"]):
        raise HTTPException(403, "Only the assigned doctor can write this prescription.")

    record = {
        "consultation_id": consultation_id,
        "patient_id": consultation["patient_id"],
        "doctor_id": str(user["_id"]),
        "hospital_name": data.hospital_name,
        "diagnosis": data.diagnosis,
        "symptoms": data.symptoms,
        "medicines": [m.model_dump() for m in data.medicines],
        "advice": data.advice,
        "follow_up": data.follow_up,
        "doctor_signature": data.doctor_signature,
        "status": "draft",
        "created_at": now(),
        "updated_at": now(),
        "reviewed_at": None,
    }
    result = db.prescriptions.insert_one(record)
    return {"prescription_id": str(result.inserted_id), "status": "draft"}


@app.put("/prescriptions/{prescription_id}/review")
def review_prescription(
    prescription_id: str,
    data: PrescriptionIn,
    user=Depends(require_roles("doctor")),
):
    record = db.prescriptions.find_one({
        "_id": oid(prescription_id),
        "doctor_id": str(user["_id"]),
    })
    if not record:
        raise HTTPException(404, "Prescription not found.")
    if not data.doctor_signature.strip():
        raise HTTPException(400, "Doctor signature is required to approve.")

    db.prescriptions.update_one(
        {"_id": record["_id"]},
        {"$set": {
            "hospital_name": data.hospital_name,
            "diagnosis": data.diagnosis,
            "symptoms": data.symptoms,
            "medicines": [m.model_dump() for m in data.medicines],
            "advice": data.advice,
            "follow_up": data.follow_up,
            "doctor_signature": data.doctor_signature,
            "status": "final",
            "reviewed_at": now(),
            "updated_at": now(),
        }},
    )
    db.audit_logs.insert_one({
        "event": "prescription_finalised",
        "prescription_id": prescription_id,
        "doctor_id": str(user["_id"]),
        "created_at": now(),
    })
    return {"status": "final", "message": "Doctor-reviewed prescription approved."}


@app.get("/patients/me/prescriptions")
def patient_prescriptions(user=Depends(require_roles("patient"))):
    items = list(db.prescriptions.find({
        "patient_id": str(user["_id"]),
        "status": "final",
    }).sort("updated_at", -1))
    return {"prescriptions": [clean(x) for x in items]}


@app.get("/prescriptions/{prescription_id}")
def read_prescription(prescription_id: str, user=Depends(current_user)):
    record = db.prescriptions.find_one({"_id": oid(prescription_id)})
    if not record:
        raise HTTPException(404, "Prescription not found.")
    if user["role"] == "patient":
        if record["patient_id"] != str(user["_id"]) or record["status"] != "final":
            raise HTTPException(403, "Prescription unavailable.")
    elif user["role"] == "doctor":
        if record["doctor_id"] != str(user["_id"]):
            raise HTTPException(403, "Not authorised.")
    elif user["role"] != "admin":
        raise HTTPException(403, "Not authorised.")
    return clean(record)


# -------------------- Medical records and referrals --------------------

@app.get("/patients/me/medical-records")
def patient_medical_records(user=Depends(require_roles("patient"))):
    records = list(db.medical_records.find({
        "patient_id": str(user["_id"])
    }).sort("created_at", -1))
    return {"records": [clean(x) for x in records]}


@app.get("/doctor/patients")
def doctor_patients(user=Depends(require_roles("doctor"))):
    ids = [
        oid(x) for x in user.get("assigned_patient_ids", [])
        if ObjectId.is_valid(str(x))
    ]
    patients = list(db.users.find({"_id": {"$in": ids}, "role": "patient"}))
    return {"patients": [{
        "patient_id": str(p["_id"]),
        "display_name": p.get("display_name", "Patient"),
        "phone": p.get("phone", ""),
        "language": p.get("language", "en"),
    } for p in patients]}


@app.get("/doctor/patients/{patient_id}/symptoms")
def doctor_patient_symptoms(
    patient_id: str,
    user=Depends(require_roles("doctor")),
):
    if not is_assigned(user["_id"], patient_id):
        raise HTTPException(403, "You are not assigned to this patient.")
    records = list(db.symptoms.find({"patient_id": patient_id}).sort("created_at", -1))
    return {"records": [clean(x) for x in records]}


@app.post("/referrals")
def create_referral(data: ReferralIn, user=Depends(require_roles("doctor"))):
    if not is_assigned(user["_id"], data.patient_id):
        raise HTTPException(403, "You are not assigned to this patient.")
    record = {
        "patient_id": data.patient_id,
        "doctor_id": str(user["_id"]),
        "specialist": data.specialist,
        "reason": data.reason,
        "facility": data.facility,
        "follow_up_date": data.follow_up_date,
        "status": "referred",
        "notes": "",
        "created_at": now(),
        "updated_at": now(),
    }
    result = db.referrals.insert_one(record)
    return {"referral_id": str(result.inserted_id), "status": "referred"}


@app.get("/patients/me/referrals")
def patient_referrals(user=Depends(require_roles("patient"))):
    items = list(db.referrals.find({
        "patient_id": str(user["_id"])
    }).sort("created_at", -1))
    return {"referrals": [clean(x) for x in items]}


@app.put("/referrals/{referral_id}")
def update_referral(
    referral_id: str,
    data: ReferralUpdateIn,
    user=Depends(require_roles("doctor")),
):
    record = db.referrals.find_one({
        "_id": oid(referral_id),
        "doctor_id": str(user["_id"]),
    })
    if not record:
        raise HTTPException(404, "Referral not found.")
    db.referrals.update_one(
        {"_id": record["_id"]},
        {"$set": {"status": data.status, "notes": data.notes, "updated_at": now()}},
    )
    return {"status": data.status}


# -------------------- Admin dashboard --------------------

@app.get("/admin/dashboard")
def admin_dashboard(user=Depends(require_roles("admin"))):
    today = now().replace(hour=0, minute=0, second=0, microsecond=0)
    return {
        "registered_patients": db.users.count_documents({"role": "patient"}),
        "patient_screenings_today": db.symptoms.count_documents({"created_at": {"$gte": today}}),
        "total_screenings": db.symptoms.count_documents({}),
        "consultations_today": db.consultations.count_documents({"created_at": {"$gte": today}}),
        "total_consultations": db.consultations.count_documents({}),
        "completed_referrals": db.referrals.count_documents({"status": "completed"}),
        "open_referrals": db.referrals.count_documents({
            "status": {"$in": ["referred", "appointment_booked", "in_treatment"]}
        }),
        "note": "Screenings, visits and completed treatments are separate metrics.",
    }