package com.aarish.minimalwidgets

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ThemePreset(val name:String,val category:String,val accent:Long,val background:Long=0xFF000000,val secondary:Long=0xFF222225,val text:Long=0xFFFFFFFF) {
    fun appearance()=Appearance(accent.toInt(),background.toInt(),secondary.toInt(),text.toInt(),theme=name)
}
private val presets=listOf(
    ThemePreset("Default","Dark",0xFFFFC000),ThemePreset("Pure Black","Dark",0xFFFFFFFF),
    ThemePreset("Midnight Blue","Dark",0xFF2196F3,secondary=0xFF001529),ThemePreset("Nothing Red","Dark",0xFFEA1622),
    ThemePreset("Neon Green","Dark",0xFF14EE05),ThemePreset("Electric Purple","Dark",0xFF9825E4,secondary=0xFF180022),
    ThemePreset("Crimson Red","Dark",0xFFDC0038),ThemePreset("Golden Yellow","Dark",0xFFFFD600),
    ThemePreset("Ocean Blue","Dark",0xFF0077CC),ThemePreset("Hot Pink","Dark",0xFFFF5CBD),ThemePreset("Turquoise","Dark",0xFF35D9CC),
    ThemePreset("Orange Fire","Dark",0xFFFF5400),ThemePreset("Magenta","Dark",0xFFEE00FF),
    ThemePreset("Slate Dark","Dark",0xFF00FFFF,0xFF191C21,0xFF30343A),ThemePreset("Emerald Night","Dark",0xFF49C77C,secondary=0xFF00260E),
    ThemePreset("Matrix","Dark",0xFF00F04F),ThemePreset("Cyberpunk Red","Dark",0xFFFF004F,0xFF1B0808,0xFF000000),
    ThemePreset("Electric Storm","Dark",0xFFFFFF00,0xFF001322,0xFF002A48),ThemePreset("Quantum Blue","Dark",0xFFFF00FF,0xFF020A2B,0xFF001248),
    ThemePreset("Dark Lavender","Dark Pastel",0xFFB39DDB,0xFF111111),ThemePreset("Deep Sea","Dark",0xFFFFFFFF,0xFF002B36,0xFF073642),
    ThemePreset("Forest Green","Dark",0xFF388E3C,0xFF19321D),ThemePreset("Royal Gold","Dark",0xFFBC9B19,0xFF001C34),
    ThemePreset("Carbon","Dark",0xFFFF7900,0xFF141414),ThemePreset("Plum Night","Dark Pastel",0xFFD568D3,0xFF210020,0xFF320030),
    ThemePreset("Sand Dark","Dark Pastel",0xFFFFCF86,0xFF211300,0xFF382000),ThemePreset("Minty Night","Dark Pastel",0xFFB2DFDB,0xFF001B18,0xFF003532),
    ThemePreset("Nord Dark","Dark Pastel",0xFF88C0D0,0xFF2E3440,0xFF434C5E),ThemePreset("Dracula","Dark",0xFFBD93F9,0xFF282A36,0xFF44475A),
    ThemePreset("Matcha Night","Dark Pastel",0xFFA5D6A7,0xFF1B321D,0xFF000000),ThemePreset("Coffee Roast","Dark Pastel",0xFFD7CCC8,0xFF20130E,0xFF432A24),
    ThemePreset("Charcoal Gold","Dark Pastel",0xFFFFDF7B,0xFF202020),ThemePreset("Ash & Lemon","Dark Pastel",0xFFFFF59D,0xFF222222,0xFF111111),
    ThemePreset("Night Mint","Dark Pastel",0xFF80CBC4,0xFF202020),ThemePreset("Shadow Peach","Dark Pastel",0xFFFFCCBC,0xFF321F1F),
    ThemePreset("White Blue","Light",0xFF2196F3,0xFFFFFFFF,0xFFE7E7EE,0xFF000000),ThemePreset("White Red","Light",0xFFF44336,0xFFFFFFFF,0xFFFFECE8,0xFF000000),
    ThemePreset("White Green","Light",0xFF4CAF50,0xFFFFFFFF,0xFFE8F5E9,0xFF000000),ThemePreset("White Pink","Light",0xFFE91E63,0xFFFFFFFF,0xFFFCE4EC,0xFF000000),
    ThemePreset("White Orange","Light",0xFFFF9800,0xFFFFFFFF,0xFFFFF3E0,0xFF000000),ThemePreset("White Teal","Light",0xFF009688,0xFFFFFFFF,0xFFE0F2F1,0xFF000000),
    ThemePreset("White Indigo","Light",0xFF3F51B5,0xFFFFFFFF,0xFFE8EAF6,0xFF000000),ThemePreset("White Amber","Light",0xFFFFC107,0xFFFFFFFF,0xFFFFF8E1,0xFF000000),
    ThemePreset("Soft Sand","Pastel",0xFF795548,0xFFFFFBF3,0xFFFAF3DD,0xFF000000),ThemePreset("Lavender Sky","Pastel",0xFF673AB7,0xFFFCFAFF,0xFFF3E5F5,0xFF000000),
    ThemePreset("Peach Mint","Pastel",0xFF4DB6AC,0xFFFFFCF8,0xFFE0F2F1,0xFF000000),ThemePreset("Snow Blue","Pastel",0xFF81D4FA,0xFFFFFFFF,0xFFE1F5FE,0xFF000000),
    ThemePreset("Cotton Cream","Pastel",0xFFD0B38B,0xFFFFFEF8,0xFFF8F6ED,0xFF000000),ThemePreset("Nord Light","Light",0xFF5E81AC,0xFFECEFF4,0xFFE5E9F0,0xFF000000)
)

