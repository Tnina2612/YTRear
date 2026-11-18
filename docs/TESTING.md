# YTRear testing

## Version 1.5.1 localization, setup, and firmware check

Version 1.5.1 adds a consolidated **Setup** checklist, firmware verification for HyperOS OS3 and
OS4, Vietnamese localization, and a language picker. The release reports `versionCode 19` /
`versionName 1.5.1`.

### Automated and on-device check — 2026-11-18

- [x] `assembleDebug` and `lintDebug` completed with zero lint errors (8 pre-existing
  dependency/label warnings).
- [x] The APK installs over the 1.5.0 build as `com.luna.music`, `versionCode 19`, and keeps
  notification access, the auto-detect target, and the Doze whitelist entry.
- [x] The **Setup** card renders all three rows. Notification access and battery read as
  **Granted** / **Unrestricted**; autostart reads **Not confirmed** until the local confirmation is
  set.
- [x] Removing `com.luna.music` from the Doze whitelist changes the battery row to **Restricted** and
  restores the **Set** action; adding it back returns the row to **Unrestricted** after reopening the
  app.
- [x] Writing `autostart_confirmed=true` in `shared_prefs/setup_state.xml` turns the autostart row
  green, checks the checkbox, and hides the card once all three rows are complete. Restoring `false`
  brings the card back.
- [x] Firmware verification recognizes the current `OS4.0.6.0.XBLCNXM` build and hides the
  **Advanced** warning. A temporary verification-list change confirmed the warning appears for an
  unlisted build, then the list was restored; `OS3.0.318.0.WBLCNXM` remains in the verified set.
- [x] The rear-screen controls remained stable on OS4 during the user's physical validation.
- [x] The language dialog shows **Theo hệ thống**, **English**, and **Tiếng Việt**, with the current
  choice selected. Switching with `cmd locale` to `vi`, `en`, and the empty system list changed the
  full UI and survived force-stop/relaunch.
- [x] Vietnamese resources are packaged for all app strings; the foreground-service channel/title
  and body plus the proxy notification appeared in Vietnamese.
- [x] The globe button is present at the top-right with the `Ngôn ngữ` accessibility label and opens
  the dialog.

Physical taps for the language options, **Open** autostart, the autostart checkbox, and **Set**
battery were not injected because firmware blocks `INJECT_EVENTS`; they still need a person:

- [ ] Tap each language option and confirm the activity recreates and the setting persists.
- [ ] Open the HyperOS autostart screen, enable YTRear, and tick **Autostart is enabled**.
- [ ] Tap **Set** from the Setup card and confirm the battery exemption screen opens and the row
  turns green on return.

The 1.5.1 release APK is archived as `releases\YTRear-1.5.1-debug.apk` (12,293,031 bytes). It
reports `com.luna.music` version 1.5.1 (`versionCode 19`), and its SHA-256 is
`1F7AE9113AE52F118469ED1DD7DEBD07804B64BEC97312E956BB086A100A395B`. The archive is local and
gitignored under `releases/`; upload this exact file and checksum to the GitHub release.

## Version 1.5.0 battery, tile, and picker candidate

Version 1.5.0 keeps the 1.4.1 behavior and adds three user-facing changes: a battery-optimization
setup prompt, a Quick Settings tile that turns the proxy on or off, and launcher icons in the
player picker.

### Automated and on-device check - 2026-11-17

- [x] `assembleDebug` and `lintDebug` completed with zero lint errors (8 pre-existing
  dependency/label warnings).
- [x] The packaged APK reports `com.luna.music`, `versionCode 18`, `versionName 1.5.0`, and installs
  over 1.4.1 with notification access and the saved auto-detect selection intact.
- [x] `aapt dump badging` lists `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` alongside the existing
  foreground-service and notification permissions.
- [x] The **Quick Settings** tile appears in the shade and reads active (blue) while the service is
  running and the target is configured.
- [x] Tapping the tile while active stops `RearControlService`, clears the proxy session, removes
  YTRear's notifications 76/78, releases the selected player's 30-day snooze, and restores its
  native media card. Both snooze lists were empty afterwards and the tile turned grey.
