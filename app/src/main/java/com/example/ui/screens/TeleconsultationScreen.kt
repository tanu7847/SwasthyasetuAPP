package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AudioRecordingState

/**
 * State representing the active video teleconsultation session.
 */
enum class TeleconsultationCallStatus {
    IDLE,
    CONNECTED,
    PAUSED,
    ENDED
}

/**
 * TeleconsultationScreen Composable
 *
 * Implements interactive doctor-patient video teleconsultation with:
 * - Dual video feed placeholders for Doctor and Patient
 * - Core action buttons: 'Start Call', 'Pause', 'Resume', and 'Upload'
 * - Realtime multilingual audio transcription (Patient's language + Doctor's English)
 * - OpenCV and CNN infection detection bounding box visualizer
 * - NLP entity extraction (symptoms, allergies, duration) & Groq doctor inquiry prompts
 * - Direct routing to Digital Prescription review
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeleconsultationScreen(
    activeConsultation: Consultation? = null,
    availableDoctors: List<Doctor> = emptyList(),
    audioState: AudioRecordingState = AudioRecordingState.IDLE,
    recordingSeconds: Int = 0,
    isCameraOn: Boolean = true,
    transcripts: List<AudioTranscription> = emptyList(),
    infectionDetection: InfectionDetection? = null,
    extractedEntities: ExtractedMedicalEntities? = null,
    onSelectDoctor: (Doctor) -> Unit = {},
    onStartCall: () -> Unit = {},
    onStartListening: () -> Unit = {},
    onPause: () -> Unit = {},
    onPauseListening: () -> Unit = {},
    onResume: () -> Unit = {},
    onResumeListening: () -> Unit = {},
    onUpload: () -> Unit = {},
    onStopAndUploadAudio: () -> Unit = {},
    onUploadCustomAudio: () -> Unit = {},
    onUploadImageForInfection: () -> Unit = {},
    onToggleCamera: () -> Unit = {},
    onNavigateToPrescription: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Internal state management to support both standalone previews/tests and parent ViewModel bindings
    var internalCallStatus by remember {
        mutableStateOf(
            if (activeConsultation != null) TeleconsultationCallStatus.CONNECTED
            else TeleconsultationCallStatus.IDLE
        )
    }
    var showUploadModal by remember { mutableStateOf(false) }
    var localCameraActive by remember { mutableStateOf(isCameraOn) }
    var internalSeconds by remember { mutableIntStateOf(recordingSeconds) }

    // Fallback doctor profile if none is actively selected
    val displayDoctorName = activeConsultation?.doctorName
        ?: availableDoctors.firstOrNull()?.name
        ?: "Dr. Priya Sharma, MD"
    val displayDoctorSpecialty = availableDoctors.find { it.id == activeConsultation?.doctorId }?.specialty
        ?: availableDoctors.firstOrNull()?.specialty
        ?: "Chief Consultant Physician"
    val displayHospital = "Apollo Apex Telehealth Clinic"

    // Derive effective call state based on external or internal triggers
    val effectiveStatus = when {
        audioState == AudioRecordingState.PAUSED -> TeleconsultationCallStatus.PAUSED
        audioState == AudioRecordingState.RECORDING || activeConsultation != null -> TeleconsultationCallStatus.CONNECTED
        else -> internalCallStatus
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("teleconsultation_screen")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Module Header Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ClinicalBlueContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(ClinicalBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Teleconsultation Icon",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Module 3: Teleconsultation Platform",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ClinicalBlueOnContainer
                        )
                        Text(
                            text = "Live Doctor-Patient Videocall, Multilingual Speech-to-Text & AI Clinical Analysis",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary
                        )
                    }
                }
            }
        }

        // ===================================================================
        // VIDEO FEEDS SECTION (Doctor and Patient Placeholders)
        // ===================================================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("video_call_container")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Session Status Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val statusColor = when (effectiveStatus) {
                                TeleconsultationCallStatus.CONNECTED -> Color(0xFF22C55E)
                                TeleconsultationCallStatus.PAUSED -> CautionYellow
                                TeleconsultationCallStatus.IDLE -> Color.Gray
                                TeleconsultationCallStatus.ENDED -> EmergencyRed
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Text(
                                text = when (effectiveStatus) {
                                    TeleconsultationCallStatus.CONNECTED -> "CALL ACTIVE • 1080P HD"
                                    TeleconsultationCallStatus.PAUSED -> "CALL PAUSED • ON HOLD"
                                    TeleconsultationCallStatus.IDLE -> "READY TO CONNECT"
                                    TeleconsultationCallStatus.ENDED -> "CALL TERMINATED"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🔒 Encrypted Telehealth",
                                color = Color(0xFFA7F3D0),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Dual Video Feeds Grid (Doctor Video Feed + Patient Video Feed Placeholders)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ---------------------------------------------------
                        // DOCTOR VIDEO FEED PLACEHOLDER
                        // ---------------------------------------------------
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                    )
                                )
                                .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                .testTag("doctor_video_placeholder"),
                            contentAlignment = Alignment.Center
                        ) {
                            // Sub-tag for feed identification
                            Box(modifier = Modifier.testTag("doctor_video_feed"))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                val pulseAnim = rememberInfiniteTransition(label = "doctor_pulse")
                                val scale by pulseAnim.animateFloat(
                                    initialValue = 0.95f,
                                    targetValue = 1.05f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(900, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "doctorPulse"
                                )

                                Box(
                                    modifier = Modifier
                                        .size((52 * (if (effectiveStatus == TeleconsultationCallStatus.CONNECTED) scale else 1f)).dp)
                                        .background(TealPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MedicalServices,
                                        contentDescription = "Doctor Avatar",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Text(
                                    text = displayDoctorName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                                Text(
                                    text = displayDoctorSpecialty,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                                Text(
                                    text = displayHospital,
                                    color = SlateTextSecondary,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }

                            // Doctor Video Feed Placeholder Badge
                            Surface(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = "Doctor Video Feed",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Live Audio Indicator for Doctor
                            if (effectiveStatus == TeleconsultationCallStatus.CONNECTED) {
                                Surface(
                                    color = SuccessGreen.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // ---------------------------------------------------
                        // PATIENT VIDEO FEED PLACEHOLDER
                        // ---------------------------------------------------
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF091E1B), Color(0xFF041210))
                                    )
                                )
                                .border(1.5.dp, Color(0xFF134E48), RoundedCornerShape(12.dp))
                                .testTag("patient_video_placeholder"),
                            contentAlignment = Alignment.Center
                        ) {
                            // Sub-tag for feed identification
                            Box(modifier = Modifier.testTag("patient_video_feed"))

                            if (localCameraActive && effectiveStatus != TeleconsultationCallStatus.PAUSED) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .background(Color(0xFF134E48), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = "Patient Camera Preview",
                                            tint = Color(0xFFA7F3D0),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Text(
                                        text = "Patient Camera (Self)",
                                        color = Color(0xFFA7F3D0),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Front Cam • 1080p 30fps",
                                        color = Color(0xFF6EE7B7),
                                        fontSize = 9.sp
                                    )
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VideocamOff,
                                        contentDescription = "Camera Off",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Text(
                                        text = if (effectiveStatus == TeleconsultationCallStatus.PAUSED) "Feed Paused" else "Camera Off",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Patient Video Feed Placeholder Badge
                            Surface(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = "Patient Video Feed",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Toggle Camera Quick Action
                            IconButton(
                                onClick = {
                                    localCameraActive = !localCameraActive
                                    onToggleCamera()
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(32.dp)
                                    .testTag("toggle_camera_button")
                            ) {
                                Icon(
                                    imageVector = if (localCameraActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    contentDescription = "Toggle Camera",
                                    tint = if (localCameraActive) Color(0xFFA7F3D0) else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Audio Waveform & Timer Indicator
                    if (effectiveStatus == TeleconsultationCallStatus.CONNECTED) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Microphone Active",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Listening & Multilingual Transcribing...",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }

                            // Animated audio equalizer bars
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val waveAnim = rememberInfiniteTransition(label = "audio_eq")
                                listOf(12, 22, 16, 28, 14).forEachIndexed { i, h ->
                                    val barH by waveAnim.animateFloat(
                                        initialValue = 4f,
                                        targetValue = h.toFloat(),
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(400 + i * 80, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "eqBar_$i"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(barH.dp)
                                            .background(Color(0xFF22C55E), RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }

                    // =======================================================
                    // CORE ACTION BUTTONS: 'Start Call', 'Pause', 'Resume', 'Upload'
                    // =======================================================
                    Divider(color = Color(0xFF334155))

                    Text(
                        text = "Call Controls & Media Actions",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. 'Start Call' Button
                        Button(
                            onClick = {
                                internalCallStatus = TeleconsultationCallStatus.CONNECTED
                                onStartCall()
                                onStartListening()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (effectiveStatus == TeleconsultationCallStatus.CONNECTED) Color(0xFF134E48) else SuccessGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Start Call",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Call", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 2. 'Pause' Button
                        Button(
                            onClick = {
                                internalCallStatus = TeleconsultationCallStatus.PAUSED
                                onPause()
                                onPauseListening()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (effectiveStatus == TeleconsultationCallStatus.PAUSED) Color(0xFF78350F) else CautionYellow
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pause_call_button")
                                .testTag("pause_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause Call",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 3. 'Resume' Button
                        Button(
                            onClick = {
                                internalCallStatus = TeleconsultationCallStatus.CONNECTED
                                onResume()
                                onResumeListening()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("resume_call_button")
                                .testTag("resume_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume Call",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 4. 'Upload' Button
                        Button(
                            onClick = {
                                showUploadModal = true
                                onUpload()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ClinicalBlue
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upload_call_button")
                                .testTag("upload_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = "Upload Media or Audio",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Available Doctors Section (if user desires to switch or select specific physician)
        if (availableDoctors.isNotEmpty()) {
            item {
                Text(
                    text = "Consulting Medical Officers",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
            }
            items(availableDoctors) { doctor ->
                val isSelected = activeConsultation?.doctorId == doctor.id
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) TealPrimaryLight else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) TealPrimary else SlateBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(TealPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = doctor.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(doctor.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(doctor.specialty, style = MaterialTheme.typography.bodySmall, color = TealPrimary)
                                Text("${doctor.qualifications} • ${doctor.experienceYears}y exp", style = MaterialTheme.typography.labelSmall, color = SlateTextSecondary)
                            }
                        }

                        Button(
                            onClick = {
                                onSelectDoctor(doctor)
                                internalCallStatus = TeleconsultationCallStatus.CONNECTED
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) SuccessGreen else TealPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("connect_doctor_${doctor.id}")
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.VideoCall,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isSelected) "Active" else "Connect", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // ===================================================================
        // OpenCV & CNN Infection Marking Visualizer Section
        // ===================================================================
        item {
            AnimatedVisibility(visible = infectionDetection != null) {
                infectionDetection?.let { detection ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, CautionYellow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("infection_detection_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Radar, contentDescription = null, tint = CautionYellow)
                                    Text(
                                        text = "OpenCV & CNN Infection Detection Engine",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = CautionYellowText
                                    )
                                }
                                Surface(
                                    color = CautionYellowContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${detection.confidencePercent}% Confidence",
                                        color = CautionYellowText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Simulated Body Contour Canvas with OpenCV Bounding Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val canvasWidth = size.width
                                    val canvasHeight = size.height

                                    // Silhouette guideline
                                    drawCircle(
                                        color = Color(0xFF334155),
                                        radius = 35.dp.toPx(),
                                        center = Offset(canvasWidth * 0.5f, canvasHeight * 0.35f),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                    drawLine(
                                        color = Color(0xFF334155),
                                        start = Offset(canvasWidth * 0.5f, canvasHeight * 0.5f),
                                        end = Offset(canvasWidth * 0.5f, canvasHeight * 0.85f),
                                        strokeWidth = 3.dp.toPx()
                                    )

                                    // Draw Infected bounding box computed by CNN/OpenCV
                                    val boxLeft = canvasWidth * detection.boxNormX
                                    val boxTop = canvasHeight * detection.boxNormY
                                    val boxW = canvasWidth * detection.boxNormWidth
                                    val boxH = canvasHeight * detection.boxNormHeight

                                    drawRect(
                                        color = Color(0x66DC2626),
                                        topLeft = Offset(boxLeft, boxTop),
                                        size = Size(boxW, boxH)
                                    )

                                    drawRect(
                                        color = EmergencyRed,
                                        topLeft = Offset(boxLeft, boxTop),
                                        size = Size(boxW, boxH),
                                        style = Stroke(
                                            width = 3.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                                        )
                                    )
                                }

                                Surface(
                                    color = EmergencyRed,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "🔴 [OpenCV/CNN MARKED LESION] ${detection.affectedAreaName}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Clinical Findings: ${detection.clinicalFindings}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = "Prescribed Action: ${detection.recommendations}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TealPrimary
                            )
                        }
                    }
                }
            }
        }

        // ===================================================================
        // Multilingual Transcripts: Patient's Language + Doctor's English
        // ===================================================================
        if (transcripts.isNotEmpty()) {
            item {
                Text(
                    text = "Multilingual Consultation Transcript (Live Synced)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
            }

            items(transcripts) { tx ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🗣 Patient's Language: ${tx.sourceLanguage}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimaryDark
                            )
                            Text("Realtime STT", style = MaterialTheme.typography.labelSmall, color = SlateTextSecondary)
                        }
                        Text(
                            text = tx.originalText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateTextPrimary,
                            fontWeight = FontWeight.Medium
                        )

                        Divider(color = SlateBorder)

                        Text(
                            text = "🌐 English Translation (For Doctor's Comprehension)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ClinicalBlueOnContainer
                        )
                        Text(
                            text = tx.englishText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF0369A1)
                        )
                    }
                }
            }
        }

        // ===================================================================
        // NLP Medical Entity Extraction & Groq Questions Section
        // ===================================================================
        item {
            AnimatedVisibility(visible = extractedEntities != null) {
                extractedEntities?.let { entities ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TealPrimaryLight),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nlp_entities_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = TealPrimary)
                                Text(
                                    text = "NLP Medical Entities & Groq Realtime Questions",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TealPrimaryDark
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Extracted Symptoms: ${entities.symptomsExtracted.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SlateTextPrimary
                                )
                                Text(
                                    text = "Identified Allergies: ${entities.allergiesExtracted.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmergencyRedText
                                )
                                Text(
                                    text = "Duration: ${entities.durationExtracted}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateTextSecondary
                                )
                            }

                            Divider(color = TealPrimary.copy(alpha = 0.2f))

                            Text(
                                text = "System / Groq Realtime Inquiries for Doctor:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimaryDark
                            )

                            entities.suggestedDoctorQuestions.forEach { q ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("❓", fontSize = 12.sp)
                                    Text(
                                        text = q,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlateTextPrimary
                                    )
                                }
                            }

                            Button(
                                onClick = onNavigateToPrescription,
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("generate_prescription_btn")
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Digital Prescription with Official Stamp", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // =======================================================================
    // Upload Modal / BottomSheet for Media & Audio
    // =======================================================================
    if (showUploadModal) {
        AlertDialog(
            onDismissRequest = { showUploadModal = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = ClinicalBlue)
                    Text("Upload Media & Audio", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select consultation audio, diagnostic file, or infected body area image:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextSecondary
                    )

                    // Option 1: Live Audio Stop & Upload
                    OutlinedButton(
                        onClick = {
                            showUploadModal = false
                            onStopAndUploadAudio()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_upload_live_audio"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop & Upload Live Conversation Audio")
                    }

                    // Option 2: Upload Custom Audio File (.wav/.mp3)
                    OutlinedButton(
                        onClick = {
                            showUploadModal = false
                            onUploadCustomAudio()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_upload_custom_audio"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Audio File from PC / Device")
                    }

                    // Option 3: Upload Body Image for OpenCV / CNN
                    Button(
                        onClick = {
                            showUploadModal = false
                            onUploadImageForInfection()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ClinicalBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_upload_body_image"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Body Image (OpenCV/CNN)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUploadModal = false }) {
                    Text("Close")
                }
            }
        )
    }
}
