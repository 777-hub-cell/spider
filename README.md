# VISTORA

**Your entertainment, all in one place.**

VISTORA is a private, personal entertainment library and watch diary. This repository contains the separate native Android application written in Kotlin and Jetpack Compose. It is not a streaming player.

## Features

- Local-first library for Movies, K-Dramas, Anime and TV Series.
- Home, Library, Discover, Stats, Settings, Title Details and My Notes.
- Manual watch progress, personal statuses, ratings and date tracking.
- Separate overall thoughts and one editable note per episode.
- ZIP backup and restore through Android's document picker.
- Optional catalogue discovery: TMDB for films, TV and K-Dramas; AniList GraphQL for anime.
- Keystore-backed encryption for the optional TMDB credential.
- Core library, notes, progress and settings remain usable offline.

A fresh install starts with an empty personal library. VISTORA does not provide video playback, an account system, a backend, cloud sync or a remote database.

## Design

The app preserves VISTORA's ink-black background, muted burgundy, dusty rose accents, serif display type and restrained status colours. The original HTML prototype is preserved in `reference/Vistora.prototype.html` as a design and behaviour reference.

## Project layout

- `android/` — native Kotlin/Jetpack Compose application.
- `docs/PRODUCT_SPEC.md` — product and interaction specification.
- `docs/RESEARCH_AND_ENGINEERING_STANDARD.md` — architecture, dependency and quality standards.
- `docs/ROADMAP.md` — milestones and release gates.
- `docs/BUILD_AND_OFFLINE_HANDOFF.md` — build, CI and release instructions.
- `reference/Vistora.prototype.html` — original HTML prototype, retained unchanged.
- `.github/workflows/vistora-android.yml` — Android CI and APK artifact workflow.

## Build locally

Requirements: JDK 17 and Android SDK platform 37.

```bash
cd android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Debug APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions APKs

Every push to `main`, pull request, or manual workflow run builds the debug APK and verifies the project. Successful builds publish the `vistora-debug-apk` artifact. The workflow also builds a release APK. Without release signing secrets, that release artifact is unsigned and is for release-build verification only.

To produce a signed release APK, configure the four GitHub Actions repository secrets documented in [the build handoff](docs/BUILD_AND_OFFLINE_HANDOFF.md). Never commit a keystore, password or API credential.

## Before public release

The build and unit tests are automated, but a public release still requires passing Android lint and instrumentation tests, physical-device checks, accessibility review, backup/restore testing and the provider attribution/terms checklist in the docs. A green build is not a substitute for testing on real devices.
