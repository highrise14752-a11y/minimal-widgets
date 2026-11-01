package com.aarish.minimalwidgets

data class WidgetEntry(val name: String, val category: String, val size: String, val receiver: Class<*>)
val widgetCatalog = listOf(
    WidgetEntry("Clock & date", "Clock", "3 × 2", ClockWidgetReceiver::class.java),
    WidgetEntry("Photo", "Photos", "3 × 2", PhotoReceiver::class.java),
    WidgetEntry("App folder", "Folders", "3 × 2", FolderReceiver::class.java),
    WidgetEntry("Volume", "Controls", "3 × 2", VolumeReceiver::class.java),
    WidgetEntry("Wi-Fi settings", "Controls", "3 × 2", WifiReceiver::class.java),
    WidgetEntry("Mobile data settings", "Controls", "3 × 2", DataReceiver::class.java),
    WidgetEntry("Personal note", "Personal", "3 × 2", NoteReceiver::class.java)
)
