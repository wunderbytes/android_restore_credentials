# android_restore_credentials

Android-only Flutter plugin for **Zero-Tap sign-in restoration** using
[Credential Manager Restore Credentials](https://developer.android.com/identity/sign-in/restore-credentials).

This package is intended to help Flutter apps meet Google Play’s upcoming
[Zero-Tap Sign-In Restoration](https://support.google.com/googleplay/android-developer/answer/17492799#zero-tap_sign-in_restoration)
requirement by wrapping Android’s Restore Credentials APIs.

> **Status:** the plugin project is scaffolded. Restore Credentials create / get / clear
> APIs are not implemented yet. The current Dart surface is the default Flutter plugin
> template (`getPlatformVersion`).

## Platform support

| Platform | Supported |
| --- | --- |
| Android | Yes (API 28 / Android 9+) when Restore Credentials is implemented |
| iOS | No |
| Web | No |
| Desktop | No |

Restore Credentials is not available on other Flutter platforms. Calls from a
multi-platform app should be guarded with `defaultTargetPlatform == TargetPlatform.android`.

## Background

From **April 2027**, Play apps that support user sign-in (optional or mandatory)
must restore signed-in state when a user moves to a new Android device and
chooses to restore data (device-to-device or cloud backup).

Manual sign-in during device setup adds friction and can expose users to
phishing. The [Android Restore Credentials API](https://developer.android.com/identity/sign-in/restore-credentials)
is the primary way to meet this requirement. It is available from **Android 9**
and requires Google Play services (GMS) core **24220000+** and
`androidx.credentials` **1.5.0+**.

The requirement currently applies to **mobile and tablet** form factors only.

### Scope notes (Play policy)

These points come from Play’s technical quality documentation and may change:

- Apps that integrated [Block Store](https://developers.google.com/identity/blockstore/android)
  on or before **30 September 2026** to restore sign-in state are considered compliant.
- Permanently private and enterprise device-management apps are out of scope.
- Apps with strict regulatory constraints (for example financial services or healthcare)
  may request an exemption in Play Console before enforcement.
- Games are currently out of scope; dedicated guidance is expected in 2027.

See [Play Console technical quality requirements](https://support.google.com/googleplay/android-developer/answer/17492799#zero-tap_sign-in_restoration)
for the authoritative wording.

## Getting started

This repository is managed with [FVM](https://fvm.app/). The pinned Flutter SDK
is **3.35.7** (see `.fvmrc`).

```sh
dart pub global activate fvm
fvm install
fvm flutter pub get
```

VS Code / Cursor should pick up the SDK via `.vscode/settings.json`
(`dart.flutterSdkPath`: `.fvm/flutter_sdk`).

Add a path or pub dependency when the package is published:

```yaml
dependencies:
  android_restore_credentials:
    path: ../android_restore_credentials
```

## Example

The `example/` app is a standard Flutter plugin example (Android only). From the
repo root:

```sh
fvm flutter test
cd example
fvm flutter run
```

## Development

Native Android code lives in
`android/src/main/kotlin/eu/wunderbytes/android_restore_credentials/`.
Dart API and method-channel wiring live in `lib/`.

Planned (not implemented): `createRestoreKey`, `getRestoreKey`, and
`clearRestoreKey`. Architecture and implementation notes for agents are in
[AGENTS.md](AGENTS.md) and [docs/agent/](docs/agent/README.md).

## License

BSD 3-Clause. See [LICENSE](LICENSE).
