# Minimal Widgets

A personal Android 10+ widget app for Aarish. Kotlin, Jetpack Compose, Material 3 and Jetpack Glance. Package: `com.aarish.minimalwidgets`.

## Reference gallery · v0.2

The first screen is the widget gallery. It has eleven categories: Battery, Digital Clock, Hybrid Clock, Contacts, Folder Apps, Games, Music, Personalize, Photo, Productivity and Quick Settings. Themes, Studio and Settings complete the fourteen requested pages.

Every gallery card uses the same two-column dimensions: 236dp tall, with a 164dp preview area. Wide widget designs are scaled inside the same card. Contacts, Games, Productivity and Quick Settings carry the requested 1×1 size; Battery, clocks, folders, music, personalize and photos retain their individual native size labels. Launcher grids determine the final placed cell dimensions.

Includes search within a category, persisted favorites, original code-drawn previews, 49 theme presets, editable background/secondary/accent colors, reactive text colors, a sizing preference, and validated JSON appearance backup export/import/share. Default accent: `#FFC000`. Saved appearance changes enqueue refreshes of placed widgets. Themes are available without a paywall.

## Working widgets and preview designs

**READY** cards connect to an implemented provider. The new nineteen battery designs read actual battery level, retain the selected style separately for each placed instance, and refresh through Glance/WorkManager and Android's periodic widget updates. No battery time-remaining estimates are fabricated. The launcher widget picker defaults to Battery Widget 1; choosing a gallery battery card selects its style after pin confirmation.

Existing live clock/date, basic photo, four-app folder, media volume, Wi-Fi settings, mobile-data settings and personal-text widgets remain available. Photo/folder/text cards explicitly describe the basic placed version. Tap a placed photo/folder/text widget to configure it. Wi-Fi and data open Android settings panels.

**PREVIEW** cards show the requested design direction; most clock typography variants, contact actions, games, music control, photo masks and productivity providers still need implementation. They do not show a nonfunctional Add button. Photos and avatar previews use original procedural drawings; supplied artwork and proprietary fonts were not copied. Counts show the layouts actually included, rather than the reference app's larger catalog totals.

Studio's widget mode is a persisted preference only; it does not retroactively resize widgets or implement a new sizing engine. The 1×1 provider minima are enabled for the existing quick-setting providers, with launcher-dependent sizing.

## Build and verification

Use JDK 17 and Android SDK 35. The Gradle 8.9 wrapper is included.

```sh
bash ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
bash ./gradlew :app:connectedDebugAndroidTest
```

On Windows use `gradlew.bat`. See [BUILDING.md](BUILDING.md) for device checks and current limitations. GitHub Actions builds and lints the app, then separately boots an Android 10 emulator, runs instrumentation tests and uploads test reports/screenshots. Check the current branch's Actions result before assuming those device checks passed.

