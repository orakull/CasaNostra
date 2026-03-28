# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CasaNostra is a multitrack audio player and music collaboration app built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**, targeting Android, iOS, and Web (Wasm/JS). Backend is powered by **Supabase** (Auth + PostgreSQL + Storage).

## Build Commands

```bash
# Android debug APK
./gradlew :composeApp:assembleDebug

# Run Android app
./gradlew :composeApp:installDebug

# Web (Wasm — modern browsers, faster)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Web (JS — broader compatibility)
./gradlew :composeApp:jsBrowserDevelopmentRun

# Run all tests
./gradlew :composeApp:allTests

# Lint
./gradlew :composeApp:lint
```

For iOS, open `/iosApp` in Xcode.

## Architecture

**Single module** (`composeApp/`) with platform source sets:
- `commonMain` — all shared code: UI, ViewModels, repositories, audio logic, cache
- `androidMain` / `iosMain` / `wasmJsMain` — platform-specific `actual` implementations

**MVVM + Repository pattern:**

```
Compose UI (Screen) → ViewModel → Repository → Supabase / local cache
                                             ↘ MultitrackPlayer (expect/actual)
```

**Key layers:**
- `ui/` — Composable screens (`AuthScreen`, `ProjectsScreen`, `PlayerScreen`) + ViewModels. State via `mutableStateOf`/`StateFlow`.
- `data/models/` — `Project`, `ProjectTrack` (kotlinx-serialization)
- `data/repository/` — `ProjectRepository`, `TrackRepository` — remote Supabase ops + cache coordination
- `audio/MultitrackPlayer` — `expect`/`actual`: Android uses Media3/ExoPlayer, iOS uses AVFoundation, Web uses Web Audio API
- `cache/AudioFileCache` — `expect`/`actual`: Android = filesystem (`cacheDir/audio_tracks/`), iOS = NSCachesDirectory, Web = in-memory HashMap
- `SupabaseModule.kt` — Koin DI module, creates singleton `SupabaseClient`

**App navigation** is a state machine in `App.kt`: `Loading → NotAuthenticated → Authenticated`, switching between `ProjectsScreen` and `PlayerScreen`.

## Dependency Injection

[Koin 4.0](https://insert-koin.io/) is used throughout. The Koin graph is initialized in `CasaNostraApplication` (Android) and the equivalent entry points on other platforms. ViewModels are injected via `koinViewModel()`.

## Backend: Supabase

- **Auth**: email/password via `supabaseClient.auth`
- **Database**: `projects` and `project_tracks` tables (see `supabase_db_schema.sql`)
- **Storage**: `tracks/` bucket for audio files
- Session state is observed via `supabaseClient.auth.sessionStatus` (StateFlow)

### Secrets

A `Secrets.kt` file (not committed) must provide:
```kotlin
object Secrets {
    const val SUPABASE_URL = "..."
    const val SUPABASE_ANON_KEY = "..."
}
```

## Audio Playback Flow

1. User opens a project → `PlayerViewModel.setProject()`
2. `TrackRepository.fetchTracks(projectId)` loads metadata from Supabase
3. Per track: `TrackRepository.getOrDownloadTrackBytes()` — cache-first: local hit or download from Supabase Storage
4. Bytes loaded into `MultitrackPlayer.loadTracks()`
5. Player prepares platform-specific instances (ExoPlayer on Android), enables UI controls

Each track has independent volume (0.0–1.0), mute, and solo controls.

## Стиль работы

- Описания (`description`) всех вызовов инструмента Bash писать на **русском языке**.

## Platform-Specific Notes

- `expect`/`actual` is the pattern for all platform-divergent code (audio player, cache, file picking)
- Android minimum SDK is 29
- Web target uses Wasm; JS target also exists for broader compatibility
- `FileKit` is used for multiplatform file picking (audio upload)
