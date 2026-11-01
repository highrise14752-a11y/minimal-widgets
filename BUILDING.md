# Build and verify

Open in Android Studio with JDK 17 (or 21), SDK 35 and the bundled Gradle 8.9 wrapper.

Windows:
```
gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
gradlew.bat :app:connectedDebugAndroidTest
```
macOS/Linux: use `bash ./gradlew` with the same task names. The connected test task needs an Android 10+ device or emulator. APK: app/build/outputs/apk/debug/app-debug.apk.

## Usage
Tap a gallery card to request placement. If pinning is unsupported, long-press the home screen and choose Widgets. Photo, folder and note configuration happens after placement: tap the widget. Folders allow up to four apps, notes up to 240 characters. Photos use the document picker and a small private copy; no storage permission is requested. Remove the widget to clear its configuration.

Clock/date uses live TextClock views inside Glance and respects 12/24-hour preferences. Volume controls adjust media volume or open the volume panel. Wi-Fi/data open system settings panels with a fallback to general settings.

The grid uses equal 218dp cards and 130dp previews. Suggested widget size is 3 x 2; launcher grids vary and all widgets resize. Appearance changes are saved in DataStore and refresh every widget provider through WorkManager. Active sessions observe accent changes. Photos keep their natural colors.

## Validation
The current application and instrumentation-test APKs compile locally. Instrumentation tests have been compiled, not executed. Hardware emulator acceleration is unavailable; software-mode startup has not produced a connected device. Local lint analysis ran, but its report writer hit a filesystem restriction. GitHub Actions independently runs APK compilation, test-APK compilation and lint; check its latest run for the result.

## Device checks before closing issues
1. Launch into gallery; verify equal cards on small screens and at large font scale.
2. Search/filter/clear, change accents, restart and verify persistence.
3. Place each widget from gallery and launcher picker; resize each.
4. Check multiple clocks, 12/24-hour format, midnight/date/timezone updates and accent refresh while sessions are active.
5. Configure two photos/folders/notes independently, edit again and cancel; verify saved values survive restart.
6. Test volume, fixed-volume devices, app launching and Wi-Fi/data navigation.
7. Remove configured widgets and verify remaining instances still work.
