package com.aarish.minimalwidgets

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun galleryStartsFirstAndFiltersSearchResults() {
        compose.onNodeWithText("Widgets").assertIsDisplayed()
        compose.onNodeWithText("Battery Widget 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Search widgets").performClick()
        compose.onNodeWithText("Search widgets").performTextInput("no-such-widget")
        compose.onNodeWithText("No widgets found").assertIsDisplayed()
        compose.onNodeWithText("Search widgets").performTextReplacement("Battery Widget 1")
        compose.onNodeWithContentDescription("Battery Widget 1, 2 × 2").assertIsDisplayed()
    }
    @Test fun compactPolicyAndEveryPreviewRenders() {
        assertEquals(11,galleryCategories.size)
        assertEquals(designCatalog.size,designCatalog.map {it.id}.toSet().size)
        assertTrue(designCatalog.filter {it.category in compactCategories}.all {it.size=="1 × 1"})
        assertTrue(designCatalog.any {it.category=="Battery" && it.size=="4 × 2"})
        designCatalog.forEach { val bitmap=DesignRenderer.render(it,Appearance());assertEquals(280,bitmap.width);bitmap.recycle() }
    }
    @Test fun favoriteSurvivesNavigation() {
        val store=AccentStore(compose.activity.applicationContext)
        val old=runBlocking{store.appearance.first()}
        try {
            runBlocking {store.update{it.copy(favorites=emptySet())}}
            compose.onNodeWithContentDescription("Favorite Battery Widget 1").performClick()
            compose.onNodeWithContentDescription("Favorites",useUnmergedTree=true).performClick()
            compose.onNodeWithText("Battery Widget 1").assertIsDisplayed()
            compose.onNodeWithContentDescription("Settings",useUnmergedTree=true).performClick()
            compose.onNodeWithText("Permissions & widgets").assertIsDisplayed()
            compose.onNodeWithContentDescription("Favorites",useUnmergedTree=true).performClick()
            compose.onNodeWithText("Battery Widget 1").assertIsDisplayed()
        } finally {runBlocking{store.update{old}}}
    }
    @Test fun referenceScreensNavigateAndCapture() {
        fun capture(name:String) {
            compose.waitForIdle()
            val directory=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"screenshots").apply{mkdirs()}
            val imageFile=File(directory,"$name.png")
            imageFile.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
            // UTP may uninstall the target after the suite. Preserve artifacts in shell-owned storage.
            fun shell(command:String):String {
                val descriptor=InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
                return android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use{it.readText()}
            }
            val target="/sdcard/minimal-widgets-screenshots/$name.png"
            shell("mkdir -p /sdcard/minimal-widgets-screenshots")
            shell("cp ${imageFile.absolutePath} $target")
            assertEquals(target,shell("ls $target").trim())
        }
        capture("battery")
        galleryCategories.drop(1).forEach { category ->
            compose.onNodeWithText(category,useUnmergedTree=true).performScrollTo().performClick()
            capture(category.lowercase().replace(' ','-'))
        }
        compose.onNodeWithContentDescription("Customize",useUnmergedTree=true).performClick()
        compose.onNodeWithText("ACTIVE THEME").assertIsDisplayed()
        capture("themes")
        compose.onNodeWithText("Studio",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Reactive Color").assertIsDisplayed()
        capture("studio")
        compose.onNodeWithContentDescription("Settings",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Permissions & widgets").assertIsDisplayed()
        capture("settings")
    }
    @Test fun themeBackupRoundTripsAndRejectsInvalidInput() {
        val a=Appearance(accent=0xFF42A5F5.toInt(),theme="Test",favorites=setOf("battery-1"),reactive="Auto contrast")
        assertEquals(a,Appearance.parse(a.json()))
        assertTrue(runCatching{Appearance.parse("{}")} .isFailure)
        assertTrue(runCatching{Appearance.parse(a.json().replace("Auto contrast","Invalid"))}.isFailure)
    }
    @Test fun accentPersistsAndPickerOpens() {
        val store=AccentStore(compose.activity.applicationContext)
        val old=runBlocking{store.appearance.first()}
        try {
            compose.onNodeWithText("Accent").performClick()
            compose.onNodeWithText("Your accent").assertIsDisplayed()
            compose.onNodeWithContentDescription("Accent #42A5F5").performClick()
            compose.onNodeWithText("Apply accent").performClick()
            compose.waitUntil(5000){runBlocking{store.accent.first()}==0xFF42A5F5.toInt()}
        } finally {runBlocking{store.update{old}}}
    }
}





