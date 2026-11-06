package com.aarish.minimalwidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.res.XmlResourceParser
import android.os.Build
import androidx.glance.appwidget.SizeMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetSizingTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun batteryEightHasTransparentSurroundingsAndSolidBattery() {
        val design=designCatalog.first{it.category=="Battery" && it.style==8}
        val appearance=Appearance()
        val images=listOf(70f to 200f,140f to 140f,280f to 80f).map { (w,h) ->
            BatteryRenderer.render(design,appearance,50,true,w,h)
        } + DesignRenderer.render(design,appearance,50,true)
        images.forEach { bitmap ->
            try {
                for(x in listOf(0,bitmap.width-1)) {
                    assertEquals(0,android.graphics.Color.alpha(bitmap.getPixel(x,bitmap.height/2)))
                }
                assertEquals(0,android.graphics.Color.alpha(bitmap.getPixel(bitmap.width/2,0)))
                assertEquals(0,android.graphics.Color.alpha(bitmap.getPixel(bitmap.width/2,bitmap.height-1)))
                val opaque=(0 until bitmap.height).filter {
                    android.graphics.Color.alpha(bitmap.getPixel(bitmap.width/2,it))==255
                }
                assertTrue(opaque.isNotEmpty())
                val top=opaque.first();val height=opaque.last()-top
                assertEquals(appearance.background,bitmap.getPixel(bitmap.width/2,top+height/4))
                assertEquals(appearance.accent,bitmap.getPixel(bitmap.width/2,top+height*3/4))
            } finally { bitmap.recycle() }
        }
        val other=BatteryRenderer.render(designCatalog.first(),appearance,50,true,70f,200f)
        assertTrue(android.graphics.Color.alpha(other.getPixel(0,other.height/2))>0)
        other.recycle()
    }

    @Test fun everyReadyCardMatchesItsInstalledProviderGrid() {
        val providers=AppWidgetManager.getInstance(context).installedProviders.associateBy{it.provider}
        designCatalog.filter{it.readyReceiver()!=null}.forEach { card ->
            val receiver=card.readyReceiver()!!
            val info=providers[ComponentName(context,receiver)] ?: error("Missing provider for ${card.id}")
            val metadata=context.packageManager.getReceiverInfo(info.provider,android.content.pm.PackageManager.GET_META_DATA).metaData
            val xml=context.resources.getXml(metadata.getInt("android.appwidget.provider"))
            xml.use {
                while(it.eventType!=XmlResourceParser.START_TAG)it.next()
                val ns="http://schemas.android.com/apk/res/android"
                val cells=card.size.split(" × ").map(String::toInt)
                assertEquals(card.id,cells[0],it.getAttributeIntValue(ns,"targetCellWidth",0))
                assertEquals(card.id,cells[1],it.getAttributeIntValue(ns,"targetCellHeight",0))
                assertNotNull(card.id,it.getAttributeValue(ns,"minResizeHeight"))
            }
            if(Build.VERSION.SDK_INT>=31) {
                val cells=card.size.split(" × ").map(String::toInt)
                assertEquals(cells[0],info.targetCellWidth);assertEquals(cells[1],info.targetCellHeight)
            }
        }
    }
    @Test fun liveWidgetsUseActualHostSizeAndBatteryKeepsHostAspect() {
        listOf(BatteryWidget(),BatteryWideWidget(),BatteryTallWidget(),BatteryLargeWidget(),BatterySmallWidget(),ClockWidget(),PhotoWidget(),FolderWidget(),NoteWidget(),WifiWidget(),DataWidget(),VolumeWidget(),ContactWidget(),MusicWidget(),QuickSettingsWidget()).forEach {
            assertEquals(SizeMode.Exact,it.sizeMode)
        }
        for((w,h) in listOf(130f to 220f,260f to 100f,130f to 100f,300f to 220f)) {
            val bitmap=BatteryRenderer.render(designCatalog.first(),Appearance(),60,true,w,h)
            assertEquals(w/h,bitmap.width.toFloat()/bitmap.height,.01f)
            assertTrue(android.graphics.Color.alpha(bitmap.getPixel(bitmap.width/2,bitmap.height/2))>0)
            bitmap.recycle()
        }
    }
}

