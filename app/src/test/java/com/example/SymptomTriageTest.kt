package com.example

import com.example.ui.components.SymptomTriagePlaceholder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SymptomTriageTest {

    @Test
    fun testCriticalSymptomEvaluationWithChestPain() {
        val result = SymptomTriagePlaceholder.evaluate(
            symptoms = "Sudden severe chest pain with shortness of breath",
            duration = "30 minutes",
            allergies = "None"
        )
        assertTrue("Expected symptoms to be evaluated as critical", result.isCritical)
        assertTrue(result.matchedFlags.contains("chest pain") || result.matchedFlags.contains("shortness of breath"))
        assertTrue(result.summaryTitle.contains("CRITICAL"))
    }

    @Test
    fun testCriticalSymptomEvaluationWithDifficultyBreathing() {
        val result = SymptomTriagePlaceholder.evaluate(
            symptoms = "Patient is coughing blood and experiencing difficulty breathing",
            duration = "1 hour",
            allergies = "Sulfa"
        )
        assertTrue(result.isCritical)
        assertTrue(result.matchedFlags.contains("difficulty breathing") || result.matchedFlags.contains("coughing blood"))
    }

    @Test
    fun testCriticalAllergyEvaluationWithAnaphylaxis() {
        val result = SymptomTriagePlaceholder.evaluate(
            symptoms = "Mild hives and lip itching",
            duration = "10 minutes",
            allergies = "Anaphylaxis to peanuts"
        )
        assertTrue(result.isCritical)
        assertTrue(result.matchedFlags.contains("anaphylaxis"))
    }

    @Test
    fun testNonCriticalSymptomEvaluationWithMildCold() {
        val result = SymptomTriagePlaceholder.evaluate(
            symptoms = "Mild runny nose, sneezing and mild throat tickle",
            duration = "3 days",
            allergies = "None"
        )
        assertFalse("Expected mild symptoms to be evaluated as non-critical", result.isCritical)
        assertTrue(result.matchedFlags.isEmpty())
        assertTrue(result.summaryTitle.contains("NON-CRITICAL"))
        assertTrue(result.recommendedAction.contains("teleconsultation", ignoreCase = true))
    }

    @Test
    fun testNonCriticalSymptomEvaluationWithMildHeadache() {
        val result = SymptomTriagePlaceholder.evaluate(
            symptoms = "Mild tension headache after long screen time",
            duration = "2 hours",
            allergies = "No known drug allergies"
        )
        assertFalse(result.isCritical)
        assertFalse(result.isCritical)
    }
}
