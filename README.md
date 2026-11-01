# Minimal Widgets

A personal Android app offering minimal dark home screen widgets.

## Requirements
- Open the widget gallery first.
- Use equal-sized gallery cards with a 130dp preview area.
- Let the user change the accent color inside widgets; default #FFC000.

## Design
Black background, dark gray cards and one configurable accent. Title bar with search and an accent-color dot, scrollable category tabs, two-column widget grid, preview above name and size/free/pro metadata, and a pill-shaped bottom navigation. Gallery categories and cards must match real available widgets.

## Technology
- Kotlin; Jetpack Compose and Material 3 dark theme for app UI.
- Jetpack Glance for home screen widgets.
- Package: `com.aarish.minimalwidgets`.
- DataStore Preferences stores the accent as an Int; default `0xFFFFC000`.
- Required UI dependencies include `material-icons-extended` and `datastore-preferences`.
- Wi-Fi and mobile data controls open system settings panels rather than directly toggling connectivity on Android 10+.

## Existing work (reported, not uploaded or verified)
Three Kotlin files have been written but not compiled or run:
- `MainActivity.kt`: launcher opens gallery, collects saved accent, passes it to HomeScreen.
- `HomeScreen.kt`: category tabs; fixed-size LazyVerticalGrid cards; 130dp previews; Canvas ring, bar, dots and vertical bar previews; bottom navigation; ModalBottomSheet color picker with seven swatches and a hue slider, opened by the accent dot beside search. Currently uses sample battery cards.
- `AccentStore.kt`: DataStore Preferences accent persistence.

This repository currently contains planning documentation. Existing source files must be imported before implementation is continued.

## Roadmap
1. Import existing source, configure the Android build, compile and run the gallery.
2. Build clock and date Glance widgets reading the shared saved accent.
3. Add photo, app folder, volume, Wi-Fi/data and personalized widgets.
4. Replace sample battery cards with the real widget catalog.
5. Refresh placed widgets when the accent changes.

## Validation goals
Verify gallery-first launch, equal card dimensions, accent persistence across restart, widget placement and updates, clock/date behavior, and system settings navigation on a device or emulator.
