package com.aarish.minimalwidgets

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.provider.AlarmClock
import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetActionTest {
    @Test fun batteryTapRoutesToSystemPowerUsage() = interceptDestination("battery", Intent.ACTION_POWER_USAGE_SUMMARY)
    @Test fun clockTapRoutesToClockAlarms() = interceptDestination("clock", AlarmClock.ACTION_SHOW_ALARMS)

    // Intercept external launches; tests never place calls or change system settings.
    private fun interceptDestination(panel: String, action: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = Instrumentation.ActivityMonitor(IntentFilter(action), Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()), true)
        instrumentation.addMonitor(monitor)
        try {
            ActivityScenario.launch<SettingsActivity>(widgetDestination(instrumentation.targetContext, panel)).use {
                instrumentation.waitForIdleSync()
                assertEquals(1, monitor.hits)
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }

    @Test fun directCallAndSmsHaveCorrectActionsAndNoInjectedUri() {
        val call = contactActionIntent("+91 (98765) 43210", false)!!
        assertEquals(Intent.ACTION_CALL, call.action)
        assertEquals("tel", call.data?.scheme)
        assertEquals("+919876543210", call.data?.schemeSpecificPart)
        val message = contactActionIntent("123456789", true)!!
        assertEquals(Intent.ACTION_SENDTO, message.action)
        assertEquals("smsto", message.data?.scheme)
        for (invalid in listOf("", "tel:12345", "*123#", "12345;6789", "1234,56", "123abc", "12", "12345?body=test")) {
            assertNull(contactActionIntent(invalid, false))
        }
        assertFalse(ownsContactWidget(InstrumentationRegistry.getInstrumentation().targetContext, -1))
    }

    @Test fun networkAndVolumePanelsHaveDeviceFallbacks() {
        assertEquals(Settings.Panel.ACTION_WIFI, widgetDestinationActions("wifi").first())
        assertEquals(Settings.Panel.ACTION_INTERNET_CONNECTIVITY, widgetDestinationActions("data").first())
        assertEquals(Settings.Panel.ACTION_VOLUME, widgetDestinationActions("volume").first())
        assertEquals(Settings.ACTION_SETTINGS, widgetDestinationActions("battery").last())
    }
}
