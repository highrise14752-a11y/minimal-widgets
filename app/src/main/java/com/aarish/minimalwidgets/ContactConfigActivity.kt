package com.aarish.minimalwidgets

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.UUID
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private data class ContactInput(val name:String="",val number:String="",val telegram:String="")
/** Editing and photo selection never execute the selected contact's action. */
class ContactConfigActivity:ComponentActivity() {
    private var widgetId=-1
    private var style by mutableIntStateOf(1)
    private var slot by mutableIntStateOf(0)
    private var inputs by mutableStateOf(List(8){ContactInput()})
    private var error by mutableStateOf<String?>(null)
    private var allowed by mutableStateOf(false)
    private var saving by mutableStateOf(false)
    private var photoRevision by mutableIntStateOf(0)
    private var token=UUID.randomUUID().toString()
    private var pickerSlot=0
    private var pendingContactPhoto:Uri?=null
    private val drafts=mutableSetOf<Int>()
    private val removed=mutableSetOf<Int>()
    private fun draft(index:Int)=File(filesDir,"contact-draft-$token-$index.jpg")
    private fun change(index:Int,transform:(ContactInput)->ContactInput){inputs=inputs.mapIndexed{i,entry->if(i==index)transform(entry)else entry}}
    private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()){allowed=it;AccentRefreshWorker.enqueue(this)}
    private val contactPhotoPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        if(granted)pendingContactPhoto?.let{uri->try{importPhoto(uri,pickerSlot)}catch(_:Exception){error="Could not read this contact photo. Choose a photo manually."}}
        else error="Contact photo access was denied. You can choose a photo manually."
    }
    private fun importPhoto(uri:Uri,index:Int){
        require(uri.scheme=="content")
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds)}
        require(bounds.outWidth>0 && bounds.outHeight>0)
        val options=BitmapFactory.Options();var sample=1
        while(bounds.outWidth/sample>512 || bounds.outHeight/sample>512)sample*=2
        options.inSampleSize=sample
        val bitmap=contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,options)}?:kotlin.error("Invalid image")
        try{draft(index).outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,90,it)}}finally{bitmap.recycle()}
        drafts.add(index);removed.remove(index);photoRevision++
    }
    private val contactPicker=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        val uri=result.data?.data
        if(result.resultCode==RESULT_OK && uri!=null)try {
            var photo:Uri?=null
            val projection=arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
            contentResolver.query(uri,projection,null,null,null)?.use{cursor->
                if(cursor.moveToFirst()){
                    change(pickerSlot){it.copy(name=cursor.getString(0).orEmpty().take(60),number=cursor.getString(1).orEmpty().take(40))}
                    photo=(cursor.getString(2)?:cursor.getString(3))?.let(Uri::parse)?.takeIf{it.scheme=="content"}
                }
            }
            // A different person must never retain the previous person's avatar.
            drafts.remove(pickerSlot);draft(pickerSlot).delete();removed.add(pickerSlot);photoRevision++
            pendingContactPhoto=photo
            photo?.let{picked->
                try{importPhoto(picked,pickerSlot)}
                catch(_:SecurityException){
                    if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)contactPhotoPermission.launch(Manifest.permission.READ_CONTACTS)
                    else error="Could not load this contact photo. Choose a photo manually."
                }catch(_:Exception){error="Could not load this contact photo. Choose a photo manually."}
            }
        }catch(_:Exception){error="Could not read that contact. Enter the number instead."}
    }
    private val photoPicker=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)try{importPhoto(uri,pickerSlot)}catch(_:Exception){error="Could not load that image. Choose another photo."}
    }
    override fun onResume(){super.onResume();allowed=checkSelfPermission(Manifest.permission.CALL_PHONE)==PackageManager.PERMISSION_GRANTED}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        widgetId=intent.getIntExtra("widgetId",-1)
        if(!ownsContactWidget(this,widgetId)){finish();return}
        val provider=AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider?.className
        val default=when(provider){CallReceiver::class.java.name->11;MessageReceiver::class.java.name->12;else->1}
        style=savedInstanceState?.getInt("style")?:savedContactStyle(this,widgetId,default)
        slot=(savedInstanceState?.getInt("slot")?:intent.getIntExtra("slot",0)).coerceIn(0,contactSlotCount(style)-1)
        token=savedInstanceState?.getString("token")?:token
        pickerSlot=savedInstanceState?.getInt("pickerSlot")?:slot
        pendingContactPhoto=savedInstanceState?.getString("pendingContactPhoto")?.let(Uri::parse)
        drafts.addAll(savedInstanceState?.getIntArray("drafts")?.toList().orEmpty())
        removed.addAll(savedInstanceState?.getIntArray("removed")?.toList().orEmpty())
        val prefs=widgetPrefs(this)
        val saved=savedInstanceState?.getString("entries")?.let{runCatching{JSONArray(it)}.getOrNull()}
        inputs=List(8){i->val row=saved?.optJSONObject(i)
            ContactInput(row?.optString("name")?:prefs.getString(contactField("name",widgetId,i),"").orEmpty(),
                row?.optString("number")?:prefs.getString(contactField("number",widgetId,i),"").orEmpty(),
                row?.optString("telegram")?:prefs.getString(contactField("telegram",widgetId,i),"").orEmpty())}
        setContent {MaterialTheme(colorScheme=darkColorScheme()){Surface(Modifier.fillMaxSize()){
            Column(Modifier.systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Text("Configure contacts",style=MaterialTheme.typography.headlineSmall)
                Text("Calls dial the saved number immediately after permission. Saving, changing photos or granting permission never places a call.")
                var expanded by remember{mutableStateOf(false)}
                Box {
                    OutlinedButton(onClick={expanded=true}){Text(designCatalog.first{it.category=="Contacts" && it.style==style}.name)}
                    DropdownMenu(expanded,{expanded=false}){designCatalog.filter{it.category=="Contacts"}.forEach{design->
                        DropdownMenuItem(text={Text(design.name)},onClick={style=design.style;slot=slot.coerceAtMost(contactSlotCount(style)-1);expanded=false})}}
                }
                if(contactSlotCount(style)>1)Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    repeat(contactSlotCount(style)){i->FilterChip(selected=slot==i,onClick={slot=i},label={Text("Contact ${i+1}")})}
                }
                val entry=inputs[slot]
                OutlinedTextField(entry.name,{v->change(slot){it.copy(name=v.take(60))}},label={Text("Name")},singleLine=true,modifier=Modifier.fillMaxWidth())
                if(style!=13)OutlinedTextField(entry.number,{v->change(slot){it.copy(number=v.take(40))}},label={Text(if(style==14)"International phone number with country code"else"Phone number")},singleLine=true,
                    keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Phone),modifier=Modifier.fillMaxWidth())
                if(style==13){
                    OutlinedTextField(entry.telegram,{v->change(slot){it.copy(telegram=v.take(40))}},label={Text("Telegram username")},singleLine=true,modifier=Modifier.fillMaxWidth())
                    Text("Use a public username, without a link. The shortcut opens that chat in Telegram or its web page.")
                }else TextButton(onClick={pickerSlot=slot;try{contactPicker.launch(Intent(Intent.ACTION_PICK,ContactsContract.CommonDataKinds.Phone.CONTENT_URI))}catch(_:android.content.ActivityNotFoundException){error="No contact picker is available."}}){Text("Choose from contacts")}
                if(style<=6){
                    val revision=photoRevision
                    val photo=remember(slot,revision){if(slot in removed)null else BitmapFactory.decodeFile(if(slot in drafts)draft(slot).path else contactPhoto(this@ContactConfigActivity,widgetId,slot).path)}
                    photo?.let{Image(it.asImageBitmap(),"Contact photo",Modifier.size(80.dp))}
                    Row {
                        TextButton(onClick={pickerSlot=slot;try{photoPicker.launch(arrayOf("image/*"))}catch(_:android.content.ActivityNotFoundException){error="No photo picker is available."}}){Text("Choose photo")}
                        TextButton(onClick={drafts.remove(slot);draft(slot).delete();removed.add(slot);photoRevision++}){Text("Use avatar")}
                    }
                }
                if(contactAllowedActions(style).contains("call") && !allowed){
                    Button(onClick={permission.launch(Manifest.permission.CALL_PHONE)}){Text("Allow direct calls")}
                    TextButton(onClick={startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.fromParts("package",packageName,null)))}){Text("App permissions")}
                }
                Text("Picking a contact imports its saved photo automatically. Android may ask for Contacts permission to read that photo; a manual photo remains optional.")
                Text("1x1 supported. Enlarge the home-screen widget for the full photo, group or button layout. Use the small menu on the widget to edit it again.")
                error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
                Button(enabled=!saving,onClick={save()}){Text(if(saving)"Saving..."else"Save contacts")}
                TextButton(enabled=!saving,onClick={finish()}){Text("Cancel")}
            }
        }}}
    }
    private fun save(){
        val count=contactSlotCount(style)
        val relevant=inputs.take(count)
        if(style==13){if(telegramUsername(relevant[0].telegram)==null){error="Enter a valid Telegram username.";return}}
        else {
            val numberCheck:(String)->String?=if(style==14)::whatsappNumber else ::contactNumber
            if(relevant.none{numberCheck(it.number)!=null}){error="Set at least one valid phone number.";return}
            if(relevant.any{it.number.isNotBlank() && numberCheck(it.number)==null}){error="Check each phone number; include the country code for WhatsApp.";return}
        }
        saving=true
        try {
            // Finish copies before saving references; configuration never launches a contact action.
            repeat(count){i->if(i in drafts)draft(i).copyTo(contactPhoto(this,widgetId,i),overwrite=true)}
            val edit=widgetPrefs(this).edit().putInt("contact-design-$widgetId",style)
            inputs.forEachIndexed{i,entry->edit.putString(contactField("name",widgetId,i),entry.name.trim().ifBlank{"Contact ${i+1}"})
                .putString(contactField("number",widgetId,i),contactNumber(entry.number).orEmpty())
                .putString(contactField("telegram",widgetId,i),telegramUsername(entry.telegram).orEmpty())}
            edit.apply()
            removed.forEach{contactPhoto(this,widgetId,it).delete()}
            AccentRefreshWorker.enqueue(this)
            lifecycleScope.launch {
                try{refreshContactWidgets(applicationContext)}catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}catch(_:Exception){}
                finish()
            }
        }catch(_:Exception){saving=false;error="Could not save contacts. Please try again."}
    }
    override fun onSaveInstanceState(outState:Bundle){
        super.onSaveInstanceState(outState)
        outState.putString("pendingContactPhoto",pendingContactPhoto?.toString())
        outState.putInt("style",style);outState.putInt("slot",slot);outState.putInt("pickerSlot",pickerSlot);outState.putString("token",token)
        outState.putIntArray("drafts",drafts.toIntArray());outState.putIntArray("removed",removed.toIntArray())
        outState.putString("entries",JSONArray().apply{inputs.forEach{put(JSONObject().put("name",it.name).put("number",it.number).put("telegram",it.telegram))}}.toString())
    }
    override fun onDestroy(){if(isFinishing)repeat(8){draft(it).delete()};super.onDestroy()}
}
