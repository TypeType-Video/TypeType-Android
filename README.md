<!-- markdownlint-disable MD033 MD041 -->

<div align="center">
  <img src="assets/banner.png" alt="TypeType" width="100%">
  <h1>TypeType Android</h1>
  <p>Your TypeType library, in a fast native app for Android.</p>
</div>

<div align="center">

[<img src="assets/widgets/license.svg" alt="GPL v3">](LICENSE)
[<img src="assets/widgets/typetype.svg" alt="TypeType">](https://github.com/TypeType-Video/TypeType)

</div>

<p align="center">
  <a href="https://github.com/TypeType-Video/TypeType-Android/releases/latest"><strong>Download the latest signed APK</strong></a>
  ·
  <a href="https://typetype-video.github.io/Docs-TypeType/guide/">User guide</a>
  ·
  <a href="https://github.com/TypeType-Video/TypeType/issues">Report a problem</a>
</p>

TypeType Android brings your self-hosted TypeType experience to phones and
tablets. Browse your recommendations and subscriptions, search across supported
platforms, continue where you stopped, and watch through a native player built
for Android.

The app communicates exclusively with the TypeType instance you choose. Your
account, history, subscriptions, playlists, playback sessions, and downloads
remain under the control of that instance.

## See it in action

| Home and Continue Watching | Search | Subscriptions |
| --- | --- | --- |
| ![TypeType home feed and Continue Watching on Android](assets/screenshots/android-home.png) | ![TypeType video search on Android](assets/screenshots/android-search.png) | ![TypeType subscriptions feed on Android](assets/screenshots/android-subscriptions.png) |

| Library and history | Native player | Comments |
| --- | --- | --- |
| ![TypeType library and history on Android](assets/screenshots/android-library.png) | ![TypeType native video player on Android](assets/screenshots/android-player.png) | ![TypeType video comments on Android](assets/screenshots/android-comments.png) |

| Notifications | Profile | Settings |
| --- | --- | --- |
| ![TypeType notifications on Android](assets/screenshots/android-notifications.png) | ![TypeType profile and animated avatar support on Android](assets/screenshots/android-profile.png) | ![TypeType settings on Android](assets/screenshots/android-settings.png) |

These are real captures from the signed Android application connected to the
TypeType beta with a demonstration account. The displayed content changes over
time.

## Everything that matters, close at hand

- Continue Watching keeps unfinished videos at the top of your home feed.
- Search videos, channels, playlists, music, and other supported content.
- Follow subscriptions and clearly identify live, premiere, and special videos.
- Keep history, favorites, Watch Later, playlists, notifications, and profile
  settings together.
- Watch with background audio, Picture in Picture, a mini-player, and an
  audio-only mode.
- Choose quality, codec, audio track, captions, playback speed, and image mode.
- Use chapters, SponsorBlock controls, a queue, comments, related videos, and a
  sleep timer.
- Download supported videos and keep useful cached content visible through
  temporary network interruptions.
- Import existing data and manage multiple TypeType instances and accounts.

TypeType Android supports Android 6.0 and newer, and does not require Google
Play Services.

## Install

1. Open the
   [latest Release](https://github.com/TypeType-Video/TypeType-Android/releases/latest).
2. Download the signed `TypeType-vX.Y.Z.apk`. A matching SHA-256 file is
   available if you want to verify the download.
3. Open the APK and allow installation from your browser or file manager when
   Android asks.
4. Launch TypeType, enter the address of your TypeType instance, then sign in
   with a local account or OIDC. Guest access appears when the instance supports
   it.

Installing a newer signed Release over an existing Release keeps the
application data. If Android reports an incompatible signature, remove any
Debug build before installing the signed APK.

> [!NOTE]
> TypeType Android is a client for
> [TypeType-Server](https://github.com/TypeType-Video/TypeType-Server). You need
> access to a compatible TypeType instance. If you want to host one, start with
> the [self-hosting guide](https://typetype-video.github.io/Docs-TypeType/self-hosting/introduction).

## Built for unreliable mobile networks

The app keeps locally cached pages visible while refreshing, resumes
progressive feeds, and reacts to network loss and recovery without treating
every interruption as a permanent failure. Playback, downloads, and account
operations still depend on the selected instance and its upstream providers.

When something fails, open **Settings > Diagnostics** to review a redacted,
local request history before sharing it. Diagnostics do not include raw
credentials, tokens, cookies, or private response bodies.

## Help and feedback

- Read the [TypeType user guide](https://typetype-video.github.io/Docs-TypeType/guide/).
- Check the [latest Android Releases](https://github.com/TypeType-Video/TypeType-Android/releases).
- Report bugs and request features in the
  [central TypeType issue tracker](https://github.com/TypeType-Video/TypeType/issues).

When reporting a playback or network problem, include the Android version, app
version, TypeType instance version, video URL, the action that failed, and the
redacted diagnostics export when available.

<details>
<summary><strong>Development and architecture</strong></summary>

### Technology

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| Interface | Jetpack Compose and Material 3 |
| Playback | Media3 with the TypeType playback module |
| Networking | Retrofit, OkHttp, and kotlinx.serialization |
| Persistence | Room and DataStore |
| Background work | WorkManager and MediaSessionService |
| Dependency injection | Hilt |

The project targets API 37 and keeps runtime compatibility down to API 23.
Server capabilities are discovered at runtime. Extraction, YouTube session
state, SABR, PO tokens, recommendations, synchronization, and server-side
downloads remain TypeType-Server responsibilities.

Run the complete local verification with the repository Gradle wrapper:

```sh
./gradlew --no-daemon \
  :player:testDebugUnitTest \
  :app:testDebugUnitTest \
  :player:lintDebug \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleRelease
```

Development changes target `dev`; `main` represents the stable release line.
Release signing material, instance URLs, accounts, and credentials are never
stored in this repository.

</details>

## TypeType ecosystem

- [TypeType](https://github.com/TypeType-Video/TypeType), stack installation,
  releases, and coordination
- [TypeType-Server](https://github.com/TypeType-Video/TypeType-Server), API,
  extraction, playback sessions, and private user data
- [TypeType-Frontend](https://github.com/TypeType-Video/TypeType-Frontend),
  browser client
- [TypeType-IOS](https://github.com/TypeType-Video/TypeType-IOS), native iPhone
  and iPad client
- [Docs-TypeType](https://github.com/TypeType-Video/Docs-TypeType), user and
  self-hosting documentation

## License

TypeType Android is licensed under the [GNU General Public License v3.0](LICENSE).

Copyright © 2026 Priveetee and TypeType contributors.
