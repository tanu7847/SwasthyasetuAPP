package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DigitalPrescription
import com.example.data.model.Medicine
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalPrescriptionScreen(
    prescription: DigitalPrescription? = null,
    onSavePrescription: (DigitalPrescription) -> Unit = {},
    onApprovePrescription: (DigitalPrescription) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    isDoctorMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Initial sample fallback for direct preview/testing
    val initialPrescription = remember(prescription) {
        prescription ?: DigitalPrescription(
            id = "RX-2026-9812",
            consultationId = "CONS-7741",
            patientId = "PAT-881",
            patientName = "Ramesh Kulkarni",
            patientAge = 46,
            patientGender = "Male",
            doctorId = "DOC-101",
            doctorName = "Dr. Aakash Verma",
            hospitalName = "SwasthyaSetu Telemedicine & Healthcare Center",
            diagnosis = "Acute Upper Respiratory Tract Infection (URTI) with bronchospasm",
            symptoms = "Persistent dry cough, mild fever (99.8°F), throat irritation for 4 days",
            medicines = listOf(
                Medicine(
                    name = "Tab. Azithromycin 500mg",
                    dosage = "1 tablet (500mg)",
                    frequency = "Once daily (OD)",
                    duration = "3 days",
                    instructions = "After lunch with plenty of water"
                ),
                Medicine(
                    name = "Tab. Paracetamol 650mg",
                    dosage = "1 tablet (650mg)",
                    frequency = "SOS (as needed for fever)",
                    duration = "5 days",
                    instructions = "After food, maximum 3 times a day"
                ),
                Medicine(
                    name = "Syrup Levocetirizine + Ambroxol",
                    dosage = "10 ml",
                    frequency = "Twice daily (BD)",
                    duration = "5 days",
                    instructions = "Morning and night at bedtime"
                )
            ),
            guidelines = "Perform warm saline gargles twice daily. Regular steam inhalation. Stay well hydrated with warm fluids. Avoid cold drinks.",
            followUp = "Review after 5 days if fever exceeds 100°F or if difficulty in breathing occurs.",
            doctorSignature = "Dr. Aakash Verma, MD (Internal Medicine), MMC-Reg: 2018/04/1829",
            hospitalStamp = "SWASHTHYA SETU - VERIFIED TELEHEALTH STAMP",
            isFinalApproved = false,
            createdAt = System.currentTimeMillis()
        )
    }

    var rxState by remember(initialPrescription) { mutableStateOf(initialPrescription) }
    var isVerified by remember(initialPrescription.isFinalApproved) { mutableStateOf(initialPrescription.isFinalApproved) }
    var showAddMedicineDialog by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Dialog state for new medicine
    var newMedName by remember { mutableStateOf("") }
    var newMedDosage by remember { mutableStateOf("") }
    var newMedFrequency by remember { mutableStateOf("Twice daily (BD)") }
    var newMedDuration by remember { mutableStateOf("5 days") }
    var newMedInstructions by remember { mutableStateOf("After meals") }

    val quickDiagnoses = listOf(
        "Acute Viral URTI",
        "Acute Bronchitis",
        "Allergic Rhinitis",
        "Gastroenteritis",
        "Hypertension (Grade 1)",
        "Tension Headache"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp)
            .testTag("digital_prescription_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Navigation & Screen Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prescription_header_banner")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (onNavigateBack != null) {
                                IconButton(
                                    onClick = onNavigateBack,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("prescription_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Prescription",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                            Column {
                                Text(
                                    text = "Digital Prescription (Rx)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "ID: ${rxState.id} • Consult #${rxState.consultationId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        // Status badge
                        Surface(
                            color = if (isVerified) SuccessGreen else CautionYellow,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("prescription_status_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isVerified) Icons.Default.Verified else Icons.Default.Pending,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isVerified) "DOCTOR VERIFIED" else "PENDING REVIEW",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Official Telehealth Medical Prescription with clinical entity extraction and verifiable physician signature.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Success banner
        if (saveSuccessMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SuccessGreenContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_success_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreenText
                        )
                        Text(
                            text = saveSuccessMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SuccessGreenText,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { saveSuccessMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = SuccessGreenText)
                        }
                    }
                }
            }
        }

        // 2. Official Hospital Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hospital_header_section")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TealPrimaryLight,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocalHospital,
                                    contentDescription = "Hospital Icon",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "HOSPITAL / CLINIC FACILITY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                            if (isDoctorMode) {
                                OutlinedTextField(
                                    value = rxState.hospitalName,
                                    onValueChange = {
                                        rxState = rxState.copy(hospitalName = it)
                                        onSavePrescription(rxState)
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("hospital_name_field"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            } else {
                                Text(
                                    text = rxState.hospitalName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary,
                                    modifier = Modifier.testTag("hospital_name_text")
                                )
                            }
                        }
                    }

                    val formattedDate = remember(rxState.createdAt) {
                        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        sdf.format(Date(rxState.createdAt))
                    }
                    Text(
                        text = "Issued Date: $formattedDate • NABH Telemedicine Standard Compliant",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 3. Patient Information Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("patient_info_section")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = ClinicalBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Patient Demographics & Vitals",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                    }

                    Divider(color = SlateBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("PATIENT NAME", fontSize = 10.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                            Text(
                                text = rxState.patientName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary,
                                modifier = Modifier.testTag("patient_name_text")
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("AGE / GENDER", fontSize = 10.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${rxState.patientAge} Years / ${rxState.patientGender}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateTextPrimary,
                                modifier = Modifier.testTag("patient_age_gender_text")
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("PATIENT ID", fontSize = 10.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                            Text(
                                text = rxState.patientId,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateTextPrimary,
                                modifier = Modifier.testTag("patient_id_text")
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("CLINICAL VITALS", fontSize = 10.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                            Text(
                                text = "BP: 120/80 • HR: 74 bpm • SpO2: 98%",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondary,
                                modifier = Modifier.testTag("patient_vitals_text")
                            )
                        }
                    }

                    // Known allergies badge
                    Surface(
                        color = EmergencyRedContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("patient_allergies_box")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Allergy Warning",
                                tint = EmergencyRedText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Known Allergies: Penicillin, Sulfa Drugs (Caution Advised)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = EmergencyRedText,
                                modifier = Modifier.testTag("patient_allergies_text")
                            )
                        }
                    }
                }
            }
        }

        // 4. Diagnosis & Clinical Impression (Editable for Doctor Verification)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("diagnosis_section")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Biotech,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Diagnosis & Clinical Findings",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )
                        }
                        if (isDoctorMode) {
                            Text(
                                text = "Doctor Editable",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealPrimary
                            )
                        }
                    }

                    Text(
                        text = "Primary Diagnosis",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextSecondary
                    )
                    OutlinedTextField(
                        value = rxState.diagnosis,
                        onValueChange = {
                            rxState = rxState.copy(diagnosis = it)
                            onSavePrescription(rxState)
                        },
                        placeholder = { Text("Enter formal diagnosis or clinical impression") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("diagnosis_field"),
                        shape = RoundedCornerShape(8.dp),
                        readOnly = !isDoctorMode
                    )

                    // Quick Diagnosis Chips
                    if (isDoctorMode) {
                        Text(
                            text = "Quick Clinical Templates:",
                            fontSize = 11.sp,
                            color = SlateTextSecondary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickDiagnoses) { template ->
                                SuggestionChip(
                                    onClick = {
                                        rxState = rxState.copy(diagnosis = template)
                                        onSavePrescription(rxState)
                                    },
                                    label = { Text(template, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reported Symptoms / Chief Complaints",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextSecondary
                    )
                    OutlinedTextField(
                        value = rxState.symptoms,
                        onValueChange = {
                            rxState = rxState.copy(symptoms = it)
                            onSavePrescription(rxState)
                        },
                        placeholder = { Text("Chief symptoms and duration") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("symptoms_field"),
                        shape = RoundedCornerShape(8.dp),
                        readOnly = !isDoctorMode
                    )
                }
            }
        }

        // 5. Medicine Dosages Section (with Editable fields & Add/Delete)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medicine_dosages_section")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TealPrimaryLight,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "℞",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TealPrimary
                                    )
                                }
                            }
                            Text(
                                text = "Prescribed Medicines & Dosages (${rxState.medicines.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )
                        }

                        if (isDoctorMode) {
                            FilledTonalButton(
                                onClick = {
                                    newMedName = ""
                                    newMedDosage = "1 tablet (500mg)"
                                    newMedFrequency = "Twice daily (BD)"
                                    newMedDuration = "5 days"
                                    newMedInstructions = "After meals"
                                    showAddMedicineDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("add_medicine_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Medicine", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (rxState.medicines.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No medicines added yet. Tap 'Add Medicine' above.",
                                color = SlateTextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Editable list of medicines
        itemsIndexed(rxState.medicines) { index, med ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medicine_item_$index")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header row with medicine title and delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TealPrimary,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Medicine Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TealPrimaryDark
                            )
                        }

                        if (isDoctorMode) {
                            IconButton(
                                onClick = {
                                    val updated = rxState.medicines.toMutableList()
                                    if (index in updated.indices) {
                                        updated.removeAt(index)
                                        rxState = rxState.copy(medicines = updated)
                                        onSavePrescription(rxState)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("delete_medicine_btn_$index")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Medicine",
                                    tint = EmergencyRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Medicine Name Input
                    Text("Medicine Name & Form", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                    OutlinedTextField(
                        value = med.name,
                        onValueChange = { newName ->
                            val updated = rxState.medicines.toMutableList()
                            updated[index] = med.copy(name = newName)
                            rxState = rxState.copy(medicines = updated)
                            onSavePrescription(rxState)
                        },
                        placeholder = { Text("e.g. Tab. Paracetamol 650mg") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("medicine_name_$index"),
                        shape = RoundedCornerShape(8.dp),
                        readOnly = !isDoctorMode
                    )

                    // Dosage & Frequency row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dosage Quantity", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            OutlinedTextField(
                                value = med.dosage,
                                onValueChange = { newDosage ->
                                    val updated = rxState.medicines.toMutableList()
                                    updated[index] = med.copy(dosage = newDosage)
                                    rxState = rxState.copy(medicines = updated)
                                    onSavePrescription(rxState)
                                },
                                placeholder = { Text("e.g. 1 Tab") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("medicine_dosage_$index"),
                                shape = RoundedCornerShape(8.dp),
                                readOnly = !isDoctorMode
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Frequency", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            OutlinedTextField(
                                value = med.frequency,
                                onValueChange = { newFreq ->
                                    val updated = rxState.medicines.toMutableList()
                                    updated[index] = med.copy(frequency = newFreq)
                                    rxState = rxState.copy(medicines = updated)
                                    onSavePrescription(rxState)
                                },
                                placeholder = { Text("e.g. Once daily") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("medicine_frequency_$index"),
                                shape = RoundedCornerShape(8.dp),
                                readOnly = !isDoctorMode
                            )
                        }
                    }

                    // Duration & Instructions row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Duration", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            OutlinedTextField(
                                value = med.duration,
                                onValueChange = { newDur ->
                                    val updated = rxState.medicines.toMutableList()
                                    updated[index] = med.copy(duration = newDur)
                                    rxState = rxState.copy(medicines = updated)
                                    onSavePrescription(rxState)
                                },
                                placeholder = { Text("e.g. 5 days") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("medicine_duration_$index"),
                                shape = RoundedCornerShape(8.dp),
                                readOnly = !isDoctorMode
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Instructions / Timing", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            OutlinedTextField(
                                value = med.instructions,
                                onValueChange = { newInst ->
                                    val updated = rxState.medicines.toMutableList()
                                    updated[index] = med.copy(instructions = newInst)
                                    rxState = rxState.copy(medicines = updated)
                                    onSavePrescription(rxState)
                                },
                                placeholder = { Text("e.g. After meals") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("medicine_instructions_$index"),
                                shape = RoundedCornerShape(8.dp),
                                readOnly = !isDoctorMode
                            )
                        }
                    }
                }
            }
        }

        // 6. Guidelines & Follow-Up Advice
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("guidelines_section")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Guidelines, Diet & Homecare Tips",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                    }

                    OutlinedTextField(
                        value = rxState.guidelines,
                        onValueChange = {
                            rxState = rxState.copy(guidelines = it)
                            onSavePrescription(rxState)
                        },
                        placeholder = { Text("Enter patient lifestyle, dietary precautions and homecare instructions") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("guidelines_field"),
                        shape = RoundedCornerShape(8.dp),
                        readOnly = !isDoctorMode
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Follow-up Advice",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextSecondary
                    )
                    OutlinedTextField(
                        value = rxState.followUp,
                        onValueChange = {
                            rxState = rxState.copy(followUp = it)
                            onSavePrescription(rxState)
                        },
                        placeholder = { Text("e.g. Review in OPD or Teleconsult after 5 days") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("follow_up_field"),
                        shape = RoundedCornerShape(8.dp),
                        readOnly = !isDoctorMode
                    )
                }
            }
        }

        // 7. Mock Doctor's Signature & Hospital Stamp Component
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, if (isVerified) SuccessGreen else CautionYellow),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("doctor_signature_section")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Physician Authentication & Seal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )

                        // Doctor verification switch
                        if (isDoctorMode) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.testTag("doctor_verification_row")
                            ) {
                                Text(
                                    text = if (isVerified) "Verified" else "Verify",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isVerified) SuccessGreenText else CautionYellowText
                                )
                                Switch(
                                    checked = isVerified,
                                    onCheckedChange = { checked ->
                                        isVerified = checked
                                        rxState = rxState.copy(isFinalApproved = checked)
                                        if (checked) {
                                            onApprovePrescription(rxState)
                                            saveSuccessMessage = "Prescription verified & signed successfully!"
                                        } else {
                                            onSavePrescription(rxState)
                                        }
                                    },
                                    modifier = Modifier.testTag("doctor_verification_switch")
                                )
                            }
                        }
                    }

                    // Signature & Stamp Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Official Hospital Stamp Badge
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .rotate(-8f)
                                .border(2.dp, if (isVerified) SuccessGreen else CautionYellow, CircleShape)
                                .padding(4.dp)
                                .testTag("hospital_stamp_box"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isVerified) Icons.Default.VerifiedUser else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isVerified) SuccessGreen else CautionYellow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "SWASTHYA SETU",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isVerified) SuccessGreenText else CautionYellowText,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (isVerified) "OFFICIAL SEAL" else "DRAFT SEAL",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isVerified) SuccessGreenText else CautionYellowText,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "E-Rx VERIFIED",
                                    fontSize = 6.sp,
                                    color = SlateTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Mock Doctor Signature Visual Canvas & Info
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            // Canvas drawing mock cursive signature curves
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White)
                                    .border(1.dp, SlateBorder, RoundedCornerShape(4.dp))
                                    .testTag("doctor_signature_canvas"),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeColor = if (isVerified) Color(0xFF0369A1) else Color(0xFF475569)
                                    val path = Path().apply {
                                        // Stylized cursive doctor signature flourish
                                        moveTo(size.width * 0.15f, size.height * 0.65f)
                                        quadraticBezierTo(
                                            size.width * 0.25f, size.height * 0.15f,
                                            size.width * 0.35f, size.height * 0.55f
                                        )
                                        quadraticBezierTo(
                                            size.width * 0.42f, size.height * 0.85f,
                                            size.width * 0.50f, size.height * 0.40f
                                        )
                                        quadraticBezierTo(
                                            size.width * 0.60f, size.height * 0.10f,
                                            size.width * 0.70f, size.height * 0.60f
                                        )
                                        lineTo(size.width * 0.85f, size.height * 0.55f)
                                        // Flourish loop
                                        quadraticBezierTo(
                                            size.width * 0.65f, size.height * 0.90f,
                                            size.width * 0.90f, size.height * 0.75f
                                        )
                                    }
                                    drawPath(
                                        path = path,
                                        color = strokeColor,
                                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }

                                Text(
                                    text = rxState.doctorName,
                                    fontStyle = FontStyle.Italic,
                                    fontFamily = FontFamily.Cursive,
                                    fontSize = 17.sp,
                                    color = ClinicalBlueOnContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("doctor_signature_rendered_name")
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = rxState.doctorSignature,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.testTag("doctor_signature_text")
                            )
                            Text(
                                text = "Authorized Medical Practitioner",
                                fontSize = 10.sp,
                                color = SlateTextSecondary,
                                textAlign = TextAlign.End
                            )
                        }
                    }

                    // Doctor signature details edit field
                    if (isDoctorMode) {
                        Text(
                            text = "Doctor Registration & Signature Credentials",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateTextSecondary
                        )
                        OutlinedTextField(
                            value = rxState.doctorSignature,
                            onValueChange = {
                                rxState = rxState.copy(doctorSignature = it)
                                onSavePrescription(rxState)
                            },
                            placeholder = { Text("Dr Name, Degree, Registration Number") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("doctor_signature_field"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // 8. Doctor Verification and Action Buttons
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isDoctorMode) {
                    Button(
                        onClick = {
                            isVerified = true
                            rxState = rxState.copy(isFinalApproved = true)
                            onApprovePrescription(rxState)
                            saveSuccessMessage = "Prescription verified, officially signed & synchronized with database!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("verify_prescription_button")
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Verify & Officially Sign Prescription",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onSavePrescription(rxState)
                            saveSuccessMessage = "Draft saved to patient record successfully."
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_prescription_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Draft", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            saveSuccessMessage = "Exporting official PDF with hospital seal & QR code..."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ClinicalBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export / Share PDF", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Add Medicine Dialog
    if (showAddMedicineDialog) {
        AlertDialog(
            onDismissRequest = { showAddMedicineDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Medication, contentDescription = null, tint = TealPrimary)
                    Text("Add Prescribed Medicine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newMedName,
                        onValueChange = { newMedName = it },
                        label = { Text("Medicine Name (e.g. Tab. Amoxicillin)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_med_name_input")
                    )
                    OutlinedTextField(
                        value = newMedDosage,
                        onValueChange = { newMedDosage = it },
                        label = { Text("Dosage (e.g. 500mg / 1 tablet)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_med_dosage_input")
                    )
                    OutlinedTextField(
                        value = newMedFrequency,
                        onValueChange = { newMedFrequency = it },
                        label = { Text("Frequency (e.g. TDS / 3 times daily)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_med_frequency_input")
                    )
                    OutlinedTextField(
                        value = newMedDuration,
                        onValueChange = { newMedDuration = it },
                        label = { Text("Duration (e.g. 5 days)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_med_duration_input")
                    )
                    OutlinedTextField(
                        value = newMedInstructions,
                        onValueChange = { newMedInstructions = it },
                        label = { Text("Instructions (e.g. After meals)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_med_instructions_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMedName.isNotBlank()) {
                            val newMed = Medicine(
                                name = newMedName.trim(),
                                dosage = if (newMedDosage.isNotBlank()) newMedDosage.trim() else "1 Tab",
                                frequency = if (newMedFrequency.isNotBlank()) newMedFrequency.trim() else "Twice daily",
                                duration = if (newMedDuration.isNotBlank()) newMedDuration.trim() else "5 days",
                                instructions = if (newMedInstructions.isNotBlank()) newMedInstructions.trim() else "After meals"
                            )
                            rxState = rxState.copy(medicines = rxState.medicines + newMed)
                            onSavePrescription(rxState)
                            showAddMedicineDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_confirm_add_med_btn")
                ) {
                    Text("Add to Prescription")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddMedicineDialog = false },
                    modifier = Modifier.testTag("dialog_cancel_add_med_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
