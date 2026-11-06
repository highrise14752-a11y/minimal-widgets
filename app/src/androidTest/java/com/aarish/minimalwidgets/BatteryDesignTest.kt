package com.aarish.minimalwidgets

import android.graphics.*
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BatteryDesignTest {
    private val designs get()=designCatalog.filter{it.category=="Battery"}
    private val appearance=Appearance()
    private fun render(style:Int,level:Int=50)=BatteryRenderer.render(designs.first{it.style==style},appearance,level,true,100f,100f)

    @Test fun progressFillsInTheDirectionShownByTheReferences() {
        val vertical=render(14)
        assertEquals(appearance.accentDim(),vertical.getPixel(30,50))
        assertEquals(appearance.accent,vertical.getPixel(30,150));vertical.recycle()
        val horizontal=render(5)
        assertEquals(appearance.accent,horizontal.getPixel(40,130))
        assertEquals(appearance.accentDim(),horizontal.getPixel(160,130));horizontal.recycle()
        val segments=render(10)
        assertEquals(appearance.accent,segments.getPixel(130,95))
        assertEquals(appearance.accentDim(),segments.getPixel(170,95))
        assertEquals(appearance.accent,segments.getPixel(150,160));segments.recycle()
        val dots=render(19,60)
        assertEquals(appearance.textColor(),dots.getPixel(28,67))
        assertEquals(appearance.secondary,dots.getPixel(28,167));dots.recycle()
    }

    @Test fun horizontalBatteryHasNoSurroundingPanel() {
        val design=designs.first{it.style==9}
        val image=BatteryRenderer.render(design,appearance,50,true,200f,100f)
        assertEquals(0,Color.alpha(image.getPixel(0,100)))
        assertEquals(0,Color.alpha(image.getPixel(200,0)))
        assertEquals(appearance.accent,image.getPixel(100,100))
        assertEquals(appearance.background,image.getPixel(300,100));image.recycle()
    }

    @Test fun timeLabelsUseOnlyAvailableSystemEstimates() {
        assertEquals("Discharging",BatteryRenderer.timeLabel(60,false,999999))
        assertEquals("Charging",BatteryRenderer.timeLabel(60,true,null))
        assertEquals("Charging",BatteryRenderer.timeLabel(60,true,-1))
        assertEquals("1 h 2 m until full",BatteryRenderer.timeLabel(60,true,3720000))
        assertEquals("Fully charged",BatteryRenderer.timeLabel(100,true,null))
        assertEquals("Battery unavailable",BatteryRenderer.timeLabel(-1,false,null))
    }

    @Test fun allNineteenDesignsAreDistinctAndExportForVisualReview() {
        val sheet=Bitmap.createBitmap(1400,1040,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(sheet);canvas.drawColor(Color.BLACK)
        val p=Paint(Paint.ANTI_ALIAS_FLAG)
        val signatures=mutableSetOf<Int>()
        designs.forEachIndexed { index,design ->
            val sample=when(design.style){3,7,8,9,10->50;12->54;13->65;14,15->82;19->62;else->60}
            val image=DesignRenderer.render(design,appearance,sample,design.style!=13)
            val pixels=IntArray(image.width*image.height);image.getPixels(pixels,0,image.width,0,0,image.width,image.height)
            assertTrue("Duplicate battery design ${design.style}",signatures.add(pixels.contentHashCode()))
            val x=(index%5)*280f;val y=(index/5)*260f
            p.color=Color.rgb(39,40,45);canvas.drawRect(x+4,y+4,x+276,y+256,p)
            canvas.drawBitmap(image,x,y,p)
            p.color=Color.WHITE;p.textSize=18f;p.typeface=Typeface.DEFAULT_BOLD
            canvas.drawText("Battery ${design.style}  (${design.size})",x+12,y+246,p)
            image.recycle()
            // Also exercise every design on compact, tall and wide launcher allocations.
            for((w,h) in listOf(70f to 70f,110f to 220f,300f to 90f)) {
                BatteryRenderer.render(design,appearance,0,false,w,h).recycle()
                BatteryRenderer.render(design,appearance,100,true,w,h).recycle()
            }
        }
        assertEquals(19,signatures.size)
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val file=File(instrumentation.targetContext.getExternalFilesDir(null),"battery-designs.png")
        file.outputStream().use{sheet.compress(Bitmap.CompressFormat.PNG,100,it)};sheet.recycle()
        fun shell(command:String):String {
            val fd=instrumentation.uiAutomation.executeShellCommand(command)
            return ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().use{it.readText()}
        }
        shell("mkdir -p /sdcard/minimal-widgets-screenshots")
        val target="/sdcard/minimal-widgets-screenshots/battery-designs.png"
        shell("cp ${file.absolutePath} $target")
        assertEquals(target,shell("ls $target").trim())
    }
}
