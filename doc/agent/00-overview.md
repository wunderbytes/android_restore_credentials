# Overview

## Goal

Let Flutter apps create, retrieve, and clear Android **restore keys** so that after
device-to-device transfer or cloud backup, the user can be signed in with **no tap**
during setup / first launch.

Restore keys are **independent** of the app’s primary auth (password, passkey,
Sign in with Google). Hosts do not need to add passkeys to use restore keys.
Server-side crypto is the same family as passkeys (WebAuthn), but restore keys
must be stored and displayed as a **separate** credential type on the RP.

## Compatibility (when implemented)

| Requirement | Value |
| --- | --- |
| OS | Android 9 (API 28)+ |
| GMS core | 24220000+ (version 24.22 or newer) |
| Library | `androidx.credentials` 1.5.0+ (prefer latest **stable**) |
| Play form factors | Mobile / tablet only |
| Play enforcement | April 2027 (see Play Help; exceptions exist) |

This plugin’s `minSdk` is currently **24**. Calls on API 24–27 must fail with a
clear unsupported error, not crash.

## Plugin vs host vs backend

```
Host Flutter app                         This plugin                      Host RP server
────────────────                         ───────────                      ─────────────
After sign-in: fetch creation JSON  →    createRestoreKey(requestJson)
                                      ←  attestation JSON            →    register restore key

BackupAgent / first launch: fetch
assertion JSON                      →    getRestoreKey(requestJson)
                                      ←  assertion JSON              →    verify & establish session

On sign-out                         →    clearRestoreKey()           (local only; server is not notified)
```

The plugin **does not**:

- Build WebAuthn `requestJson` / `authenticationJson`
- Call the host backend
- Start the Flutter engine inside `BackupAgent`
- Change `allowBackup` or replace the host `BackupAgent` via manifest merge
- Implement passkeys, passwords, or federated Credential Manager options

## Current repo state

Federated-style Flutter plugin (platform interface + method channel + Kotlin).
Channel name: `android_restore_credentials`. Android package:
`eu.wunderbytes.android_restore_credentials`.
