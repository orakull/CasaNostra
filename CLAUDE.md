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
- `commonMain` — all shared code: UI, ViewModels, repositories, audio logic, cache, storage
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
- `ui/workspaces/` — `WorkspacesScreen`, `WorkspacesViewModel`, `GuestWorkspaceScreen`, `GuestWorkspaceViewModel`
- `ui/projects/` — `ProjectsScreen`, `ProjectsViewModel`, `ProjectsState`
- `ui/player/` — `PlayerScreen`, `PlayerViewModel`, `TrackRow`, `PlaybackControls`, `PlayerHeader`, `MuteSoloButton`, `UploadProgressOverlay`
- `data/models/` — `Workspace`, `WorkspaceMember`, `Project`, `ProjectTrack` (all in `ProjectModels.kt`, kotlinx-serialization)
- `data/repository/` — `WorkspaceRepository`, `ProjectRepository`, `TrackRepository` — remote Supabase ops + cache coordination
- `audio/MultitrackPlayer` — `expect`/`actual`: Android uses Media3/ExoPlayer, iOS uses AVFoundation, Web uses Web Audio API
- `cache/AudioFileCache` — `expect`/`actual`: Android = filesystem (`cacheDir/audio_tracks/`), iOS = NSCachesDirectory, Web = in-memory HashMap
- `storage/LocalStorage` — `expect`/`actual`: persistent key-value store (Web = `localStorage`, Android/iOS = no-op stubs)
- `storage/TrackSettingsStorage` — per-project track volume/mute/solo persistence via `LocalStorage`
- `deeplink/DeepLinkProvider` — `expect`/`actual`: extracts workspace share tokens from deep links / URL paths
- `di/AppModule.kt` — Koin DI module (`appModule`), creates singleton `SupabaseClient`, all repositories, cache

## App Navigation

Navigation is a state machine in `App.kt`:

```
Loading
  → NotAuthenticated
      ↳ deep-link token present → GuestWorkspaceScreen (unauthenticated workspace preview)
      ↳ no token              → AuthScreen
  → Authenticated
      → WorkspacesScreen  (workspace list + logout)
      → ProjectsScreen    (projects scoped to selected workspace)
      → PlayerScreen      (multitrack player for selected project)
```

**Read-only mode:** `isReadOnly = workspace.ownerId != currentUserId`. Passed down to `ProjectsScreen` and `PlayerScreen` to disable mutations for non-owners.

**Auto-join on sign-in:** When an unauthenticated user opens a workspace share link, the token is saved to `LocalStorage` (`KEY_PENDING_SHARE_TOKEN`). After sign-in or registration, `App.kt` reads the token, calls `WorkspaceRepository.joinWorkspace()`, and clears the stored key.

## Dependency Injection

