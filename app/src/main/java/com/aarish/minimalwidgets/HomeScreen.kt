package com.aarish.minimalwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

val CardTop=Color(0xFF27282D)
val CardBottom=Color(0xFF222222)
val Script=FontFamily.Cursive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(appearance:Appearance, update:suspend ((Appearance)->Appearance)->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var destination by rememberSaveable { mutableStateOf("Widgets") }
    var category by rememberSaveable { mutableStateOf("Battery") }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var picker by rememberSaveable { mutableStateOf(false) }
    var customizeTab by rememberSaveable { mutableStateOf("Themes") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val selected=designCatalog.find { it.id==selectedId }
    fun change(transform:(Appearance)->Appearance) { scope.launch {
        try { update(transform) } catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled} catch(_:Exception){error="Could not save. Please try again."}
    } }
    fun favorite(d:Design)=change { it.copy(favorites=if(d.id in it.favorites)it.favorites-d.id else it.favorites+d.id) }
    fun pin(d:Design) {
        val receiver=d.readyReceiver()?:return
        val manager=AppWidgetManager.getInstance(context)
        if(!manager.isRequestPinAppWidgetSupported){Toast.makeText(context,"Long-press your home screen and choose Widgets → Minimal Widgets.",Toast.LENGTH_LONG).show();return}
        val callback=if(d.category=="Battery") PendingIntent.getBroadcast(context,d.style,Intent(context,BatteryPinReceiver::class.java).apply { action="${context.packageName}.PIN_BATTERY_${d.style}";putExtra("style",d.style) },PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE) else null
        if(!manager.requestPinAppWidget(ComponentName(context,receiver),null,callback))Toast.makeText(context,"The launcher could not add this widget.",Toast.LENGTH_LONG).show()
        selectedId=null
    }
    BackHandler(destination!="Widgets" || selected!=null || searching) { when {selected!=null->selectedId=null;searching->{searching=false;query=""};else->destination="Widgets"} }
    Scaffold(containerColor=Color.Black,topBar={
        Column(Modifier.statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
                Text(when(destination){"Customize"->"Customize";"Settings"->"Settings";"Favorites"->"Favorites";else->"Widgets"},fontSize=30.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                FilledTonalButton(onClick={picker=true},colors=ButtonDefaults.filledTonalButtonColors(containerColor=Color(appearance.accent),contentColor=Color.Black),contentPadding=PaddingValues(horizontal=14.dp,vertical=8.dp)) { Text("Accent",fontWeight=FontWeight.Bold) }
                if(destination=="Widgets"||destination=="Favorites")IconButton(onClick={searching=!searching;if(!searching)query=""}){Icon(Icons.Outlined.Search,"Search widgets")}
            }
            if(searching && (destination=="Widgets"||destination=="Favorites"))OutlinedTextField(query,{query=it},label={Text("Search widgets")},singleLine=true,modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp))
            if(destination=="Widgets")ScrollableTabRow(galleryCategories.indexOf(category),containerColor=Color.Black,edgePadding=14.dp,divider={}) {
                galleryCategories.forEach { name->Tab(selected=category==name,onClick={category=name;query=""},text={Text(name,fontFamily=Script,fontSize=16.sp,color=if(category==name)Color.White else Color.Gray)}) }
            }
            if(destination=="Customize")TabRow(if(customizeTab=="Themes")0 else 1,containerColor=Color.Black,divider={}) {
                listOf("Themes","Studio").forEach{name->Tab(customizeTab==name,{customizeTab=name},text={Text(name,fontFamily=Script,fontSize=19.sp)})}
            }
        }
    }) { padding->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when(destination) {
                "Customize" -> if(customizeTab=="Themes")ThemesPage(appearance,{ preset->change { preset.copy(favorites=it.favorites,mode=it.mode,reactive=it.reactive) } }) else StudioPage(appearance,::change,{picker=true})
                "Settings" -> SettingsPage(appearance)
                else -> {
                    val visible=designCatalog.filter { (if(destination=="Favorites")it.id in appearance.favorites else it.category==category) && (query.isBlank() || it.name.contains(query,true)) }
                    if(visible.isEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(if(destination=="Favorites"&&query.isBlank())"Tap a heart to save your favorite designs" else "No widgets found",color=Color.Gray,modifier=Modifier.padding(24.dp))}
                    else LazyVerticalGrid(GridCells.Fixed(2),modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(start=12.dp,end=12.dp,top=16.dp,bottom=116.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        items(visible,key={it.id}){d->DesignCard(d,appearance,{selectedId=d.id},{favorite(d)})}
                    }
                }
            }
            Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start=22.dp,end=22.dp,bottom=14.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                Row(Modifier.weight(1f).height(64.dp).background(Color(0xF2131313),CircleShape).border(1.dp,Color(0xFF303030),CircleShape).padding(6.dp),horizontalArrangement=Arrangement.SpaceAround,verticalAlignment=Alignment.CenterVertically) {
                    NavIcon(Icons.Outlined.Widgets,"Widget gallery",destination=="Widgets"&&category!="Photo",appearance.accent){destination="Widgets";category="Battery";searching=false;query=""}
                    NavIcon(Icons.Outlined.Image,"Photo gallery",destination=="Widgets"&&category=="Photo",appearance.accent){destination="Widgets";category="Photo";searching=false;query=""}
                    NavIcon(Icons.Outlined.Settings,"Settings",destination=="Settings",appearance.accent){destination="Settings";searching=false}
                    NavIcon(Icons.Outlined.FavoriteBorder,"Favorites",destination=="Favorites",appearance.accent){destination="Favorites";query="";searching=false}
                }
                Box(Modifier.size(64.dp).background(Color(0xFF141414),CircleShape).border(1.dp,Color(0xFF303030),CircleShape),contentAlignment=Alignment.Center){NavIcon(Icons.Outlined.Edit,"Customize",destination=="Customize",appearance.accent){destination="Customize";searching=false}}
            }
        }
    }
    if(picker)AccentPicker(appearance.accent,{picker=false}){color->change{it.copy(accent=color,theme="Custom")};picker=false}
    if(selected!=null)ModalBottomSheet(onDismissRequest={selectedId=null},containerColor=CardBottom) {
        Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(selected.name,fontSize=24.sp,fontWeight=FontWeight.Bold)
            DesignPreview(selected,appearance,Modifier.fillMaxWidth().height(200.dp))
            Text("Home-screen size: ${selected.size}",color=Color.LightGray)
            if(selected.category in compactCategories)Text("1×1 supported. Final cell size depends on your launcher.",fontSize=13.sp,color=Color.Gray)
            if(selected.readyReceiver()!=null) {
                Text(when(selected.category){"Battery"->"Uses your real battery level. Tap to open Android battery settings.";"Contacts"->"Adds a basic contact shortcut. Tap Set contact to choose a number. Call widgets dial immediately after you grant phone permission; message widgets open a message composer. Edit changes the saved contact. At 1×1, the contact name is the primary action.";"Music"->"Play/pause for Android's current media player. Start a song in your music app first. This basic control does not display album artwork or song metadata.";"Photo"->"Adds the existing basic photo widget. After adding, tap the widget to choose a photo.";"Folder Apps"->"Adds the existing basic folder widget. After adding, tap Edit folder to choose up to four apps.";"Personalize"->"Adds the existing basic text widget. After adding, tap the widget to write your text.";"Quick Settings"->"Wi-Fi and data open their Android settings panel. Volume uses +/− buttons to change media volume; Settings opens the volume panel.";else->"Live clock respects your system time format. Tap to open your clock app's alarms."},fontSize=14.sp,color=Color.LightGray)
                Button(onClick={pin(selected)},modifier=Modifier.fillMaxWidth()){Text("Add to home screen")}
            } else {
                Text("Design preview",color=Color(appearance.accent),fontWeight=FontWeight.Bold)
                Text("This layout is in the gallery; its home-screen provider is not implemented yet.",color=Color.LightGray)
            }
            TextButton(onClick={favorite(selected)}){Text(if(selected.id in appearance.favorites)"Remove favorite" else "Save favorite")}
            Spacer(Modifier.navigationBarsPadding().height(12.dp))
        }
    }
    error?.let{message->AlertDialog(onDismissRequest={error=null},title={Text("Save failed")},text={Text(message)},confirmButton={TextButton(onClick={error=null}){Text("OK")}})}
}

