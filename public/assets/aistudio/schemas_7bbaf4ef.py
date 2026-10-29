from typing import Literal
from pydantic import BaseModel, Field, EmailStr

LanguageCode = Literal[
    "as", "bn", "brx", "doi", "en", "gu", "hi", "kn", "ks",
    "kok", "mai", "ml", "mni", "mr", "ne", "or", "pa",
    "sa", "sat", "sd", "ta", "te", "ur"
]


class RegisterIn(BaseModel):
    email: EmailStr
    password: str = Field(min_length=10, max_length=128)
    role: Literal["patient", "doctor"] = "patient"
    language: LanguageCode = "en"
    display_name: str = Field(min_length=2, max_length=100)
    phone: str = Field(default="", max_length=30)
    age: int | None = Field(default=None, ge=0, le=120)
    gender: str = Field(default="", max_length=40)
    registration_number: str = Field(default="", max_length=100)
    qualifications: str = Field(default="", max_length=300)


class LoginIn(BaseModel):
    email: EmailStr
    password: str = Field(min_length=1, max_length=128)


class SymptomIn(BaseModel):
    symptoms: str = Field(min_length=1, max_length=5000)
    severity: Literal["mild", "moderate", "severe"] = "moderate"
    duration: str = Field(min_length=1, max_length=200)
    language: LanguageCode = "en"
    allergies: str = Field(default="", max_length=2000)
    current_medications: str = Field(default="", max_length=2000)
    consent_to_ai_processing: bool = False
    latitude: float | None = Field(default=None, ge=-90, le=90)
    longitude: float | None = Field(default=None, ge=-180, le=180)


class ConsultationIn(BaseModel):
    doctor_id: str = Field(min_length=1, max_length=100)
    reason: str = Field(min_length=3, max_length=3000)
    language: LanguageCode = "en"


class TranscriptIn(BaseModel):
    text: str = Field(min_length=1, max_length=20000)
    source_language: str = Field(default="unknown", max_length=40)


class ClinicalAssistIn(BaseModel):
    transcript: str = Field(min_length=1, max_length=20000)


class MedicineIn(BaseModel):
    name: str = Field(min_length=1, max_length=200)
    dosage: str = Field(default="", max_length=200)
    frequency: str = Field(default="", max_length=200)
    duration: str = Field(default="", max_length=200)
    instructions: str = Field(default="", max_length=500)


class PrescriptionIn(BaseModel):
    hospital_name: str = Field(default="", max_length=200)
    diagnosis: str = Field(default="", max_length=3000)
    symptoms: str = Field(default="", max_length=5000)
    medicines: list[MedicineIn] = Field(default_factory=list, max_length=30)
    advice: str = Field(default="", max_length=5000)
    follow_up: str = Field(default="", max_length=1000)
    doctor_signature: str = Field(default="", max_length=200)


class ReferralIn(BaseModel):
    patient_id: str
    specialist: str = Field(min_length=2, max_length=200)
    reason: str = Field(min_length=2, max_length=3000)
    facility: str = Field(default="", max_length=300)
    follow_up_date: str = Field(default="", max_length=50)


class ReferralUpdateIn(BaseModel):
    status: Literal[
        "referred", "appointment_booked", "in_treatment",
        "completed", "not_completed"
    ]
    notes: str = Field(default="", max_length=3000)