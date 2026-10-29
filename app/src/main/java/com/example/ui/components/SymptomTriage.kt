package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Result data class for the Symptom Triage evaluation.
 */
data class TriageEvaluation(
    val isCritical: Boolean,
    val matchedFlags: List<String> = emptyList(),
    val summaryTitle: String,
    val description: String,
    val recommendedAction: String
)

/**
 * Placeholder evaluation logic to determine if symptoms, duration, or allergies
 * indicate a critical emergency or non-critical condition.
 */
object SymptomTriagePlaceholder {
    private val criticalKeywords = listOf(
        "chest pain",
        "difficulty breathing",
        "shortness of breath",
        "cannot breathe",
        "unconscious",
        "loss of consciousness",
        "severe bleeding",
        "stroke",
        "heart attack",
        "seizure",
        "anaphylaxis",
        "choking",
        "cyanosis",
        "bluish lips",
        "paralysis",
        "sudden numbness",
        "severe head injury",
        "coughing blood"
    )

    private val criticalAllergyTerms = listOf(
        "anaphylaxis",
        "throat swelling",
        "airway closing",
        "severe swelling"
    )

    /**
     * Placeholder triage function evaluating inputs.
     */
    fun evaluate(
        symptoms: String,
        duration: String = "",
        allergies: String = ""
    ): TriageEvaluation {
        val sLower = symptoms.trim().lowercase()
        val dLower = duration.trim().lowercase()
        val aLower = allergies.trim().lowercase()

        val foundSymptomFlags = criticalKeywords.filter { sLower.contains(it) }
        val foundAllergyFlags = criticalAllergyTerms.filter { aLower.contains(it) }

        val isSuddenSeverePain = (sLower.contains("pain") || sLower.contains("severe")) &&
                (dLower.contains("sudden") || dLower.contains("acute") || dLower.contains("immediate"))

        val isCritical = foundSymptomFlags.isNotEmpty() ||
                foundAllergyFlags.isNotEmpty() ||
                isSuddenSeverePain

        val allTriggers = (foundSymptomFlags + foundAllergyFlags).distinct()

        return if (isCritical) {
            TriageEvaluation(
                isCritical = true,
                matchedFlags = allTriggers,
                summaryTitle = "CRITICAL CONDITION DETECTED",
                description = "Patient inputs exhibit high-risk emergency markers requiring immediate clinical intervention.",
                recommendedAction = "Do not wait. Dispatch or call emergency ambulance (112 / 108) and proceed directly to the nearest trauma/emergency hospital."
            )
        } else {
            TriageEvaluation(
                isCritical = false,
                matchedFlags = emptyList(),
                summaryTitle = "NON-CRITICAL SYMPTOMS",
                description = "Symptoms appear stable without acute life-threatening triage flags.",
                recommendedAction = "Suitable for teleconsultation with our attending doctors or primary homecare and symptom observation."
            )
        }
    }
}

/**
 * Stateful 'SymptomTriage' composable providing an intake form for:
 * 1. Symptoms
 * 2. Duration
 * 3. Allergies
 *
 * Uses placeholder logic to evaluate and clearly display whether symptoms are critical or not.
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
    var symptoms by remember { mutableStateOf(initialSymptoms) }
    var duration by remember { mutableStateOf(initialDuration) }
    var allergies by remember { mutableStateOf(initialAllergies) }
    var triageEvaluation by remember { mutableStateOf<TriageEvaluation?>(null) }
    var inputError by remember { mutableStateOf<String?>(null) }

    SymptomTriage(
        symptoms = symptoms,
        duration = duration,
        allergies = allergies,
        onSymptomsChange = {
            symptoms = it
            inputError = null
        },
        onDurationChange = {
            duration = it
            inputError = null
        },
        onAllergiesChange = { allergies = it },
        triageEvaluation = triageEvaluation,
        inputError = inputError,
        onEvaluate = {
            if (symptoms.isBlank()) {
                inputError = "Please describe the symptoms before evaluating."
            } else {
                inputError = null
                val result = SymptomTriagePlaceholder.evaluate(
                    symptoms = symptoms,
                    duration = duration,
                    allergies = allergies
                )
                triageEvaluation = result
                onTriageEvaluated?.invoke(result.isCritical)
            }
        },
        onNavigateToEmergency = onNavigateToEmergency,
        onNavigateToTeleconsultation = onNavigateToTeleconsultation,
        onApplyPreset = { presetSymptoms, presetDuration, presetAllergies ->
            symptoms = presetSymptoms
            duration = presetDuration
            allergies = presetAllergies
            inputError = null
            val result = SymptomTriagePlaceholder.evaluate(
                symptoms = presetSymptoms,
                duration = presetDuration,
                allergies = presetAllergies
            )
            triageEvaluation = result
            onTriageEvaluated?.invoke(result.isCritical)
        },
        modifier = modifier
    )
}

/**
 * Stateless / Controlled 'SymptomTriage' composable.
 */
