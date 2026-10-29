package com.example

import com.example.data.model.TriageSignal
import com.example.data.model.UserRole
import com.example.data.repository.HealthcareRepository
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testCriticalChestPainTriggersRedTriage() {
        val repo = HealthcareRepository()
        val intake = repo.processSymptomIntake(
            symptoms = "I have crushing chest pain and difficulty breathing",
            severity = "severe",
            duration = "1 hour",
            allergies = "None",
            currentMedications = "None",
            language = "en"
        )

        assertEquals(TriageSignal.RED, intake.signal)
        assertTrue(intake.warningFlags.contains("chest pain"))
        assertTrue(intake.warningFlags.contains("difficulty breathing"))
        assertTrue(intake.urgencyText.contains("CRITICAL EMERGENCY"))
    }

    @Test
    fun testRoutineSymptomsTriggerGreenTriage() {
        val repo = HealthcareRepository()
        val intake = repo.processSymptomIntake(
            symptoms = "Mild runny nose, sneezing and slight scratchy throat",
            severity = "mild",
            duration = "2 days",
            allergies = "None",
            currentMedications = "None",
            language = "en"
        )

        assertEquals(TriageSignal.GREEN, intake.signal)
        assertTrue(intake.warningFlags.isEmpty())
        assertTrue(intake.urgencyText.contains("ROUTINE"))
    }

    @Test
    fun testNearbyHospitalsDistanceCalculation() {
        val repo = HealthcareRepository()
        val hospitals = repo.getNearbyHospitals(18.5204, 73.8567)

        assertTrue(hospitals.isNotEmpty())
        assertTrue(hospitals.first().distanceKm > 0.0)
        // Check sorted by closest distance
        for (i in 0 until hospitals.size - 1) {
            assertTrue(hospitals[i].distanceKm <= hospitals[i + 1].distanceKm)
        }
    }

    @Test
    fun testDigitalPrescriptionGenerationAndDoctorApproval() {
        val repo = HealthcareRepository()
        val user = repo.login("patient@swasthyasetu.org", UserRole.PATIENT, "Ramesh Kulkarni")
        val doctor = repo.availableDoctors.first()

        val initialRx = repo.generateInitialPrescription("cons_test_01", user, doctor)
        assertFalse(initialRx.isFinalApproved)
        assertEquals("SwasthyaSetu Multispeciality Telehealth & Research Hospital", initialRx.hospitalName)
        assertTrue(initialRx.medicines.isNotEmpty())

        // Doctor reviews and approves
        val updatedRx = initialRx.copy(doctorSignature = "Dr. Aakash Verma, MD [APPROVED]")
        repo.updateAndApprovePrescription(updatedRx)

        val approvedList = repo.prescriptions.value
        val saved = approvedList.first { it.id == initialRx.id }
        assertTrue(saved.isFinalApproved)
        assertEquals("Dr. Aakash Verma, MD [APPROVED]", saved.doctorSignature)
    }

    @Test
    fun testSpecialistReferralLifecycle() {
        val repo = HealthcareRepository()
        val ref = repo.createReferral(
            patientId = "pat_881",
            patientName = "Ramesh Kulkarni",
            specialist = "Pulmonologist",
            reason = "Chronic asthma evaluation",
            facility = "Apollo Pulmonology Dept",
            followUpDate = "2026-11-20"
        )

        assertEquals("referred", ref.status)

        // Doctor updates to completed
        repo.updateReferralStatus(ref.id, "completed", "Patient completed spirometry and maintenance therapy.")
        val updated = repo.referrals.value.first { it.id == ref.id }
        assertEquals("completed", updated.status)
        assertTrue(updated.treatmentNotes.contains("completed spirometry"))
    }
}
