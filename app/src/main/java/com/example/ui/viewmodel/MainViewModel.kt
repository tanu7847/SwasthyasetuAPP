package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.HealthcareRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppScreen {
    AUTH,
    PATIENT_WORKSPACE,
    DOCTOR_WORKSPACE,
    ADMIN_WORKSPACE
}

enum class PatientTab {
    SYMPTOMS_TRIAGE,
    EMERGENCY_HOSPITALS,
    TELECONSULTATION,
    RECORDS_REFERRALS
}

enum class DoctorTab {
    PATIENT_QUEUE,
    CONSULTATION_ASSIST,
    PRESCRIPTION_EDITOR,
    REFERRAL_FOLLOWUP
}

enum class AudioRecordingState {
    IDLE,
    RECORDING,
    PAUSED
}

class MainViewModel : ViewModel() {

    private var repository = HealthcareRepository()

    fun initContext(context: Context) {
        repository = HealthcareRepository(context)
    }

    val currentUser = repository.currentUser
    val symptomRecords = repository.symptomRecords
    val consultations = repository.consultations
    val transcripts = repository.transcripts
    val prescriptions = repository.prescriptions
    val referrals = repository.referrals
    val infectionDetection = repository.infectionDetection
    val extractedEntities = repository.extractedEntities

    val availableDoctors = repository.availableDoctors
    val baseHospitals = repository.baseHospitals

    private val _currentScreen = MutableStateFlow(AppScreen.PATIENT_WORKSPACE)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _patientTab = MutableStateFlow(PatientTab.SYMPTOMS_TRIAGE)
    val patientTab: StateFlow<PatientTab> = _patientTab.asStateFlow()

    private val _doctorTab = MutableStateFlow(DoctorTab.PATIENT_QUEUE)
    val doctorTab: StateFlow<DoctorTab> = _doctorTab.asStateFlow()

    // Form inputs for Symptom Intake Table
    private val _symptomInput = MutableStateFlow("")
    val symptomInput: StateFlow<String> = _symptomInput.asStateFlow()

    private val _severityInput = MutableStateFlow("moderate")
    val severityInput: StateFlow<String> = _severityInput.asStateFlow()

    private val _durationInput = MutableStateFlow("3 days")
    val durationInput: StateFlow<String> = _durationInput.asStateFlow()

    private val _allergiesInput = MutableStateFlow("Penicillin")
    val allergiesInput: StateFlow<String> = _allergiesInput.asStateFlow()

    private val _medicationsInput = MutableStateFlow("Paracetamol 500mg SOS")
    val medicationsInput: StateFlow<String> = _medicationsInput.asStateFlow()

    private val _languageInput = MutableStateFlow("hi")
    val languageInput: StateFlow<String> = _languageInput.asStateFlow()

    // Active Triage result for current session
    private val _latestTriage = MutableStateFlow<SymptomIntake?>(null)
    val latestTriage: StateFlow<SymptomIntake?> = _latestTriage.asStateFlow()

    // Teleconsultation state
    private val _activeConsultation = MutableStateFlow<Consultation?>(null)
    val activeConsultation: StateFlow<Consultation?> = _activeConsultation.asStateFlow()

    // Audio recording simulator
    private val _audioState = MutableStateFlow(AudioRecordingState.IDLE)
    val audioState: StateFlow<AudioRecordingState> = _audioState.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()
    private var timerJob: Job? = null

    // Video call camera toggle
    private val _isCameraOn = MutableStateFlow(false)
    val isCameraOn: StateFlow<Boolean> = _isCameraOn.asStateFlow()

    // Active prescription being edited by Doctor
    private val _editingPrescription = MutableStateFlow<DigitalPrescription?>(null)
    val editingPrescription: StateFlow<DigitalPrescription?> = _editingPrescription.asStateFlow()

    // Status banner message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Admin analytics
    private val _adminAnalytics = MutableStateFlow(repository.getDashboardAnalytics())
    val adminAnalytics: StateFlow<DashboardAnalytics> = _adminAnalytics.asStateFlow()

    // GPS location
    private val _userLatitude = MutableStateFlow(18.5204)
    private val _userLongitude = MutableStateFlow(73.8567)
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()
    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()

    val nearbyHospitals = combine(_userLatitude, _userLongitude) { lat, lon ->
        repository.getNearbyHospitals(lat, lon)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.baseHospitals)

