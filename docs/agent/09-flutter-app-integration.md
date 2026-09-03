# Flutter app integration (for agents wiring this plugin into a host app)

Audience: an agent (or developer) integrating `android_restore_credentials`
into a **Flutter host app**. This is the client side; the plugin does **not**
talk to your backend — your Dart code coordinates with your relying-party (RP)
server around the three plugin calls.

This doc complements [02-dart-api.md](02-dart-api.md) (API shape),
[01-architecture.md](01-architecture.md) (two-tier), and
[04-error-handling.md](04-error-handling.md) (codes). For the server side see
[10-backend-integration.md](10-backend-integration.md).

## What the plugin does and does not do

The plugin owns **only** the Credential Manager ceremony:

- `createRestoreKey(requestJson)` → attestation JSON
- `getRestoreKey(requestJson)` → assertion JSON
- `clearRestoreKey()` → deletes the **local** key

It passes JSON strings through. It does **not**:

- Build WebAuthn `requestJson` (your RP must)
- Call your backend
- Persist a session
- Know about `device_id`s

So every plugin call is sandwiched between your own backend calls.

## Parameters your app must resolve

Before any plugin call, resolve these (from config, build env, or your RP):

| Parameter | Source | Used for |
| --- | --- | --- |
| `backendBaseUrl` | app config / env | All RP HTTP calls |
| `rp.id` | RP (returned inside `requestJson`) | Scopes the credential; the RP decides it |
| `user.id` | RP (returned inside `requestJson` as `user.id`) | Binds the credential to a user; the RP issues it |
| `device_id` | app-generated install id or RP-issued device id | Sent to RP so it can store the key per-device |

You do **not** pass `rp.id` / `user.id` to the plugin directly — they live
inside the `requestJson` your RP returns. Your job is to fetch that JSON at
the right time and hand it to the plugin.

### `device_id`

A restore key is bound to a **device** (package + authenticator), and a user
may own several devices. Generate or obtain a stable `device_id` per install
and send it to your RP alongside create/verify so the RP can store keys as
`(user_id, device_id)`. See [10-backend-integration.md](10-backend-integration.md)
for why per-device storage matters. Do **not** assume 1:1 user↔key.

## When to call what (full flows)

### Create — after the user is signed in

```
1. User signs in (password / Google / passkey / …) → you have a session
2. POST {backendBaseUrl}/restore-credential/options
        body: { device_id, … }
   ← 200 { requestJson: PublicKeyCredentialCreationOptionsJSON }
3. createRestoreKey(requestJson: <that JSON>, preferCloudBackup: true)
   ← attestation JSON
4. POST {backendBaseUrl}/restore-credential/finish
        body: { device_id, attestation }
   ← 200 (RP stores the public key as a RESTORE credential for this device)
```

Call create:
- after sign-up / sign-in, and
- on a later launch if the user is signed in but has no restore key yet.

Keep a local flag (e.g. `has_synced_restore_credential`) so you do **not**
create on every launch. The plugin is stateless; the host tracks this.

`preferCloudBackup: true` (default) is recommended — the plugin retries
locally on `E2eeUnavailableException` automatically, so you usually do not
need to handle `e2ee_unavailable` specially.

### Get — restoration on a new device (two tiers)

```
1. POST {backendBaseUrl}/restore-credential/assertion-options
        body: { device_id, … }
   ← 200 { requestJson: PublicKeyCredentialRequestOptionsJSON }
2. getRestoreKey(requestJson: <that JSON>)
   ← assertion JSON  (silent; no UI)
3. POST {backendBaseUrl}/restore-credential/verify
        body: { device_id, assertion }
   ← 200 { session }   (RP verifies and signs the user in)
```

Drive this from **two tiers** (see [01-architecture.md](01-architecture.md)):

- **Tier 1 — background (`BackupAgent.onRestoreFinished`)** if your Android
  manifest has `android:allowBackup="true"`. This runs **without a Flutter
  engine**, so it must be pure Kotlin calling the plugin's public
  `RestoreCredentialsClient` + your RP HTTP API directly (see the README
  snippet). Do it synchronously (`runBlocking`) so sign-in finishes before
  first UI. Do **not** flip `allowBackup`.
