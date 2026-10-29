package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DigitalPrescription
import com.example.data.model.SpecialistReferral
import com.example.ui.theme.*

@Composable
fun PrescriptionAndReferralScreen(
    prescriptions: List<DigitalPrescription>,
    referrals: List<SpecialistReferral>,
    onSwitchToDoctorToReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Module Title Header
        item {
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
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Module 4: Digital Prescription & Specialist Referrals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimaryDark
                        )
                        Text(
                            text = "Official Hospital Header, Medicine Dosages, Doctor Review Stamp, Signature & Treatment Follow-ups",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary
                        )
                    }
                }
            }
        }

        // Section 1: Digital Prescriptions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Digital Prescriptions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )

                OutlinedButton(
                    onClick = onSwitchToDoctorToReview,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("switch_to_doctor_review_btn")
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Doctor Edit & Review Mode", fontSize = 11.sp)
                }
            }
        }

        if (prescriptions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No final approved prescriptions yet. Complete teleconsultation or switch to Doctor Review mode to finalize.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(prescriptions) { rx ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, if (rx.isFinalApproved) TealPrimary else CautionYellow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prescription_card_${rx.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. HOSPITAL NAME AT TOP
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = rx.hospitalName.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TealPrimaryDark,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Government Registered National Telehealth Unit • NABH Accredited",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateTextSecondary
                            )
                            Text(
                                text = "24x7 Emergency Help: 112 / 108 • rx@swasthyasetu.org",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateTextSecondary
                            )
                        }

                        Divider(color = TealPrimary, thickness = 2.dp)

                        // 2. PATIENT'S PERSONAL INFORMATION
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Patient Name: ${rx.patientName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary
                                )
                                Text(
                                    text = "Age / Gender: ${rx.patientAge} Yrs / ${rx.patientGender}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateTextSecondary
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Rx ID: ${rx.id}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary
                                )
                                Text(
                                    text = "Status: ${if (rx.isFinalApproved) "Doctor Approved ✅" else "Doctor Review Pending ⏳"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rx.isFinalApproved) SuccessGreenText else CautionYellowText
                                )
                            }
                        }

                        Divider(color = SlateBorder)

                        // 3. DIAGNOSED SYMPTOMS
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Clinical Impression & Diagnosed Symptoms:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                            Text(
                                text = "Diagnosis: ${rx.diagnosis}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = "Assessed Symptoms: ${rx.symptoms}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondary
                            )
                        }

                        Divider(color = SlateBorder)

                        // 4. MEDICINE NAME WITH DOSAGE QUANTITY (Rx Table)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "℞",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TealPrimary
                            )
                            Text(
                                text = "Prescribed Medications & Dosages:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateSurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rx.medicines.forEachIndexed { idx, med ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(
                                            text = "${idx + 1}. ${med.name}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SlateTextPrimary
                                        )
                                        Text(
                                            text = "Instructions: ${med.instructions}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SlateTextSecondary
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "Dose: ${med.dosage}",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TealPrimaryDark
                                        )
                                        Text(
                                            text = "${med.frequency} • ${med.duration}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlateTextSecondary
                                        )
                                    }
                                }
                                if (idx < rx.medicines.lastIndex) {
                                    Divider(color = SlateBorder.copy(alpha = 0.5f))
                                }
                            }
                        }

                        // 5. TIPS / GUIDELINES TO FOLLOW
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Healthcare Guidelines & Care Advice:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                            Text(
                                text = rx.guidelines,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = "Follow-up: ${rx.followUp}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ClinicalBlueOnContainer
                            )
                        }

                        Divider(color = SlateBorder)

                        // 6. STAMP OF HOSPITAL AND SIGNATURE OF DOCTOR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Official Hospital Stamp Box
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .border(2.dp, ClinicalBlue, CircleShape)
                                    .rotate(-8f)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "★ SWASTHYA SETU ★",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ClinicalBlue
                                    )
                                    Text(
                                        text = "VERIFIED\nCLINICAL STAMP",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ClinicalBlue,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 11.sp
                                    )
                                    Text(
                                        text = "GOVT COMPLIANT",
                                        fontSize = 7.sp,
                                        color = ClinicalBlue
                                    )
                                }
                            }

                            // Doctor's Signature Block
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Dr. Aakash Verma",
                                    fontFamily = FontFamily.Cursive,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimaryDark
                                )
                                Divider(
                                    color = SlateTextSecondary,
                                    thickness = 1.dp,
                                    modifier = Modifier.width(130.dp)
                                )
                                Text(
                                    text = rx.doctorSignature,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = SlateTextPrimary,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = "Digitally Reviewed & Verified",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SuccessGreenText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Specialist Referrals & Follow-Up Tracking
        item {
            Text(
                text = "Specialist Referrals & Doctor Follow-Up",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary
            )
        }

        if (referrals.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No specialist referrals created yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateTextSecondary
                        )
                    }
                }
            }
        } else {
            items(referrals) { ref ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Specialist: ${ref.specialist}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = TealPrimary
                            )
                            Surface(
                                color = when (ref.status) {
                                    "completed" -> SuccessGreenContainer
                                    "in_treatment" -> CautionYellowContainer
                                    else -> ClinicalBlueContainer
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = ref.status.replace("_", " ").uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = when (ref.status) {
                                        "completed" -> SuccessGreenText
                                        "in_treatment" -> CautionYellowText
                                        else -> ClinicalBlueOnContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Reason for Referral: ${ref.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextPrimary
                        )

                        Text(
                            text = "Recommended Facility: ${ref.facility}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary
                        )

                        Text(
                            text = "Follow-up Scheduled: ${ref.followUpDate}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TealPrimaryDark
                        )

                        if (ref.treatmentNotes.isNotBlank()) {
                            Surface(
                                color = SlateSurfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Doctor Follow-Up Notes: ${ref.treatmentNotes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateTextPrimary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
