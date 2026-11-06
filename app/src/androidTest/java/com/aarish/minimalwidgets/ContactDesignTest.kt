package com.aarish.minimalwidgets

import android.content.Intent
import android.graphics.*
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ContactDesignTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun routesUseConfiguredContactAndRejectInjectedLinks() {
        val tile=ContactTile("Name","+919876543210",telegram="example_user")
        assertEquals(Intent.ACTION_CALL,contactDestination(tile,"call")?.action)
        assertEquals("tel:+919876543210",contactDestination(tile,"call")?.dataString)
        assertEquals("smsto:+919876543210",contactDestination(tile,"message")?.dataString)
        assertEquals("https://t.me/example_user",contactDestination(tile,"telegram")?.dataString)
        assertEquals("https://wa.me/919876543210",contactDestination(tile,"whatsapp")?.dataString)
        for(raw in listOf("https://t.me/user","user?start=test","user/name","login","*123#"))assertNull(telegramUsername(raw))
        for(raw in listOf("tel:123456789","1234567?text=test","*123#","00123456789","12"))assertNull(whatsappNumber(raw))
        assertNull(contactDestination(tile,"unknown"))
        assertEquals(4,contactSlotCount(3));assertEquals(8,contactSlotCount(4))
    }
    @Test fun configuredTapsBypassMinimalWidgetsAndPermissionGrantsDoNotCall() {
        val id=710003;val prefs=widgetPrefs(context)
        try {
            prefs.edit().putString(contactField("number",id,0),"+919876543210").commit()
            val direct=contactTapDestination(context,id,0,"call",phoneAllowed=true)
            assertEquals(Intent.ACTION_CALL,direct.action)
            assertEquals("tel:+919876543210",direct.dataString)
            assertNull(direct.component)
            val permission=contactTapDestination(context,id,0,"call",phoneAllowed=false)
            assertEquals(ContactActionActivity::class.java.name,permission.component?.className)
            assertNotEquals(Intent.ACTION_CALL,permission.action)
            val message=contactTapDestination(context,id,0,"message",phoneAllowed=false)
            assertEquals(Intent.ACTION_SENDTO,message.action);assertNull(message.component)
            assertEquals(ContactConfigActivity::class.java.name,contactTapDestination(context,id,1,"call",true).component?.className)
        }finally{prefs.edit().remove(contactField("number",id,0)).commit()}
    }
    @Test fun everyLayoutInflatesWithRealClickableRegionsAtSmallAndLargeSizes() {
        val designs=designCatalog.filter{it.category=="Contacts"}
        assertEquals(14,designs.size)
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            designs.forEach{design->
                assertEquals(ContactDesignReceiver::class.java,design.readyReceiver())
                assertEquals("1 \u00d7 1",design.size)
                for((w,h) in listOf(70f to 70f,200f to 200f,400f to 200f)) {
                    val view=contactRemoteViews(context,710001,design.style,Appearance(),w,h).apply(context,FrameLayout(context))
                    assertTrue(view.findViewById<View>(R.id.contact_picture).hasOnClickListeners())
                    assertTrue(view.findViewById<View>(R.id.contact_edit).hasOnClickListeners())
                    if(design.style==3)assertEquals(View.VISIBLE,view.findViewById<View>(R.id.contact_grid_four).visibility)
                    if(design.style==4) {
                        val group=if(w>h*1.35f)R.id.contact_grid_eight_wide else R.id.contact_grid_eight_tall
                        assertEquals(View.VISIBLE,view.findViewById<View>(group).visibility)
                        assertTrue(view.findViewById<View>(if(w>h*1.35f)R.id.contact_wide_7 else R.id.contact_tall_7).hasOnClickListeners())
                    }
                }
            }
        }
    }
    @Test fun contactsAndStylesStaySeparateAndUnownedPinIsRejected() {
        val prefs=widgetPrefs(context)
        val first=710001;val second=710002
        try {
            prefs.edit().putInt("contact-design-$first",4).putInt("contact-design-$second",14)
                .putString(contactField("number",first,0),"1234567").putString(contactField("number",first,7),"9876543")
                .putString(contactField("number",second,0),"4567890").commit()
            assertEquals(4,savedContactStyle(context,first));assertEquals(14,savedContactStyle(context,second))
            assertEquals("1234567",savedContact(context,first,0).number)
            assertEquals("9876543",savedContact(context,first,7).number)
            assertEquals("4567890",savedContact(context,second,0).number)
            ContactPinReceiver().onReceive(context,Intent().putExtra("appWidgetId",first).putExtra("style",1))
            assertEquals(4,savedContactStyle(context,first))
        }finally{
            val edit=prefs.edit();for(id in listOf(first,second)){
                edit.remove("contact-design-$id");repeat(8){slot->for(kind in listOf("name","number","telegram"))edit.remove(contactField(kind,id,slot))}}
            edit.commit()
        }
    }
    @Test fun savedContactPhotoReplacesTheDrawnAvatar() {
        val id=710004
        val photo=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        try {
            contactPhoto(context,id,0).outputStream().use{photo.compress(Bitmap.CompressFormat.PNG,100,it)}
            val saved=savedContact(context,id,0)
            assertNotNull(saved.photo)
            val portrait=ContactRenderer.render(1,Appearance(),listOf(saved),200,200)
            assertEquals(Color.RED,portrait.getPixel(100,75));portrait.recycle()
            val group=ContactRenderer.render(3,Appearance(),listOf(saved,ContactTile(),ContactTile(),ContactTile()),200,200)
            assertEquals(Color.RED,group.getPixel(50,50));group.recycle()
            saved.photo?.recycle()
        }finally{photo.recycle();contactPhoto(context,id,0).delete()}
    }
    @Test fun allContactDesignsAreDistinctAndExportForReview() {
        val sheet=Bitmap.createBitmap(1400,780,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(sheet);canvas.drawColor(Color.BLACK);val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        val signatures=mutableSetOf<Int>()
        designCatalog.filter{it.category=="Contacts"}.forEachIndexed{i,design->
            val image=ContactRenderer.preview(design,Appearance())
            val pixels=IntArray(image.width*image.height);image.getPixels(pixels,0,image.width,0,0,image.width,image.height)
            assertTrue("Duplicate contact ${design.style}",signatures.add(pixels.contentHashCode()))
            val x=i%5*280f;val y=i/5*260f
            paint.color=Color.rgb(39,40,45);canvas.drawRect(x+4,y+4,x+276,y+256,paint)
            canvas.drawBitmap(image,x,y,paint);image.recycle()
            paint.color=Color.WHITE;paint.textSize=18f;paint.typeface=Typeface.DEFAULT_BOLD
            canvas.drawText(design.name,x+12,y+246,paint)
        }
        assertEquals(14,signatures.size)
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val file=File(context.getExternalFilesDir(null),"contact-designs.png")
        file.outputStream().use{sheet.compress(Bitmap.CompressFormat.PNG,100,it)};sheet.recycle()
        fun shell(cmd:String):String {val fd=instrumentation.uiAutomation.executeShellCommand(cmd);return ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().use{it.readText()}}
        shell("mkdir -p /sdcard/minimal-widgets-screenshots")
        val target="/sdcard/minimal-widgets-screenshots/contact-designs.png"
        shell("cp ${file.absolutePath} $target");assertEquals(target,shell("ls $target").trim())
    }
}
