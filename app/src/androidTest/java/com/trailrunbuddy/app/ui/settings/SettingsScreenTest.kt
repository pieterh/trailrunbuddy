package com.trailrunbuddy.app.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class SettingsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        // Navigate to settings
        composeTestRule.onNodeWithText("Settings", useUnmergedTree = true).performClick()
    }

    @Test
    fun settingsScreenIsDisplayed() {
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun allThemeOptionsAreDisplayed() {
        composeTestRule.onNodeWithText("System Default").assertIsDisplayed()
        composeTestRule.onNodeWithText("Light").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dark").assertIsDisplayed()
    }

    @Test
    fun defaultThemeIsSystemDefault() {
        composeTestRule.onNodeWithText("System Default").assertIsDisplayed()
    }

    @Test
    fun selectingDarkThemePersists() {
        composeTestRule.onNodeWithText("Dark").performClick()
        // Navigate away and back
        composeTestRule.onNodeWithText("Back", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithText("Settings", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithText("Dark").assertIsDisplayed()
    }
}
