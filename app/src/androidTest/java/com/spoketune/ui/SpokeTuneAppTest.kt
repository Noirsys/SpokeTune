package com.spoketune.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Smoke coverage for the primary flows; intentionally uses user-visible copy. */
@RunWith(AndroidJUnit4::class)
class SpokeTuneAppTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun showApp() {
        compose.setContent { SpokeTuneApp() }
    }

    @Test
    fun welcomeCtaIsVisibleAndNavigatesToWheelList() {
        compose.onNodeWithText("Set up a wheel")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        compose.onNodeWithText("Your wheels").assertIsDisplayed()
    }

    @Test
    fun wheelListShows36SpokeFatTireAndOpensIt() {
        compose.onNodeWithText("Set up a wheel").performClick()

        compose.onNodeWithText("Trail e-bike · 27-inch fat tire")
            .assertIsDisplayed()
            .performClick()

        compose.onNodeWithText("36 spokes · local only").assertIsDisplayed()
        compose.onNodeWithText("Start a new pass").assertIsDisplayed()
    }

    @Test
    fun welcomeContentRemainsReachableOnConstrainedViewport() {
        // The CTA lives in Scaffold.bottomBar, so it must remain visible even when
        // the explanatory content requires scrolling on a short emulator viewport.
        compose.onNodeWithText("Set up a wheel").assertIsDisplayed()
        compose.onNode(hasText("Before you begin")).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Set up a wheel").assertIsDisplayed()
    }
}
