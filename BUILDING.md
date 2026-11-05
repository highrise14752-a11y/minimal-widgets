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

PREVIEW cards intentionally cannot be pinned. They include most digital/hybrid variants, additional contact layouts, game engines, full media-session integration, photo mask processing and productivity data sources. QR previews are decorative samples, not generated personal payment codes. No premium billing or links to the reference developer are included. Studio's sizing preference is stored for later provider expansion; it does not resize existing widgets.

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

## Action acceptance

Battery widgets open power-usage settings with OEM fallbacks; the live clock opens alarms with timers as a fallback. Three basic contact providers store a private name/number per instance. Set contact and Edit are separate from call/message actions. CALL_PHONE is requested only for direct calling; configuring a contact or granting permission does not place a call. No READ_CONTACTS permission is needed for the system phone-number picker. Removing a contact widget clears its saved number. Message actions open a composer. Music 10 dispatches a headset-style play/pause event to the current media consumer; other music designs remain previews.

Action instrumentation intercepts battery/clock destinations, checks call/SMS intent construction, rejects injected telephone/USSD/post-dial data, and checks panel fallback order. It never places a real call. On a physical phone, verify two independently configured contact widgets, permission denial/revocation, direct call and message routing, deletion/re-add cleanup, the OEM battery screen, the chosen clock app, music playback with an active compatible player, and media volume buttons. These hardware/telephony checks are not replaced by emulator intent interception.
## Quick controls

All 55 Quick Settings cards are placeable. Three existing connectivity/volume cards retain their original providers. The others use QuickSettingsWidget with a validated per-instance design ID. The pin callback checks both the returned widget provider and catalog design before saving. Deletion clears the saved selection. The launcher picker defaults to Wi-Fi; Edit selects another control without executing it. Four simple text/symbol layout families adapt to compact cell sizes; unknown system state is never labeled on/off.

Restricted controls open settings: Wi-Fi, mobile data, Bluetooth, airplane mode, alarm, dark theme, DND, hotspot, location and rotation. OEM-specific DND/hotspot actions have public settings fallbacks. Sound opens the volume panel. Ringer/Silent/Vibrate attempt AudioManager changes and open sound settings if denied, fixed, or ignored. No notification-policy access is requested automatically. Torch uses optional camera hardware, runtime camera permission, and a bounded hardware-state callback before toggling. No photos are taken. Permission grants and configuration do not execute actions. Android may turn off torch when the owning app exits or another camera app needs the flash.

Instrumentation validates every quick-card mapping, compact sizes, independent saved selections, rejected unowned pin callbacks, settings fallback order and ringer transitions. It does not change a real device's ringer or flash. Physical acceptance: pin two different controls, test Edit/cancel/re-add, resize all four families, revoke camera permission, test a busy camera/no flash device, check OEM settings navigation and verify all three ringer modes under the device's DND restrictions.
### Widget sizing fix (0.2.1)

Ready cards now select providers matching their labelled cell size. Battery has separate 1x1, 1x2, 2x1, 2x2 and 4x2 providers. Photo and folder default to 2x2, clock and text to 2x1, and compact controls/contacts/music button to 1x1. Android 12+ receives explicit targetCellWidth/targetCellHeight; older launchers use dp fallbacks. Actual grid placement remains launcher-dependent. All live Glance layouts now use SizeMode.Exact; battery renders at the allocated aspect ratio and the clock text scales within its host.

After updating, remove and re-add affected widgets. Android launchers retain the existing placed cell spans; an app cannot force-resize a widget already on the home screen. Existing battery selections remain saved until removal. Long-press and resize if a launcher uses a different grid. Preview categories are unchanged pending the user's sizing acceptance.
