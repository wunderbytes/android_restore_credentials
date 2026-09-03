# Agent instructions

This repository is an **Android-only Flutter plugin** for Google Play
[Zero-Tap Sign-In Restoration](https://support.google.com/googleplay/android-developer/answer/17492799#zero-tap_sign-in_restoration)
via Credential Manager **Restore Credentials**.

Use [FVM](https://fvm.app/) (`fvm flutter …`). Pinned SDK: **3.35.7** (`.fvmrc`).

## Before changing code

1. Read `doc/agent/README.md` and the implementation plan.
2. Do **not** implement Restore Credentials until a human asks for implementation.
3. This plugin wraps **client-side** `androidx.credentials` only. Do **not** implement relying-party / WebAuthn server code. After client work, remind the human of `doc/agent/07-backend-reminders.md` (copy those points; do not code them).

## Non-negotiables

- **Android only.** Do not add iOS/web/desktop plugin platforms.
- Do **not** change a host app’s `android:allowBackup` value.
- Do **not** merge a `BackupAgent` into this plugin’s `AndroidManifest.xml` (it would hijack host backup).
- `GetRestoreCredentialOption` must be the **only** option in a `GetCredentialRequest`.
- The get response credential type is **`RestoreCredential`**, not `PublicKeyCredential`.
- Sign-out must call `clearCredentialState` with **`TYPE_CLEAR_RESTORE_CREDENTIAL`**. A typeless clear does **not** remove restore keys.
- Create must try `isCloudBackupEnabled = true` first, then retry `false` on `E2eeUnavailableException`.
- Encapsulate create / get / clear in dedicated functions (Dart *and* a public Kotlin client for `BackupAgent`).

## Layout

| Path | Role |
| --- | --- |
| `lib/` | Dart API, platform interface, method channel |
| `android/` | Kotlin plugin + (planned) public Credential Manager client |
| `example/` | Android example app |
| `doc/agent/` | Spec and implementation plan for agents |

## Verify

```sh
fvm flutter pub get
fvm flutter analyze
fvm flutter test
```
