package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DoctorTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorWorkspaceScreen(
    currentTab: DoctorTab,
    onTabSelected: (DoctorTab) -> Unit,
    consultations: List<Consultation>,
    editingPrescription: DigitalPrescription?,
    referrals: List<SpecialistReferral>,
    onUpdatePrescription: (DigitalPrescription) -> Unit,
    onApprovePrescription: () -> Unit,
    onAddMedicine: (Medicine) -> Unit,
    onRemoveMedicine: (Int) -> Unit,
    onCreateReferral: (specialist: String, reason: String, facility: String, followUpDate: String) -> Unit,
    onUpdateReferralStatus: (referralId: String, newStatus: String, notes: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Clinician Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = TealPrimary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalInformation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Doctor Clinical Command Center",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Review AI drafts, edit medicine dosages, approve digital prescriptions, and track specialist referrals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Module Tabs
        ScrollableTabRow(
            selectedTabIndex = currentTab.ordinal,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
        ) {
            DoctorTab.entries.forEach { tab ->
                val label = when (tab) {
                    DoctorTab.PATIENT_QUEUE -> "1. Patient Queue"
                    DoctorTab.CONSULTATION_ASSIST -> "2. Clinical Assist"
                    DoctorTab.PRESCRIPTION_EDITOR -> "3. Review Rx (Edit)"
                    DoctorTab.REFERRAL_FOLLOWUP -> "4. Referral & Follow-up"
                }
                Tab(
                    selected = currentTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (currentTab) {
            DoctorTab.PATIENT_QUEUE -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text(
                            text = "Incoming Patient Consultation Requests",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (consultations.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("No pending patient requests right now.", color = SlateTextSecondary)
                                }
                            }
                        }
                    } else {
                        itemsIndexed(consultations) { _, c ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(c.patientName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        Surface(
                                            color = TealPrimaryLight,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                c.status.uppercase(),
                                                color = TealPrimaryDark,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text("Reason: ${c.reason}", style = MaterialTheme.typography.bodySmall, color = SlateTextPrimary)
                                    Text("Language: ${c.language.uppercase()} • Room: ${c.roomId.take(14)}", style = MaterialTheme.typography.labelSmall, color = SlateTextSecondary)

                                    Button(
                                        onClick = { onTabSelected(DoctorTab.PRESCRIPTION_EDITOR) },
                                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open & Review Digital Prescription", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            DoctorTab.CONSULTATION_ASSIST -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = TealPrimary)
                                    Text(
                                        "Realtime Clinical Decision Assistance (Groq API)",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TealPrimaryDark
                                    )
                                }
                                Text(
                                    "AI acts strictly as a clinical assistant to extract symptoms, drug interactions, and suggest diagnostic questions. Doctor holds ultimate diagnostic & legal authority.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateTextSecondary
                                )
                                Divider(color = SlateBorder)
                                Text("Suggested Diagnostic Checks:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                listOf(
                                    "Rule out bronchospasm if chest tightness accompanies dry cough.",
                                    "Cross-verify Penicillin allergy before prescribing Beta-lactam antibiotic alternatives.",
                                    "Examine marked OpenCV/CNN lesion image before prescribing topical antibiotics."
                                ).forEach {
                                    Text("• $it", style = MaterialTheme.typography.bodySmall, color = SlateTextPrimary)
                                }
                            }
                        }
                    }
                }
            }

            DoctorTab.PRESCRIPTION_EDITOR -> {
                // Editable Prescription Section
                if (editingPrescription == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No active prescription to review. Select a patient first.", color = SlateTextSecondary)
                    }
                } else {
                    var rxState by remember(editingPrescription) { mutableStateOf(editingPrescription) }

                    // Add new medicine dialog state
                    var showAddMedDialog by remember { mutableStateOf(false) }
                    var newMedName by remember { mutableStateOf("") }
                    var newMedDose by remember { mutableStateOf("") }
                    var newMedFreq by remember { mutableStateOf("Once daily (OD)") }
                    var newMedDur by remember { mutableStateOf("3 days") }
                    var newMedInst by remember { mutableStateOf("After meals") }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CautionYellowContainer),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = CautionYellowText)
                                    Text(
                                        text = "Doctor Edit Mode: You can change medicine names, modify dosage quantities, and verify diagnosis before signing.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CautionYellowText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        item {
                            Text("Hospital & Clinic Header", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            OutlinedTextField(
                                value = rxState.hospitalName,
                                onValueChange = {
                                    rxState = rxState.copy(hospitalName = it)
                                    onUpdatePrescription(rxState)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        item {
                            Text("Diagnosis & Clinical Impression", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            OutlinedTextField(
                                value = rxState.diagnosis,
                                onValueChange = {
                                    rxState = rxState.copy(diagnosis = it)
                                    onUpdatePrescription(rxState)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Medicines & Dosages (${rxState.medicines.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                OutlinedButton(
                                    onClick = { showAddMedDialog = true },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Medicine", fontSize = 11.sp)
                                }
                            }
                        }

                        // Editable Medicine Rows
                        itemsIndexed(rxState.medicines) { idx, med ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(10.dp),
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${idx + 1}. Medicine Name", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TealPrimary)
                                        IconButton(
                                            onClick = { onRemoveMedicine(idx) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = EmergencyRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    OutlinedTextField(
                                        value = med.name,
                                        onValueChange = { newName ->
                                            val updated = rxState.medicines.toMutableList()
                                            updated[idx] = med.copy(name = newName)
                                            rxState = rxState.copy(medicines = updated)
                                            onUpdatePrescription(rxState)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(6.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Dosage Quantity", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            OutlinedTextField(
                                                value = med.dosage,
                                                onValueChange = { newDose ->
                                                    val updated = rxState.medicines.toMutableList()
                                                    updated[idx] = med.copy(dosage = newDose)
                                                    rxState = rxState.copy(medicines = updated)
                                                    onUpdatePrescription(rxState)
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Frequency", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            OutlinedTextField(
                                                value = med.frequency,
                                                onValueChange = { newFreq ->
                                                    val updated = rxState.medicines.toMutableList()
                                                    updated[idx] = med.copy(frequency = newFreq)
                                                    rxState = rxState.copy(medicines = updated)
                                                    onUpdatePrescription(rxState)
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("Guidelines & Advice", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            OutlinedTextField(
                                value = rxState.guidelines,
                                onValueChange = {
                                    rxState = rxState.copy(guidelines = it)
                                    onUpdatePrescription(rxState)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        item {
                            Text("Doctor Signature & Stamp Declaration", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            OutlinedTextField(
                                value = rxState.doctorSignature,
                                onValueChange = {
                                    rxState = rxState.copy(doctorSignature = it)
                                    onUpdatePrescription(rxState)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        item {
                            Button(
                                onClick = onApprovePrescription,
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("doctor_approve_prescription_btn")
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Approve & Officially Sign Digital Prescription", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Add Medicine Dialog
                    if (showAddMedDialog) {
                        AlertDialog(
                            onDismissRequest = { showAddMedDialog = false },
                            title = { Text("Add Medicine Row") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = newMedName, onValueChange = { newMedName = it }, label = { Text("Medicine Name") })
                                    OutlinedTextField(value = newMedDose, onValueChange = { newMedDose = it }, label = { Text("Dosage (e.g. 1 Tab)") })
                                    OutlinedTextField(value = newMedFreq, onValueChange = { newMedFreq = it }, label = { Text("Frequency (e.g. Once daily)") })
                                    OutlinedTextField(value = newMedDur, onValueChange = { newMedDur = it }, label = { Text("Duration (e.g. 5 days)") })
                                    OutlinedTextField(value = newMedInst, onValueChange = { newMedInst = it }, label = { Text("Instructions") })
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    if (newMedName.isNotBlank()) {
                                        onAddMedicine(Medicine(newMedName, newMedDose, newMedFreq, newMedDur, newMedInst))
                                        showAddMedDialog = false
                                        newMedName = ""
                                    }
                                }) {
                                    Text("Add")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddMedDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }

            DoctorTab.REFERRAL_FOLLOWUP -> {
                // Referral Creator & Treatment Follow-up
                var specialist by remember { mutableStateOf("Pulmonologist / Chest Specialist") }
                var reason by remember { mutableStateOf("Persistent wheezing, requiring Spirometry and specialized pulmonary follow-up.") }
                var facility by remember { mutableStateOf("Ruby Hall Clinic Pulmonology Wing") }
                var followUpDate by remember { mutableStateOf("2026-11-12") }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Create Specialist Referral for Patient",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TealPrimary
                                )

                                OutlinedTextField(
                                    value = specialist,
                                    onValueChange = { specialist = it },
                                    label = { Text("Specialist Field") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                OutlinedTextField(
                                    value = reason,
                                    onValueChange = { reason = it },
                                    label = { Text("Clinical Reason for Referral") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                OutlinedTextField(
                                    value = facility,
                                    onValueChange = { facility = it },
                                    label = { Text("Recommended Facility / Hospital") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                OutlinedTextField(
                                    value = followUpDate,
                                    onValueChange = { followUpDate = it },
                                    label = { Text("Follow-up Target Date") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Button(
                                    onClick = {
                                        onCreateReferral(specialist, reason, facility, followUpDate)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("create_referral_btn")
                                ) {
                                    Icon(Icons.Default.ShareLocation, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Issue Specialist Referral", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Track Patient Referral Treatment Completion:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    itemsIndexed(referrals) { _, ref ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(ref.specialist, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text("Target: ${ref.followUpDate}", fontSize = 11.sp, color = SlateTextSecondary)
                                }
                                Text("Patient: ${ref.patientName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("Reason: ${ref.reason}", style = MaterialTheme.typography.bodySmall, color = SlateTextPrimary)

                                Text("Update Treatment Status:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TealPrimary)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("referred", "in_treatment", "completed").forEach { st ->
                                        FilterChip(
                                            selected = ref.status == st,
                                            onClick = {
                                                onUpdateReferralStatus(ref.id, st, "Doctor confirmed treatment status: $st on ${java.time.LocalDate.now()}")
                                            },
                                            label = { Text(st.replace("_", " "), fontSize = 10.sp) }
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
}
