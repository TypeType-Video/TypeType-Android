# TypeType TV

This module is the native Android TV client for TypeType. It owns TV-specific
layouts, focus navigation, remote-key interaction, playback presentation, and
TV appearance choices. Shared server contracts, authentication, sessions,
network policy, and Media3 adapters come from TypeType-SDK.

## Local build

From the repository root:

```sh
./gradlew :tv:testDebugUnitTest :tv:lintDebug :tv:assembleDebug
```

The Gradle settings use `../TypeType-SDK` when that checkout exists. Set
`TYPETYPE_SDK_PATH` to another local checkout when needed. The debug build uses
the configured beta instance by default; do not place credentials in the
repository.
