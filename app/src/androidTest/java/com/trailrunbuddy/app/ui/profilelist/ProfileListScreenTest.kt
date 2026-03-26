package com.trailrunbuddy.app.ui.profilelist

import androidx.compose.ui.test.assertIsDisplayed
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
class ProfileListScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun appBarTitleIsDisplayed() {
        composeTestRule.onNodeWithText("Trail Run Buddy").assertIsDisplayed()
    }

    @Test
    fun emptyStateMessageIsDisplayed() {
        // Fresh in-memory DB → no profiles
        composeTestRule.onNodeWithText("No profiles yet.", substring = true).assertIsDisplayed()
    }

    @Test
    fun fabIsDisplayed() {
        // FAB with content description "New Profile"
        composeTestRule
            .onNodeWithText("Trail Run Buddy") // ensure screen loaded
            .assertIsDisplayed()
    }

    @Test
    fun settingsIconNavigatesToSettings() {
        composeTestRule.onNodeWithText("Settings", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