- **Tier 2 — foreground (first launch)** in Dart: call `getRestoreKey` in
  `initState` / first frame to cover the cases where background restore did
  not run, backup is off, or restore is independent of app-data backup.

If `allowBackup` is false, implement **Tier 2 only**.

### Clear — on sign-out

```
1. clearRestoreKey()   (local only; deletes TYPE_CLEAR_RESTORE_CREDENTIAL)
2. POST {backendBaseUrl}/signout   (optional; revokes the session)
```

`clearRestoreKey` is **local to this device** and does not notify the server.
The server must **not** delete the stored restore key on this signal — the
user may be restoring to another device that still needs it. Server-side
deletion is lifecycle/TTL-based; see [10-backend-integration.md](10-backend-integration.md).
A typeless `clearCredentialState` would not remove restore keys — the plugin
uses `TYPE_CLEAR_RESTORE_CREDENTIAL`, so you do not need to worry about that.

## Platform guarding

Android only. Guard in Dart:

```dart
import 'package:flutter/foundation.dart';

if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android) {
  // call the plugin
} else {
  // skip; or fall back to your normal sign-in
}
```

On API 24–27 the plugin returns `unsupported_android_version` rather than
crashing — handle it by falling back to your normal sign-in flow.

## Error handling expectations

Wrap calls in `try / on PlatformException`. Common, expected outcomes you
should **not** treat as fatal:

| Code | Meaning | App response |
| --- | --- | --- |
| `no_credential` | Nothing to restore (first launch, no key) | Show normal sign-in UI |
| `unsupported_android_version` | API < 28 | Fall back to normal sign-in |
| `e2ee_unavailable` | Cloud off and local retry also failed (rare) | Inform / fall back; key may still be creatable later |

Treat as real errors: `invalid_argument`, `create_dom`, `get_failed`,
`create_failed`, `clear_failed`, `unexpected_type`, `unknown`. See
[04-error-handling.md](04-error-handling.md).

Do **not** log `requestJson` (it may contain `user.id` / challenges).

## Minimal Dart wiring sketch

```dart
final plugin = AndroidRestoreCredentials();

Future<void> createRestoreKeyAfterSignIn({required String deviceId}) async {
  final options = await api.post('/restore-credential/options', body: {'device_id': deviceId});
  final attestation = await plugin.createRestoreKey(requestJson: options['requestJson']);
  await api.post('/restore-credential/finish', body: {'device_id': deviceId, 'attestation': attestation});
}

Future<void> restoreOnFirstLaunch({required String deviceId}) async {
  try {
    final options = await api.post('/restore-credential/assertion-options', body: {'device_id': deviceId});
    final assertion = await plugin.getRestoreKey(requestJson: options['requestJson']);
    final session = await api.post('/restore-credential/verify', body: {'device_id': deviceId, 'assertion': assertion});
    // establish session from `session`
  } on PlatformException catch (e) {
    if (e.code == 'no_credential' || e.code == 'unsupported_android_version') {
      // show normal sign-in UI
    } else {
      rethrow; // or surface
    }
  }
}

Future<void> signOut() async {
  await plugin.clearRestoreKey();
  await api.post('/signout');
}
```

## Checklist for an integrating agent

- [ ] `defaultTargetPlatform == TargetPlatform.android` guard around all calls.
- [ ] `createRestoreKey` called after sign-in, gated by a local
      `has_synced_restore_credential` flag (not every launch).
- [ ] `getRestoreKey` on first launch (Tier 2), with `no_credential` /
      `unsupported_android_version` handled as "show normal sign-in".
- [ ] `clearRestoreKey` on sign-out.
- [ ] A stable `device_id` sent to the RP on create/verify.
- [ ] If `allowBackup="true"`: a Kotlin `BackupAgent` calling
      `RestoreCredentialsClient` + the RP directly (Tier 1), registered only
      in the **app** manifest. `allowBackup` left unchanged.
- [ ] No WebAuthn JSON built in Dart; all `requestJson` comes from the RP.
- [ ] No reliance on the server deleting keys on `clearRestoreKey`.