@Composable
fun NavIcon(icon:ImageVector,label:String,selected:Boolean,accent:Int,onClick:()->Unit) {
    IconButton(onClick=onClick,modifier=Modifier.size(50.dp).background(if(selected)Color(accent) else Color.Transparent,CircleShape)) { Icon(icon,label,tint=if(selected)Color.Black else Color(0xFF999999),modifier=Modifier.size(25.dp)) }
}

@Composable
fun DesignPreview(d:Design,a:Appearance,modifier:Modifier=Modifier) {
    val bitmap=remember(d.id,a.accent,a.background,a.secondary,a.text,a.reactive){DesignRenderer.render(d,a).asImageBitmap()}
    Image(bitmap,contentDescription="${d.name} preview",modifier=modifier)
}

@Composable
fun DesignCard(d:Design,a:Appearance,open:()->Unit,favorite:()->Unit) {
    Column(Modifier.fillMaxWidth().height(236.dp).clip(RoundedCornerShape(17.dp)).background(CardBottom).clickable(onClick=open).semantics { contentDescription="${d.name}, ${d.size}" }) {
        Box(Modifier.fillMaxWidth().height(164.dp).background(CardTop).padding(8.dp),contentAlignment=Alignment.Center){DesignPreview(d,a,Modifier.fillMaxSize())}
        Row(Modifier.fillMaxWidth().height(72.dp).padding(start=10.dp,end=2.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(d.name,maxLines=1,overflow=TextOverflow.Ellipsis,fontFamily=Script,fontSize=15.sp)
                Text("${d.size} · ${if(d.readyReceiver()!=null)"READY" else "PREVIEW"}",fontSize=10.sp,color=Color.Gray)
            }
            IconButton(onClick=favorite,modifier=Modifier.size(40.dp)){Icon(if(d.id in a.favorites)Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,"Favorite ${d.name}",tint=if(d.id in a.favorites)Color(a.accent) else Color.Gray,modifier=Modifier.size(21.dp))}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccentPicker(initial:Int,dismiss:()->Unit,save:(Int)->Unit) {
    var draft by remember { mutableIntStateOf(initial) }
    val hsv=remember(draft){FloatArray(3).also{android.graphics.Color.colorToHSV(draft,it)}}
    ModalBottomSheet(onDismissRequest=dismiss,containerColor=CardBottom) {
        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("Your accent",fontSize=24.sp)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                listOf(0xFFFFC000,0xFFFF7043,0xFFEF5350,0xFFAB47BC,0xFF42A5F5,0xFF26A69A,0xFF9CCC65).forEach{value->val color=value.toInt();Box(Modifier.size(34.dp).background(Color(color),CircleShape).clickable{draft=color}.semantics{contentDescription="Accent ${String.format("#%06X",color and 0xFFFFFF)}"},contentAlignment=Alignment.Center){if(draft==color)Text("✓",color=Color.Black)}}
            }
            Text("Hue")
            Slider(hsv[0],{draft=android.graphics.Color.HSVToColor(floatArrayOf(it,.85f,1f))},valueRange=0f..360f)
            Box(Modifier.fillMaxWidth().height(5.dp).background(Color(draft),CircleShape))
            Button(onClick={save(draft)},modifier=Modifier.fillMaxWidth()){Text("Apply accent")}
            Spacer(Modifier.navigationBarsPadding().height(12.dp))
        }
    }
}

