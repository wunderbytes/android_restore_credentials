# Architecture

## Two-tier restoration (host app)

Google’s client skill requires a **two-tier** get flow in the **host application**:

1. **Tier 1 — background:** `BackupAgent.onRestoreFinished()` after app data restore.
   Use this only if the host manifest has `android:allowBackup="true"`.
   Run get **synchronously** there (e.g. `runBlocking`) so sign-in can finish
   before first UI. Do **not** use `onRestore` (key-value only).
2. **Tier 2 — foreground:** first launch (typically launcher `Activity.onCreate` /
   Flutter first frame) if background restore did not complete, backup is off,
   or restore is independent of app-data backup.

If `allowBackup` is **false**, implement **tier 2 only**. Never flip `allowBackup`.

Restore Credentials still work when `allowBackup` is false: the restore **key**
is transferred by the restore-credential service, not by the app’s own backup
payload.

## Why a public Kotlin client is required

`BackupAgent` runs **without a Flutter engine**. Dart `getRestoreKey` cannot be
the only entry point.

```
┌─────────────────────────────────────────────────────────────┐
│ RestoreCredentialsClient (Kotlin, public)                   │
│   createRestoreKey / getRestoreKey / clearRestoreKey        │
│   E2EE retry, exception mapping, CredentialManager          │
└───────────────┬──────────────────────────────┬──────────────┘
                │                              │
                ▼                              ▼
   AndroidRestoreCredentialsPlugin    Optional host BackupAgent
   (MethodChannel, applicationCtx)    (uses plugin AAR as library)
                │
                ▼
   Dart AndroidRestoreCredentials
```

- Method channel uses `FlutterPluginBinding.applicationContext`. Restore
  create/get is silent; **ActivityAware is not required** for these three APIs.
- Host `BackupAgent` calls the **same** Kotlin client with the agent as `Context`.

Do **not** declare `android:backupAgent` in this plugin’s library manifest.

Optional later: an abstract `AndroidRestoreCredentialsBackupAgent` in the AAR
that hosts subclass, still registered only from the **app** manifest. If adding
a new `BackupAgent` to an app that already had `allowBackup=true`, the host may
need `android:fullBackupOnly="true"` so backup behavior stays full-backup.
**The plugin must not set that on the host.**

## Create timing (host)

Create a restore key when the user is signed in:

- After sign-up / sign-in
- On a later launch if already signed in and no key yet

Hosts should keep a local flag (e.g. `has_synced_restore_credential`) so they
do not create on every launch. The plugin may stay stateless.

A restore key is bound to **application package name**. Multi-app orgs need
one key per package.

## Clear timing (host)

Credential Manager will **not** delete restore keys on sign-out. Hosts must
call `clearRestoreKey` on logout. Uninstall / clear-data is a system-level
delete. Server is not notified (see backend reminders).

## JSON ownership

`requestJson` for create: WebAuthn
[PublicKeyCredentialCreationOptionsJSON](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson).

`requestJson` for get: WebAuthn
[PublicKeyCredentialRequestOptionsJSON](https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson).

The plugin passes strings through. It does not parse beyond what the Android
library validates.
