package com.trailrunbuddy.app.ui.profiledetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trailrunbuddy.app.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ProfileDetailScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        // Navigate to new profile screen via the FAB
        composeTestRule.onNodeWithText("New Profile", useUnmergedTree = true).performClick()
    }

    @Test
    fun newProfileScreenIsDisplayed() {
        composeTestRule.onNodeWithText("New Profile").assertIsDisplayed()
    }

    @Test
    fun savingWithoutNameShowsError() {
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.onNodeWithText("Profile name cannot be empty", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun savingWithoutTimersShowsError() {
        composeTestRule.onNodeWithText("Profile name").performTextInput("Test Profile")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.onNodeWithText("Add at least one timer", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun addTimerButtonOpensDialog() {
        composeTestRule.onNodeWithText("TIMERS", useUnmergedTree = true).assertIsDisplayed()
        // Tap the Add icon next to TIMERS
        composeTestRule.onNodeWithText("Add Timer", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithText("Timer name").assertIsDisplayed()
    }
}
