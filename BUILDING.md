# Build and verify

Open in Android Studio with JDK 17 (or 21), SDK 35 and the bundled Gradle 8.9 wrapper.

```sh
bash ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
bash ./gradlew :app:connectedDebugAndroidTest
```

Windows: use `gradlew.bat` for the same tasks. The connected task requires an Android 10+ device or emulator. APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Implemented behavior

Gallery first; eleven category pages plus Themes, Studio and Settings. Equal 236dp cards with 164dp previews. All dimensions are shared through DesignCard. The compact-size policy is centralized in DesignCatalog: Contacts, Games, Productivity and Quick Settings use 1×1 labels; other categories retain native ratios. Gallery dimensions remain equal regardless of widget ratio.

Appearance and favorite design IDs are saved atomically in Preferences DataStore. Backups are versioned JSON with a 64KiB input limit, color validation, allowed text/mode choices and known favorite IDs. Theme changes enqueue WorkManager refreshes; active clock and basic widget sessions observe appearance. Photo pixels keep their original colors.

Battery pin callbacks are explicit, internal PendingIntents. The launcher-returned widget ID is validated against BatteryReceiver before saving an instance's style. Deleting the widget clears that style. Battery Glance state has a revision key so refreshes update active sessions after pinning. Battery values are real; updates follow Android scheduling, not second-by-second background polling.

The original clock uses live TextClock RemoteViews and respects system time format. Basic photo/folder/text configuration is per instance. Photos use the document picker and a small private copy; no storage permission is requested. Remove the widget to clear its configuration. Connectivity widgets open Android settings panels with a fallback to general settings.

## Scope still in progress

PREVIEW cards intentionally cannot be pinned. They include most digital/hybrid variants, contacts, game engines, media-session integration, photo mask processing and productivity data sources. QR previews are decorative samples, not generated personal payment codes. No premium billing or links to the reference developer are included. Studio's sizing preference is stored for later provider expansion; it does not resize existing widgets.

## Validation

Both APK targets have compiled locally. This environment blocks ADB's normal Android directory and some Java/lint report operations; local device execution is not claimed. GitHub Actions runs build/lint and an Android 10 emulator test job. Instrumentation tests cover the first screen, search, favorites, accent persistence, category sizing, rendering every preview, backup validation, and navigation/capture of all fourteen requested pages. Check the latest branch run for actual execution results.

## Manual device acceptance

1. Verify equal gallery cards and readable navigation on small screens and large font scale.
2. Apply a theme and custom accent, restart, verify persistence and placed-widget refresh.
3. Pin two different battery styles, cancel another pin, and verify the first two remain independent.
4. Compare battery readings with Android; verify refresh and cleanup after widget deletion.
5. Place clocks from the launcher; test 12/24-hour, midnight, date and timezone changes.
6. Configure two photos/folders/text widgets independently; edit, cancel and restart.
7. Resize the quick-setting widgets to 1×1; verify panel navigation and volume controls on the device.
8. Export a palette, change it, import the backup; malformed/unsupported backups must preserve existing settings.
