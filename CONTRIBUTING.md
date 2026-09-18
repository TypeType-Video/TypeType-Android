# Contributing to TypeType Android

Thanks for taking the time to help. Bug fixes, translations, tests,
accessibility improvements, performance work, and interface changes are all
useful contributions.

You do not need to understand the complete TypeType stack before getting
started. If you are unsure where a change belongs, open an issue and describe
the problem from a user's point of view. We can work out the technical boundary
together.

## Before you start

Bug reports and feature requests for the Android app live in the
[TypeType Android issue tracker](https://github.com/TypeType-Video/TypeType-Android/issues).
Problems with TypeType-Server, extraction, the web client, or deployment belong
in the [main TypeType issue tracker](https://github.com/TypeType-Video/TypeType/issues).
For a large feature, or anything that changes the Server API, it is best to
agree on the expected behavior before writing a lot of code.

This repository owns the Android app: its Compose interface, local storage,
downloads, background work, and Media3 integration. TypeType-Server owns
extraction, YouTube sessions, SABR, recommendations, synchronization, and
server-side download jobs. Keeping that separation lets every TypeType client
benefit from the same server fixes.

## Getting the app running

You will need JDK 21 or newer and an Android SDK with API 37 installed. Clone the
repository, start from the `dev` branch, and let Android Studio use the included
Gradle wrapper.

```sh
git switch dev
./gradlew :app:assembleDebug
# Android TV
./gradlew :tv:assembleDebug
```

On first launch, the app asks for a TypeType instance. Use your own test
instance or account, and make sure its address, credentials, cookies, signing
files, and diagnostics never end up in a commit.

## How the app is built

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| Interface | Jetpack Compose and Material 3 |
| Playback | Media3 with the TypeType playback module |
| Networking | Retrofit, OkHttp, and kotlinx.serialization |
| Persistence | Room and DataStore |
| Background work | WorkManager and MediaSessionService |
| Dependency injection | Hilt |

The project compiles against API 37 and keeps runtime compatibility down to API
23. Server capabilities are discovered at runtime, so a feature may depend on
the contract advertised by the selected TypeType instance.

## Source layout

| Path | Responsibility |
| --- | --- |
| `app/src/main/java/dev/typetype/android/core` | Shared UI and platform utilities |
| `app/src/main/java/dev/typetype/android/data` | Network, database, cache, and repository implementations |
| `app/src/main/java/dev/typetype/android/domain` | Models and application contracts |
| `app/src/main/java/dev/typetype/android/feature` | Compose screens and feature state |
| `app/src/main/java/dev/typetype/android/services` | Long-lived playback and Android services |
| `player` | TypeType playback integration for Media3 |
| `app/src/test` | JVM unit and repository tests |
| `app/src/androidTest` | Room, Compose, platform, and device tests |
| `tv/src/main` | Native Android TV UI, navigation, focus, and playback |
| `tv/src/test` | Android TV JVM unit tests |

Most changes only touch one or two of these areas. As a rule of thumb,
composables render state, repositories decide how data is loaded and cached,
services own long-lived playback, and WorkManager owns resumable background
jobs.

## Android compatibility

TypeType Android supports Android 6.0 through Android 17, API 23 through API 37.
Many contributors only have a recent phone, which is fine. Please use an
emulator for older versions when your change touches platform behavior.

The baseline H.264/AAC playback path must remain available everywhere. Newer
codecs, HDR, and high frame rates need a real decoder capability check. The app
must also remain usable without Google Play Services, and OIDC authentication
must open in an external browser rather than an embedded WebView.

For layout and player changes, remember to try both gesture and three-button
navigation. Lifecycle changes deserve a quick check after process recreation,
in the background, and from the notification or lock screen when applicable.

## Writing code that fits the project

Production code is written in Kotlin. We favor immutable UI state, structured
concurrency, lifecycle-aware Flow collection, and small files with one clear
responsibility. Production files should stay below 330 physical lines and test
files below 350; generated files and Room schemas are exempt.

Please avoid `!!`, unchecked casts, global mutable state, static service
locators, and swallowed exceptions. When the Server returns a stable error code
or request ID, preserve it so the app can show a useful message and a bug report
can be traced.

Clear names and structure are more useful than long explanatory comments.
User-visible text belongs in Android resources, and interactive controls need
accessible labels. New dependencies need a compatible license and a reasonable
maintenance story.

The Android client must continue to use TypeType-Server.

## Before opening a pull request

Before opening a pull request, run the same core checks as CI:

```sh
./gradlew --no-daemon \
  :player:testDebugUnitTest \
  :app:testDebugUnitTest \
  :player:lintDebug \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleRelease
```

For TV changes, also run:

```sh
./gradlew --no-daemon \
  :tv:testDebugUnitTest \
  :tv:lintDebug \
  :tv:assembleDebug
```

Add the checks that make sense for your change. You are not expected to own
every Android version or device; if something could not be tested, mention it
in the pull request so another contributor can help. Useful checks include:

- Room migration tests for every affected schema
- Compose navigation and accessibility tests for interface changes
- Media3 playback and physical codec checks for player changes
- install, launch, upgrade, and process-recreation checks for lifecycle changes
- API 23 and API 37 checks when platform compatibility can be affected
- no-Google-services checks for authentication, playback, or background work

If a Gradle task reports `NO-SOURCE`, it did not run a test suite.

## Commits and pull requests

Create your branch from `dev` and open the pull request against `dev`. Commit
messages use this form:

```text
type: short description
```

Common types are `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `perf`,
`style`, `build`, and `ci`. Use a short imperative description and keep
unrelated concerns in separate commits.

We try to keep individual commits below 290 added lines because smaller changes
are easier to understand and review. Generated Room schemas and baseline
profiles can live in their own larger commit.

In the pull request description, tell us:

- what changed and why;
- whether it is a bug fix, feature, documentation change, translation, or
  another kind of contribution;
- the related issue, when one exists;
- the TypeType-Server contract involved, when applicable;
- the Android versions and navigation modes tested;
- the exact automated and manual validation performed;
- whether another TypeType component must change with it.

For visible interface changes, include real screenshots or a short recording.
Before uploading anything, check it for private instance data, account details,
tokens, cookies, local paths, and unredacted diagnostics.

Contributions to this repository are distributed under
[GPL-3.0](LICENSE).