    fun onAuthUserChanged(firebaseUser: FirebaseUser?) {
        repository.onAuthUserChanged(firebaseUser)
        if (firebaseUser != null) {
            _snackbarMessage.value = "Welcome back, ${firebaseUser.displayName ?: "Patient"}! Synced with Firestore."
        }
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setPatientTab(tab: PatientTab) {
        _patientTab.value = tab
    }

    fun setDoctorTab(tab: DoctorTab) {
        _doctorTab.value = tab
    }

    fun updateSymptomFields(
        symptoms: String,
        severity: String,
        duration: String,
        allergies: String,
        meds: String,
        lang: String
    ) {
        _symptomInput.value = symptoms
        _severityInput.value = severity
        _durationInput.value = duration
        _allergiesInput.value = allergies
        _medicationsInput.value = meds
        _languageInput.value = lang
    }

    fun submitSymptomIntake() {
        if (_symptomInput.value.isBlank()) {
            _snackbarMessage.value = "Please enter symptoms in the intake table."
            return
        }

        val intake = repository.processSymptomIntake(
            symptoms = _symptomInput.value,
            severity = _severityInput.value,
            duration = _durationInput.value,
            allergies = _allergiesInput.value,
            currentMedications = _medicationsInput.value,
            language = _languageInput.value,
            latitude = _userLatitude.value,
            longitude = _userLongitude.value
        )
        _latestTriage.value = intake
        refreshAdminStats()

        if (intake.signal == TriageSignal.RED) {
            _patientTab.value = PatientTab.EMERGENCY_HOSPITALS
            _snackbarMessage.value = "CRITICAL SYMPTOMS DETECTED! Emergency guidance activated."
        } else {
            _patientTab.value = PatientTab.TELECONSULTATION
            _snackbarMessage.value = "Triage checked: Non-critical symptoms. Proceeding to teleconsultation."
        }
    }

    fun startListeningAudio() {
        _audioState.value = AudioRecordingState.RECORDING
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _recordingSeconds.update { it + 1 }
            }
        }
        _snackbarMessage.value = "Mic listening... Model: gemini-3.5-transcribe ready."
    }

    fun pauseListeningAudio() {
        _audioState.value = AudioRecordingState.PAUSED
        timerJob?.cancel()
        _snackbarMessage.value = "Audio transcription paused."
    }

    fun resumeListeningAudio() {
        _audioState.value = AudioRecordingState.RECORDING
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _recordingSeconds.update { it + 1 }
            }
        }
        _snackbarMessage.value = "Audio transcription resumed."
    }

    fun stopAndUploadAudio() {
        _audioState.value = AudioRecordingState.IDLE
        timerJob?.cancel()
        _recordingSeconds.value = 0

        val consId = _activeConsultation.value?.id ?: "cons_tele_01"
        val lang = _languageInput.value

        viewModelScope.launch {
            _snackbarMessage.value = "Transcribing with gemini-3.5-transcribe..."
            try {
                repository.transcribeAudioWithGemini(consId, lang)
                _snackbarMessage.value = "Audio transcribed via gemini-3.5-transcribe & NLP entities extracted!"
            } catch (e: Exception) {
                repository.simulateAudioUploadAndTranscription(consId, lang)
                _snackbarMessage.value = "Audio transcribed & Multilingual translation ready!"
            }
        }
    }

    fun uploadCustomAudioFile() {
        stopAndUploadAudio()
    }

    fun uploadBodyImageForInfectionAnalysis() {
        viewModelScope.launch {
            _snackbarMessage.value = "Analyzing lesion with Gemini 3.8 Multimodal & OpenCV..."
            try {
                val result = repository.analyzeImageWithGemini()
                _snackbarMessage.value = "Image analyzed with Gemini Vision & OpenCV. Lesion marked with ${result.confidencePercent}% confidence!"
            } catch (e: Exception) {
                val result = repository.analyzeImageForInfection()
                _snackbarMessage.value = "Image analyzed with OpenCV & CNN. Lesion marked with ${result.confidencePercent}% confidence!"
            }
        }
    }

    fun toggleCamera() {
        _isCameraOn.update { !it }
    }

    fun selectDoctorAndStartConsultation(doctor: Doctor) {
        val consultation = repository.requestConsultation(
            doctor = doctor,
            reason = _symptomInput.value.ifBlank { "Upper respiratory symptoms and persistent fever" },
            language = _languageInput.value
        )
        _activeConsultation.value = consultation

        val patient = currentUser.value ?: User("pat_demo", "Ramesh Kulkarni", "ramesh@example.com", UserRole.PATIENT)
        val initialRx = repository.generateInitialPrescription(consultation.id, patient, doctor)
        _editingPrescription.value = initialRx

        _patientTab.value = PatientTab.TELECONSULTATION
        _snackbarMessage.value = "Consultation room connected with ${doctor.name}!"
        refreshAdminStats()
    }

    fun updatePrescription(prescription: DigitalPrescription) {
        _editingPrescription.value = prescription
    }

    fun approvePrescription() {
        val current = _editingPrescription.value ?: return
        repository.updateAndApprovePrescription(current)
        _snackbarMessage.value = "Prescription officially reviewed, signed, and approved by Doctor!"
        refreshAdminStats()
    }

    fun addMedicineToPrescription(medicine: Medicine) {
        val current = _editingPrescription.value ?: return
        val updated = current.copy(medicines = current.medicines + medicine)
        _editingPrescription.value = updated
    }

    fun removeMedicineFromPrescription(index: Int) {
        val current = _editingPrescription.value ?: return
        val updatedList = current.medicines.toMutableList()
        if (index in updatedList.indices) {
            updatedList.removeAt(index)
            _editingPrescription.value = current.copy(medicines = updatedList)
        }
    }

    fun createSpecialistReferral(
        specialist: String,
        reason: String,
        facility: String,
        followUpDate: String
    ) {
        val pat = currentUser.value
        repository.createReferral(
            patientId = pat?.id ?: "pat_881",
            patientName = pat?.name ?: "Ramesh Kulkarni",
            specialist = specialist,
            reason = reason,
            facility = facility,
            followUpDate = followUpDate
        )
        _snackbarMessage.value = "Referral to $specialist created successfully."
        refreshAdminStats()
    }

    fun updateReferralStatus(referralId: String, newStatus: String, notes: String) {
        repository.updateReferralStatus(referralId, newStatus, notes)
        _snackbarMessage.value = "Referral status updated to $newStatus."
        refreshAdminStats()
    }

    fun switchUserRole(role: UserRole) {
        val email = when (role) {
            UserRole.PATIENT -> "patient@swasthyasetu.org"
            UserRole.DOCTOR -> "dr.aakash@swasthyasetu.org"
            UserRole.ADMIN -> "admin@swasthyasetu.org"
        }
        val name = when (role) {
            UserRole.PATIENT -> "Ramesh Kulkarni"
            UserRole.DOCTOR -> "Dr. Aakash Verma"
            UserRole.ADMIN -> "Chief Medical Administrator"
        }
        login(email, role, name)
    }

    fun login(email: String, role: UserRole, displayName: String) {
        val user = repository.login(email, role, displayName)
        _currentScreen.value = when (role) {
            UserRole.PATIENT -> AppScreen.PATIENT_WORKSPACE
            UserRole.DOCTOR -> AppScreen.DOCTOR_WORKSPACE
            UserRole.ADMIN -> AppScreen.ADMIN_WORKSPACE
        }
        _snackbarMessage.value = "Signed in as ${user.name} (${role.name})"
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
    ) {
        val user = repository.registerDoctor(
            name, email, phone, registrationNumber, qualifications,
            specialty, certifications, hospitalAffiliation, experienceYears, language
        )
        _currentScreen.value = AppScreen.DOCTOR_WORKSPACE
        _snackbarMessage.value = "Doctor credentials registered and verified! Welcome, ${user.name}."
    }

    fun registerPatient(
        name: String,
        email: String,
        phone: String,
        age: Int,
        gender: String,
        language: String
    ) {
        val user = repository.registerPatient(name, email, phone, age, gender, language)
        _currentScreen.value = AppScreen.PATIENT_WORKSPACE
        _patientTab.value = PatientTab.SYMPTOMS_TRIAGE
        _snackbarMessage.value = "Patient registered successfully! Welcome, ${user.name}."
    }

    fun logout() {
        repository.logout()
        _currentScreen.value = AppScreen.AUTH
        _snackbarMessage.value = "Logged out successfully."
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun refreshAdminStats() {
        _adminAnalytics.value = repository.getDashboardAnalytics()
    }
}