@Composable
fun PaletteRow(a:Appearance,title:String,subtitle:String,selected:Boolean=false,onClick:(()->Unit)?=null) {
    val m=Modifier.fillMaxWidth().height(95.dp).border(if(selected)2.dp else 1.dp,if(selected)Color(a.accent) else Color(0xFF353535),RoundedCornerShape(22.dp)).background(Color(a.background),RoundedCornerShape(22.dp))
    Row((if(onClick!=null)m.clickable(onClick=onClick) else m).padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {Text(title,color=Color(a.text),fontFamily=Script,fontSize=19.sp,maxLines=1);Text(subtitle,color=Color(a.text).copy(alpha=.5f),fontSize=12.sp)}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(a.accent,a.background,a.secondary,a.text).forEach{color->Box(Modifier.size(29.dp).background(Color(color),CircleShape).border(1.dp,Color.Gray.copy(alpha=.4f),CircleShape))}}
    }
}

@Composable
fun ThemesPage(a:Appearance,select:(Appearance)->Unit) {
    var category by rememberSaveableCompat("All")
    Column(Modifier.verticalScroll(rememberScrollState()).padding(start=16.dp,end=16.dp,bottom=118.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SectionLabel("ACTIVE THEME");PaletteRow(a,a.theme,"Your widget palette")
        SectionLabel("CATEGORIES")
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("All","Dark","Light","Pastel","Dark Pastel").forEach{name->FilterChip(category==name,{category=name},label={Text(name,fontFamily=Script)})}}
        val shown=presets.filter { category=="All" || category==it.category }
        SectionLabel("${shown.size} THEMES · ALL AVAILABLE")
        shown.forEach{preset->PaletteRow(preset.appearance(),preset.name,if(a.theme==preset.name)"Active" else "Apply palette",a.theme==preset.name){select(preset.appearance())}}
    }
}

@Composable private fun rememberSaveableCompat(initial:String)=androidx.compose.runtime.saveable.rememberSaveable{mutableStateOf(initial)}
@Composable fun SectionLabel(text:String) {Text(text,fontFamily=Script,fontSize=13.sp,color=Color.Gray,modifier=Modifier.padding(top=16.dp,bottom=3.dp))}

