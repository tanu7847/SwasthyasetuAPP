package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.model.UserRole
import com.example.ui.theme.*
import com.example.util.onGoogleSignInClicked

enum class AuthTab {
    LOGIN,
    REGISTER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onLogin: (email: String, role: UserRole, displayName: String) -> Unit,
    onRegisterDoctor: (
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
    ) -> Unit,
    onRegisterPatient: (
        name: String,
        email: String,
        phone: String,
        age: Int,
        gender: String,
        language: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var authError by remember { mutableStateOf<String?>(null) }
    var isLoadingGoogle by remember { mutableStateOf(false) }

    var currentAuthTab by remember { mutableStateOf(AuthTab.LOGIN) }
    var selectedRole by remember { mutableStateOf(UserRole.DOCTOR) }

    // Common fields
    var email by remember { mutableStateOf("dr.aakash@swasthyasetu.org") }
    var password by remember { mutableStateOf("DoctorSecure#2026") }
    var fullName by remember { mutableStateOf("Dr. Aakash Verma") }
    var phone by remember { mutableStateOf("+91 94220 54321") }

    // Doctor specific registration fields
    var licenseNumber by remember { mutableStateOf("MMC-2018/04/1829") }
    var qualifications by remember { mutableStateOf("MBBS, MD (Medicine), FCCP") }
    var specialty by remember { mutableStateOf("General Medicine & Critical Care") }
    var certifications by remember { mutableStateOf("NMC Board Certified, ACLS/BLS Fellow") }
    var hospitalAffiliation by remember { mutableStateOf("Apollo Multispeciality & Research Hospital") }
    var experienceYears by remember { mutableStateOf("14") }
    var consultationLanguage by remember { mutableStateOf("en") }
    var licenseCertifiedCheckbox by remember { mutableStateOf(true) }

    // Patient specific registration fields
    var patientAge by remember { mutableStateOf("38") }
    var patientGender by remember { mutableStateOf("Male") }
    var patientLanguage by remember { mutableStateOf("hi") }

    // Admin specific field
    var adminPin by remember { mutableStateOf("ADMIN-7890") }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Logo & Healthcare Portal Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(TealPrimary, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
            Column {
                Text(
                    text = "SwasthyaSetu",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TealPrimaryDark
                )
                Text(
                    text = "National Telemedicine & Clinical Triage Portal",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateTextSecondary
                )
            }
        }

        // Quick 1-Tap Access for Instant Review / Exploration
        Card(
            colors = CardDefaults.cardColors(containerColor = TealPrimaryLight),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "⚡ Instant Quick-Access (Pre-verified Demo Roles):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimaryDark
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {
                            onLogin("dr.aakash@swasthyasetu.org", UserRole.DOCTOR, "Dr. Aakash Verma")
                        },
                        label = { Text("🩺 Doctor", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color.White),
                        modifier = Modifier.testTag("quick_doctor_login")
                    )
                    AssistChip(
                        onClick = {
                            onLogin("patient@swasthyasetu.org", UserRole.PATIENT, "Ramesh Kulkarni")
                        },
                        label = { Text("👤 Patient", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color.White),
                        modifier = Modifier.testTag("quick_patient_login")
                    )
                    AssistChip(
                        onClick = {
                            onLogin("admin@swasthyasetu.org", UserRole.ADMIN, "Chief Medical Administrator")
                        },
                        label = { Text("🛡️ Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color.White),
                        modifier = Modifier.testTag("quick_admin_login")
                    )
                }
            }
        }

        // Primary Form Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Auth Mode Tabs: Login vs Register
                TabRow(
                    selectedTabIndex = currentAuthTab.ordinal,
                    containerColor = SlateSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = currentAuthTab == AuthTab.LOGIN,
                        onClick = { currentAuthTab = AuthTab.LOGIN },
                        text = {
                            Text(
                                "Sign In (Existing)",
                                fontWeight = if (currentAuthTab == AuthTab.LOGIN) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = currentAuthTab == AuthTab.REGISTER,
                        onClick = { currentAuthTab = AuthTab.REGISTER },
                        text = {
                            Text(
                                "Register New Account",
                                fontWeight = if (currentAuthTab == AuthTab.REGISTER) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }

                // Role Selector
                Text(
                    text = "Select Portal Account Type:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        UserRole.DOCTOR to "🩺 Doctor",
                        UserRole.PATIENT to "👤 Patient",
                        UserRole.ADMIN to "🛡️ Admin"
                    ).forEach { (role, label) ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = {
                                selectedRole = role
                                when (role) {
                                    UserRole.DOCTOR -> {
                                        email = "dr.aakash@swasthyasetu.org"
                                        fullName = "Dr. Aakash Verma"
                                    }
                                    UserRole.PATIENT -> {
                                        email = "patient@swasthyasetu.org"
                                        fullName = "Ramesh Kulkarni"
                                    }
                                    UserRole.ADMIN -> {
                                        email = "admin@swasthyasetu.org"
                                        fullName = "Chief Medical Administrator"
                                    }
                                }
                            },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedRole == role) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f).testTag("role_tab_${role.name.lowercase()}")
                        )
                    }
                }

                HorizontalDivider(color = SlateBorder)

                // ==============================
                // DOCTOR REGISTRATION FORM
                // ==============================
                if (currentAuthTab == AuthTab.REGISTER && selectedRole == UserRole.DOCTOR) {
                    Text(
                        text = "Physician & Specialist Credential Registration",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimaryDark
                    )
                    Text(
                        text = "Doctors must provide medical license, state council registration, and board certifications for verification prior to telehealth practice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Legal Name (with Dr. prefix) *") },
                        placeholder = { Text("e.g. Dr. Aakash Verma") },
                        modifier = Modifier.fillMaxWidth().testTag("doctor_reg_name"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = licenseNumber,
                        onValueChange = { licenseNumber = it },
                        label = { Text("State Medical Council / NMC License No. *") },
                        placeholder = { Text("e.g. MMC-2018/04/1829 or MCI-88492") },
                        modifier = Modifier.fillMaxWidth().testTag("doctor_reg_license"),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = {
                            Icon(Icons.Default.Verified, contentDescription = "Valid License", tint = SuccessGreenText)
                        }
                    )

                    OutlinedTextField(
                        value = qualifications,
                        onValueChange = { qualifications = it },
                        label = { Text("Medical Qualifications & Degrees *") },
                        placeholder = { Text("e.g. MBBS, MD (Medicine), FCCP, DNB") },
                        modifier = Modifier.fillMaxWidth().testTag("doctor_reg_qualifications"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = specialty,
                        onValueChange = { specialty = it },
                        label = { Text("Clinical Specialization *") },
                        placeholder = { Text("e.g. Pulmonology, Cardiology, General Medicine") },
                        modifier = Modifier.fillMaxWidth().testTag("doctor_reg_specialty"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = certifications,
                        onValueChange = { certifications = it },
                        label = { Text("Board Certifications & Fellowships") },
                        placeholder = { Text("e.g. Board Certified, ACLS/BLS Certified") },
                        modifier = Modifier.fillMaxWidth().testTag("doctor_reg_certifications"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = hospitalAffiliation,
                            onValueChange = { hospitalAffiliation = it },
                            label = { Text("Hospital / Clinic Affiliation") },
                            modifier = Modifier.weight(1.4f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = experienceYears,
                            onValueChange = { experienceYears = it },
                            label = { Text("Years Exp.") },
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Official Medical Email *") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number *") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Portal Security Password *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = licenseCertifiedCheckbox,
                            onCheckedChange = { licenseCertifiedCheckbox = it }
                        )
                        Text(
                            text = "I solemnly declare that my Medical Council License and clinical certificates are valid and compliant under National Telehealth Guidelines.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextPrimary
                        )
                    }

                    Button(
                        onClick = {
                            if (fullName.isNotBlank() && licenseNumber.isNotBlank() && email.isNotBlank()) {
                                onRegisterDoctor(
                                    fullName,
                                    email,
                                    phone,
                                    licenseNumber,
                                    qualifications,
                                    specialty,
                                    certifications,
                                    hospitalAffiliation,
                                    experienceYears.toIntOrNull() ?: 5,
                                    consultationLanguage
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_doctor_registration"),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = licenseCertifiedCheckbox
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Register Doctor with Verified License", fontWeight = FontWeight.Bold)
                    }
                }

                // ==============================
                // PATIENT REGISTRATION FORM
                // ==============================
                else if (currentAuthTab == AuthTab.REGISTER && selectedRole == UserRole.PATIENT) {
                    Text(
                        text = "Patient Health Account Registration",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimaryDark
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Patient Full Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = patientAge,
                            onValueChange = { patientAge = it },
                            label = { Text("Age") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = patientGender,
                            onValueChange = { patientGender = it },
                            label = { Text("Gender") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Emergency Phone Number *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Account Password *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            if (fullName.isNotBlank() && email.isNotBlank()) {
                                onRegisterPatient(
                                    fullName,
                                    email,
                                    phone,
                                    patientAge.toIntOrNull() ?: 30,
                                    patientGender,
                                    patientLanguage
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_patient_registration"),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Register Patient Account", fontWeight = FontWeight.Bold)
                    }
                }

                // ==============================
                // LOGIN FORM (DOCTOR, PATIENT, ADMIN)
                // ==============================
                else {
                    Text(
                        text = when (selectedRole) {
                            UserRole.DOCTOR -> "Physician Clinical Sign In"
                            UserRole.PATIENT -> "Patient Portal Sign In"
                            UserRole.ADMIN -> "Medical Administrator Sign In"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimaryDark
                    )

                    // Google Sign-In with Credential Manager for Doctor & Patient
                    if (selectedRole != UserRole.ADMIN) {
                        Button(
                            onClick = {
                                isLoadingGoogle = true
                                authError = null
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoadingGoogle = false
                                        val display = if (selectedRole == UserRole.DOCTOR) "Dr. Google Attending" else "Google Verified Patient"
                                        onLogin("google.user@swasthyasetu.org", selectedRole, display)
                                    },
                                    onAuthError = { err ->
                                        isLoadingGoogle = false
                                        authError = err
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoadingGoogle = false
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("google_sign_in_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ClinicalBlue),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isLoadingGoogle
                        ) {
                            if (isLoadingGoogle) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(Icons.Default.AccountCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign in with Google (Firebase Auth)", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (authError != null) {
                            Text(
                                text = authError ?: "",
                                color = EmergencyRedText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = SlateBorder)
                            Text(" or enter credentials ", fontSize = 11.sp, color = SlateTextSecondary, modifier = Modifier.padding(horizontal = 6.dp))
                            HorizontalDivider(modifier = Modifier.weight(1f), color = SlateBorder)
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = {
                            Text(if (selectedRole == UserRole.DOCTOR) "Doctor Email or License No." else "Account Email Address")
                        },
                        modifier = Modifier.fillMaxWidth().testTag("login_email_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password / Passcode") },
                        modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (selectedRole == UserRole.ADMIN) {
                        OutlinedTextField(
                            value = adminPin,
                            onValueChange = { adminPin = it },
                            label = { Text("Admin Authorization Token") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Button(
                        onClick = {
                            onLogin(email, selectedRole, fullName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (selectedRole) {
                                UserRole.DOCTOR -> "Sign In as Doctor (Verified)"
                                UserRole.PATIENT -> "Sign In as Patient"
                                UserRole.ADMIN -> "Enter Admin Dashboard"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
