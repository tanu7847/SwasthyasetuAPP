package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.screens.TeleconsultationScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TeleconsultationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTeleconsultationScreenRendersRequiredButtonsAndPlaceholders() {
        var startCallClicked = false
        var pauseClicked = false
        var resumeClicked = false
        var uploadClicked = false

        composeTestRule.setContent {
            TeleconsultationScreen(
                onStartCall = { startCallClicked = true },
                onPause = { pauseClicked = true },
                onResume = { resumeClicked = true },
                onUpload = { uploadClicked = true }
            )
        }

        // Verify Video Feed Placeholders
        composeTestRule.onNodeWithTag("doctor_video_placeholder").assertExists()
        composeTestRule.onNodeWithTag("patient_video_placeholder").assertExists()
        composeTestRule.onNodeWithText("Doctor Video Feed").assertExists()
        composeTestRule.onNodeWithText("Patient Video Feed").assertExists()

        // Verify Core Action Buttons exist with required text labels
        val startCallNode = composeTestRule.onNodeWithTag("start_call_button")
        startCallNode.assertExists()
        startCallNode.assertTextContains("Start Call")

        val pauseNode = composeTestRule.onNodeWithTag("pause_call_button")
        pauseNode.assertExists()
        pauseNode.assertTextContains("Pause")

        val resumeNode = composeTestRule.onNodeWithTag("resume_call_button")
        resumeNode.assertExists()
        resumeNode.assertTextContains("Resume")

        val uploadNode = composeTestRule.onNodeWithTag("upload_call_button")
        uploadNode.assertExists()
        uploadNode.assertTextContains("Upload")

        // Perform clicks to verify interactions (scrolling into view if required)
        startCallNode.performScrollTo().performClick()
        org.junit.Assert.assertTrue("startCallClicked should be true", startCallClicked)

        pauseNode.performScrollTo().performClick()
        org.junit.Assert.assertTrue("pauseClicked should be true", pauseClicked)

        resumeNode.performScrollTo().performClick()
        org.junit.Assert.assertTrue("resumeClicked should be true", resumeClicked)

        uploadNode.performScrollTo().performClick()
        org.junit.Assert.assertTrue("uploadClicked should be true", uploadClicked)
    }
}