@Composable
fun StudioPage(a:Appearance,change:((Appearance)->Appearance)->Unit,accentPicker:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var dialog by remember {mutableStateOf<String?>(null)}
    var status by remember {mutableStateOf<String?>(null)}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->if(uri!=null)scope.launch {
        status=try{withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri,"wt")?.bufferedWriter()?.use{it.write(a.json())}?:error("Unable to write")};"Backup exported"}catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}catch(_:Exception){"Could not export backup"}
    }}
    val import=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch {
        try{val restored=withContext(Dispatchers.IO){val raw=context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readTextLimited()}?:error("Unable to read");Appearance.parse(raw)};change{restored};status="Backup imported"}catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}catch(_:Exception){status="Could not import: choose a valid Minimal Widgets backup"}
    }}
    Column(Modifier.verticalScroll(rememberScrollState()).padding(start=16.dp,end=16.dp,bottom=118.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
        PaletteRow(a,"Main Text","Sub Text")
        SectionLabel("COLORS")
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            listOf("Background" to a.background,"Secondary" to a.secondary,"Accent" to a.accent).forEach{(name,color)->
                Column(Modifier.weight(1f).height(90.dp).background(Color(color),RoundedCornerShape(18.dp)).border(1.dp,Color.DarkGray,RoundedCornerShape(18.dp)).clickable{if(name=="Accent")accentPicker()else dialog=name}.padding(9.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Text(name,color=if(android.graphics.Color.red(color)+android.graphics.Color.green(color)+android.graphics.Color.blue(color)>420)Color.Black else Color.White,fontFamily=Script,fontSize=16.sp);Text(String.format("#%06X",color and 0xFFFFFF),color=Color.Gray,fontSize=11.sp)}
            }
        }
        SectionLabel("TEXT BEHAVIOR")
        SettingsRow(Icons.Outlined.AutoAwesome,"Reactive Color",a.reactive,a.accent){dialog="Reactive Color"}
        SectionLabel("NEW WIDGET PREFERENCE")
        SettingsRow(Icons.Outlined.AspectRatio,"Default Widget Mode",a.mode,a.accent){dialog="Default Widget Mode"}
        Text("Sizing is a saved preference. Your launcher controls the placed widget's final dimensions; existing widgets keep their size.",color=Color.Gray,fontSize=12.sp)
        SectionLabel("BACKUP")
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={export.launch("minimal-widgets-theme.json")},modifier=Modifier.weight(1f),contentPadding=PaddingValues(8.dp)){Text("Export")}
            OutlinedButton(onClick={import.launch(arrayOf("application/json","text/plain"))},modifier=Modifier.weight(1f),contentPadding=PaddingValues(8.dp)){Text("Import")}
            OutlinedButton(onClick={context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,a.json())},"Share theme"))},modifier=Modifier.weight(1f),contentPadding=PaddingValues(8.dp)){Text("Share")}
        }
        TextButton(onClick={dialog="Reset"},modifier=Modifier.align(Alignment.CenterHorizontally)){Text("Reset appearance to default",color=Color.Gray)}
        status?.let{Text(it,color=Color(a.accent))}
    }
    when(val open=dialog) {
        "Reactive Color","Default Widget Mode" -> {
            val options=if(open=="Reactive Color")Appearance.reactiveModes else Appearance.widgetModes
            var draft by remember(open){mutableStateOf(if(open=="Reactive Color")a.reactive else a.mode)}
            AlertDialog(onDismissRequest={dialog=null},title={Text(open,fontFamily=Script)},text={Column(Modifier.verticalScroll(rememberScrollState())){options.forEach{option->Row(Modifier.fillMaxWidth().clickable{draft=option}.padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(draft==option,{draft=option});Text(option)}}}},confirmButton={TextButton(onClick={change{if(open=="Reactive Color")it.copy(reactive=draft)else it.copy(mode=draft)};dialog=null}){Text("Apply")}},dismissButton={TextButton(onClick={dialog=null}){Text("Cancel")}})
        }
        "Background","Secondary" -> {
            var hex by remember(open){mutableStateOf(String.format("%06X",(if(open=="Background")a.background else a.secondary) and 0xFFFFFF))}
            val valid=hex.matches(Regex("[0-9a-fA-F]{6}"))
            AlertDialog(onDismissRequest={dialog=null},title={Text(open)},text={OutlinedTextField(hex,{hex=it.take(6)},label={Text("Hex color (RRGGBB)")},singleLine=true,isError=!valid)},confirmButton={TextButton(enabled=valid,onClick={val color=hex.toLong(16).toInt() or 0xFF000000.toInt();change{if(open=="Background")it.copy(background=color,theme="Custom")else it.copy(secondary=color,theme="Custom")};dialog=null}){Text("Apply")}},dismissButton={TextButton(onClick={dialog=null}){Text("Cancel")}})
        }
        "Reset" -> AlertDialog(onDismissRequest={dialog=null},title={Text("Reset appearance?")},text={Text("Restore the default palette and text behavior. Favorites and configured widget content are kept.")},confirmButton={TextButton(onClick={change{Appearance(favorites=it.favorites)};dialog=null}){Text("Reset")}},dismissButton={TextButton(onClick={dialog=null}){Text("Cancel")}})
    }
}
private fun java.io.Reader.readTextLimited():String { val buffer=CharArray(65537);var count=0;while(count<buffer.size){val n=read(buffer,count,buffer.size-count);if(n<0)break;count+=n};require(count<=65536);return String(buffer,0,count) }

