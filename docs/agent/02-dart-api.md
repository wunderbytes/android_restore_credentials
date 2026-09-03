# Planned Dart API

Keep the existing platform-interface + method-channel split. Add methods on
`AndroidRestoreCredentials`, `AndroidRestoreCredentialsPlatform`, and
`MethodChannelAndroidRestoreCredentials`. Remove `getPlatformVersion` when
implementing (it is template noise), or leave it until the first API PR if
that keeps the diff smaller—prefer **removing it** in the same change that
lands the three real methods so pub.dev API is not confusing.

## Public class

```dart
class AndroidRestoreCredentials {
  /// Registers a restore key. [requestJson] is PublicKeyCredentialCreationOptionsJSON
  /// from the relying party. Returns CreateRestoreCredentialResponse JSON
  /// (attestation) for the RP to store.
  ///
  /// Tries cloud backup first; on E2EE/backup unavailable, retries locally
  /// unless [preferCloudBackup] is false (local only).
  Future<String> createRestoreKey({
    required String requestJson,
    bool preferCloudBackup = true,
  });

  /// Retrieves a restore key. [requestJson] is PublicKeyCredentialRequestOptionsJSON
  /// from the RP. Returns RestoreCredential authentication JSON for the RP.
  /// Must not be combined with other credential types (enforced natively).
  Future<String> getRestoreKey({
    required String requestJson,
  });

  /// Deletes the restore key on this device (TYPE_CLEAR_RESTORE_CREDENTIAL).
  Future<void> clearRestoreKey();
}
```

Method channel names (stable):

| Dart | Channel method | Arguments |
| --- | --- | --- |
| `createRestoreKey` | `createRestoreKey` | `requestJson` (String), `preferCloudBackup` (bool) |
| `getRestoreKey` | `getRestoreKey` | `requestJson` (String) |
| `clearRestoreKey` | `clearRestoreKey` | none |

Success payloads:

- create/get: `String` JSON
- clear: `null`

Non-Android: `UnsupportedError` or `MissingPluginException` — document that
hosts must guard with `defaultTargetPlatform == TargetPlatform.android`.

## Types

Prefer **JSON strings** over a Dart WebAuthn model in v0.1. Hosts already have
RP JSON. Adding typed maps later is additive.

Do not depend on `credential_manager` / other Credential Manager plugins.

## Concurrency

Calls are async. Do not assume a UI thread on the Dart side. Native must
complete `MethodChannel.Result` on a thread Flutter accepts (typically main)
after work on a background dispatcher.
