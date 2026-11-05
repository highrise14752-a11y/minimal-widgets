package com.aarish.minimalwidgets

/** Gallery dimensions are independent of the launcher's widget cell dimensions. */
data class Design(val id: String, val name: String, val category: String, val style: Int, val size: String)
val galleryCategories = listOf("Battery", "Digital Clock", "Hybrid Clock", "Contacts", "Folder Apps", "Games", "Music", "Personalize", "Photo", "Productivity", "Quick Settings")
val compactCategories = setOf("Contacts", "Games", "Productivity", "Quick Settings")
private fun designs(category: String, names: List<String>, sizes: List<String> = emptyList()) = names.mapIndexed { i, name ->
    Design("${category.lowercase().replace(' ', '-')}-${i + 1}", name, category, i + 1,
        if (category in compactCategories) "1 × 1" else sizes.getOrElse(i) { "2 × 2" })
}
val designCatalog: List<Design> = buildList {
    addAll(designs("Battery", (1..19).map { "Battery Widget $it" }, listOf("2 × 2", "2 × 2", "2 × 2", "2 × 1", "2 × 2", "2 × 2", "2 × 2", "1 × 2", "2 × 1", "2 × 2", "2 × 1", "2 × 2", "2 × 2", "2 × 2", "2 × 2", "2 × 2", "4 × 2", "1 × 1", "2 × 2")))
    addAll(designs("Digital Clock", (1..34).map { "Digital Clock $it" }, (1..34).map { if (it in listOf(20, 31, 33)) "2 × 2" else if (it == 34) "2 × 1" else "4 × 2" }))
    addAll(designs("Hybrid Clock", (1..11).map { "Clock Hybrid #$it" } + listOf("World Clock Stack", "World Clock Dual", "World Clock Quad", "World Clock Horizon", "World Clock Overlap"), (1..16).map { if (it in listOf(5, 6, 8, 9, 13, 15, 16)) "4 × 1" else "2 × 2" }))
    addAll(designs("Contacts", (1..12).map { "Contact $it" } + listOf("One Click Telegram", "One Click WhatsApp")))
    addAll(designs("Folder Apps", listOf("Apps Folder 2×2", "Apps Folder 3×3", "Apps Folder Wide", "Apps Folder 4×1", "Apps Folder Mix", "Apps Folder Stack"), listOf("2 × 2", "2 × 2", "4 × 2", "4 × 1", "2 × 2", "2 × 2")))
    addAll(designs("Games", listOf("Tic", "Dice", "Breakout", "Snake", "Sliding Puzzle", "Dino")))
    addAll(designs("Music", (1..12).map { "Music $it" }, (1..12).map { when (it) { 2, 3 -> "4 × 2"; 5 -> "4 × 1"; 6 -> "3 × 2"; 7 -> "2 × 1"; 10 -> "1 × 1"; else -> "2 × 2" } }))
    addAll(designs("Personalize", listOf("Personal Card Wide", "Personal Card", "Personal Card Portrait", "Personal Card Compact", "Birthday Countdown", "Anniversary Countdown", "Countdown Bar", "Name / Text Plate", "Text Plate Square", "Age Counter", "Photo Frame", "Couple Frame"), listOf("4 × 2", "2 × 2", "2 × 3", "4 × 1", "2 × 2", "2 × 2", "4 × 1", "2 × 1", "2 × 2", "2 × 2", "2 × 2", "4 × 2")))
    addAll(designs("Photo", listOf("Heart", "Dot Matrix", "Nine Circles", "Organic", "Splash", "Double", "Pixel Triangle", "Pixel Circle", "Clover", "Daisy", "Star Flower", "Sunburst", "Soft Star", "Sharp Star", "Cloud", "Four Petals", "Ghost", "Scallop", "Ripple", "Pebble", "Soft Circle", "Soft Square", "Badge", "Six Petals", "Rounded Hexagon", "Pentagon", "Hexagon", "Diamond", "Triangle", "Tilted Oval", "Oval", "Hourglass", "Inverted Heart", "Quarter", "Arch", "Rounded Square", "Blossom" ).map { "Photo · $it" }))
    addAll(designs("Productivity", listOf("Habit Streak", "Personal QR Code", "Event Countdown", "Year Progress", "Event Countdown Bar", "Bookmark List", "Screentime", "Bookmarks Wide", "Clipboard", "Pomodoro", "Screentime Compact", "Screentime Blocks")))
    val controls = listOf("Wi-Fi", "Mobile data", "Bluetooth", "Airplane mode", "Alarm", "Dark theme", "Do Not Disturb", "Hotspot", "Location", "Auto-rotate", "Sound", "Vibrate", "Torch")
    addAll(designs("Quick Settings", listOf("Alert Slider", "Alert Slider Vertical", "Silent") + (1..4).flatMap { family -> controls.map { "$it $family" } }))
}

/** Only connect designs to providers when their placed appearance and behavior exist. */
fun Design.readyReceiver(): Class<*>? = when {
    category == "Battery" -> when(size) {
        "2 � 1" -> BatteryWideReceiver::class.java
        "1 � 2" -> BatteryTallReceiver::class.java
        "4 � 2" -> BatteryLargeReceiver::class.java
        "1 � 1" -> BatterySmallReceiver::class.java
        else -> BatteryReceiver::class.java
    }
    category == "Digital Clock" && style == 34 -> ClockWidgetReceiver::class.java
    category == "Contacts" && style == 1 -> ContactReceiver::class.java
    category == "Contacts" && style == 11 -> CallReceiver::class.java
    category == "Contacts" && style == 12 -> MessageReceiver::class.java
    category == "Music" && style == 10 -> MusicReceiver::class.java
    category == "Photo" && style == 36 -> PhotoReceiver::class.java
    category == "Folder Apps" && style == 1 -> FolderReceiver::class.java
    category == "Personalize" && style == 8 -> NoteReceiver::class.java
    category == "Quick Settings" && name == "Wi-Fi 2" -> WifiReceiver::class.java
    category == "Quick Settings" && name == "Mobile data 2" -> DataReceiver::class.java
    category == "Quick Settings" && name == "Sound 2" -> VolumeReceiver::class.java
    category == "Quick Settings" -> QuickSettingsReceiver::class.java
    else -> null
}