@Composable
fun SymptomTriage(
    symptoms: String,
    duration: String,
    allergies: String,
    onSymptomsChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onAllergiesChange: (String) -> Unit,
    triageEvaluation: TriageEvaluation?,
    onEvaluate: () -> Unit,
    modifier: Modifier = Modifier,
    inputError: String? = null,
    onApplyPreset: ((symptoms: String, duration: String, allergies: String) -> Unit)? = null,
    onNavigateToEmergency: (() -> Unit)? = null,
    onNavigateToTeleconsultation: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("symptom_triage_container"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TealPrimaryLight,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = "Triage Icon",
                            tint = TealPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Symptom Triage Assessment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = "Intake symptoms, duration & allergies for automated criticality triage",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary
                    )
                }
            }

            // Quick Preset Selection (for fast testing / accessibility)
            if (onApplyPreset != null) {
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            onApplyPreset(
                                "Severe crushing chest pain and shortness of breath",
                                "30 minutes (sudden)",
                                "Aspirin"
                            )
                        },
                        label = { Text("🚨 Critical (Chest Pain)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(containerColor = EmergencyRedContainer),
                        modifier = Modifier.testTag("preset_critical_chip")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            onApplyPreset(
                                "Mild cough, runny nose and slight scratchy throat",
                                "3 days",
                                "None"
                            )
                        },
                        label = { Text("🩺 Non-Critical (Cold)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(containerColor = ClinicalBlueContainer),
                        modifier = Modifier.testTag("preset_non_critical_chip")
                    )
                }
            }

            HorizontalDivider(color = SlateBorder)

            // Form Field 1: Symptoms
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "1. Symptoms *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary
                )
                OutlinedTextField(
                    value = symptoms,
                    onValueChange = onSymptomsChange,
                    placeholder = {
                        Text("e.g. Chest pain, difficulty breathing, rash, fever, headache...")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("symptoms_input"),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp),
                    isError = inputError != null
                )
                if (inputError != null) {
                    Text(
                        text = inputError,
                        style = MaterialTheme.typography.bodySmall,
                        color = EmergencyRedText
                    )
                }
            }

            // Form Field 2: Duration
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "2. Duration *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = onDurationChange,
                    placeholder = {
                        Text("e.g. 2 hours, 3 days, sudden onset...")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("duration_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Form Field 3: Allergies
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "3. Allergies",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary
                )
                OutlinedTextField(
                    value = allergies,
                    onValueChange = onAllergiesChange,
                    placeholder = {
                        Text("e.g. Penicillin, Peanuts, Sulfa, None...")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("allergies_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Action: Evaluate Triage Button
            Button(
                onClick = onEvaluate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("evaluate_triage_button"),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Troubleshoot,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Evaluate Criticality",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            // Triage Result Section (Placeholder Logic Outcome)
            AnimatedVisibility(
                visible = triageEvaluation != null,
                enter = fadeIn() + slideInVertically()
            ) {
                triageEvaluation?.let { result ->
                    val isCrit = result.isCritical
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("triage_result_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCrit) EmergencyRedContainer else SuccessGreenContainer
                        ),
                        border = BorderStroke(
                            width = 2.dp,
                            color = if (isCrit) EmergencyRed else SuccessGreen
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCrit) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isCrit) EmergencyRedText else SuccessGreenText,
                                    modifier = Modifier.size(26.dp)
                                )
                                Column {
                                    Text(
                                        text = result.summaryTitle,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = if (isCrit) EmergencyRedText else SuccessGreenText
                                    )
                                    Text(
                                        text = if (isCrit) "Classification: CRITICAL EMERGENCY" else "Classification: NON-CRITICAL / ROUTINE",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = SlateTextPrimary
                                    )
                                }
                            }

                            if (result.matchedFlags.isNotEmpty()) {
                                Text(
                                    text = "Detected Critical Flags: ${result.matchedFlags.joinToString(", ")}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCrit) EmergencyRedText else SlateTextPrimary
                                )
                            }

                            Text(
                                text = result.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextPrimary
                            )

                            Text(
                                text = "Recommendation: ${result.recommendedAction}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCrit) EmergencyRedText else SuccessGreenText
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Navigation CTA depending on triage criticality
                            if (isCrit && onNavigateToEmergency != null) {
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
                                        text = "Emergency Guidance & Nearby Hospitals",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            } else if (!isCrit && onNavigateToTeleconsultation != null) {
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
                                        text = "Proceed to Teleconsultation Room",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
