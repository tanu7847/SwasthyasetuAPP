package com.example.data.model

enum class UserRole {
    PATIENT, DOCTOR, ADMIN
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val phone: String = "",
    val language: String = "en",
    val age: Int? = null,
    val gender: String = "",
    val registrationNumber: String = "",
    val qualifications: String = "",
    val certifications: String = "",
    val specialty: String = "",
    val hospitalAffiliation: String = "",
    val experienceYears: Int = 0,
    val isVerifiedDoctor: Boolean = true
)

enum class TriageSignal {
    RED, YELLOW, GREEN
}

data class SymptomIntake(
    val id: String,
    val patientId: String,
    val symptoms: String,
    val severity: String, // mild, moderate, severe
    val duration: String,
    val allergies: String,
    val currentMedications: String,
    val language: String = "en",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val signal: TriageSignal,
    val urgencyText: String,
    val warningFlags: List<String>,
    val guidanceTips: List<String>,
    val ayurvedicHomeCareTips: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

data class NearbyHospital(
    val name: String,
    val phone: String,
    val helpline: String,
    val address: String,
    val distanceKm: Double,
    val latitude: Double,
    val longitude: Double,
    val emergencyAvailable: Boolean = true,
    val icuBedsAvailable: Int = 12
)

data class Doctor(
    val id: String,
    val name: String,
    val specialty: String,
    val qualifications: String,
    val experienceYears: Int,
    val language: String,
    val isOnline: Boolean = true
)

data class Consultation(
    val id: String,
    val patientId: String,
    val patientName: String,
    val doctorId: String,
    val doctorName: String,
    val reason: String,
    val language: String,
    val status: String, // "requested", "accepted", "in_progress", "completed"
    val roomId: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class AudioTranscription(
    val id: String,
    val consultationId: String,
    val originalText: String,
    val englishText: String,
    val sourceLanguage: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class InfectionDetection(
    val hasInfectedPart: Boolean,
    val affectedAreaName: String,
    val confidencePercent: Int,
    val detectionModel: String = "OpenCV-Haar + CNN MobileNetV2",
    val boxNormX: Float,
    val boxNormY: Float,
    val boxNormWidth: Float,
    val boxNormHeight: Float,
    val clinicalFindings: String,
    val recommendations: String
)

data class Medicine(
    val name: String,
    val dosage: String,
    val frequency: String,
    val duration: String,
    val instructions: String
)

data class DigitalPrescription(
    val id: String,
    val consultationId: String,
    val patientId: String,
    val patientName: String,
    val patientAge: Int,
    val patientGender: String,
    val doctorId: String,
    val doctorName: String,
    val hospitalName: String,
    val diagnosis: String,
    val symptoms: String,
    val medicines: List<Medicine>,
    val guidelines: String,
    val followUp: String,
    val doctorSignature: String,
    val hospitalStamp: String = "SwasthyaSetu Verified Official Seal",
    val isFinalApproved: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

data class SpecialistReferral(
    val id: String,
    val patientId: String,
    val patientName: String,
    val doctorId: String,
    val doctorName: String,
    val specialist: String,
    val reason: String,
    val facility: String,
    val followUpDate: String,
    val status: String, // "referred", "appointment_booked", "in_treatment", "completed"
    val treatmentNotes: String = ""
)

data class ExtractedMedicalEntities(
    val symptomsExtracted: List<String>,
    val allergiesExtracted: List<String>,
    val durationExtracted: String,
    val medicinesMentioned: List<String>,
    val suggestedDoctorQuestions: List<String>
)

data class DashboardAnalytics(
    val registeredPatients: Int,
    val screeningsToday: Int,
    val totalScreenings: Int,
    val consultationsToday: Int,
    val totalConsultations: Int,
    val completedReferrals: Int,
    val openReferrals: Int,
    val criticalEmergencyCount: Int,
    val dailyVisitsMap: Map<String, Int>
)