- [x] Tapping the tile again restarts the service, republishes notifications 76/78, and re-snoozes
  the selected player's `MediaStyle` notification.
- [x] HyperOS delivered one physical tap as two `onClick` callbacks during the tile test; the
  600 ms debounce keeps one tap to one toggle.
- [x] The **Player** picker pins the bold auto-detect row first with a tinted sync icon and renders
  each app's launcher icon beside its name and package.
- [x] The **Set battery to No restrictions** button follows the Doze whitelist. Removing the entry
  with `cmd deviceidle whitelist -com.luna.music`, restarting the activity, and dumping the UI showed
  the button on the main screen; restoring the entry hid it again. The phone was left on the
  whitelist.

The system request dialog itself was not exercised because adb input injection is blocked by the
firmware (`INJECT_EVENTS`), so these taps still need a person:

- [ ] With the app off the whitelist, tap **Set battery to No restrictions** and confirm the HyperOS
  permission screen opens from the direct request (or the fallback screens).
- [ ] Grant the exemption through that screen and confirm the button disappears on return and
  diagnostics read `Battery optimization: IGNORED`.
- [ ] Re-capture the **Excluded apps** dialog and the collapsed **Advanced** panel with the new
  picker row layout.
- [ ] Re-run the 1.4.1 auto-detect and handoff checks with the tile's off state (no snoozing while
  the service is stopped).

The packaged 1.5.0 candidate APK SHA-256 is
`E4088CD034A2D72D07414FAAC79523A984E147B8F801C3DEA9B8C89D44084664`. The archived copy at
`releases\YTRear-1.5.0-debug.apk` hashes
`DC3241D4B4F7A5573134563CB04F8BE208BC6BC753228BDFE738E069CFC6962B`; debug builds are not
byte-for-byte reproducible, so compare against the hash recorded for that exact file.

## Version 1.4.1 cleanup candidate

Version 1.4.1 keeps the 1.4.0 auto-detect mode unchanged and tightens the app around it: the full
HyperOS rear-music allowlist is now excluded from both the picker and auto-detection, the obsolete
`SYSTEM_ALERT_WINDOW` permission and overlay/custom-widget probes are gone, and GitHub Actions CI
runs the same build and lint commands as this page.

### Automated check - 2026-11-17

- [x] Clean `assembleDebug` and `lintDebug` completed with zero lint errors (8 pre-existing
  dependency/label warnings).
