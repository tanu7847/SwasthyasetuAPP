package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserRole
import com.example.ui.components.TopMedicalHeader
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.*

import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // STEP 1: Configure App Check with launch intent before UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: MainViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.initContext(context.applicationContext)
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            viewModel.onAuthUserChanged(auth.currentUser)
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val patientTab by viewModel.patientTab.collectAsStateWithLifecycle()
    val doctorTab by viewModel.doctorTab.collectAsStateWithLifecycle()

    val symptoms by viewModel.symptomInput.collectAsStateWithLifecycle()
    val severity by viewModel.severityInput.collectAsStateWithLifecycle()
    val duration by viewModel.durationInput.collectAsStateWithLifecycle()
    val allergies by viewModel.allergiesInput.collectAsStateWithLifecycle()
    val medications by viewModel.medicationsInput.collectAsStateWithLifecycle()
    val language by viewModel.languageInput.collectAsStateWithLifecycle()

    val latestTriage by viewModel.latestTriage.collectAsStateWithLifecycle()
    val activeConsultation by viewModel.activeConsultation.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val recordingSeconds by viewModel.recordingSeconds.collectAsStateWithLifecycle()
    val isCameraOn by viewModel.isCameraOn.collectAsStateWithLifecycle()
    val transcripts by viewModel.transcripts.collectAsStateWithLifecycle()
    val infectionDetection by viewModel.infectionDetection.collectAsStateWithLifecycle()
    val extractedEntities by viewModel.extractedEntities.collectAsStateWithLifecycle()
    val editingPrescription by viewModel.editingPrescription.collectAsStateWithLifecycle()
    val prescriptions by viewModel.prescriptions.collectAsStateWithLifecycle()
    val referrals by viewModel.referrals.collectAsStateWithLifecycle()
    val adminAnalytics by viewModel.adminAnalytics.collectAsStateWithLifecycle()

    val userLatitude by viewModel.userLatitude.collectAsStateWithLifecycle()
    val userLongitude by viewModel.userLongitude.collectAsStateWithLifecycle()
    val nearbyHospitals by viewModel.nearbyHospitals.collectAsStateWithLifecycle()

    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Handle back button behavior
    BackHandler(enabled = currentScreen != AppScreen.AUTH && (patientTab != PatientTab.SYMPTOMS_TRIAGE || currentScreen != AppScreen.PATIENT_WORKSPACE)) {
        if (currentScreen != AppScreen.PATIENT_WORKSPACE && currentScreen != AppScreen.AUTH) {
            viewModel.switchUserRole(UserRole.PATIENT)
        } else if (patientTab != PatientTab.SYMPTOMS_TRIAGE) {
            viewModel.setPatientTab(PatientTab.SYMPTOMS_TRIAGE)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopMedicalHeader(
                currentUser = currentUser,
                onSwitchRole = { role ->
                    viewModel.switchUserRole(role)
                },
                onSignOut = {
                    viewModel.logout()
                }
            )
        },
        bottomBar = {
            if (currentScreen == AppScreen.PATIENT_WORKSPACE && currentUser?.role == UserRole.PATIENT) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.testTag("patient_bottom_navigation")
                ) {
                    NavigationBarItem(
                        selected = patientTab == PatientTab.SYMPTOMS_TRIAGE,
                        onClick = { viewModel.setPatientTab(PatientTab.SYMPTOMS_TRIAGE) },
                        icon = { Icon(Icons.Default.TableView, contentDescription = "Symptom Table") },
                        label = { Text("1. Triage Table") },
                        modifier = Modifier.testTag("tab_symptoms_triage")
                    )
                    NavigationBarItem(
                        selected = patientTab == PatientTab.EMERGENCY_HOSPITALS,
                        onClick = { viewModel.setPatientTab(PatientTab.EMERGENCY_HOSPITALS) },
                        icon = { Icon(Icons.Default.Emergency, contentDescription = "Emergency") },
                        label = { Text("2. Emergency") },
                        modifier = Modifier.testTag("tab_emergency_hospitals")
                    )
                    NavigationBarItem(
                        selected = patientTab == PatientTab.TELECONSULTATION,
                        onClick = { viewModel.setPatientTab(PatientTab.TELECONSULTATION) },
                        icon = { Icon(Icons.Default.VideoCall, contentDescription = "Teleconsultation") },
                        label = { Text("3. Consult Room") },
                        modifier = Modifier.testTag("tab_teleconsultation")
                    )
                    NavigationBarItem(
                        selected = patientTab == PatientTab.RECORDS_REFERRALS,
                        onClick = { viewModel.setPatientTab(PatientTab.RECORDS_REFERRALS) },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Prescription") },
                        label = { Text("4. Prescription") },
                        modifier = Modifier.testTag("tab_records_referrals")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                currentScreen == AppScreen.AUTH || currentUser == null -> {
                    AuthScreen(
                        onLogin = { email, role, displayName ->
                            viewModel.login(email, role, displayName)
                        },
                        onRegisterDoctor = { name, email, phone, lic, qual, spec, cert, hosp, exp, lang ->
                            viewModel.registerDoctor(
                                name = name,
                                email = email,
                                phone = phone,
                                registrationNumber = lic,
                                qualifications = qual,
                                specialty = spec,
                                certifications = cert,
                                hospitalAffiliation = hosp,
                                experienceYears = exp,
                                language = lang
                            )
                        },
                        onRegisterPatient = { name, email, phone, age, gender, lang ->
                            viewModel.registerPatient(
                                name = name,
                                email = email,
                                phone = phone,
                                age = age,
                                gender = gender,
                                language = lang
                            )
                        }
                    )
                }

                currentScreen == AppScreen.PATIENT_WORKSPACE && currentUser?.role == UserRole.PATIENT -> {
                    when (patientTab) {
                        PatientTab.SYMPTOMS_TRIAGE -> {
                            SymptomTriageScreen(
                                symptoms = symptoms,
                                severity = severity,
                                duration = duration,
                                allergies = allergies,
                                medications = medications,
                                language = language,
                                latestTriage = latestTriage,
                                onUpdateFields = { s, sev, dur, all, med, l ->
                                    viewModel.updateSymptomFields(s, sev, dur, all, med, l)
                                },
                                onSubmit = { viewModel.submitSymptomIntake() },
                                onNavigateToEmergency = { viewModel.setPatientTab(PatientTab.EMERGENCY_HOSPITALS) },
                                onNavigateToTeleconsultation = { viewModel.setPatientTab(PatientTab.TELECONSULTATION) }
                            )
                        }
                        PatientTab.EMERGENCY_HOSPITALS -> {
                            EmergencyHospitalsScreen(
                                latestTriage = latestTriage,
                                nearbyHospitals = nearbyHospitals,
                                userLatitude = userLatitude,
                                userLongitude = userLongitude
                            )
                        }
                        PatientTab.TELECONSULTATION -> {
                            TeleconsultationScreen(
                                activeConsultation = activeConsultation,
                                availableDoctors = viewModel.availableDoctors,
                                audioState = audioState,
                                recordingSeconds = recordingSeconds,
                                isCameraOn = isCameraOn,
                                transcripts = transcripts,
                                infectionDetection = infectionDetection,
                                extractedEntities = extractedEntities,
                                onSelectDoctor = { doctor ->
                                    viewModel.selectDoctorAndStartConsultation(doctor)
                                },
                                onStartListening = { viewModel.startListeningAudio() },
                                onPauseListening = { viewModel.pauseListeningAudio() },
                                onResumeListening = { viewModel.resumeListeningAudio() },
                                onStopAndUploadAudio = { viewModel.stopAndUploadAudio() },
                                onUploadCustomAudio = { viewModel.uploadCustomAudioFile() },
                                onUploadImageForInfection = { viewModel.uploadBodyImageForInfectionAnalysis() },
                                onToggleCamera = { viewModel.toggleCamera() },
                                onNavigateToPrescription = { viewModel.setPatientTab(PatientTab.RECORDS_REFERRALS) }
                            )
                        }
                        PatientTab.RECORDS_REFERRALS -> {
                            PrescriptionAndReferralScreen(
                                prescriptions = prescriptions,
                                referrals = referrals,
                                onSwitchToDoctorToReview = {
                                    viewModel.switchUserRole(UserRole.DOCTOR)
                                    viewModel.setDoctorTab(DoctorTab.PRESCRIPTION_EDITOR)
                                }
                            )
                        }
                    }
                }

                currentScreen == AppScreen.DOCTOR_WORKSPACE || currentUser?.role == UserRole.DOCTOR -> {
                    DoctorWorkspaceScreen(
                        currentTab = doctorTab,
                        onTabSelected = { viewModel.setDoctorTab(it) },
                        consultations = viewModel.consultations.collectAsStateWithLifecycle().value,
                        editingPrescription = editingPrescription,
                        referrals = referrals,
                        onUpdatePrescription = { viewModel.updatePrescription(it) },
                        onApprovePrescription = { viewModel.approvePrescription() },
                        onAddMedicine = { viewModel.addMedicineToPrescription(it) },
                        onRemoveMedicine = { viewModel.removeMedicineFromPrescription(it) },
                        onCreateReferral = { spec, rsn, fac, date ->
                            viewModel.createSpecialistReferral(spec, rsn, fac, date)
                        },
                        onUpdateReferralStatus = { refId, st, notes ->
                            viewModel.updateReferralStatus(refId, st, notes)
                        }
                    )
                }

                currentScreen == AppScreen.ADMIN_WORKSPACE || currentUser?.role == UserRole.ADMIN -> {
                    AdminDashboardScreen(
                        analytics = adminAnalytics,
                        onRefresh = { viewModel.refreshAdminStats() }
                    )
                }

                else -> {
                    AuthScreen(
                        onLogin = { email, role, displayName ->
                            viewModel.login(email, role, displayName)
                        },
                        onRegisterDoctor = { name, email, phone, lic, qual, spec, cert, hosp, exp, lang ->
                            viewModel.registerDoctor(
                                name = name,
                                email = email,
                                phone = phone,
                                registrationNumber = lic,
                                qualifications = qual,
                                specialty = spec,
                                certifications = cert,
                                hospitalAffiliation = hosp,
                                experienceYears = exp,
                                language = lang
                            )
                        },
                        onRegisterPatient = { name, email, phone, age, gender, lang ->
                            viewModel.registerPatient(
                                name = name,
                                email = email,
                                phone = phone,
                                age = age,
                                gender = gender,
                                language = lang
                            )
                        }
                    )
                }
            }
        }
    }
}
