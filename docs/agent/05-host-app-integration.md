# Host app integration (not plugin internals)

This plugin cannot complete Play compliance by itself. Hosts must:

1. Provide a **relying party** that issues and verifies WebAuthn JSON for restore keys.
2. Call **create** whenever a user is signed in and no key is synced.
3. Call **get** on new-device restore (two tiers below).
4. Send create/get JSON to the RP and establish an app session.
5. Call **clear** on sign-out.
6. Optionally restore push (e.g. send FCM token after silent sign-in). Notifications
   are **not** restored automatically.

## Suggested host flow

```
signed in? ──no──► normal login ──► createRestoreKey(creationJson)
    │
   yes
    │
    ├── first launch / unknown session ──► getRestoreKey(assertionJson)
    │         success ──► RP verify ──► session
    │         no_credential ──► normal login
    │
    └── already have session, no local flag ──► createRestoreKey
```

## BackupAgent (tier 1)

Only if host `allowBackup` is true. Host app module (not this plugin):

- Subclass `BackupAgent` (or a helper class shipped later in the AAR).
- In `onRestoreFinished`, obtain assertion JSON (needs a **native** HTTP path
  or a previously backed-up challenge strategy—Flutter `http` is unavailable
  unless the engine is started; prefer a small OkHttp/Ktor call in the agent
  or a pre-agreed RP endpoint).
- `runBlocking { client.getRestoreKey(...) }` then complete RP sign-in natively
  or persist the assertion for Flutter to consume on first frame (document the
  chosen pattern in the example when implementing).

**Implementation choice to decide at coding time (example app):**

- **A (preferred for demo):** persist assertion JSON to app files in
  `onRestoreFinished`; Flutter reads it on startup and talks to a mock RP.
- **B:** full native RP verify in the agent (too much for the plugin example).

Do not start a full Flutter engine in `BackupAgent` unless a future requirement
forces it.

## Flutter first launch (tier 2)

Call `getRestoreKey` early (e.g. before showing login). Treat `no_credential`
as “not a restore”.

## Example app (when implementing)

Keep the example **Android-only**. Show the three Dart methods. Use **valid**
WebAuthn-shaped JSON fixtures (not empty `{}`). Label them as fixtures, not a
production RP.