[Koin 4.0](https://insert-koin.io/) is used throughout. The Koin graph is initialized in `CasaNostraApplication` (Android) and equivalent entry points on other platforms. ViewModels are injected via `koinViewModel()` or `viewModel { ... }`.

Singletons registered in `appModule`:
- `SupabaseClient`
- `AudioFileCache` (via `createAudioFileCache()` factory)
- `WorkspaceRepository`
- `ProjectRepository`
- `TrackRepository`

## Backend: Supabase

- **Auth**: email/password via `supabaseClient.auth`
- **Database**: `workspaces`, `workspace_members`, `projects`, `project_tracks` tables (see `supabase_db_schema.sql`)
- **Storage**: `tracks/` bucket for audio files
- **Workspace sharing**: `workspaces.share_token` (UUID) enables public share links; anyone with the token can join as a read-only member
- Session state is observed via `supabaseClient.auth.sessionStatus` (StateFlow)

### Secrets

A `Secrets.kt` file (not committed) must provide:
```kotlin
object Secrets {
    const val SUPABASE_URL = "..."
    const val SUPABASE_ANON_KEY = "..."
}
```

In CI, `Secrets.kt` is generated from GitHub Actions secrets at build time.

## Audio Playback Flow

1. User opens a project → `PlayerViewModel.setProject()`
2. `TrackRepository.fetchTracks(projectId)` loads metadata from Supabase
3. Per track: `TrackRepository.getOrDownloadTrackBytes()` — cache-first: local hit or download from Supabase Storage
4. Bytes loaded into `MultitrackPlayer.loadTracks()`
5. Player prepares platform-specific instances (ExoPlayer on Android), enables UI controls

Each track has independent volume (0.0–1.0), mute, and solo controls. Track settings (volume, mute, solo) are persisted per project via `TrackSettingsStorage`.

## Storage

`LocalStorage` is a multiplatform `expect object` for simple persistent key-value storage:
- **Web**: `window.localStorage`
- **Android / iOS**: no-op stubs (returns `null` / discards writes); implement with `SharedPreferences` / `NSUserDefaults` when needed

`TrackSettingsStorage` wraps `LocalStorage` and serializes a `Map<trackId, SavedTrackSettings>` as JSON under the key `casanostra_track_settings_{projectId}`.

Key constants defined in `LocalStorage.kt`:
- `KEY_PENDING_SHARE_TOKEN = "casanostra_pending_share_token"`

## Deep Linking

`deeplink/DeepLinkProvider.kt` declares two top-level `expect` functions:

```kotlin
expect fun getInitialDeepLinkToken(): String?  // e.g. from /workspace/{token}
expect fun getAppBaseUrl(): String             // e.g. "https://casanostra.orakull.ru"
```

- **Web (`wasmJs`)**: parses `window.location.pathname` for `/workspace/{token}` and reads `window.location.origin`
- **Android / iOS**: stubs returning `null` / hardcoded production URL (full deep-link support is a future task)

`App()` receives `initialDeepLinkToken: String?` from the platform entry point and drives the guest/auth flow.

## CI/CD & Deployment

Two GitHub Actions workflows trigger on push to `develop`:

### `deploy-pages.yml` — GitHub Pages
1. Builds `wasmJsBrowserDistribution`
2. Copies `dist/` to GitHub Pages with a `404.html` stub for SPA routing
3. Appends a git short SHA to `composeApp.js` for cache busting

### `deploy-vps.yml` — VPS (Docker + nginx)
1. Builds `wasmJsBrowserDistribution`
2. Builds and pushes a Docker image to `ghcr.io/orakull/casanostra`
3. SSHs into VPS, runs `docker-compose pull && docker-compose up -d`
4. App runs on `127.0.0.1:8081` behind an nginx reverse proxy

**Required secrets:** `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`

## Key Library Versions

| Library | Version |
|---------|---------|
| Kotlin | 2.3.10 |
| Compose Multiplatform | 1.10.0 |
| AGP | 8.11.2 |
| Supabase BOM | 3.4.1 |
| Ktor | 3.0.1 |
| Media3 / ExoPlayer | 1.6.0 |
| Koin | 4.0.0 |
| FileKit | 0.8.8 |

Versions are declared in `gradle/libs.versions.toml`. Always use the version catalog aliases — never hardcode version strings in `build.gradle.kts`.

## Working Style

- Write all Bash tool call `description` parameters in **Russian**.

## Keeping This File Up to Date

**Update `CLAUDE.md` whenever you make a significant code change.** Significant changes include:

- Adding a new screen, ViewModel, or feature package
- Adding a new `expect`/`actual` abstraction
- Adding or removing a repository, DI singleton, or storage layer
- Extracting or creating a reusable UI component (update the Component Registry table)
- Adding a new Supabase table, column, or RLS policy
- Changing the app navigation flow
- Adding a new library dependency (update the Key Library Versions table)
- Adding or modifying a CI/CD workflow

**What to update:**
- The relevant architecture bullet(s) in the Architecture section
- The App Navigation section if the screen flow changes
- The Component Registry table if a new shared component is added
- The Key Library Versions table if a library is added or its version changes
- Any dedicated section (Storage, Deep Linking, CI/CD, etc.) that describes the changed subsystem

Keeping this file accurate is as important as keeping the code correct — stale documentation misleads future AI sessions.

## UI Components: Reuse Rules

### Component Registry

Before writing any inline UI code, **check whether a ready-made component already exists**:

| Component | File | When to use |
|-----------|------|-------------|
| `CasaNostraCard` | `ui/common/Components.kt` | Any clickable card (workspace, project, etc.) |
| `ReadOnlyBadge` | `ui/common/Components.kt` | Wherever read-only mode needs to be indicated |
| `MuteSoloSegmentedButton` | `ui/player/MuteSoloButton.kt` | Mute/Solo control on a track row |
| `UploadProgressOverlay` | `ui/player/UploadProgressOverlay.kt` | Track upload progress display in PlayerScreen |

### When to Extract a Component

Extract a composable to its own file / `ui/common/` if **at least one** condition holds:
- The component is used (or could be used) in two or more screens
- The component contains non-trivial animation or complex state-display logic
- The component is a standalone UI element with clear semantics (Badge, SegmentedButton, EmptyState, etc.)

### No Duplication Rule

Never copy identical composable code between files. If the same visual element appears in more than one place, immediately extract it to `ui/common/Components.kt` (cross-screen) or a dedicated file within its feature package (within-feature).

### Naming Conventions

- Cross-screen shared components → `ui/common/Components.kt`
- Feature-specific components too large for one file → separate file in the feature package (`ui/player/MuteSoloButton.kt`, `ui/player/UploadProgressOverlay.kt`, etc.)
- File name = name of the primary composable inside it

## Platform-Specific Notes

- `expect`/`actual` is the pattern for all platform-divergent code: audio player, cache, file picking, local storage, deep linking
- Android minimum SDK is 29
- Web target uses Wasm; JS target also exists for broader compatibility
- `FileKit` is used for multiplatform file picking (audio upload)
- `LocalStorage` on Android/iOS is currently a no-op stub; implement with `SharedPreferences` / `NSUserDefaults` when required
