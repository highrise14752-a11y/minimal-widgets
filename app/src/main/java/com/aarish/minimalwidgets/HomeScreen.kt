package com.aarish.minimalwidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(accent: Int, saveAccent: suspend (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var category by rememberSaveable { mutableStateOf("All") }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var picker by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val categories = listOf("All", "Clock", "Photos", "Folders", "Controls", "Personal")
    fun pinWidget(entry: WidgetEntry) {
        val manager = AppWidgetManager.getInstance(context)
        if (manager.isRequestPinAppWidgetSupported) {
            val requested = manager.requestPinAppWidget(ComponentName(context, entry.receiver), null, null)
            if (!requested) Toast.makeText(context, "Long-press your home screen and choose Widgets.", Toast.LENGTH_LONG).show()
        } else Toast.makeText(context, "Long-press your home screen and choose Widgets → Minimal Widgets.", Toast.LENGTH_LONG).show()
    }
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Widgets", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { picker = true }) {
                        Box(Modifier.size(24.dp).background(Color(accent), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Palette, "Change accent color", tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = { searching = !searching; if (!searching) query = "" }) { Icon(Icons.Outlined.Search, "Search widgets") }
                }
                if (searching) OutlinedTextField(query, { query = it }, label = { Text("Search widgets") },
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                ScrollableTabRow(selectedTabIndex = categories.indexOf(category), containerColor = Color.Black, edgePadding = 12.dp) {
                    categories.forEach { name -> Tab(selected = category == name, onClick = { category = name }, text = { Text(name) }) }
                }
            }
        },
        bottomBar = {
            Row(Modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth().background(Color(0xFF222222), RoundedCornerShape(50)),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { category = "All"; query = "" }) {
                    Icon(Icons.Outlined.Widgets, null); Spacer(Modifier.width(8.dp)); Text("Gallery")
                }
                TextButton(onClick = { picker = true }) { Text("Appearance") }
            }
        }
    ) { padding ->
        val visible = widgetCatalog.filter { (category == "All" || category == it.category) && it.name.contains(query, ignoreCase = true) }
        if (visible.isEmpty()) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("No widgets found") }
        else LazyVerticalGrid(GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(visible, key = { it.name }) { widget ->
                Card(onClick = { pinWidget(widget) },
                    modifier = Modifier.fillMaxWidth().height(218.dp), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF191919))) {
                    Box(Modifier.fillMaxWidth().height(130.dp).padding(12.dp), contentAlignment = Alignment.Center) {
                        if (widget.category == "Clock") Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("10:08", color = Color(accent), fontSize = 32.sp, fontWeight = FontWeight.Light)
                            Text("Mon, 1 Jan", color = Color.LightGray, fontSize = 12.sp)
                        } else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(when(widget.name) {
                                "Photo" -> "Choose photo"
                                "App folder" -> "Choose apps"
                                "Volume" -> "Media volume"
                                "Personal note" -> "Tap to write a note"
                                else -> widget.name
                            }, color = Color(accent), fontSize = 16.sp)
                            if (widget.name == "Volume") Text("−    +    Settings", color = Color(accent), fontSize = 12.sp)
                            if (widget.name == "App folder") Text("Edit folder", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                    Column(Modifier.padding(horizontal = 12.dp)) {
                        Text(widget.name, maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("${widget.size} · Free",
                            color = Color.Gray, fontSize = 11.sp)
                        Text("Tap to add", color = Color(accent), fontSize = 11.sp)
                    }
                }
            }
        }
    }
    if (picker) {
        var draft by remember { mutableIntStateOf(accent) }
        val hsv = remember(draft) { FloatArray(3).also { android.graphics.Color.colorToHSV(draft, it) } }
        ModalBottomSheet(onDismissRequest = { if (!saving) picker = false }, containerColor = Color(0xFF191919)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Your accent", fontSize = 22.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(0xFFFFC000, 0xFFFF7043, 0xFFEF5350, 0xFFAB47BC, 0xFF42A5F5, 0xFF26A69A, 0xFF9CCC65).forEach { swatch ->
                        val color = swatch.toInt()
                        Surface(onClick = { draft = color }, shape = CircleShape, color = Color(color), modifier = Modifier.size(32.dp)) {
                            if (draft == color) Box(contentAlignment = Alignment.Center) { Text("✓", color = Color.Black) }
                        }
                    }
                }
                Text("Hue", color = Color.LightGray)
                Slider(value = hsv[0], onValueChange = { draft = android.graphics.Color.HSVToColor(floatArrayOf(it, 0.85f, 1f)) }, valueRange = 0f..360f)
                Box(Modifier.fillMaxWidth().height(6.dp).background(Color(draft), CircleShape))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(enabled = !saving, onClick = {
                    saving = true; error = null
                    scope.launch {
                        try { saveAccent(draft); picker = false }
                        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                        catch (_: Exception) { error = "Could not save. Please try again." }
                        finally { saving = false }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(if (saving) "Saving…" else "Apply accent") }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
