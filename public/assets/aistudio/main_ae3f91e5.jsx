import React, { useEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import "./style.css";

const API = import.meta.env.VITE_API_URL || "http://127.0.0.1:8000";

const LANGUAGES = [
  ["en", "English"], ["hi", "हिन्दी"], ["mr", "मराठी"],
  ["bn", "বাংলা"], ["gu", "ગુજરાતી"], ["ta", "தமிழ்"],
  ["te", "తెలుగు"], ["kn", "ಕನ್ನಡ"], ["ml", "മലയാളം"],
  ["pa", "ਪੰਜਾਬੀ"], ["ur", "اردو"], ["as", "অসমীয়া"],
  ["or", "ଓଡ଼ିଆ"], ["ks", "کٲشُر"], ["ne", "नेपाली"],
];

async function api(path, token, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (options.body && !(options.body instanceof FormData)) {
    headers["Content-Type"] = "application/json";
  }
  const response = await fetch(`${API}${path}`, { ...options, headers });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data.detail || data.message || `Request failed (${response.status})`);
  }
  return data;
}

const json = (value) => JSON.stringify(value);

function Panel({ title, description, children, className = "" }) {
  return (
    <section className={`panel ${className}`}>
      <div className="panel-heading">
        <h2>{title}</h2>
        {description && <p>{description}</p>}
      </div>
      {children}
    </section>
  );
}

function Field({ label, children }) {
  return <label className="field"><span>{label}</span>{children}</label>;
}

function Notice({ message, kind = "info" }) {
  if (!message) return null;
  return <div className={`notice ${kind}`}>{message}</div>;
}

