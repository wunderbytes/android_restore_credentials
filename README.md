# android_restore_credentials

Android-only Flutter plugin for **Zero-Tap sign-in restoration** using
[Credential Manager Restore Credentials](https://developer.android.com/identity/sign-in/restore-credentials).

This package is intended to help Flutter apps meet Google Play’s upcoming
[Zero-Tap Sign-In Restoration](https://support.google.com/googleplay/android-developer/answer/17492799#zero-tap_sign-in_restoration)
requirement by wrapping Android’s Restore Credentials APIs.

> **Status:** Restore Credentials create / get / clear are implemented on
> Android (Kotlin client + Dart facade). iOS / web / desktop are intentionally
> not supported.

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

## API

```dart
final plugin = AndroidRestoreCredentials();

// After sign-in, with `requestJson` fetched from your RP server.
final attestation = await plugin.createRestoreKey(requestJson: requestJson);

// On first launch / BackupAgent restore, with `requestJson` fetched from your RP.
final assertion = await plugin.getRestoreKey(requestJson: requestJson);

// On sign-out.
await plugin.clearRestoreKey();
```

`requestJson` is WebAuthn `PublicKeyCredentialCreationOptionsJSON` for create
and `PublicKeyCredentialRequestOptionsJSON` for get. The plugin passes strings
through; it does not parse WebAuthn JSON beyond what `androidx.credentials`
validates.

### Host integration (two-tier restoration)

Restore Credentials should be driven from **two tiers** in the host app:

1. **Tier 1 — background (`BackupAgent.onRestoreFinished`):** if the host
   manifest has `android:allowBackup="true"`, call the **Kotlin**
   `RestoreCredentialsClient.getRestoreKey` synchronously (e.g. `runBlocking`)
   inside `onRestoreFinished` so sign-in completes before first UI. Do **not**
   use `onRestore` (key-value only). The plugin does **not** register a
   `BackupAgent` for you; hosts subclass their own and call the same Kotlin
   client. Do **not** change `allowBackup`.

   This runs **without a Flutter engine**, so the host must do the backend
   calls in Kotlin directly (fetch options from the RP, call the client, POST
   the assertion, persist the session). The plugin only provides the client:

   ```kotlin
   class MyBackupAgent : BackupAgentHelper() {
       override fun onRestoreFinished() {
           runBlocking {
               val options = myRp.fetchRestoreCredentialRequestOptions() // your Kotlin API
               val assertion = RestoreCredentialsClient(applicationContext)
                   .getRestoreKey(requestJson = options)
               myRp.verifyRestoreCredentialAndSignIn(assertion)          // your Kotlin API
           }
       }
   }
   ```
   Register it only in the **app** manifest: `android:allowBackup="true"` +
   `android:backupAgent=".MyBackupAgent"`.

2. **Tier 2 — foreground (first launch):** in the launcher `Activity.onCreate`
   / Flutter first frame, call `getRestoreKey` to cover the cases where
   background restore did not complete, backup is off, or restore is
   independent of app-data backup. This is what the `example/` app
   demonstrates.

Create a restore key when the user is signed in (after sign-up / sign-in, or on
a later launch if already signed in and no key yet). Keep a local flag such as
`has_synced_restore_credential` so you do not create on every launch. Call
`clearRestoreKey` on logout — Credential Manager will **not** delete restore
keys on sign-out automatically, and a typeless `clearCredentialState` does
**not** remove restore keys.

### `PlatformException` codes

Native failures surface as `PlatformException` with stable `code` values:

| Code | Typical cause |
| --- | --- |
| `invalid_argument` | Missing/blank `requestJson`; invalid WebAuthn JSON / `user.id` |
| `unsupported_android_version` | Device API < 28 (Android 9) |
| `create_dom` | `CreateRestoreCredentialDomException` (malformed creation options) |
| `e2ee_unavailable` | Cloud backup requested but unavailable, and the local retry also failed |
| `no_credential` | `NoCredentialException` on get (nothing to restore) |
| `get_interrupted` / `get_canceled` | Get interrupted or canceled |
| `get_failed` | Other `GetCredentialException` |
| `create_failed` | Other `CreateCredentialException` |
| `clear_failed` | `ClearCredentialException` |
| `unexpected_type` | Response credential was not `RestoreCredential` |
| `unknown` | Anything else |

Create retries locally on `E2eeUnavailableException` before surfacing
`e2ee_unavailable`, so that code is rare.

## Backend requirements

This plugin is **client-side only**. Your relying-party (RP) server must
implement the WebAuthn registration/assertion verification and store restore
keys. Adapted from Google's Restore Credentials client skill (2026-08-21):

1. **Differentiate restore credentials from passkeys in storage.** Standard
   WebAuthn often assumes user verification. Restore keys are system-managed
   and hidden. Use a distinct credential type or metadata. Do not show them on
   passkey management screens. Background restore may skip explicit UV.
2. **Prevent orphaned keys.** Uninstall / clear-data deletes the **local** key
   and does not call the server. Plan cleanup: replace old keys on new
   registration, TTL unused keys, at most one key per user per device.
3. **Balance lifespan and TTL.** If the user restores, then signs out on the
   **old** device, local clear must not immediately invalidate the key the
   **new** device still needs. TTL should survive that transition; delete
   based on registration/usage rules, not only on client clear.
4. **Support multiple devices.** Schema: multiple active restore credentials
   per user (e.g. per device id), not 1:1 user↔key.

The same RP stack as passkeys can verify restore keys, but **persist them as a
separate class**. See [Play Help — Zero-Tap Sign-In Restoration](https://support.google.com/googleplay/android-developer/answer/17492799#zero-tap_sign-in_restoration)
and the [Android Restore Credentials implementation guide](https://developer.android.com/identity/sign-in/restore-credentials-implementation).
Full agent notes live in [`docs/agent/07-backend-reminders.md`](docs/agent/07-backend-reminders.md),
with detailed guides for [Flutter app integration](docs/agent/09-flutter-app-integration.md)
and [backend integration](docs/agent/10-backend-integration.md).

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

Planned (not implemented): optional `BackupAgent` helper hosts can subclass.
Architecture and implementation notes for agents are in
[AGENTS.md](AGENTS.md) and [docs/agent/](docs/agent/README.md).

## License

BSD 3-Clause. See [LICENSE](LICENSE).
