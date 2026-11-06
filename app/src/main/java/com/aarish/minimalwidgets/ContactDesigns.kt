package com.aarish.minimalwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import java.io.File

fun contactSlotCount(style:Int)=when(style){3->4;4->8;else->1}
fun savedContactStyle(context:Context,id:Int,default:Int=1)=widgetPrefs(context).getInt("contact-design-$id",default).coerceIn(1,14)
fun contactField(kind:String,id:Int,slot:Int)=if(slot==0)"contact-$kind-$id" else "contact-$kind-$id-$slot"
fun contactPhoto(context:Context,id:Int,slot:Int)=File(context.filesDir,"contact-photo-$id-$slot.jpg")
fun savedContact(context:Context,id:Int,slot:Int):ContactTile {
    val prefs=widgetPrefs(context)
    return ContactTile(prefs.getString(contactField("name",id,slot),"Contact").orEmpty().ifBlank{"Contact"},
        prefs.getString(contactField("number",id,slot),"").orEmpty(),
        BitmapFactory.decodeFile(contactPhoto(context,id,slot).path),
        prefs.getString(contactField("telegram",id,slot),"").orEmpty())
}
fun telegramUsername(raw:String)=raw.trim().removePrefix("@").takeIf { Regex("[A-Za-z][A-Za-z0-9_]{4,31}").matches(it) && it.lowercase() !in setOf("login","joinchat","proxy","socks","share","addstickers","addemoji","confirmphone","setlanguage") }
fun whatsappNumber(raw:String)=contactNumber(raw)?.removePrefix("+")?.takeIf{Regex("[1-9][0-9]{6,14}").matches(it)}
fun contactDestination(tile:ContactTile,kind:String):Intent?=when(kind) {
    "call"->contactActionIntent(tile.number,false)
    "message"->contactActionIntent(tile.number,true)
    "telegram"->telegramUsername(tile.telegram)?.let{Intent(Intent.ACTION_VIEW,Uri.Builder().scheme("https").authority("t.me").appendPath(it).build())}
    "whatsapp"->whatsappNumber(tile.number)?.let{Intent(Intent.ACTION_VIEW,Uri.Builder().scheme("https").authority("wa.me").appendPath(it).build())}
    else->null
}
fun contactPrimary(style:Int)=when(style){6,12->"message";13->"telegram";14->"whatsapp";else->"call"}
fun contactAllowedActions(style:Int)=if(style in listOf(1,2,7,8,9,10))setOf("call","message")else setOf(contactPrimary(style))
fun contactPending(context:Context,id:Int,slot:Int,kind:String):PendingIntent {
    val intent=if(kind=="edit")contactConfigIntent(context,id)else Intent(context,ContactActionActivity::class.java).putExtra("widgetId",id).putExtra("slot",slot).putExtra("kind",kind)
    intent.data=Uri.Builder().scheme("minimalwidgets").authority("contact").appendPath(id.toString()).appendPath(slot.toString()).appendPath(kind).build()
    return PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
fun contactRemoteViews(context:Context,id:Int,style:Int,appearance:Appearance,width:Float,height:Float):RemoteViews {
    val entries=List(contactSlotCount(style)){savedContact(context,id,it)}
    val bitmap=ContactRenderer.render(style,appearance,entries,(width*2).toInt().coerceIn(40,1200),(height*2).toInt().coerceIn(40,1200))
    val views=RemoteViews(context.packageName,R.layout.contact_widget)
    views.setImageViewBitmap(R.id.contact_picture,bitmap)
    views.setOnClickPendingIntent(R.id.contact_picture,contactPending(context,id,0,contactPrimary(style)))
    views.setContentDescription(R.id.contact_picture,"${entries[0].name}; ${contactPrimary(style)}")
    views.setOnClickPendingIntent(R.id.contact_edit,contactPending(context,id,0,"edit"))
    views.setTextColor(R.id.contact_edit,appearance.accent)
    val group=when(style){2,10->R.id.contact_pair_quarter;7,8->R.id.contact_pair_half;9->R.id.contact_pair_side;3->R.id.contact_grid_four;4->if(width>height*1.35f)R.id.contact_grid_eight_wide else R.id.contact_grid_eight_tall;else->null}
    if(group!=null) {
        views.setViewVisibility(group,View.VISIBLE)
        val ids=when(group){
            R.id.contact_pair_quarter->listOf(R.id.contact_quarter_left,R.id.contact_quarter_right)
            R.id.contact_pair_half->listOf(R.id.contact_half_left,R.id.contact_half_right)
            R.id.contact_pair_side->listOf(R.id.contact_side_top,R.id.contact_side_bottom)
            R.id.contact_grid_four->listOf(R.id.contact_four_0,R.id.contact_four_1,R.id.contact_four_2,R.id.contact_four_3)
            R.id.contact_grid_eight_wide->listOf(R.id.contact_wide_0,R.id.contact_wide_1,R.id.contact_wide_2,R.id.contact_wide_3,R.id.contact_wide_4,R.id.contact_wide_5,R.id.contact_wide_6,R.id.contact_wide_7)
            else->listOf(R.id.contact_tall_0,R.id.contact_tall_1,R.id.contact_tall_2,R.id.contact_tall_3,R.id.contact_tall_4,R.id.contact_tall_5,R.id.contact_tall_6,R.id.contact_tall_7)
        }
        ids.forEachIndexed{i,view->
            val slot=if(style in listOf(3,4))i else 0
            val kind=if(style in listOf(3,4))"call"else if(style==9 || style==10)if(i==0)"message"else"call"else if(i==0)"call"else"message"
            views.setOnClickPendingIntent(view,contactPending(context,id,slot,kind))
            views.setContentDescription(view,"${entries[slot].name}; $kind")
        }
    }
    return views
}
class ContactDesignWidget:ContactWidget("design")
class ContactDesignReceiver:CleanWidgetReceiver(){override val glanceAppWidget=ContactDesignWidget()}
class ContactPinReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        val id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1)
        val style=intent.getIntExtra("style",-1)
        val provider=AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider
        if(style !in 1..14 || provider?.packageName!=context.packageName || provider.className!=ContactDesignReceiver::class.java.name)return
        widgetPrefs(context).edit().putInt("contact-design-$id",style).apply()
        AccentRefreshWorker.enqueue(context)
    }
}
