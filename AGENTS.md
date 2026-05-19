# AGENTS.md

## Project Intent

Roofy Music Mobile is a GPLv3 Android YouTube Music client forked from MetroList. It keeps the streaming, caching, search, lyrics, playlists, account sync, widgets, and listen-together features, with Roofy Music branding and a compact retro CRT visual system.

Keep downloader and cache behavior user-directed. Do not add telemetry.

## Architecture

- `app/` is the Android application.
- Kotlin package names currently remain `com.metrolist.music` to avoid a risky namespace rewrite.
- Application id is `app.roofymusic.mobile`.
- `innertube/`, `kugou/`, `lrclib/`, `betterlyrics/`, `lastfm/`, `discordrpc/`, `shazamkit/`, and `paxsenix/` are supporting modules.
- `metroproto/` is a required source dependency copied from MetroList's submodule.

## Development Rules

- Preserve GPLv3 license notices and source availability obligations.
- Brand visible UI as Roofy Music, not MetroList.
- Keep the retro CRT design direction: near-black surfaces, square shapes, gray borders, monospace typography, magenta/yellow accents, and scanline/noise treatment.
- Prefer narrow changes over broad package renames unless the user explicitly asks for a namespace migration.

## Verification

Build with Java 21:

```powershell
.\gradlew.bat :app:assembleFossDebug
```

Expected APK:

```text
app/build/outputs/apk/foss/debug/app-foss-debug.apk
```