@Composable
fun SettingsRow(icon:ImageVector,title:String,subtitle:String?=null,accent:Int,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().defaultMinSize(minHeight=67.dp).clickable(onClick=onClick).padding(horizontal=16.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,tint=Color(accent),modifier=Modifier.size(23.dp));Spacer(Modifier.width(16.dp));Column(Modifier.weight(1f)){Text(title,fontFamily=Script,fontSize=18.sp);if(subtitle!=null)Text(subtitle,fontSize=12.sp,color=Color.Gray)};Icon(Icons.Outlined.ChevronRight,null,tint=Color.Gray)
    }
}

@Composable
fun SettingsPage(a:Appearance) {
    val context=LocalContext.current
    var info by remember{mutableStateOf<Pair<String,String>?>(null)}
    fun open(intent:Intent) { try{context.startActivity(intent)}catch(_:android.content.ActivityNotFoundException){info="Not available" to "Your device does not expose this settings page."} }
    @Composable fun Group(title:String,content:@Composable ()->Unit) {
        Column(Modifier.fillMaxWidth().background(Color(0xFF19191F),RoundedCornerShape(20.dp)).border(1.dp,Color(0xFF29292F),RoundedCornerShape(20.dp))) {Text(title,fontFamily=Script,fontSize=20.sp,color=Color.LightGray,modifier=Modifier.padding(16.dp));content()}
    }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(start=16.dp,end=16.dp,bottom=118.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Group("App · 0.2.0") {
            SettingsRow(Icons.Outlined.Update,"Source & updates","Open your GitHub repository",a.accent){open(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/highrise14752-a11y/minimal-widgets")))}
            HorizontalDivider(color=Color.DarkGray,modifier=Modifier.padding(start=55.dp,end=16.dp))
            SettingsRow(Icons.Outlined.NewReleases,"What's new",accent=a.accent){info="What's new" to "Reference-inspired gallery across 11 categories; Themes, Studio, favorites and real battery widgets. Cards marked Preview do not yet have a home-screen provider."}
        }
        Group("Permissions & widgets") {
            SettingsRow(Icons.Outlined.Notifications,"Notifications","Manage Android notification preferences",a.accent){open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))}
            SettingsRow(Icons.Outlined.BatteryChargingFull,"Background running","Open app battery settings",a.accent){open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))}
            SettingsRow(Icons.Outlined.Sync,"Refresh placed widgets","Apply saved accent and refresh content",a.accent){AccentRefreshWorker.enqueue(context);info="Refresh requested" to "Android will refresh your placed widgets. Battery updates also follow the system widget schedule."}
            SettingsRow(Icons.Outlined.HelpOutline,"Help & FAQ",accent=a.accent){info="Help & FAQ" to "Tap a READY card, then Add to home screen. Your launcher confirms placement. Tap photos, folders or text widgets to configure them. Wi-Fi and data open Android settings panels. PREVIEW cards are visual layouts under development. Launcher grids may round the suggested widget size."}
        }
        Group("Feedback & support") {
            SettingsRow(Icons.Outlined.BugReport,"Report a bug","Open repository issues",a.accent){open(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/highrise14752-a11y/minimal-widgets/issues")))}
            SettingsRow(Icons.Outlined.PrivacyTip,"Privacy",accent=a.accent){info="Privacy" to "Widget content, photos, app selections and appearance are stored locally. This app has no analytics or advertising SDK. Exported theme backups contain your palette, preferences and favorite design IDs. External links open in another app."}
        }
        Group("About") {
            SettingsRow(Icons.Outlined.Info,"Minimal Widgets","Aarish's personal widget collection",a.accent){info="Minimal Widgets" to "Built with Kotlin, Jetpack Compose, Material 3 and Glance. Original code-drawn previews are inspired by your supplied layouts. No premium subscription is required. Some reference fonts and artwork are approximated."}
        }
    }
    info?.let{(title,message)->AlertDialog(onDismissRequest={info=null},title={Text(title)},text={Text(message)},confirmButton={TextButton(onClick={info=null}){Text("OK")}})}
}


