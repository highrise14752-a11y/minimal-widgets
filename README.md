# Minimal Widgets

A personal Android 10+ widget app for Aarish. Kotlin, Jetpack Compose, Material 3 and Jetpack Glance. Package: `com.aarish.minimalwidgets`.

## Reference gallery · v0.2

The first screen is the widget gallery. It has eleven categories: Battery, Digital Clock, Hybrid Clock, Contacts, Folder Apps, Games, Music, Personalize, Photo, Productivity and Quick Settings. Themes, Studio and Settings complete the fourteen requested pages.

Every gallery card uses the same two-column dimensions: 236dp tall, with a 164dp preview area. Wide widget designs are scaled inside the same card. Contacts, Games, Productivity and Quick Settings carry the requested 1×1 size; Battery, clocks, folders, music, personalize and photos retain their individual native size labels. Launcher grids determine the final placed cell dimensions.

Includes search within a category, persisted favorites, original code-drawn previews, 49 theme presets, editable background/secondary/accent colors, reactive text colors, a sizing preference, and validated JSON appearance backup export/import/share. Default accent: `#FFC000`. Saved appearance changes enqueue refreshes of placed widgets. Themes are available without a paywall.

## Working widgets and preview designs

**READY** cards connect to an implemented provider. The new nineteen battery designs read actual battery level, retain the selected style separately for each placed instance, and refresh through Glance/WorkManager and Android's periodic widget updates. No battery time-remaining estimates are fabricated. The launcher widget picker defaults to Battery Widget 1; choosing a gallery battery card selects its style after pin confirmation.

Existing live clock/date, basic photo, four-app folder, media volume, Wi-Fi settings, mobile-data settings and personal-text widgets remain available. Photo/folder/text cards explicitly describe the basic placed version. Tap a placed photo/folder/text widget to configure it. Wi-Fi and data open Android settings panels.

**PREVIEW** cards show the requested design direction; most clock typography variants, additional contact layouts, games, full music layouts/media-session metadata, photo masks and productivity providers still need implementation. They do not show a nonfunctional Add button. Photos and avatar previews use original procedural drawings; supplied artwork and proprietary fonts were not copied. Counts show the layouts actually included, rather than the reference app's larger catalog totals.

Studio's widget mode is a persisted preference only; it does not retroactively resize widgets or implement a new sizing engine. The 1×1 provider minima are enabled for the existing quick-setting providers, with launcher-dependent sizing.

## Home-screen actions

Battery taps open Android's battery usage screen (battery saver/general settings are OEM fallbacks). The live clock opens the clock app's alarms. Contact 1, Contact 11 and Contact 12 add basic per-instance call/message shortcuts: tap Set contact, enter a number or choose one from the system picker, and save. Direct calls use CALL_PHONE runtime permission and begin immediately on a subsequent widget tap, without opening the contacts app. Permission grants and saving configuration never place a call. Message actions open a composer; sending remains in the messaging app. Edit changes the saved contact. A compact 1x1 contact uses the name as its primary action; enlarge Contact 1 to show its second Message action.

Music 10 is a real play/pause media button for Android's current media consumer, like a headset button. Start playback in a compatible music app first. This basic widget has no song/album metadata and cannot start an unconfigured player. Volume +/- changes the media stream; Settings opens Android's volume panel. Photo and text taps edit their content; app-folder entries launch their selected app. Only READY designs can be placed. All 55 Quick Settings cards now have a provider: existing Wi-Fi 2, Mobile data 2 and Sound 2 keep their providers; other cards use the new quick-control provider with per-instance selection. Settings controls open their relevant screen (with OEM fallbacks). Ringer, Silent and Vibrate attempt a direct ringer-mode change and fall back to sound settings when Android restricts it. Torch requests camera permission on demand, then uses CameraManager and actual torch callbacks. No image is captured. Granting permission does not toggle the torch; tap again. Camera hardware is optional. The torch can turn off if Android stops the app or another camera app takes over. Tap Edit on a new quick-control widget to select a different control or visual family. The placed layouts are simple text/symbol versions and do not display an assumed live on/off state.

## Build and verification

Use JDK 17 and Android SDK 35. The Gradle 8.9 wrapper is included.

```sh
bash ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
bash ./gradlew :app:connectedDebugAndroidTest
```

On Windows use `gradlew.bat`. See [BUILDING.md](BUILDING.md) for device checks and current limitations. GitHub Actions builds and lints the app, then separately boots an Android 10 emulator, runs instrumentation tests and uploads test reports/screenshots. Check the current branch's Actions result before assuming those device checks passed.


### Widget sizing fix (0.2.1)

Ready cards now select providers matching their labelled cell size. Battery has separate 1x1, 1x2, 2x1, 2x2 and 4x2 providers. Photo and folder default to 2x2, clock and text to 2x1, and compact controls/contacts/music button to 1x1. Android 12+ receives explicit targetCellWidth/targetCellHeight; older launchers use dp fallbacks. Actual grid placement remains launcher-dependent. All live Glance layouts now use SizeMode.Exact; battery renders at the allocated aspect ratio and the clock text scales within its host.

After updating, remove and re-add affected widgets. Android launchers retain the existing placed cell spans; an app cannot force-resize a widget already on the home screen. Existing battery selections remain saved until removal. Long-press and resize if a launcher uses a different grid. Preview categories are unchanged pending the user's sizing acceptance.
