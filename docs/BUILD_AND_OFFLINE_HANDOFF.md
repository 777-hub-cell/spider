# VISTORA — build and offline handoff

This document applies to the standalone VISTORA Android repository. The original application and prototype in the main project repository are not changed by this copy.

## Project

- Repository: `777-hub-cell/spider`
- Default branch: `main`
- Android project: `android/`
- Original prototype: `reference/Vistora.prototype.html`

## Included

- Native Kotlin/Jetpack Compose Android application.
- Room-backed local library and episode notes.
- DataStore preferences.
- Home, Library, Discover, Stats, Settings, Title Details, My Notes and add/edit title flows.
- Manual progress, statuses, ratings, date tracking, overall thoughts and per-episode notes.
- Explicit ZIP backup/restore using Android's document picker.
- Optional direct TMDB and AniList catalogue search.
- Keystore-encrypted TMDB credential storage.
- Unit and Room instrumentation tests.
- GitHub Actions workflow for build, tests, lint, emulator run and APK artifacts.

## Build

Requirements: JDK 17 and Android SDK platform 37.

From the repository root:

```bash
cd android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Debug APK output: `android/app/build/outputs/apk/debug/app-debug.apk`.

Room instrumentation tests require an Android emulator or connected device:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## GitHub Actions artifacts

Open the repository's Actions tab and select the latest **VISTORA Android** run.

- `vistora-debug-apk` is a debug APK for device testing.
- `vistora-release-apk` is signed only when the release-signing secrets are configured. Otherwise the APK is unsigned and cannot be installed as a normal release.

The workflow uploads the debug artifact immediately after its assembly succeeds. Later unit-test, lint or emulator failures do not retroactively make that artifact a verified release.

## Configure release signing

Create a dedicated Android release keystore on a trusted computer. For example:

```bash
keytool -genkeypair -v -keystore vistora-release.jks -keyalg RSA -keysize 3072 -validity 10000 -alias vistora
```

Base64-encode the keystore file locally, then add these four **Actions repository secrets** in GitHub Settings → Secrets and variables → Actions:

- `VISTORA_KEYSTORE_BASE64` — base64 contents of the keystore file.
- `VISTORA_KEYSTORE_PASSWORD` — keystore password.
- `VISTORA_KEY_ALIAS` — alias created above.
- `VISTORA_KEY_PASSWORD` — key password.

The workflow decodes the keystore into the runner's temporary directory and does not commit it. Never put the keystore or its passwords in the repository, a source file, an issue, or a chat. Keep a secure offline backup of the keystore: Android updates must use the same signing identity.

## Offline behaviour

The app stores titles, statuses, progress, ratings, dates, preferences, episode notes, overall thoughts and poster copies on the device. These core features work without a network connection.

Discover can call provider APIs when online:
- TMDB for films, TV series and K-Dramas (user-supplied credential).
- AniList public GraphQL for anime (no user key required).

There is no VISTORA server, login, account, cloud database or sync service. Internet access is only for optional catalogue discovery and image retrieval.

## Before a public release

- Confirm GitHub Actions build, unit tests, Android lint and emulator instrumentation are green.
- Validate the signed release variant with R8 and inspect the resulting APK.
- Test on physical Android phones, including small screens and large font settings.
- Complete TalkBack, focus order, contrast and reduced-motion checks.
- Add the current official TMDB logo asset in the About/attribution area as required by TMDB's current branding rules; keep the required text attribution.
- Review current TMDB and AniList terms and rate limits.
- Test ZIP backup/restore round-trips with posters and long episode notes.
- Verify app update installation with the same release signing key.

A debug build or unsigned release artifact is not a production release.
