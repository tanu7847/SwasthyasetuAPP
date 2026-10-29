package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SymptomIntake
import com.example.data.model.TriageSignal
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomTriageScreen(
    symptoms: String,
    severity: String,
    duration: String,
    allergies: String,
    medications: String,
    language: String,
    latestTriage: SymptomIntake?,
    onUpdateFields: (symptoms: String, severity: String, duration: String, allergies: String, meds: String, lang: String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNavigateToTeleconsultation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val quickSymptoms = listOf(
        "Chest pain with difficulty breathing",
        "High fever (102°F), dry cough & throat pain",
        "Spreading skin rash with swelling & itch",
        "Severe headache with nausea & dizziness"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Module Title Card
        Card(
            colors = CardDefaults.cardColors(containerColor = TealPrimaryLight),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AssignmentLate,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Module 1: Symptom Intake Table",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimaryDark
                    )
                    Text(
                        text = "Fill in clinical details for automated Emergency Triage Priority management.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary
                    )
                }
            }
        }

        // Quick Clinical Presets
        Text(
            text = "Quick Presets for Testing:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = SlateTextPrimary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = {
                    onUpdateFields(
                        "Severe chest pain and difficulty breathing since morning",
                        "severe",
                        "2 hours",
                        "Aspirin",
                        "None",
                        language
                    )
                },
                label = { Text("🚨 Critical (Chest Pain)", fontSize = 11.sp) },
                colors = AssistChipDefaults.assistChipColors(containerColor = EmergencyRedContainer)
            )
            AssistChip(
                onClick = {
                    onUpdateFields(
                        "High fever, dry cough, throat irritation and body pain",
                        "moderate",
                        "3 days",
                        "Penicillin",
                        "Paracetamol 500mg",
                        language
                    )
                },
                label = { Text("🩺 Non-Critical (Fever/Cough)", fontSize = 11.sp) },
                colors = AssistChipDefaults.assistChipColors(containerColor = ClinicalBlueContainer)
            )
        }

        // Structured Intake Table
        Text(
            text = "Patient Clinical Information Table",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Table Row 1: Symptoms
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "1. Current Symptoms *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                    OutlinedTextField(
                        value = symptoms,
                        onValueChange = {
                            onUpdateFields(it, severity, duration, allergies, medications, language)
                        },
                        placeholder = { Text("e.g. Chest pain, difficulty breathing, fever, cough...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("symptoms_input"),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Divider(color = SlateBorder)

                // Table Row 2: Severity & Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "2. Severity Level *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = severity.replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("severity_dropdown"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                listOf("mild", "moderate", "severe").forEach { level ->
                                    DropdownMenuItem(
                                        text = { Text(level.replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            onUpdateFields(symptoms, level, duration, allergies, medications, language)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "3. Duration *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                        OutlinedTextField(
                            value = duration,
                            onValueChange = {
                                onUpdateFields(symptoms, severity, it, allergies, medications, language)
                            },
                            placeholder = { Text("e.g. 3 days") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("duration_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Divider(color = SlateBorder)

                // Table Row 3: Known Allergies & Current Medications
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "4. Known Allergies",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                        OutlinedTextField(
                            value = allergies,
                            onValueChange = {
                                onUpdateFields(symptoms, severity, duration, it, medications, language)
                            },
                            placeholder = { Text("e.g. Penicillin, Sulfa") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("allergies_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "5. Current Medicines",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                        OutlinedTextField(
                            value = medications,
                            onValueChange = {
                                onUpdateFields(symptoms, severity, duration, allergies, it, language)
                            },
                            placeholder = { Text("e.g. Paracetamol 500mg") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("medications_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Divider(color = SlateBorder)

                // Table Row 4: Preferred Language
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "6. Preferred Language for Consultation & Translation",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val languages = listOf(
                            "hi" to "हिन्दी (Hindi)",
                            "mr" to "मराठी (Marathi)",
                            "ta" to "தமிழ் (Tamil)",
                            "en" to "English"
                        )
                        languages.forEach { (code, label) ->
                            FilterChip(
                                selected = language == code,
                                onClick = {
                                    onUpdateFields(symptoms, severity, duration, allergies, medications, code)
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.testTag("lang_chip_$code")
                            )
                        }
                    }
                }
            }
        }

        // Triage Evaluation Action Button
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("evaluate_triage_button"),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.HealthAndSafety,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Check Criticality & Execute Triage Management",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        // Animated Triage Result Section
        AnimatedVisibility(visible = latestTriage != null) {
            latestTriage?.let { triage ->
                val isRed = triage.signal == TriageSignal.RED
                val isYellow = triage.signal == TriageSignal.YELLOW

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isRed -> EmergencyRedContainer
                            isYellow -> CautionYellowContainer
                            else -> SuccessGreenContainer
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        2.dp,
                        when {
                            isRed -> EmergencyRed
                            isYellow -> CautionYellow
                            else -> SuccessGreen
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("triage_result_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isRed -> Icons.Default.Warning
                                    isYellow -> Icons.Default.ReportProblem
                                    else -> Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                tint = when {
                                    isRed -> EmergencyRedText
                                    isYellow -> CautionYellowText
                                    else -> SuccessGreenText
                                },
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "TRIAGE SIGNAL: ${triage.signal.name}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = when {
                                        isRed -> EmergencyRedText
                                        isYellow -> CautionYellowText
                                        else -> SuccessGreenText
                                    }
                                )
                                Text(
                                    text = triage.urgencyText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = SlateTextPrimary
                                )
                            }
                        }

                        if (triage.warningFlags.isNotEmpty()) {
                            Text(
                                text = "Detected Warning Phrases: ${triage.warningFlags.joinToString(", ")}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isRed -> EmergencyRedText
                                    isYellow -> CautionYellowText
                                    else -> SlateTextSecondary
                                }
                            )
                        }

                        // Guidance list
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            triage.guidanceTips.forEach { tip ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("•", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = tip,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlateTextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Navigation Button based on Triage
                        if (isRed) {
                            Button(
                                onClick = onNavigateToEmergency,
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("go_to_emergency_button")
                            ) {
                                Icon(Icons.Default.Emergency, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Open Emergency Guidance & Nearby Hospitals",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = onNavigateToTeleconsultation,
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("go_to_teleconsultation_button")
                            ) {
                                Icon(Icons.Default.VideoCall, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Proceed to Doctor Teleconsultation Room",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Direct 'SymptomTriage' composable accessible under com.example.ui.screens as well.
 * Provides a form for users to input symptoms, duration, and allergies,
 * and uses placeholder logic to determine if the symptoms are critical or not.
 */
@Composable
fun SymptomTriage(
    modifier: Modifier = Modifier,
    initialSymptoms: String = "",
    initialDuration: String = "",
    initialAllergies: String = "",
    onNavigateToEmergency: (() -> Unit)? = null,
    onNavigateToTeleconsultation: (() -> Unit)? = null,
    onTriageEvaluated: ((Boolean) -> Unit)? = null
) {
    com.example.ui.components.SymptomTriage(
        modifier = modifier,
        initialSymptoms = initialSymptoms,
        initialDuration = initialDuration,
        initialAllergies = initialAllergies,
        onNavigateToEmergency = onNavigateToEmergency,
        onNavigateToTeleconsultation = onNavigateToTeleconsultation,
        onTriageEvaluated = onTriageEvaluated
    )
}

