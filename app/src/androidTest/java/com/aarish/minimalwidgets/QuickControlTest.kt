package com.aarish.minimalwidgets

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickControlTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun everyQuickCardHasAnActionAndCorrectControl() {
        val cards = designCatalog.filter { it.category == "Quick Settings" }
        assertEquals(55, cards.size)
        cards.forEach { card ->
            assertNotNull(card.readyReceiver())
            assertEquals("1 × 1", card.size)
            val selected = quickDesign(card.id)!!
            assertTrue(selected.family in 1..4)
            if(card.style > 3) assertEquals(selected.control.label, card.name.substringBeforeLast(' '))
        }
        assertEquals(QuickControl.RINGER, quickDesign(cards[0].id)?.control)
        assertEquals(QuickControl.SILENT, quickDesign(cards[2].id)?.control)
        assertNull(quickDesign("battery-1"))
        assertNull(quickDesign("quick-settings-999"))
    }

    @Test fun allSettingsControlsRouteAndHandleMissingOemScreens() {
        QuickControl.entries.filter { it.panel != null }.forEach { control ->
            val attempts = mutableListOf<String?>()
            val actions = widgetDestinationActions(control.panel)
            assertTrue(openWidgetDestination(control.panel) {
                attempts.add(it.action)
                if(it.action == actions.first() && actions.size > 1) throw ActivityNotFoundException()
            })
            assertEquals(actions.take(if(actions.size > 1) 2 else 1), attempts)
        }
        assertEquals(Settings.ACTION_BLUETOOTH_SETTINGS, widgetDestinationActions("bluetooth").first())
        assertEquals(Settings.ACTION_LOCATION_SOURCE_SETTINGS, widgetDestinationActions("location").first())
        assertEquals(Settings.ACTION_DISPLAY_SETTINGS, widgetDestinationActions("rotate").first())
    }

    @Test fun ringerActionsCycleAndTogglePredictably() {
        val normal = AudioManager.RINGER_MODE_NORMAL
        val vibrate = AudioManager.RINGER_MODE_VIBRATE
        val silent = AudioManager.RINGER_MODE_SILENT
        assertEquals(vibrate, nextRingerMode(QuickControl.RINGER, normal))
        assertEquals(silent, nextRingerMode(QuickControl.RINGER, vibrate))
        assertEquals(normal, nextRingerMode(QuickControl.RINGER, silent))
        assertEquals(normal, nextRingerMode(QuickControl.SILENT, silent))
        assertEquals(silent, nextRingerMode(QuickControl.SILENT, normal))
        assertEquals(normal, nextRingerMode(QuickControl.VIBRATE, vibrate))
        assertEquals(vibrate, nextRingerMode(QuickControl.VIBRATE, silent))
    }

    @Test fun autoContrastUsesThePlacedFamilyBackgroundAndKeepsForcedColors() {
        val appearance = Appearance(background = android.graphics.Color.WHITE, secondary = android.graphics.Color.BLACK, reactive = "Auto contrast")
        assertEquals(android.graphics.Color.BLACK, quickControlTextColor(appearance, 1))
        assertEquals(android.graphics.Color.WHITE, quickControlTextColor(appearance, 4))
        assertEquals(android.graphics.Color.BLACK, quickControlTextColor(appearance.copy(reactive = "Always black"), 4))
        assertEquals(appearance.background, quickControlTextColor(appearance.copy(reactive = "Background color"), 4))
    }

    @Test fun instancesKeepIndependentSelectionsAndRejectUnownedPins() {
        val prefs = widgetPrefs(context)
        val ids = listOf(-70001, -70002)
        val previous = ids.associateWith { prefs.getString("quick-design-$it", null) }
        try {
            prefs.edit().putString("quick-design-${ids[0]}", "quick-settings-6")
                .putString("quick-design-${ids[1]}", "quick-settings-16").commit()
            assertEquals(QuickControl.BLUETOOTH, savedQuickDesign(context, ids[0]).control)
            assertEquals(QuickControl.TORCH, savedQuickDesign(context, ids[1]).control)
            QuickPinReceiver().onReceive(context, Intent().putExtra("appWidgetId", ids[0]).putExtra("designId", "quick-settings-10"))
            assertEquals(QuickControl.BLUETOOTH, savedQuickDesign(context, ids[0]).control)
            assertFalse(ownsQuickWidget(context, ids[0]))
            prefs.edit().putString("quick-design-${ids[1]}", "invalid").commit()
            assertEquals(QuickControl.WIFI, savedQuickDesign(context, ids[1]).control)
        } finally {
            val editor = prefs.edit()
            previous.forEach { (id, value) -> if(value == null) editor.remove("quick-design-$id") else editor.putString("quick-design-$id", value) }
            editor.commit()
        }
    }
}
