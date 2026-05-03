<div align="center">
  <img src="assets/banner.png" alt="TypeType-Android" width="100%">
</div>

TypeType-Android is the native Android client for [TypeType](https://github.com/Priveetee/TypeType), a self-hosted, privacy-respecting video platform.

This project is in **very early development**. Expect breakage, missing features, and incomplete UI.

## What this is

A Kotlin Android application that talks exclusively to a TypeType-Server instance over HTTP. The client carries no extraction engine of its own — all extraction, recommendation, and user-data work happens server-side.

## What this is not

- Not a standalone YouTube client. It does not work without a reachable TypeType-Server.
- Not a Piped, Invidious, or NewPipe client.
- Not maintained by the LibreTube team. This is an independent project that started from a fork of their codebase and was substantially rewritten.

## Stack

| Role | Tool |
|---|---|
| Language | Kotlin |
| Min SDK | 26 (Android 8.0) |
| UI | Jetpack Compose for new modules, XML for inherited LibreTube screens |
| Navigation | Navigation Compose with type-safe routes |
| DI | Hilt |
| Network | Retrofit + OkHttp + kotlinx.serialization |
| Local DB | Room |
| Image loading | Coil |
| Player | Media3 / ExoPlayer |
| Background work | WorkManager |
| Build | Gradle (Kotlin DSL) |
| License | GPL v3 |

## Backend requirement

TypeType-Android requires a running TypeType-Server. The first launch will ask you for the instance URL.

You can either:
- Use a public TypeType instance (none are advertised yet)
- Self-host TypeType-Server (see [TypeType](https://github.com/Priveetee/TypeType) for the full stack)

## Building

Open the project in Android Studio (Hedgehog or newer), let Gradle sync, and run on a device or emulator with API 26 or newer.

A signed release flow and CI publishing will be added later.

## License

GPL v3 — inherited from LibreTube. See [LICENSE](LICENSE).

The original LibreTube `LICENSE` file is preserved as-is to honor the obligations of the GPL.

## Acknowledgments

A huge thanks to the projects without which TypeType-Android would not exist:

- [LibreTube](https://github.com/libre-tube/LibreTube) — the entire UI/UX foundation, player architecture, navigation patterns, downloads logic, and a great deal of Android-specific polish are inherited from their work. TypeType-Android started from their codebase and rewires the data layer to TypeType-Server.
- [Findroid](https://github.com/jarnedemeulemeester/findroid) by Jarne Demeulemeester — architectural reference for the multi-server setup flow, the MVI pattern in the setup module, Compose Navigation with type-safe routes, and the overall discipline of a fully server-side native Android client.
- [Bnyro](https://github.com/Bnyro) and the LibreTube contributors — for years of work refining one of the cleanest Android video player UIs available.

Both LibreTube and Findroid are licensed under GPL v3. TypeType-Android is published under the same license.