- [x] The packaged APK reports `com.luna.music`, `versionCode 17`, `versionName 1.4.1`.
- [x] `aapt dump badging` lists only `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, and
  `POST_NOTIFICATIONS`; `android.permission.SYSTEM_ALERT_WINDOW` is absent.
- [x] `gradlew`, `gradlew.bat`, and `gradle/wrapper/*` are tracked, so the new CI workflow can run
  on a clean checkout.

No device was attached while this candidate was packaged, so the on-device checks stay open:

- [ ] Install 1.4.1 over 1.4.0 and confirm notification access and the saved player survive.
- [ ] Confirm `Spotify`, `QQ Music`, and the other HyperOS-native players no longer appear in the
  picker or the **Excluded apps** list, and that auto-detect skips them while playing.
- [ ] Confirm an app outside that allowlist (`SoundCloud`, `YT Music Morphe`) still becomes the auto
  target and completes a bidirectional handoff.
- [ ] Confirm the picker, auto-detect, exclusions, and the collapsed **Advanced** panel render
  without the removed overlay buttons.
- [ ] Re-run the complete 1.4.0 check list below.

The packaged 1.4.1 candidate APK SHA-256 is
`1D0CEEDCE8DA1218B52E362330CD1CBDCC97F2397A09A5246C45DF458EDE9F38`.

## Version 1.4.0 auto-detect player candidate

Version 1.4.0 adds an opt-in **Auto-detect active player** mode (`PlayerSelection.Mode.AUTO`) on
top of the 1.3.2 searchable player picker, plus a searchable **Excluded apps** dialog. `Mode.MANUAL`
remains the default.

### Automated and on-device check — 2026-11-17

- [x] Clean `assembleDebug` and `lintDebug` completed with zero lint errors.
- [x] Version 1.4.0 (`versionCode 16`) installed on `Xiaomi 25098PN5AC` while retaining
  notification access and app data.
- [x] Auto-detect mode selected `YT Music Morphe` (`app.morphe.android.apps.youtube.music`),
  mirrored metadata/artwork, snoozed its `MediaStyle` notification for 30 days, and forwarded
  Play, Pause, and Next commands.
- [x] Live bidirectional handoffs between `YT Music Morphe` and `SoundCloud`
  (`com.soundcloud.android`) switched targets immediately on the rising `STATE_PLAYING` edge,
  restored the previous player's notification within 250 ms, snoozed the new player's notification
  for 30 days, updated Notification 76's title and Notification 78's click `PendingIntent`, and
  logged `Skipping update music data for same app: com.luna.music` in `SubScreenCenter` with zero
  `Removed current widget` events.
- [x] Excluding the active auto target immediately restored its notification and fell back to the
  next eligible session; re-including switched back to the playing session.
- [x] Picker dialog pinned **Auto-detect active player** at position 0 and main screen toggled
  **Excluded apps** visibility and Now Playing app attribution based on the active mode.

The installed 1.4.0 candidate APK SHA-256 is
`6F6AE1C6545FBA6C064FFBA9DDD9833E415F40DBDB0B40D44CF2A179A35A2900`.

## Version 1.3.2 multi-player candidate

Version 1.3.2 starts from the tagged 1.2 baseline and adds a persisted, searchable player picker.
The picker is shown whenever the main activity is launched and lists every enabled app with a
launcher activity. Only apps that publish a standard Android `MediaSession` can be proxied.

Before promoting 1.3.2:

- [ ] Confirm the picker opens on launch and can be searched by label or package name.
- [ ] Confirm Spotify is omitted because it is supported by the rear screen natively.
- [ ] Confirm cancelling keeps the previous selection and selecting an app starts or refreshes the
  proxy service.
- [ ] Confirm a selected player remains selected across process death and reboot.
- [ ] Confirm proxy notification taps open the selected player.
- [ ] Run play/pause, next, previous, metadata, artwork, screen-off, and lifecycle checks for each
  supported player.
- [ ] Confirm switching players clears the old session and binds only the newly selected package.
- [ ] Re-run the complete 1.2 YouTube Music regression below.

Every selected player receives the validated 30-day notification-snooze workaround. YTRear
replaces the selected app's original MediaStyle notification so it cannot retake HyperOS's
top-media rank and collapse the expanded Dynamic Island during a track change.

### Automated candidate check — 2026-09-13

- [x] Clean `assembleDebug` and `lintDebug` completed with zero lint errors.
- [x] Version 1.3.1 installed while retaining notification access and app data.
- [x] A normal launch displayed the searchable picker; repeated launch intents produced one dialog.
- [x] The picker explained that Spotify is omitted, and a previously stored Spotify target was
  rejected.
- [x] The selected Poweramp package persisted across force-stop/relaunch.
- [x] Poweramp and `YTRearProxy` sessions were simultaneously active while an unrelated YouTube
  Music session was also present, confirming exact-package selection.
- [x] Poweramp's MediaStyle notification was moved into both `Snoozed notifications` and
  `Pending snoozed notifications` for 30 days, while its underlying MediaSession remained active.
- [x] Targeted Play, Next, and Pause commands were forwarded through YTRear to
  `com.maxmpz.audioplayer`; the Poweramp and proxy states stayed synchronized.
- [x] Version 1.3.1 kept notifications 76 and 78 active, kept Poweramp's original card out of the
  active list, and logged zero island-removal or `EXPANDED_TO_DELETED` events during Play/Next.
- [x] Version 1.3.2 restored Poweramp when it was no longer selected: the Poweramp key disappeared
  from both Android snooze lists within the short release deadline, and the persisted
  pending-restore set cleared itself after processing.
- [x] An active YouTube Music → Poweramp handoff restored YouTube Music notification 2 immediately.
  Switching back to YouTube Music snoozed only notification 2 again, restored YTRear notifications
  76 and 78, and left the pending-restore set empty.
- [x] A 1.3 → exact archived 1.2 → 1.3 install round-trip succeeded while preserving the Poweramp
  selection and notification access.
- [ ] Expand the Dynamic Island and press Next with Poweramp selected; confirm it physically stays
  expanded. ADB input injection is denied by this firmware.
- [ ] Physically type into the search field and tap a result. Shell input injection is denied by
  this firmware, so this cannot be automated over adb.
- [ ] Repeat the rear-panel controls with display 0 off; physical rear input cannot be injected.

The installed candidate APK SHA-256 is
`9E1314CF8359614F9562A569C7E1CE0950A7D57F295C56550CC7C6A34B995D3E`.

## Version 1.2 stable baseline

This is the validation record for the baseline build tagged `baseline-v1.2`. It was captured on
HyperOS `OS3.0.318.0.WBLCNXM`; the test phone has since moved to `OS4.0.6.0.XBLCNXM`, whose
localization/setup regression is recorded at the top of this page.

## Baseline

The validated configuration is:

```text
Device       Xiaomi 17 Pro
OS           Android 16, HyperOS OS3.0.318.0.WBLCNXM
Package      com.luna.music
Version      1.2 (versionCode 12, targetSdk 36)
```

Confirm the APK installed on the phone matches your own build output by SHA-256 before running
these checks, and that notification access is granted with `RearControlService` in the
foreground.

## Confirmed behavior

- The native Xiaomi MAML card receives YTRear proxy controls while display 0 is off.
- Rear play/pause, next, and previous control YouTube Music.
- Version 0.9's 80 ms / 3.2-second rank lease keeps the rear card stationary during track changes.
- Version 1.0's saved trace shows the main-display artwork converging to the current track after
  the refresh and confirm posts.
- The version 1.0 trace contains no rear-widget removal, recreation, or notification-rate shedding.
- Version 1.1's installed proxy MediaSession activity and notification 78 content intent both
  resolve to `com.google.android.apps.youtube.music/.activities.MusicActivity`.
- The Dynamic Island player and proxy media notification were both physically confirmed to open
  YouTube Music instead of YTRear.
- Version 1.2 automatically snoozes only YouTube Music's MediaStyle notification for 30 days. A
  forced YouTube Music restart produced `MEDIA LISTENER: snoozed YouTube Music media notification
  for 30 days`; both the real and proxy MediaSessions remained `PLAYING`.
- A targeted command against the installed 1.2 `YTRearProxy` changed tracks and logged zero
  YouTube Music SystemUI media loads, zero YTRear island removals, and zero
  `EXPANDED_TO_DELETED` transitions.
- The same notification-snooze state was physically confirmed to keep the Dynamic Island expanded
  when Next was pressed.

These results were established across the 0.4, 0.9, 1.0, 1.1, and 1.2 device runs. The checks below
are the remaining regression and durability coverage for the exact currently installed 1.2 APK.

## Remaining checks

### 1. Version 1.2 physical regression

- [x] Tap the expanded Dynamic Island player and confirm YouTube Music opens instead of YTRear.
- [x] Tap the proxy media notification card and confirm YouTube Music opens instead of YTRear.
- [x] Expand the Dynamic Island, press Next, and confirm the island stays expanded.
- [ ] Open the main display's media card and press next.
- [ ] Confirm title and artist update immediately and artwork follows in about 1.5 seconds.
- [ ] Confirm the artwork remains on the new track rather than falling one track behind.
- [ ] During the same skips, confirm the rear card stays visible and does not slide away/reopen.
- [ ] Repeat with previous and with several consecutive track changes.

Expected log pair for each changed track:

```text
PROXY NOTIFICATION: artwork refreshed for '<new track>'
PROXY NOTIFICATION: artwork post confirmed for '<new track>'
```

During these checks, `cmd notification list` should contain YTRear notification 78 and no
YouTube Music notification 2. `dumpsys notification --noredact` should list notification 2 under
`Snoozed notifications` and `Pending snoozed notifications`.

### 2. All controls with display 0 off

- [ ] Start YouTube Music and ensure YTRear reports **Rear controls active**.
- [ ] Turn off display 0 and wait at least 30 seconds.
- [ ] Wake only the rear panel and test previous, play/pause, and next separately.
- [ ] Confirm each action changes playback and logs `PROXY CONTROL: sent ... to YouTube Music`.
- [ ] Confirm `dumpsys display` still reports display 0 as `OFF`.

Previous was proven after more than 70 seconds in the original screen-off run; play/pause and
next passed shorter runs. This repetition closes the symmetric matrix on version 1.2.

### 3. Automatic takeover after idle

Repeat this test at least twice:

1. Keep YTRear's foreground-service notification, but pause playback and dismiss the media card
   used in the original failure.
2. Wait at least five minutes without reopening YTRear.
3. Open only YouTube Music and start a track.
4. Confirm YTRear becomes top media within about one to two seconds without touching either
   notification.
5. Confirm the log ends with `proxy ranking lease complete; sessionOnly=true` and contains no
   notification enqueue-rate shedding.

### 4. Lifecycle and stress matrix

- [ ] Kill and restart YouTube Music while YTRear stays alive.
- [ ] Revoke and re-grant notification-listener access.
- [ ] Force-stop YTRear, relaunch it, and tap **Start / Refresh rear controls**.
- [ ] Reboot, start YouTube Music, and verify automatic listener reconnection.
- [ ] Pause for more than 60 minutes and verify the widget can be restored normally.
- [ ] Press next three times rapidly; confirm title, artwork, and playback converge on the final
  track with no enqueue-rate warning.
- [ ] Disable YTRear notification access, force-stop/reopen YouTube Music, and confirm its native
  media notification returns; then re-enable YTRear and confirm automatic snoozing resumes.

Do not disable YouTube Music notifications app-wide. Version 1.2 intentionally snoozes only its
active MediaStyle notification through the authorized notification listener. The verified device
run kept the underlying YouTube Music MediaSession and playback alive.

## Commands

Run in PowerShell on the Windows host:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$DEVICE = (& $adb devices) [1].Split()[0]   # your adb serial

# Installed build
& $adb -s $DEVICE shell dumpsys package com.luna.music |
  Select-String -Pattern 'versionCode|versionName|targetSdk|lastUpdateTime'

# Proxy notification and MediaSession
& $adb -s $DEVICE shell dumpsys notification --noredact |
  Select-String -Pattern 'com.luna.music|android.mediaSession' -Context 3,12
& $adb -s $DEVICE shell dumpsys media_session |
  Select-String -Pattern 'YTRearProxy|com.luna.music' -Context 2,8

# Dynamic Island suppression state
& $adb -s $DEVICE shell cmd notification list
& $adb -s $DEVICE shell dumpsys notification --noredact |
  Select-String -Pattern 'Snoozed notifications|Pending snoozed|youtube.music' -Context 0,3

# Focused log
& $adb -s $DEVICE logcat -d -v threadtime |
  Select-String -Pattern 'PROXY SESSION|PROXY NOTIFICATION|PROXY CONTROL|proxy rank|topMediaDataChanged|unified.music|Removed current widget|Creating new music widget|Shedding'

# Rear capture: use the physical id, not logical display id 1.
# Read $REAR from your own `dumpsys display` output; it is vendor-assigned per unit.
& $adb -s $DEVICE shell screencap -p -d $REAR `
  /data/local/tmp/rear.png
& $adb -s $DEVICE pull /data/local/tmp/rear.png .

# Power/display state
& $adb -s $DEVICE shell dumpsys display
& $adb -s $DEVICE shell dumpsys power
```

An all-black rear PNG around 3.5 KB usually means the panel was asleep. A live capture is commonly
90–220 KB, so check display state and file size before treating a black capture as a render failure.

Device traces captured during these checks contain personal activity (running apps, wake/sleep
timing, and whatever media happens to be playing). Keep them local and redact them before sharing.
