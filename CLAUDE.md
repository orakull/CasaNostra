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
- `ui/theme/` — `CasaNostraTheme`, `CasaNostraLightColors`, `CasaNostraTypography` (Material3 light color scheme)
- `ui/common/` — `LockScreenOrientation` (`expect`/`actual`), `CasaNostraCard`, `ReadOnlyBadge` (shared UI components)
- `ui/auth/` — `AuthScreen`, `AuthViewModel`, `AuthState`
- `ui/projects/` — `ProjectsScreen`, `ProjectsViewModel`, `ProjectsState`
- `ui/player/` — `PlayerScreen`, `PlayerViewModel`, `TrackRow`, `TrackState`, `UploadItemState`
- `data/models/` — `Project`, `ProjectTrack` (kotlinx-serialization)
- `data/repository/` — `ProjectRepository`, `TrackRepository` — remote Supabase ops + cache coordination
- `audio/MultitrackPlayer` — `expect`/`actual`: Android uses Media3/ExoPlayer, iOS uses AVFoundation, Web uses Web Audio API
- `cache/AudioFileCache` — `expect`/`actual`: Android = filesystem (`cacheDir/audio_tracks/`), iOS = NSCachesDirectory, Web = in-memory HashMap
- `di/AppModule.kt` — Koin DI module (`appModule`), creates singleton `SupabaseClient`, repositories, cache

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

## UI-компоненты: правила повторного использования

### Переиспользуемые компоненты — регистр

Перед написанием inline-кода UI **обязательно проверь** наличие готового компонента:

| Компонент | Файл | Когда использовать |
|-----------|------|--------------------|
| `CasaNostraCard` | `ui/common/Components.kt` | Любая кликабельная карточка (воркспейс, проект и т.д.) |
| `ReadOnlyBadge` | `ui/common/Components.kt` | Везде, где нужно показать режим "Только просмотр" |
| `MuteSoloSegmentedButton` | `ui/player/MuteSoloButton.kt` | Контрол Mute/Solo на строке трека |

### Правило выноса компонента

Выноси composable в отдельный файл / `ui/common/`, если выполняется **хотя бы одно** условие:
- Компонент используется (или **может быть использован**) в двух и более экранах
- Компонент содержит нетривиальную анимацию или сложную логику отображения состояния
- Компонент представляет собой самостоятельный UI-элемент с чёткой семантикой (Badge, SegmentedButton, EmptyState и т.п.)

### Запрет дублирования

Никогда не копируй одинаковый composable-код между файлами. Если один и тот же визуальный элемент появляется более чем в одном месте — немедленно выноси его в `ui/common/Components.kt` (cross-screen) или в отдельный файл внутри своего пакета (within-feature).

### Именование

- Общие компоненты (cross-screen): `ui/common/Components.kt`
- Компоненты, специфичные для фичи, но слишком большие для одного файла: отдельный файл в пакете фичи (`ui/player/MuteSoloButton.kt`, `ui/player/UploadProgressOverlay.kt` и т.д.)
- Имя файла = имя основного composable внутри него

## Platform-Specific Notes

- `expect`/`actual` is the pattern for all platform-divergent code (audio player, cache, file picking)
- Android minimum SDK is 29
- Web target uses Wasm; JS target also exists for broader compatibility
- `FileKit` is used for multiplatform file picking (audio upload)