function App() {
  const [token, setToken] = useState(localStorage.getItem("ss_token") || "");
  const [profile, setProfile] = useState(null);
  const [authMode, setAuthMode] = useState("login");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [messageKind, setMessageKind] = useState("info");
  const [module, setModule] = useState("symptoms");

  const [auth, setAuth] = useState({
    display_name: "", email: "", password: "", phone: "",
    role: "patient", language: "en", age: "", gender: "",
    registration_number: "", qualifications: "",
  });

  const [symptomForm, setSymptomForm] = useState({
    symptoms: "", severity: "moderate", duration: "",
    allergies: "", current_medications: "", language: "en",
    consent_to_ai_processing: false,
  });
  const [triage, setTriage] = useState(null);
  const [symptomHistory, setSymptomHistory] = useState([]);
  const [hospitals, setHospitals] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [consultations, setConsultations] = useState([]);
  const [activeConsultation, setActiveConsultation] = useState("");
  const [transcripts, setTranscripts] = useState([]);
  const [uploadedImage, setUploadedImage] = useState(null);
  const [prescriptions, setPrescriptions] = useState([]);
  const [referrals, setReferrals] = useState([]);
  const [medicalRecords, setMedicalRecords] = useState([]);
  const [adminStats, setAdminStats] = useState(null);
  const [clinicalAssist, setClinicalAssist] = useState("");
  const [prescriptionId, setPrescriptionId] = useState("");
  const [prescription, setPrescription] = useState({
    hospital_name: "SwasthyaSetu Healthcare",
    diagnosis: "", symptoms: "", medicines: [{
      name: "", dosage: "", frequency: "", duration: "", instructions: "",
    }],
    advice: "", follow_up: "", doctor_signature: "",
  });
  const [referralForm, setReferralForm] = useState({
    patient_id: "", specialist: "", reason: "", facility: "", follow_up_date: "",
  });
  const [cameraActive, setCameraActive] = useState(false);
  const [recordingState, setRecordingState] = useState("idle");
  const [recordingSeconds, setRecordingSeconds] = useState(0);

  const videoRef = useRef(null);
  const mediaStreamRef = useRef(null);
  const mediaRecorderRef = useRef(null);
  const audioChunksRef = useRef([]);
  const recordingTimerRef = useRef(null);

  function notify(text, kind = "info") {
    setMessage(text);
    setMessageKind(kind);
  }

  async function run(task) {
    setBusy(true);
    try {
      await task();
    } catch (error) {
      notify(error.message || "Something went wrong.", "error");
    } finally {
      setBusy(false);
    }
  }

  async function loadProfile(currentToken = token) {
    const me = await api("/auth/me", currentToken);
    setProfile(me);
    setAuth((old) => ({ ...old, language: me.language || old.language }));
    return me;
  }

  async function loadPatientData() {
    const [symptomsResult, consultationResult, prescriptionResult, referralResult, recordResult] =
      await Promise.all([
        api("/patients/me/symptoms", token),
        api("/consultations/mine", token),
        api("/patients/me/prescriptions", token),
        api("/patients/me/referrals", token),
        api("/patients/me/medical-records", token),
      ]);
    setSymptomHistory(symptomsResult.records || []);
    setConsultations(consultationResult.consultations || []);
    setPrescriptions(prescriptionResult.prescriptions || []);
    setReferrals(referralResult.referrals || []);
    setMedicalRecords(recordResult.records || []);
  }

  async function loadDoctors() {
    const result = await api("/doctors/available", token);
    setDoctors(result.doctors || []);
  }

  async function loadDoctorData() {
    const result = await api("/consultations/mine", token);
    setConsultations(result.consultations || []);
  }

  async function loadAdminData() {
    setAdminStats(await api("/admin/dashboard", token));
  }

  useEffect(() => {
    if (!token) return;
    run(async () => {
      const me = await loadProfile(token);
      if (me.role === "patient") {
        await loadPatientData();
        await loadDoctors();
      } else if (me.role === "doctor") {
        await loadDoctorData();
      } else if (me.role === "admin") {
        await loadAdminData();
      }
    });
    // Load only when the session changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  useEffect(() => {
    return () => {
      if (recordingTimerRef.current) clearInterval(recordingTimerRef.current);
      if (mediaStreamRef.current) {
        mediaStreamRef.current.getTracks().forEach((track) => track.stop());
      }
    };
  }, []);

  async function submitAuth(event) {
    event.preventDefault();
    await run(async () => {
      const path = authMode === "login" ? "/auth/login" : "/auth/register";
      const body = authMode === "login"
        ? { email: auth.email, password: auth.password }
        : {
            ...auth,
            age: auth.age === "" ? null : Number(auth.age),
          };
      const result = await api(path, "", { method: "POST", body: json(body) });
      if (authMode === "register") {
        notify(result.message || "Account created. Please sign in.", "success");
        setAuthMode("login");
        return;
      }
      localStorage.setItem("ss_token", result.access_token);
      setToken(result.access_token);
      notify("Signed in successfully.", "success");
    });
  }

  function logout() {
    stopCamera();
    stopRecordingTimer();
    localStorage.removeItem("ss_token");
    setToken("");
    setProfile(null);
    setTriage(null);
    setActiveConsultation("");
    setModule("symptoms");
    setMessage("");
  }

  async function submitSymptoms(event) {
    event.preventDefault();
    if (!symptomForm.symptoms.trim()) {
      notify("Enter your symptoms first.", "error");
      return;
    }
    await run(async () => {
      let location = {};
      if (navigator.geolocation) {
        try {
          location = await new Promise((resolve) => {
            navigator.geolocation.getCurrentPosition(
              (position) => resolve({
                latitude: position.coords.latitude,
                longitude: position.coords.longitude,
              }),
              () => resolve({}),
              { timeout: 5000, maximumAge: 60000 }
            );
          });
        } catch { /* location is optional */ }
      }
      const result = await api("/patients/me/symptoms", token, {
        method: "POST",
        body: json({ ...symptomForm, ...location }),
      });
      setTriage(result);
      setModule(result.signal === "RED" ? "emergency" : "consult");
      await loadPatientData();
      if (result.signal === "RED") {
        notify("Potential emergency warning detected. Do not delay emergency care.", "error");
        if (location.latitude != null) {
          await searchHospitals(location.latitude, location.longitude, false);
        } else {
          findHospitals();
        }
      } else {
        notify("Screening saved. A qualified clinician should review your symptoms.", "success");
        await loadDoctors();
      }
    });
  }

  async function findHospitals() {
    if (!navigator.geolocation) {
      notify("Your browser does not support location. Call 112 in India for emergencies.", "error");
      return;
    }
    await run(async () => {
      const position = await new Promise((resolve, reject) =>
        navigator.geolocation.getCurrentPosition(resolve, reject, { timeout: 10000 })
      );
      await searchHospitals(
        position.coords.latitude,
        position.coords.longitude,
        false
      );
    });
  }

  async function searchHospitals(latitude, longitude, useBusy = true) {
    const task = async () => {
      const result = await api(
        `/hospitals/nearby?latitude=${latitude}&longitude=${longitude}&radius_m=15000`,
        token
      );
      setHospitals(result.hospitals || []);
      if (!result.hospitals?.length) {
        notify("No hospitals were returned nearby. Call 112 if this is an emergency.", "error");
      }
    };
    if (useBusy) await run(task);
    else {
      try { await task(); }
      catch (error) { notify(error.message, "error"); }
    }
  }

  async function requestConsultation(doctor) {
    await run(async () => {
      const reason = triage?.urgency || symptomForm.symptoms || "Patient requested consultation";
      const result = await api("/consultations", token, {
        method: "POST",
        body: json({
          doctor_id: doctor.doctor_id,
          reason,
          language: symptomForm.language || profile?.language || "en",
        }),
      });
      setActiveConsultation(result.consultation_id);
      setModule("consult");
      await loadPatientData();
      notify("Consultation request sent. The doctor must accept it before the consultation proceeds.", "success");
    });
  }

  async function acceptConsultation(consultation) {
    await run(async () => {
      const result = await api(
        `/consultations/${consultation.id}/accept`,
        token,
        { method: "POST" }
      );
      setActiveConsultation(consultation.id);
      await loadDoctorData();
      notify(`Consultation accepted. Room reference: ${result.room_id}. Configure a WebRTC provider for real video calling.`, "success");
    });
  }

  async function loadTranscripts(consultationId = activeConsultation) {
    if (!consultationId) return;
    const result = await api(`/consultations/${consultationId}/transcripts`, token);
    setTranscripts(result.transcripts || []);
  }

  async function uploadAudioFile(file) {
    if (!file || !activeConsultation) {
      notify("Select an active consultation before uploading audio.", "error");
      return;
    }
    await run(async () => {
      const form = new FormData();
      form.append("file", file);
      const result = await api(
        `/consultations/${activeConsultation}/audio`,
        token,
        { method: "POST", body: form }
      );
      if (result.transcript || result.english_text) {
        setTranscripts((old) => [...old, {
          original_text: result.transcript || "",
          english_text: result.english_text || "",
          source_language: result.source_language || "unknown",
        }]);
      }
      await loadTranscripts(activeConsultation);
      notify(result.transcription_configured
        ? "Audio uploaded and transcription processed."
        : "Audio uploaded. Configure GROQ_API_KEY to enable transcription.",
        "success");
    });
  }

  async function uploadImageFile(file) {
    if (!file || !activeConsultation) {
      notify("Select an active consultation before uploading an image.", "error");
      return;
    }
    await run(async () => {
      const form = new FormData();
      form.append("file", file);
      const result = await api(
        `/consultations/${activeConsultation}/image`,
        token,
        { method: "POST", body: form }
      );
      setUploadedImage({ name: file.name, note: result.note });
      notify(result.message, "success");
    });
  }

  async function saveTypedTranscript() {
    if (!activeConsultation) {
      notify("Select an active consultation first.", "error");
      return;
    }
    const text = window.prompt("Paste the conversation transcript:");
    if (!text?.trim()) return;
    await run(async () => {
      const result = await api(`/consultations/${activeConsultation}/transcript`, token, {
        method: "POST",
        body: json({
          text,
          source_language: symptomForm.language || profile?.language || "unknown",
        }),
      });
      setTranscripts((old) => [...old, result]);
      await loadTranscripts(activeConsultation);
      notify("Transcript saved.", "success");
    });
  }

  function stopRecordingTimer() {
    if (recordingTimerRef.current) clearInterval(recordingTimerRef.current);
    recordingTimerRef.current = null;
  }

  async function startRecording() {
    if (!activeConsultation) {
      notify("Choose an accepted consultation first.", "error");
      return;
    }
    if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
      notify("Audio recording is not supported in this browser.", "error");
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      mediaStreamRef.current = stream;
      audioChunksRef.current = [];
      const recorder = new MediaRecorder(stream);
      mediaRecorderRef.current = recorder;
      recorder.ondataavailable = (event) => {
        if (event.data?.size) audioChunksRef.current.push(event.data);
      };
      recorder.onstop = async () => {
        const blob = new Blob(audioChunksRef.current, {
          type: recorder.mimeType || "audio/webm",
        });
        stream.getTracks().forEach((track) => track.stop());
        mediaStreamRef.current = null;
        setRecordingState("idle");
        stopRecordingTimer();
        if (blob.size) {
          const ext = (recorder.mimeType || "").includes("mp4") ? "mp4" : "webm";
          await uploadAudioFile(new File([blob], `consultation.${ext}`, {
            type: recorder.mimeType || "audio/webm",
          }));
        }
      };
      recorder.start();
      setRecordingSeconds(0);
      setRecordingState("recording");
      recordingTimerRef.current = setInterval(
        () => setRecordingSeconds((value) => value + 1), 1000
      );
      notify("Recording started. Obtain consent before recording a consultation.", "success");
    } catch {
      notify("Microphone permission was denied or the device is unavailable.", "error");
    }
  }

  function pauseRecording() {
    if (mediaRecorderRef.current?.state === "recording") {
      mediaRecorderRef.current.pause();
      setRecordingState("paused");
      stopRecordingTimer();
    }
  }

  function resumeRecording() {
    if (mediaRecorderRef.current?.state === "paused") {
      mediaRecorderRef.current.resume();
      setRecordingState("recording");
      recordingTimerRef.current = setInterval(
        () => setRecordingSeconds((value) => value + 1), 1000
      );
    }
  }

  function stopRecording() {
    if (mediaRecorderRef.current &&
        ["recording", "paused"].includes(mediaRecorderRef.current.state)) {
      mediaRecorderRef.current.stop();
    }
  }

  async function startCamera() {
    if (!navigator.mediaDevices?.getUserMedia) {
      notify("Camera preview is not supported in this browser.", "error");
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
      mediaStreamRef.current = stream;
      if (videoRef.current) videoRef.current.srcObject = stream;
      setCameraActive(true);
    } catch {
      notify("Camera/microphone permission was denied or the device is unavailable.", "error");
    }
  }

  function stopCamera() {
    if (mediaStreamRef.current) {
      mediaStreamRef.current.getTracks().forEach((track) => track.stop());
      mediaStreamRef.current = null;
    }
    if (videoRef.current) videoRef.current.srcObject = null;
    setCameraActive(false);
  }

  async function requestClinicalAssist() {
    if (!activeConsultation) {
      notify("Select an accepted consultation first.", "error");
      return;
    }
    const transcript = window.prompt("Paste the transcript to analyse:");
    if (!transcript?.trim()) return;
    await run(async () => {
      const result = await api(
        `/consultations/${activeConsultation}/clinical-assist`,
        token,
        { method: "POST", body: json({ transcript }) }
      );
      setClinicalAssist(result.clinical_assistance);
      notify("AI assistance generated for doctor review.", "success");
    });
  }

  async function savePrescriptionDraft() {
    if (!activeConsultation) {
      notify("Select a consultation first.", "error");
      return;
    }
    await run(async () => {
      const result = await api(
        `/consultations/${activeConsultation}/prescription`,
        token,
        { method: "POST", body: json(prescription) }
      );
      setPrescriptionId(result.prescription_id);
      notify("Prescription draft saved. It is not visible to the patient until finalised.", "success");
    });
  }

  async function finalisePrescription() {
    if (!prescriptionId) {
      notify("Save the prescription draft first.", "error");
      return;
    }
    if (!prescription.doctor_signature.trim()) {
      notify("Enter the doctor's signature/name before finalising.", "error");
      return;
    }
    await run(async () => {
      await api(`/prescriptions/${prescriptionId}/review`, token, {
        method: "PUT", body: json(prescription),
      });
      notify("Prescription finalised and made available to the patient.", "success");
    });
  }

  async function createReferral(event) {
    event.preventDefault();
    await run(async () => {
      await api("/referrals", token, {
        method: "POST", body: json(referralForm),
      });
      setReferralForm({
        patient_id: "", specialist: "", reason: "", facility: "", follow_up_date: "",
      });
      notify("Referral created.", "success");
    });
  }

  function updateMedicine(index, key, value) {
    setPrescription((old) => ({
      ...old,
      medicines: old.medicines.map((medicine, i) =>
        i === index ? { ...medicine, [key]: value } : medicine
      ),
    }));
  }

  function renderAuth() {
    return (
      <main className="auth-layout">
        <section className="auth-hero">
          <div className="brand-mark">SS</div>
          <p className="eyebrow">AI-ASSISTED HEALTHCARE COORDINATION</p>
          <h1>SwasthyaSetu</h1>
          <p>Connect patients, doctors and care pathways through one coordinated platform.</p>
          <ul>
            <li>Structured symptom intake</li>
            <li>Emergency-first guidance</li>
            <li>Doctor-reviewed medical records</li>
          </ul>
          <p className="small-muted">Prototype only. Not a replacement for emergency services or professional medical care.</p>
        </section>
        <Panel title={authMode === "login" ? "Welcome back" : "Create an account"}
          description={authMode === "login" ? "Sign in to continue." : "Doctor accounts require administrator approval."}
          className="auth-card">
          <form onSubmit={submitAuth} className="form-grid">
            {authMode === "register" && <>
              <Field label="Full name">
                <input required minLength="2" value={auth.display_name}
                  onChange={(e) => setAuth({ ...auth, display_name: e.target.value })} />
              </Field>
              <Field label="Phone number">
                <input value={auth.phone}
                  onChange={(e) => setAuth({ ...auth, phone: e.target.value })} />
              </Field>
              <Field label="Account type">
                <select value={auth.role}
                  onChange={(e) => setAuth({ ...auth, role: e.target.value })}>
                  <option value="patient">Patient</option>
                  <option value="doctor">Doctor</option>
                </select>
              </Field>
              <Field label="Preferred language">
                <select value={auth.language}
                  onChange={(e) => setAuth({ ...auth, language: e.target.value })}>
                  {LANGUAGES.map(([code, label]) => <option key={code} value={code}>{label}</option>)}
                </select>
              </Field>
              <Field label="Age">
                <input type="number" min="0" max="120" value={auth.age}
                  onChange={(e) => setAuth({ ...auth, age: e.target.value })} />
              </Field>
              <Field label="Gender (optional)">
                <input value={auth.gender}
                  onChange={(e) => setAuth({ ...auth, gender: e.target.value })} />
              </Field>
              {auth.role === "doctor" && <>
                <Field label="Medical registration number">
                  <input value={auth.registration_number}
                    onChange={(e) => setAuth({ ...auth, registration_number: e.target.value })} />
                </Field>
                <Field label="Qualifications">
                  <input value={auth.qualifications}
                    onChange={(e) => setAuth({ ...auth, qualifications: e.target.value })} />
                </Field>
              </>}
            </>}
            <Field label="Email address">
              <input type="email" required value={auth.email}
                onChange={(e) => setAuth({ ...auth, email: e.target.value })} />
            </Field>
            <Field label="Password">
              <input type="password" required minLength={authMode === "register" ? 10 : 1}
                value={auth.password}
                onChange={(e) => setAuth({ ...auth, password: e.target.value })} />
            </Field>
            {authMode === "register" && <p className="hint full-width">Use at least 10 characters. Never reuse a password from another service.</p>}
            <button className="primary full-width" disabled={busy}>
              {busy ? "Please wait…" : authMode === "login" ? "Sign in" : "Create account"}
            </button>
          </form>
          <button className="link-button" onClick={() => setAuthMode(authMode === "login" ? "register" : "login")}>
            {authMode === "login" ? "New here? Create an account" : "Already registered? Sign in"}
          </button>
          <Notice message={message} kind={messageKind} />
        </Panel>
      </main>
    );
  }

  function renderPatient() {
    const active = consultations.find((c) => c.id === activeConsultation);
    return <>
      <div className="welcome-row">
        <div><p className="eyebrow">PATIENT WORKSPACE</p><h1>Hello, {profile?.display_name || "Patient"}</h1>
          <p>Complete one step at a time. Your records are private to you and authorised care providers.</p>
        </div>
        <span className="status-pill">Patient account</span>
      </div>
      <nav className="module-nav">
        {[
          ["symptoms", "1. Symptoms & triage"],
          ["emergency", "2. Emergency help"],
          ["consult", "3. Teleconsultation"],
          ["records", "4. My records"],
          ["requests", "5. Requests & referrals"],
        ].map(([key, label]) =>
          <button key={key} className={module === key ? "active" : ""}
            onClick={() => setModule(key)}>{label}</button>
        )}
      </nav>

      {module === "symptoms" && <Panel title="Patient symptom intake"
        description="Enter the current symptoms and relevant history. The warning screen is not a diagnosis.">
        <form onSubmit={submitSymptoms}>
          <div className="table-scroll">
            <table className="intake-table">
              <thead><tr><th>Information</th><th>Your response</th></tr></thead>
              <tbody>
                <tr><th><label htmlFor="symptoms">Symptoms</label></th><td>
                  <textarea id="symptoms" required rows="3" value={symptomForm.symptoms}
                    placeholder="Describe what you feel, where it hurts, and any other symptoms."
                    onChange={(e) => setSymptomForm({ ...symptomForm, symptoms: e.target.value })} />
                </td></tr>
                <tr><th>Severity</th><td>
                  <select value={symptomForm.severity}
                    onChange={(e) => setSymptomForm({ ...symptomForm, severity: e.target.value })}>
                    <option value="mild">Mild</option><option value="moderate">Moderate</option>
                    <option value="severe">Severe</option>
                  </select>
                </td></tr>
                <tr><th>Duration</th><td>
                  <input required value={symptomForm.duration} placeholder="e.g. 2 days"
                    onChange={(e) => setSymptomForm({ ...symptomForm, duration: e.target.value })} />
                </td></tr>
                <tr><th>Known allergies</th><td>
                  <textarea rows="2" value={symptomForm.allergies} placeholder="List allergies or enter None"
                    onChange={(e) => setSymptomForm({ ...symptomForm, allergies: e.target.value })} />
                </td></tr>
                <tr><th>Current medicines</th><td>
                  <textarea rows="2" value={symptomForm.current_medications} placeholder="Medicine name and dose, if known"
                    onChange={(e) => setSymptomForm({ ...symptomForm, current_medications: e.target.value })} />
                </td></tr>
                <tr><th>Preferred language</th><td>
                  <select value={symptomForm.language}
                    onChange={(e) => setSymptomForm({ ...symptomForm, language: e.target.value })}>
                    {LANGUAGES.map(([code, label]) => <option key={code} value={code}>{label}</option>)}
                  </select>
                </td></tr>
              </tbody>
            </table>
          </div>
          <label className="check-row">
            <input type="checkbox" checked={symptomForm.consent_to_ai_processing}
              onChange={(e) => setSymptomForm({ ...symptomForm, consent_to_ai_processing: e.target.checked })} />
            I consent to sending relevant information to the configured AI service for assistance.
          </label>
          <button className="primary" disabled={busy}>Check symptoms and continue</button>
        </form>
        {triage && <div className={`triage-result ${triage.signal.toLowerCase()}`}>
          <h3>{triage.signal} — {triage.urgency}</h3>
          <p>{triage.disclaimer}</p>
          <ul>{triage.guidance.map((tip, i) => <li key={i}>{tip}</li>)}</ul>
          {triage.signal === "RED" && <button className="danger" onClick={() => setModule("emergency")}>Open emergency help</button>}
        </div>}
        <Notice message={message} kind={messageKind} />
      </Panel>}

      {module === "emergency" && <Panel title="Emergency guidance and nearby hospitals"
        description="If you suspect an emergency, call 112 in India immediately. Do not wait for this page to load.">
        <div className="emergency-banner">
          <div><span className="eyebrow">INDIA EMERGENCY NUMBER</span><strong>112</strong>
            <p>Use your phone to call emergency services. Hospital listings may be incomplete or out of date.</p>
          </div>
          <a className="danger-button" href="tel:112">Call 112</a>
        </div>
        <h3>Immediate safety steps</h3>
        <ul className="tips-list">
          <li>Follow emergency dispatcher instructions and ask someone nearby to stay with the patient.</li>
          <li>Do not drive yourself if severely unwell.</li>
          <li>Do not give food, drink, medicines, or home remedies to an unconscious person.</li>
          <li>Ayurvedic or home remedies must never delay emergency assessment.</li>
        </ul>
        <button className="primary" onClick={findHospitals} disabled={busy}>Use GPS to find nearby hospitals</button>
        <div className="hospital-list">
          {hospitals.map((hospital, i) => <article className="hospital-card" key={`${hospital.name}-${i}`}>
            <div><h3>{hospital.name}</h3><p>{hospital.address}</p>
              <p><strong>Distance:</strong> {hospital.distance_km} km</p>
              <p><strong>Phone:</strong> {hospital.phone}</p>
              <p className="hint">{hospital.source}</p>
            </div>
            <a href={hospital.directions_url} target="_blank" rel="noreferrer">Directions</a>
          </article>)}
        </div>
        <Notice message={message} kind={messageKind} />
      </Panel>}

      {module === "consult" && <div className="module-stack">
        <Panel title="Step 1 — Select an approved doctor"
          description="A consultation request must be accepted before a clinician can access this consultation.">
          <div className="doctor-grid">
            {doctors.map((doctor) => <article className="doctor-card" key={doctor.doctor_id}>
              <div className="doctor-avatar">Dr</div><h3>{doctor.name}</h3>
              <p>{doctor.qualifications || "Qualifications not listed"}</p>
              <p>Language: {doctor.language}</p>
              <button className="primary" onClick={() => requestConsultation(doctor)} disabled={busy}>
                Request consultation
              </button>
            </article>)}
          </div>
          {!doctors.length && <p>No approved doctors are available in the directory yet.</p>}
        </Panel>

        <Panel title="Step 2 — Consultation room"
          description="The video element below is a local camera preview. Real remote video calls require a configured WebRTC provider.">
          <Field label="Select your consultation">
            <select value={activeConsultation} onChange={async (e) => {
              setActiveConsultation(e.target.value);
              setTranscripts([]);
              if (e.target.value) {
                try {
                  const result = await api(`/consultations/${e.target.value}/transcripts`, token);
                  setTranscripts(result.transcripts || []);
                } catch (error) { notify(error.message, "error"); }
              }
            }}>
              <option value="">Choose a consultation</option>
              {consultations.map((c) => <option key={c.id} value={c.id}>
                {c.id} — {c.status}
              </option>)}
            </select>
          </Field>
          {active && <p className="hint">Status: {active.status}. {active.status !== "accepted" &&
            "Wait for the doctor to accept before starting the consultation."}</p>}
          <div className="video-layout">
            <div className="video-box">
              <video ref={videoRef} autoPlay muted playsInline />
              {!cameraActive && <div className="video-placeholder"><span>Camera preview</span><small>Start camera to preview yourself</small></div>}
              <span className="video-label">Your device</span>
            </div>
            <div className="video-box doctor-video">
              <div className="video-placeholder"><span>Doctor video</span><small>Connect a WebRTC service for remote video</small></div>
              <span className="video-label">Doctor</span>
            </div>
          </div>
          <div className="control-row">
            {!cameraActive
              ? <button onClick={startCamera}>Start camera preview</button>
              : <button onClick={stopCamera}>Stop camera</button>}
            {recordingState === "idle" && <button className="primary" onClick={startRecording}>Start listening / record</button>}
            {recordingState === "recording" && <button onClick={pauseRecording}>Pause</button>}
            {recordingState === "paused" && <button onClick={resumeRecording}>Resume</button>}
            {recordingState !== "idle" && <button className="danger" onClick={stopRecording}>Stop & upload audio</button>}
            <span className="timer">{Math.floor(recordingSeconds / 60).toString().padStart(2, "0")}:{(recordingSeconds % 60).toString().padStart(2, "0")}</span>
          </div>
          <p className="hint">Recording is not streamed live to the backend; it is uploaded after you stop. Obtain consent from everyone before recording.</p>
          <div className="upload-grid">
            <Field label="Upload an audio file">
              <input type="file" accept="audio/*" onChange={(e) => uploadAudioFile(e.target.files?.[0])} />
            </Field>
            <Field label="Upload an image from your computer">
              <input type="file" accept="image/png,image/jpeg,image/webp"
                onChange={(e) => uploadImageFile(e.target.files?.[0])} />
            </Field>
          </div>
          {uploadedImage && <Notice message={`Image uploaded: ${uploadedImage.name}. ${uploadedImage.note}`} />}
          <div className="control-row">
            <button onClick={saveTypedTranscript}>Add/paste transcript</button>
            <button onClick={() => loadTranscripts()} disabled={!activeConsultation}>Refresh transcript</button>
          </div>
          <h3>Conversation transcript</h3>
          {transcripts.length === 0 && <p className="hint">Uploaded transcripts will appear here in the original language and English, when translation is available.</p>}
          {transcripts.map((item, index) => <article className="transcript-card" key={item.id || index}>
            <h4>Original ({item.source_language || "language not detected"})</h4>
            <p>{item.original_text || item.transcript || "No original transcript returned."}</p>
            <h4>English for clinician</h4>
            <p>{item.english_text || item.original_text || "English transcript unavailable."}</p>
          </article>)}
          <Notice message={message} kind={messageKind} />
        </Panel>
      </div>}

      {module === "records" && <div className="module-stack">
        <Panel title="Approved digital prescriptions" description="Only doctor-finalised prescriptions appear here.">
          {prescriptions.map((item) => <article className="record-card" key={item.id}>
            <h3>{item.hospital_name || "Healthcare provider"}</h3>
            <p><strong>Diagnosis:</strong> {item.diagnosis || "Not stated"}</p>
            <p><strong>Symptoms:</strong> {item.symptoms || "Not stated"}</p>
            <h4>Medicines</h4>
            <ul>{(item.medicines || []).map((m, i) => <li key={i}>
              {m.name} — {m.dosage}; {m.frequency}; {m.duration}. {m.instructions}
            </li>)}</ul>
            <p><strong>Guidance:</strong> {item.advice}</p>
            <p><strong>Follow-up:</strong> {item.follow_up}</p>
            <p className="signature">Doctor: {item.doctor_signature} · Digitally recorded review</p>
          </article>)}
          {!prescriptions.length && <p>No final prescriptions are available yet.</p>}
        </Panel>
        <Panel title="Medical records">
          {medicalRecords.map((item) => <article className="record-card" key={item.id}>
            <p>{item.note || item.text || item.title || "Medical record"}</p>
            <small>{item.created_at || ""}</small>
          </article>)}
          {!medicalRecords.length && <p>No additional medical records are available.</p>}
        </Panel>
      </div>}

      {module === "requests" && <div className="module-stack">
        <Panel title="My consultation requests">
          {consultations.map((item) => <article className="record-card" key={item.id}>
            <p><strong>Consultation ID:</strong> {item.id}</p>
            <p><strong>Status:</strong> <span className="status-pill">{item.status}</span></p>
            <p>{item.reason}</p>
            <button onClick={() => { setActiveConsultation(item.id); setModule("consult"); }}>Open consultation</button>
          </article>)}
          {!consultations.length && <p>No consultation requests yet.</p>}
        </Panel>
        <Panel title="Specialist referrals and follow-up">
          {referrals.map((item) => <article className="record-card" key={item.id}>
            <h3>{item.specialist}</h3><p>{item.reason}</p>
            <p><strong>Facility:</strong> {item.facility || "Not specified"}</p>
            <p><strong>Follow-up:</strong> {item.follow_up_date || "Not scheduled"}</p>
            <p><strong>Status:</strong> {item.status}</p>
            <p>{item.notes}</p>
          </article>)}
          {!referrals.length && <p>No referrals are available.</p>}
        </Panel>
      </div>}
    </>;
  }

  function renderDoctor() {
    const requested = consultations.filter((c) => c.status === "requested");
    const active = consultations.filter((c) => c.status === "accepted");
    return <>
      <div className="welcome-row">
        <div><p className="eyebrow">CLINICIAN WORKSPACE</p><h1>Doctor dashboard</h1>
          <p>Review patient-submitted information and approve all final clinical decisions.</p>
        </div>
        <span className="status-pill">Doctor account</span>
      </div>
      <nav className="module-nav">
        {[["queue", "1. Request queue"], ["consult", "2. Consultation"], ["prescription", "3. Prescription"], ["referral", "4. Referral"]].map(([key, label]) =>
          <button key={key} className={module === key ? "active" : ""} onClick={() => setModule(key)}>{label}</button>
        )}
      </nav>

      {module === "queue" && <Panel title="Incoming consultation requests">
        <button onClick={() => run(loadDoctorData)}>Refresh requests</button>
        {requested.map((c) => <article className="record-card" key={c.id}>
          <h3>Patient consultation</h3><p>{c.reason}</p>
          <p><strong>Created:</strong> {c.created_at || "—"}</p>
          <button className="primary" onClick={() => acceptConsultation(c)}>Accept consultation</button>
        </article>)}
        {!requested.length && <p>No pending requests.</p>}
        <h3>Accepted consultations</h3>
        {active.map((c) => <article className="record-card" key={c.id}>
          <p><strong>Consultation:</strong> {c.id}</p>
          <button onClick={() => { setActiveConsultation(c.id); setModule("consult"); }}>Open consultation</button>
        </article>)}
      </Panel>}

      {module === "consult" && <Panel title="Consultation and clinical assistance"
        description="The AI can organise information and suggest questions. It must not diagnose or prescribe autonomously.">
        <Field label="Accepted consultation">
          <select value={activeConsultation} onChange={(e) => setActiveConsultation(e.target.value)}>
            <option value="">Choose consultation</option>
            {active.map((c) => <option key={c.id} value={c.id}>{c.id} — {c.reason}</option>)}
          </select>
        </Field>
        <button className="primary" onClick={requestClinicalAssist}>Extract symptoms and suggest questions</button>
        {clinicalAssist && <pre className="ai-output">{clinicalAssist}</pre>}
        <p className="hint">Verify allergies, duration, medicines, negations, and all extracted information directly with the patient.</p>
        <button onClick={() => loadTranscripts()}>Load saved transcripts</button>
        {transcripts.map((item, i) => <article className="transcript-card" key={item.id || i}>
          <h4>Original transcript</h4><p>{item.original_text}</p>
          <h4>English transcript</h4><p>{item.english_text}</p>
        </article>)}
      </Panel>}

      {module === "prescription" && <Panel title="Editable digital prescription"
        description="Save a draft, verify every field, then finalise only after your clinical review.">
        <form onSubmit={(e) => { e.preventDefault(); savePrescriptionDraft(); }}>
          <div className="form-grid">
            <Field label="Hospital / clinic name"><input value={prescription.hospital_name}
              onChange={(e) => setPrescription({ ...prescription, hospital_name: e.target.value })} /></Field>
            <Field label="Diagnosis / clinical impression"><textarea value={prescription.diagnosis}
              onChange={(e) => setPrescription({ ...prescription, diagnosis: e.target.value })} /></Field>
            <Field label="Symptoms assessed"><textarea value={prescription.symptoms}
              onChange={(e) => setPrescription({ ...prescription, symptoms: e.target.value })} /></Field>
          </div>
          <h3>Medicine entries — doctor authored</h3>
          {prescription.medicines.map((medicine, index) => <div className="medicine-row" key={index}>
            {["name", "dosage", "frequency", "duration", "instructions"].map((key) =>
              <Field key={key} label={key[0].toUpperCase() + key.slice(1)}>
                <input value={medicine[key]} onChange={(e) => updateMedicine(index, key, e.target.value)} />
              </Field>
            )}
            {prescription.medicines.length > 1 && <button type="button" onClick={() =>
              setPrescription((old) => ({ ...old, medicines: old.medicines.filter((_, i) => i !== index) }))}>Remove</button>}
          </div>)}
          <button type="button" onClick={() => setPrescription((old) => ({
            ...old, medicines: [...old.medicines, { name: "", dosage: "", frequency: "", duration: "", instructions: "" }],
          }))}>Add medicine row</button>
          <div className="form-grid">
            <Field label="Advice and care guidelines"><textarea value={prescription.advice}
              onChange={(e) => setPrescription({ ...prescription, advice: e.target.value })} /></Field>
            <Field label="Follow-up plan"><textarea value={prescription.follow_up}
              onChange={(e) => setPrescription({ ...prescription, follow_up: e.target.value })} /></Field>
            <Field label="Doctor signature / authorised name"><input required value={prescription.doctor_signature}
              onChange={(e) => setPrescription({ ...prescription, doctor_signature: e.target.value })} /></Field>
          </div>
          <div className="control-row">
            <button className="primary" type="submit">Save draft</button>
            <button type="button" onClick={finalisePrescription}>Approve and finalise</button>
          </div>
          {prescriptionId && <p className="hint">Draft ID: {prescriptionId}. Finalise only after checking the details.</p>}
        </form>
      </Panel>}

      {module === "referral" && <Panel title="Specialist referral and follow-up">
        <form className="form-grid" onSubmit={createReferral}>
          <Field label="Patient ID"><input required value={referralForm.patient_id}
            onChange={(e) => setReferralForm({ ...referralForm, patient_id: e.target.value })} /></Field>
          <Field label="Specialist"><input required value={referralForm.specialist}
            onChange={(e) => setReferralForm({ ...referralForm, specialist: e.target.value })} /></Field>
          <Field label="Reason for referral"><textarea required value={referralForm.reason}
            onChange={(e) => setReferralForm({ ...referralForm, reason: e.target.value })} /></Field>
          <Field label="Facility"><input value={referralForm.facility}
            onChange={(e) => setReferralForm({ ...referralForm, facility: e.target.value })} /></Field>
          <Field label="Follow-up date"><input type="date" value={referralForm.follow_up_date}
            onChange={(e) => setReferralForm({ ...referralForm, follow_up_date: e.target.value })} /></Field>
          <button className="primary">Create referral</button>
        </form>
      </Panel>}
      <Notice message={message} kind={messageKind} />
    </>;
  }

  function renderAdmin() {
    return <>
      <div className="welcome-row"><div><p className="eyebrow">ADMINISTRATION</p>
        <h1>Platform overview</h1><p>Operational counts are not a substitute for clinical outcome auditing.</p></div></div>
      <Panel title="Service statistics">
        <button className="primary" onClick={() => run(loadAdminData)}>Refresh statistics</button>
        {adminStats && <div className="stats-grid">
          {[
            ["Registered patients", adminStats.registered_patients],
            ["Screenings today", adminStats.patient_screenings_today],
            ["Total screenings", adminStats.total_screenings],
            ["Consultations today", adminStats.consultations_today],
            ["Total consultations", adminStats.total_consultations],
            ["Completed referrals", adminStats.completed_referrals],
            ["Open referrals", adminStats.open_referrals],
          ].map(([label, value]) => <article className="stat-card" key={label}>
            <span>{label}</span><strong>{value ?? 0}</strong>
          </article>)}
        </div>}
        <p className="hint">{adminStats?.note || "Statistics are loaded from backend records."}</p>
      </Panel>
    </>;
  }

  if (!token) return renderAuth();

  return <div className="app-shell">
    <header className="topbar">
      <div className="brand-lockup"><div className="brand-mark small">SS</div>
        <div><strong>SwasthyaSetu</strong><span>Healthcare coordination</span></div>
      </div>
      <div className="topbar-actions">
        <span className="role-chip">{profile?.role || "Loading"}</span>
        <button onClick={() => {
          if (profile?.role === "patient") run(loadPatientData);
          else if (profile?.role === "doctor") run(loadDoctorData);
          else if (profile?.role === "admin") run(loadAdminData);
        }}>Refresh</button>
        <button className="outline" onClick={logout}>Sign out</button>
      </div>
    </header>
    <main className="dashboard-main">
      <Notice message={message} kind={messageKind} />
      {profile?.role === "patient" && renderPatient()}
      {profile?.role === "doctor" && renderDoctor()}
      {profile?.role === "admin" && renderAdmin()}
      {profile && !["patient", "doctor", "admin"].includes(profile.role) &&
        <Panel title="Role not supported"><p>Please contact the administrator.</p></Panel>}
      <footer className="app-footer">
        <strong>SwasthyaSetu</strong>
        <span>Prototype only · AI output is not a diagnosis · Emergency care must not be delayed</span>
      </footer>
    </main>
    {busy && <div className="busy-indicator"><span className="spinner" /> Working…</div>}
  </div>;
}

export default App;

const rootElement = document.getElementById("root");

if (rootElement) {
  createRoot(rootElement).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>
  );
}