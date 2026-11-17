# Version and rollback

## Source

The untouched 1.2 source is pinned at tag `baseline-v1.2` and commit
`9044cf025908a08b81141c60fda871e14b20fec4`. The published multi-player work is on `main`.

```powershell
# Stable 1.2 source (detached checkout of the immutable baseline tag)
git switch --detach baseline-v1.2

# Return to the current multi-player source
git switch main
```

## Device APK

The exact APK originally pulled from the test phone is:

```text
releases\YTRear-1.2-installed-baseline.apk
SHA-256 86AE87EFB6C9BDE18F144238AACA59B7D2701B5E5A48E2A873DAB01E6E669E03
```

The 1.3.2 candidate is:

```text
releases\YTRear-1.3.2-player-picker-candidate.apk
SHA-256 9E1314CF8359614F9562A569C7E1CE0950A7D57F295C56550CC7C6A34B995D3E
```

The current 1.5.0 battery/tile/picker candidate is:

```text
app\build\outputs\apk\debug\app-debug.apk (1.5.0, versionCode 18)
SHA-256 E4088CD034A2D72D07414FAAC79523A984E147B8F801C3DEA9B8C89D44084664
```

The previous 1.4.1 cleanup candidate is:

```text
app\build\outputs\apk\debug\app-debug.apk (1.4.1, versionCode 17)
SHA-256 1D0CEEDCE8DA1218B52E362330CD1CBDCC97F2397A09A5246C45DF458EDE9F38
```

The previous 1.4.0 auto-detect candidate is:

```text
releases\YTRear-1.4.0-debug.apk
SHA-256 D5B1940BC1712C65890A590F75BBC078FBEB4ED734C260707126CF76F156D12B
```

Debug builds are not byte-for-byte reproducible, so this archived 1.4.0 APK hashes differently from
the 1.4.0 build that was installed for the recorded device run
(`6F6AE1C6545FBA6C064FFBA9DDD9833E415F40DBDB0B40D44CF2A179A35A2900`, see
[Testing](TESTING.md)). Compare a downloaded APK against the hash recorded for that exact file.

With `$adb` and `$DEVICE` set as in [Testing](TESTING.md):

```powershell
# Roll back while preserving app data and permissions.
& $adb -s $DEVICE install -r -d releases\YTRear-1.2-installed-baseline.apk

# Return to the archived multi-player build.
& $adb -s $DEVICE install -r releases\YTRear-1.4.0-debug.apk
```

The downgrade/upgrade route was successfully exercised on the test phone with the 1.3 candidate.
Version 1.2 ignores the player-selection preference; reinstalling the multi-player candidate
restores the saved selection.

## Removing YTRear

YTRear does not modify the selected player's settings or application data. Uninstalling it stops
future proxying and snoozing, but Android may retain the media notification that was already
snoozed. If the player's native notification does not reappear immediately after uninstall,
force-stop the player, reopen it, and start playback. Its normal notification and Dynamic Island
behavior will then return.
