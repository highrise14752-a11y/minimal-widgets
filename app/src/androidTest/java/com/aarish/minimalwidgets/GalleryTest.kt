package com.aarish.minimalwidgets

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun galleryStartsFirstAndFiltersSearchResults() {
        compose.onNodeWithText("Widgets").assertIsDisplayed()
        compose.onNodeWithText("Clock & date").assertIsDisplayed()
        compose.onNodeWithContentDescription("Search widgets").performClick()
        compose.onNodeWithText("Search widgets").performTextInput("no-such-widget")
        compose.onNodeWithText("No widgets found").assertIsDisplayed()
        compose.onNodeWithText("Search widgets").performTextReplacement("Photo")
        compose.onNodeWithText("Photo", useUnmergedTree = true).assertIsDisplayed()
    }
    @Test fun accentPersistsAndPickerOpens() {
        val store = AccentStore(compose.activity.applicationContext)
        val old = runBlocking { store.accent.first() }
        try {
            runBlocking { store.save(0xFF42A5F5.toInt()) }
            assertEquals(0xFF42A5F5.toInt(), runBlocking { AccentStore(compose.activity.applicationContext).accent.first() })
            compose.onNodeWithContentDescription("Change accent color").performClick()
            compose.onNodeWithText("Your accent").assertIsDisplayed()
            compose.onNodeWithText("Apply accent").performClick()
            compose.waitForIdle()
        } finally { runBlocking { store.save(old) } }
    }
}
