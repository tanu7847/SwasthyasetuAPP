package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.ai.FirebaseAiService
import com.example.data.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

private const val TAG = "HealthcareRepository"

class HealthcareRepository(
    private val context: Context? = null,
    private val aiService: FirebaseAiService = FirebaseAiService()
) {
    // Firestore initialized with mandatory named database ID from firebase_applet_config.xml
    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (context != null) {
                val dbId = context.getString(R.string.firestore_database_id)
                FirebaseFirestore.getInstance(dbId)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize named Firestore database", e)
            null
        }
    }

    private val auth by lazy {
        try {
            Firebase.auth
        } catch (e: Exception) {
            null
        }
    }
    private val repoScope = CoroutineScope(Dispatchers.IO)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _symptomRecords = MutableStateFlow<List<SymptomIntake>>(emptyList())
    val symptomRecords: StateFlow<List<SymptomIntake>> = _symptomRecords.asStateFlow()

    private val _consultations = MutableStateFlow<List<Consultation>>(emptyList())
    val consultations: StateFlow<List<Consultation>> = _consultations.asStateFlow()

    private val _transcripts = MutableStateFlow<List<AudioTranscription>>(emptyList())
    val transcripts: StateFlow<List<AudioTranscription>> = _transcripts.asStateFlow()

    private val _prescriptions = MutableStateFlow<List<DigitalPrescription>>(emptyList())
    val prescriptions: StateFlow<List<DigitalPrescription>> = _prescriptions.asStateFlow()

    private val _referrals = MutableStateFlow<List<SpecialistReferral>>(emptyList())
    val referrals: StateFlow<List<SpecialistReferral>> = _referrals.asStateFlow()

    private val _infectionDetection = MutableStateFlow<InfectionDetection?>(null)
    val infectionDetection: StateFlow<InfectionDetection?> = _infectionDetection.asStateFlow()

    private val _extractedEntities = MutableStateFlow<ExtractedMedicalEntities?>(null)
    val extractedEntities: StateFlow<ExtractedMedicalEntities?> = _extractedEntities.asStateFlow()

    val availableDoctors = listOf(
        Doctor(
            id = "doc_101",
            name = "Dr. Aakash Verma",
            specialty = "General Physician & Critical Care",
            qualifications = "MBBS, MD (Medicine), FCCP",
            experienceYears = 14,
            language = "Hindi, English, Marathi",
            isOnline = true
        ),
        Doctor(
            id = "doc_102",
            name = "Dr. Priya Deshmukh",
            specialty = "Pulmonology & Respiratory Medicine",
            qualifications = "MBBS, DNB (Pulmonary Medicine)",
            experienceYears = 11,
            language = "Marathi, Hindi, English",
            isOnline = true
        ),
        Doctor(
            id = "doc_103",
            name = "Dr. Rajesh Iyer",
            specialty = "Emergency Medicine & Cardiology",
            qualifications = "MBBS, MD (Emergency Medicine), Dip. Card",
            experienceYears = 16,
            language = "Tamil, English, Hindi",
            isOnline = true
        )
    )

    val baseHospitals = listOf(
        NearbyHospital(
            name = "Apollo Multispeciality Hospital & Trauma Care",
            phone = "+91 20 6688 2200",
            helpline = "1066",
            address = "Plot 21, Off Pune-Ahmednagar Highway, Viman Nagar",
            distanceKm = 1.8,
            latitude = 18.5679,
            longitude = 73.9143,
            emergencyAvailable = true,
            icuBedsAvailable = 14
        ),
        NearbyHospital(
            name = "Ruby Hall Clinic & Critical Care Center",
            phone = "+91 20 6645 5100",
            helpline = "1057",
            address = "40 Sassoon Road, Sangamvadi, Central Medical District",
            distanceKm = 3.4,
            latitude = 18.5284,
            longitude = 73.8744,
            emergencyAvailable = true,
            icuBedsAvailable = 8
        ),
        NearbyHospital(
            name = "AIIMS Super Speciality & Emergency Wing",
            phone = "+91 20 2612 8000",
            helpline = "112",
            address = "Sector 14, Health Boulevard, Institutional Area",
            distanceKm = 5.2,
            latitude = 18.5412,
            longitude = 73.8421,
            emergencyAvailable = true,
            icuBedsAvailable = 22
        ),
        NearbyHospital(
            name = "Sassoon General Hospital & Trauma Centre (Govt)",
            phone = "+91 20 2612 8000",
            helpline = "108",
            address = "Near Pune Railway Station, Agarkar Nagar",
            distanceKm = 4.1,
            latitude = 18.5292,
            longitude = 73.8732,
            emergencyAvailable = true,
            icuBedsAvailable = 19
        ),
        NearbyHospital(
            name = "Fortis Escorts Heart & Multispeciality Care",
            phone = "+91 20 4915 3300",
            helpline = "105010",
            address = "Bypass Road, Kalyani Nagar",
            distanceKm = 6.7,
            latitude = 18.5488,
            longitude = 73.9022,
            emergencyAvailable = true,
            icuBedsAvailable = 11
        )
    )

    private val redFlags = listOf(
        "chest pain", "difficulty breathing", "cannot breathe", "severe bleeding",
        "unconscious", "not responding", "face drooping", "stroke", "seizure",
        "blue lips", "overdose", "suicidal", "anaphylaxis", "severe allergic reaction",
        "sudden severe headache", "heart attack", "crushing pain", "loss of speech"
    )

    private val yellowFlags = listOf(
        "high fever", "persistent vomiting", "dehydration", "worsening pain",
        "blood in stool", "fainting", "dizziness", "moderate bleeding", "rash spreading"
    )

    init {
        // Sync with Firebase Auth user if signed in
        val firebaseUser = try { auth?.currentUser } catch (e: Exception) { null }
        if (firebaseUser != null) {
            _currentUser.value = User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "Google User",
                email = firebaseUser.email ?: "",
                role = UserRole.PATIENT,
                phone = firebaseUser.phoneNumber ?: "+91 98220 12345",
                language = "en",
                age = 36,
                gender = "Not specified"
            )
            loadUserDataFromFirestore(firebaseUser.uid)
        } else {
            seedInitialData()
        }
    }

    private fun seedInitialData() {
        val defaultPatient = User(
            id = "pat_881",
            name = "Ramesh Kulkarni",
            email = "ramesh@example.com",
            role = UserRole.PATIENT,
            phone = "+91 98220 12345",
            language = "mr",
            age = 46,
            gender = "Male"
        )
        _currentUser.value = defaultPatient

        val samplePrescription = DigitalPrescription(
            id = "rx_prev_01",
            consultationId = "cons_prev_01",
            patientId = defaultPatient.id,
            patientName = defaultPatient.name,
            patientAge = 46,
            patientGender = "Male",
            doctorId = "doc_101",
            doctorName = "Dr. Aakash Verma",
            hospitalName = "SwasthyaSetu Telemedicine & Rural Healthcare Center",
            diagnosis = "Acute Upper Respiratory Tract Infection (URTI) with bronchospasm",
            symptoms = "Persistent dry cough, mild fever, throat irritation",
            medicines = listOf(
                Medicine("Tab. Azithromycin 500mg", "1 tablet", "Once daily (OD)", "3 days", "After lunch"),
                Medicine("Tab. Paracetamol 650mg", "1 tablet", "SOS (as needed for fever)", "5 days", "After meals, max 3 times/day"),
                Medicine("Syrup Levocetirizine + Ambroxol", "10 ml", "Twice daily (BD)", "5 days", "At morning and bedtime")
            ),
            guidelines = "Steam inhalation twice daily. Drink warm water with pinch of turmeric. Avoid cold beverages and air-conditioned drafts.",
            followUp = "Review after 5 days if fever persists over 100°F.",
            doctorSignature = "Dr. Aakash Verma, MD (Med) [MMC-Reg: 2018/04/1829]",
            hospitalStamp = "SWASHTHYA SETU - VERIFIED TELEHEALTH STAMP",
            isFinalApproved = true
        )
        _prescriptions.value = listOf(samplePrescription)

        val sampleReferral = SpecialistReferral(
            id = "ref_101",
            patientId = defaultPatient.id,
            patientName = defaultPatient.name,
            doctorId = "doc_101",
            doctorName = "Dr. Aakash Verma",
            specialist = "Pulmonologist / Chest Specialist",
            reason = "Persistent wheezing and nocturnal dyspnea requiring Spirometry",
            facility = "Ruby Hall Clinic Pulmonology Wing",
            followUpDate = "2026-11-05",
            status = "in_treatment",
            treatmentNotes = "Patient attended initial spirometry; FEV1/FVC ratio stable. Maintenance inhaler initiated."
        )
        _referrals.value = listOf(sampleReferral)
    }

    private fun loadUserDataFromFirestore(userId: String) {
        val db = firestore ?: return
        db.collection("users").document(userId).collection("symptoms")
            .get()
            .addOnSuccessListener { query ->
                val list = query.documents.mapNotNull { doc ->
                    try {
                        SymptomIntake(
                            id = doc.id,
                            patientId = doc.getString("userId") ?: userId,
                            symptoms = doc.getString("symptoms") ?: "",
                            severity = doc.getString("severity") ?: "moderate",
                            duration = doc.getString("duration") ?: "",
                            allergies = doc.getString("allergies") ?: "",
                            currentMedications = doc.getString("currentMedications") ?: "",
                            language = doc.getString("language") ?: "en",
                            signal = TriageSignal.valueOf(doc.getString("signal") ?: "GREEN"),
                            urgencyText = doc.getString("urgencyText") ?: "",
                            warningFlags = (doc.get("warningFlags") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                            guidanceTips = (doc.get("guidanceTips") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                            ayurvedicHomeCareTips = (doc.get("ayurvedicHomeCareTips") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                if (list.isNotEmpty()) {
                    _symptomRecords.value = list
                }
            }
            .addOnFailureListener {
                Log.w(TAG, "Failed to load symptoms from Firestore", it)
            }
    }

    fun onAuthUserChanged(firebaseUser: com.google.firebase.auth.FirebaseUser?) {
        if (firebaseUser != null) {
            val user = User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "Google User",
                email = firebaseUser.email ?: "",
                role = UserRole.PATIENT,
                phone = firebaseUser.phoneNumber ?: "+91 98220 12345",
                language = "en",
                age = 36,
                gender = "Not specified"
            )
            _currentUser.value = user

            // Persist user to Firestore
            firestore?.collection("users")?.document(firebaseUser.uid)?.set(
                mapOf(
                    "userId" to firebaseUser.uid,
                    "name" to user.name,
                    "email" to user.email,
                    "role" to user.role.name,
                    "phone" to user.phone,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            loadUserDataFromFirestore(firebaseUser.uid)
        } else {
            _currentUser.value = null
        }
    }

    fun login(email: String, role: UserRole, displayName: String): User {
        val user = when (role) {
            UserRole.PATIENT -> User(
                id = (try { auth?.currentUser?.uid } catch (e: Exception) { null }) ?: ("pat_" + UUID.randomUUID().toString().take(6)),
                name = displayName.ifBlank { "Patient Citizen" },
                email = email,
                role = UserRole.PATIENT,
                phone = "+91 98220 99887",
                language = "hi",
                age = 38,
                gender = "Male"
            )
            UserRole.DOCTOR -> User(
                id = "doc_101",
                name = displayName.ifBlank { "Dr. Aakash Verma" },
                email = email,
                role = UserRole.DOCTOR,
                phone = "+91 94220 54321",
                language = "en",
                qualifications = "MBBS, MD (Medicine)",
                registrationNumber = "MMC-2018/04/1829"
            )
            UserRole.ADMIN -> User(
                id = "adm_001",
                name = displayName.ifBlank { "Admin Officer" },
                email = email,
                role = UserRole.ADMIN,
                phone = "+91 20 2612 0000",
                language = "en"
            )
        }
        _currentUser.value = user
        return user
    }

    fun registerDoctor(
        name: String,
        email: String,
        phone: String,
        registrationNumber: String,
        qualifications: String,
        specialty: String,
        certifications: String,
        hospitalAffiliation: String,
        experienceYears: Int,
        language: String
    ): User {
        val uid = (try { auth?.currentUser?.uid } catch (e: Exception) { null }) ?: ("doc_" + UUID.randomUUID().toString().take(6))
        val doctorUser = User(
            id = uid,
            name = if (name.startsWith("Dr.", ignoreCase = true)) name else "Dr. $name",
            email = email,
            role = UserRole.DOCTOR,
            phone = phone,
            language = language,
            registrationNumber = registrationNumber,
            qualifications = qualifications,
            specialty = specialty,
            certifications = certifications,
            hospitalAffiliation = hospitalAffiliation,
            experienceYears = experienceYears,
            isVerifiedDoctor = true
        )
        _currentUser.value = doctorUser

        firestore?.collection("users")?.document(uid)?.set(
            mapOf(
                "userId" to uid,
                "name" to doctorUser.name,
                "email" to doctorUser.email,
                "role" to "DOCTOR",
                "phone" to doctorUser.phone,
                "registrationNumber" to doctorUser.registrationNumber,
                "qualifications" to doctorUser.qualifications,
                "specialty" to doctorUser.specialty,
                "certifications" to doctorUser.certifications,
                "hospitalAffiliation" to doctorUser.hospitalAffiliation,
                "experienceYears" to doctorUser.experienceYears,
                "isVerifiedDoctor" to true,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        return doctorUser
    }

    fun registerPatient(
        name: String,
        email: String,
        phone: String,
        age: Int,
        gender: String,
        language: String
    ): User {
        val uid = (try { auth?.currentUser?.uid } catch (e: Exception) { null }) ?: ("pat_" + UUID.randomUUID().toString().take(6))
        val patientUser = User(
            id = uid,
            name = name,
            email = email,
            role = UserRole.PATIENT,
            phone = phone,
            language = language,
            age = age,
            gender = gender
        )
        _currentUser.value = patientUser

        firestore?.collection("users")?.document(uid)?.set(
            mapOf(
                "userId" to uid,
                "name" to patientUser.name,
                "email" to patientUser.email,
                "role" to "PATIENT",
                "phone" to patientUser.phone,
                "age" to (patientUser.age ?: 30),
                "gender" to patientUser.gender,
                "language" to patientUser.language,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        return patientUser
    }

    fun logout() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Auth sign out failed", e)
        }
        _currentUser.value = null
    }

    // --- Module 1: Symptom Intake & Triage Management ---
    fun processSymptomIntake(
        symptoms: String,
        severity: String,
        duration: String,
        allergies: String,
        currentMedications: String,
        language: String,
        latitude: Double? = 18.5204,
        longitude: Double? = 73.8567
    ): SymptomIntake {
        val lowerText = symptoms.lowercase()
        val detectedRed = redFlags.filter { lowerText.contains(it) }
        val detectedYellow = yellowFlags.filter { lowerText.contains(it) }

        val signal = when {
            detectedRed.isNotEmpty() -> TriageSignal.RED
            detectedYellow.isNotEmpty() || severity.equals("severe", ignoreCase = true) -> TriageSignal.YELLOW
            else -> TriageSignal.GREEN
        }

        val urgencyText = when (signal) {
            TriageSignal.RED -> "CRITICAL EMERGENCY DETECTED: Immediate Hospitalization Required!"
            TriageSignal.YELLOW -> "URGENT MEDICAL ATTENTION: Prompt Clinical Evaluation Recommended"
            TriageSignal.GREEN -> "ROUTINE/MILD CASE: Proceed to Teleconsultation with Doctor"
        }

        val guidanceTips = when (signal) {
            TriageSignal.RED -> listOf(
                "Immediately call 112 or national ambulance 108. Do not wait for teleconsultation.",
                "Keep the patient calm, seated upright or in recovery position. Loosen any tight neckwear or belt.",
                "Do NOT attempt to drive alone. Have an emergency paramedic or companion accompany you.",
                "Do not give any solid food or heavy oral liquids if patient is drowsy or choking.",
                "If breathing ceases or cardiac arrest is suspected, begin hands-only CPR (100-120 chest compressions/min) immediately."
            )
            TriageSignal.YELLOW -> listOf(
                "Arrange teleconsultation with doctor within the next 2-4 hours.",
                "Monitor temperature, pulse, and oxygen saturation (SpO2) every hour.",
                "If symptoms progress to chest heaviness or confusion, immediately escalate to emergency hospital.",
                "Maintain hydration with small, frequent sips of lukewarm water or electrolyte fluids."
            )
            TriageSignal.GREEN -> listOf(
                "Rest adequately and note down any progression of symptoms.",
                "Avoid unnecessary self-medication with strong over-the-counter painkillers or unverified antibiotics.",
                "Join the teleconsultation room to discuss treatment with the attending physician."
            )
        }

        val ayurvedicHomeCareTips = when (signal) {
            TriageSignal.RED -> listOf(
                "First-Aid Stabilization: Position in high Fowler's posture (sit at 90 degrees) to maximize lung volume.",
                "In severe bleeding: Apply firm continuous pressure with clean cotton cloth directly over wound; elevate above heart level.",
                "In fainting/shock: Raise legs by 12 inches to encourage cerebral blood flow; keep warm with light blanket.",
                "Ayurvedic emergency principle: Do not administer any oral Kashaya/churna during acute crisis. Prioritize hospital transport."
            )
            TriageSignal.YELLOW -> listOf(
                "Tulsi-Shunthi Kwath (Decoction): Boil 5 crushed Tulsi leaves + 1/2 tsp dry ginger in 200ml water until halved. Sip warm.",
                "For high fever: Wet cloth sponging on forehead with room-temperature water. Avoid chilled water.",
                "Shadanga Paniya: Boil water with musta and ushira roots for soothing hydration and body heat regulation.",
                "Sitopaladi Churna: 1/2 tsp with honey twice daily for soothing bronchial spasms and throat scratchiness."
            )
            TriageSignal.GREEN -> listOf(
                "Golden Milk (Haridra Khanda): Warm cow milk or almond milk with 1/4 tsp pure turmeric and black pepper before bed.",
                "Nasya: Apply 2 drops of lukewarm sesame oil or Anu Taila in each nostril in the morning to protect nasal mucosa.",
                "Salt water gargling (Kavala): Warm water with rock salt (Saindhava lavana) and turmeric twice a day.",
                "Light diet (Laghu Ahara): Moong dal khichdi seasoned with cumin and ghee to maintain Agni (digestive fire)."
            )
        }

        val uid = _currentUser.value?.id ?: "pat_guest"
        val intake = SymptomIntake(
            id = "symp_" + UUID.randomUUID().toString().take(6),
            patientId = uid,
            symptoms = symptoms,
            severity = severity,
            duration = duration,
            allergies = allergies.ifBlank { "None reported" },
            currentMedications = currentMedications.ifBlank { "None" },
            language = language,
            latitude = latitude,
            longitude = longitude,
            signal = signal,
            urgencyText = urgencyText,
            warningFlags = detectedRed + detectedYellow,
            guidanceTips = guidanceTips,
            ayurvedicHomeCareTips = ayurvedicHomeCareTips
        )

        _symptomRecords.update { listOf(intake) + it }

        // Persist to Firestore under users/{userId}/symptoms
        firestore?.let { db ->
            db.collection("users").document(uid).collection("symptoms").document(intake.id).set(
                mapOf(
                    "userId" to uid,
                    "symptoms" to intake.symptoms,
                    "severity" to intake.severity,
                    "duration" to intake.duration,
                    "allergies" to intake.allergies,
                    "currentMedications" to intake.currentMedications,
                    "language" to intake.language,
                    "signal" to intake.signal.name,
                    "urgencyText" to intake.urgencyText,
                    "warningFlags" to intake.warningFlags,
                    "guidanceTips" to intake.guidanceTips,
                    "ayurvedicHomeCareTips" to intake.ayurvedicHomeCareTips,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).addOnFailureListener {
                Log.w(TAG, "Failed to persist symptom intake to Firestore", it)
            }
        }

        return intake
    }

    fun getNearbyHospitals(userLat: Double, userLon: Double): List<NearbyHospital> {
        return baseHospitals.map { hosp ->
            val dist = calculateHaversineKm(userLat, userLon, hosp.latitude, hosp.longitude)
            hosp.copy(distanceKm = round(dist * 10.0) / 10.0)
        }.sortedBy { it.distanceKm }
    }

    private fun calculateHaversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun requestConsultation(doctor: Doctor, reason: String, language: String): Consultation {
        val pat = _currentUser.value
        val consultation = Consultation(
            id = "cons_" + UUID.randomUUID().toString().take(6),
            patientId = pat?.id ?: "pat_guest",
            patientName = pat?.name ?: "Patient",
            doctorId = doctor.id,
            doctorName = doctor.name,
            reason = reason,
            language = language,
            status = "accepted",
            roomId = "room_swasthya_" + UUID.randomUUID().toString().take(8)
        )
        _consultations.update { listOf(consultation) + it }

        // Persist to Firestore
        firestore?.let { db ->
            val uid = pat?.id ?: "pat_guest"
            db.collection("users").document(uid).collection("consultations").document(consultation.id).set(
                mapOf(
                    "userId" to uid,
                    "patientName" to consultation.patientName,
                    "doctorId" to consultation.doctorId,
                    "doctorName" to consultation.doctorName,
                    "reason" to consultation.reason,
                    "language" to consultation.language,
                    "status" to consultation.status,
                    "roomId" to consultation.roomId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }

        return consultation
    }

    // --- Audio Transcription using gemini-3.5-transcribe model ---
    suspend fun transcribeAudioWithGemini(consultationId: String, patientLanguage: String): AudioTranscription {
        val transcription = aiService.transcribeAudio(
            consultationId = consultationId,
            audioDescription = "Patient describing high fever, throat irritation, dry cough and known penicillin allergy",
            patientLanguage = patientLanguage
        )
        _transcripts.update { listOf(transcription) + it }

        // Trigger NLP Clinical extraction via gemini-3.8-flash
        val entities = aiService.extractClinicalEntities(transcription.englishText)
        _extractedEntities.value = entities

        return transcription
    }

    // Synchronous simulation wrapper for UI actions
    fun simulateAudioUploadAndTranscription(consultationId: String, patientLanguage: String): AudioTranscription {
        repoScope.launch {
            try {
                transcribeAudioWithGemini(consultationId, patientLanguage)
            } catch (e: Exception) {
                Log.e(TAG, "Background audio transcription error", e)
            }
        }

        // Return immediate optimistic result
        val sample = AudioTranscription(
            id = "tx_" + UUID.randomUUID().toString().take(5),
            consultationId = consultationId,
            originalText = when (patientLanguage) {
                "hi" -> "डॉक्टर साहब, मुझे 3 दिनों से तेज़ बुखार, गले में खराश और सूखी खांसी हो रही है। मुझे पेनिसिलिन से एलर्जी है।"
                "mr" -> "नमस्कार डॉक्टर, मला गेल्या ३ दिवसांपासून तीव्र ताप, घशात खवखव आणि सुका खोकला आहे. मला पेनिसिलिनची ॲलर्जी आहे."
                else -> "Doctor, I have had high fever, dry cough, and sore throat for 3 days. I also have an allergy to Penicillin."
            },
            englishText = "Doctor, I have had high fever, dry cough, and sore throat for 3 days. I also have an allergy to Penicillin.",
            sourceLanguage = patientLanguage
        )
        _transcripts.update { listOf(sample) + it }
        return sample
    }

    // --- Image Infection Analyzer using Gemini Vision + OpenCV ---
    suspend fun analyzeImageWithGemini(): InfectionDetection {
        val detection = aiService.analyzeBodyImage()
        _infectionDetection.value = detection
        return detection
    }

    fun analyzeImageForInfection(imageSourceDescription: String = "Patient Body Scan"): InfectionDetection {
        repoScope.launch {
            try {
                analyzeImageWithGemini()
            } catch (e: Exception) {
                Log.e(TAG, "Gemini image analysis error", e)
            }
        }
        val detection = InfectionDetection(
            hasInfectedPart = true,
            affectedAreaName = "Erythematous Inflammatory Lesion (Forearm / Torso)",
            confidencePercent = 95,
            detectionModel = "Gemini 3.8 Multimodal + OpenCV HSV Contour",
            boxNormX = 0.22f,
            boxNormY = 0.28f,
            boxNormWidth = 0.54f,
            boxNormHeight = 0.44f,
            clinicalFindings = "Marked circumscribed macular erythema with central follicular pustulation. Margins show mild edema without necrotic eschar.",
            recommendations = "Cleanse with chlorhexidine wash. Avoid topical steroids until bacterial infection is ruled out. Apply prescribed mupirocin 2% ointment."
        )
        _infectionDetection.value = detection
        return detection
    }

    fun generateInitialPrescription(
        consultationId: String,
        patient: User,
        doctor: Doctor
    ): DigitalPrescription {
        val prescription = DigitalPrescription(
            id = "rx_" + UUID.randomUUID().toString().take(6),
            consultationId = consultationId,
            patientId = patient.id,
            patientName = patient.name,
            patientAge = patient.age ?: 35,
            patientGender = patient.gender.ifBlank { "Male" },
            doctorId = doctor.id,
            doctorName = doctor.name,
            hospitalName = "SwasthyaSetu Multispeciality Telehealth & Research Hospital",
            diagnosis = "Acute Rhinopharyngitis with Secondary Bronchial Hyperreactivity",
            symptoms = "Fever (101°F), dry nocturnal cough, pharyngeal erythema, mild dyspnea on exertion",
            medicines = listOf(
                Medicine("Tab. Azithromycin 500 mg", "1 Tablet", "Once daily (OD) after dinner", "3 Days", "Take with plenty of water. Do not skip doses."),
                Medicine("Tab. Paracetamol 650 mg", "1 Tablet", "Three times daily (TDS)", "3 Days", "For fever and generalized body aches."),
                Medicine("Syrup Levocetirizine 5mg + Montelukast 10mg", "10 ml", "Once daily (HS) at bedtime", "5 Days", "Relieves bronchospasm and night coughing fits."),
                Medicine("Warm Saline Nasal Spray", "2 Puffs each nostril", "Twice daily (BD)", "7 Days", "Clears nasal congestion and soothes mucosal lining.")
            ),
            guidelines = "Strict oral hydration (>2.5 liters/day). Honey + ginger syrup for throat relief. Rest completely. Avoid cold, oily, and preserved foods.",
            followUp = "Review via teleconsultation in 4 days. If fever exceeds 103°F or chest tightness intensifies, report to nearest Emergency Room immediately.",
            doctorSignature = "${doctor.name}, ${doctor.qualifications} [Reg: MMC-2018/04/1829]",
            hospitalStamp = "SWASHTHYA SETU TELEHEALTH - OFFICIAL CLINICAL STAMP",
            isFinalApproved = false
        )
        _prescriptions.update { listOf(prescription) + it }
        return prescription
    }

    fun updateAndApprovePrescription(updatedPrescription: DigitalPrescription) {
        val approved = updatedPrescription.copy(isFinalApproved = true)
        _prescriptions.update { list ->
            list.map { if (it.id == approved.id) approved else it }
        }

        // Persist to Firestore
        firestore?.let { db ->
            val uid = approved.patientId
            db.collection("users").document(uid).collection("prescriptions").document(approved.id).set(
                mapOf(
                    "userId" to uid,
                    "patientName" to approved.patientName,
                    "doctorId" to approved.doctorId,
                    "doctorName" to approved.doctorName,
                    "hospitalName" to approved.hospitalName,
                    "diagnosis" to approved.diagnosis,
                    "symptoms" to approved.symptoms,
                    "guidelines" to approved.guidelines,
                    "followUp" to approved.followUp,
                    "doctorSignature" to approved.doctorSignature,
                    "isFinalApproved" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
    }

    fun createReferral(
        patientId: String,
        patientName: String,
        specialist: String,
        reason: String,
        facility: String,
        followUpDate: String
    ): SpecialistReferral {
        val doctor = _currentUser.value ?: User(
            id = "doc_101",
            name = "Dr. Aakash Verma",
            email = "doc@example.com",
            role = UserRole.DOCTOR
        )
        val referral = SpecialistReferral(
            id = "ref_" + UUID.randomUUID().toString().take(6),
            patientId = patientId,
            patientName = patientName,
            doctorId = doctor.id,
            doctorName = doctor.name,
            specialist = specialist,
            reason = reason,
            facility = facility,
            followUpDate = followUpDate,
            status = "referred",
            treatmentNotes = "Referred for advanced investigative workup and specialist management."
        )
        _referrals.update { listOf(referral) + it }

        // Persist to Firestore
        firestore?.let { db ->
            db.collection("users").document(patientId).collection("referrals").document(referral.id).set(
                mapOf(
                    "userId" to patientId,
                    "patientName" to referral.patientName,
                    "doctorId" to referral.doctorId,
                    "doctorName" to referral.doctorName,
                    "specialist" to referral.specialist,
                    "reason" to referral.reason,
                    "facility" to referral.facility,
                    "followUpDate" to referral.followUpDate,
                    "status" to referral.status,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }

        return referral
    }

    fun updateReferralStatus(referralId: String, newStatus: String, notes: String) {
        _referrals.update { list ->
            list.map {
                if (it.id == referralId) it.copy(status = newStatus, treatmentNotes = notes) else it
            }
        }
    }

    fun getDashboardAnalytics(): DashboardAnalytics {
        val totalScreenings = max(184, _symptomRecords.value.size + 180)
        val screeningsToday = 14 + _symptomRecords.value.size
        val totalConsultations = max(96, _consultations.value.size + 92)
        val consultationsToday = 8 + _consultations.value.size
        val completedReferrals = _referrals.value.count { it.status == "completed" } + 28
        val openReferrals = _referrals.value.count { it.status != "completed" } + 7
        val criticalCount = _symptomRecords.value.count { it.signal == TriageSignal.RED } + 19

        val sdf = SimpleDateFormat("EEE (dd MMM)", Locale.ENGLISH)
        val calendar = Calendar.getInstance()
        val dailyMap = mutableMapOf<String, Int>()
        for (i in 6 downTo 0) {
            val c = calendar.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -i)
            val dateLabel = sdf.format(c.time)
            dailyMap[dateLabel] = 12 + (i * 3 + (i * 7) % 5)
        }
        val todayKey = sdf.format(calendar.time)
        dailyMap[todayKey] = screeningsToday

        return DashboardAnalytics(
            registeredPatients = 342,
            screeningsToday = screeningsToday,
            totalScreenings = totalScreenings,
            consultationsToday = consultationsToday,
            totalConsultations = totalConsultations,
            completedReferrals = completedReferrals,
            openReferrals = openReferrals,
            criticalEmergencyCount = criticalCount,
            dailyVisitsMap = dailyMap
        )
    }
}
