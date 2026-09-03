# Backend integration (for agents implementing the relying-party server)

Audience: an agent (or developer) implementing the **relying-party (RP)
server** that supports Restore Credentials. The Flutter plugin is client-side
only; this doc describes what the server must provide and ensure. It expands
the short reminders in [07-backend-reminders.md](07-backend-reminders.md).

Restore keys use the same WebAuthn crypto family as passkeys, so you can reuse
a passkey RP — but you **must** persist restore keys as a **separate class**.

## Endpoints to provide

Your RP needs at least these (names are illustrative):

### 1. Create options — `POST /restore-credential/options`

Request (from app): `{ "device_id": "…", …session context… }`

Response: `{ "requestJson": <PublicKeyCredentialCreationOptionsJSON> }`

The `requestJson` you return must be a valid WebAuthn
[PublicKeyCredentialCreationOptionsJSON](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson).
The plugin passes it straight to `androidx.credentials`. Required fields:

- `challenge` — server-generated random, base64url. **Store it server-side,
  tied to the session**, so the later `/finish` can verify it (anti-replay).
- `rp.id` — your RP id (e.g. `example.com`); the authenticator enforces it.
- `user.id` — stable, opaque per-user id (base64url). Must be present or
  `androidx.credentials` throws `IllegalArgumentException`.
- `pubKeyCredParams` — accepted COSE algorithms (e.g. `[{ "type": "public-key", "alg": -7 }]`).

For restore keys, `authenticatorSelection.userVerification` is typically
`discouraged` (background restore may skip explicit UV). Do not require UV.

### 2. Create finish — `POST /restore-credential/finish`

Request: `{ "device_id": "…", "attestation": <CreateRestoreCredentialResponse JSON> }`

Server: verify the attestation against the stored challenge, then store the
public key as a **restore credential** for `(user_id, device_id)`. Enforce
uniqueness on `(user_id, device_id)` — see "Replace on re-registration" below.

### 3. Assertion options — `POST /restore-credential/assertion-options`

Request: `{ "device_id": "…", …session context… }`

Response: `{ "requestJson": <PublicKeyCredentialRequestOptionsJSON> }`

`requestJson` is
[PublicKeyCredentialRequestOptionsJSON](https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson):
`challenge`, `rpId`, `allowCredentials` (optional). Store the challenge
server-side, tied to the session/request.

### 4. Assertion verify — `POST /restore-credential/verify`

Request: `{ "device_id": "…", "assertion": <RestoreCredential.authenticationResponseJson> }`

Server: verify the assertion against the stored challenge and the user's
stored restore public key(s). On success, establish a session and return it.

## Stale challenge handling

Challenges are single-use and short-lived. Enforce:

- **One challenge per ceremony.** A challenge issued for create must not be
  accepted on verify, and vice versa.
- **Single use.** Mark a challenge consumed after the first successful (or
  failed) verification; reject replays.
- **TTL.** Expire unused challenges after a short window (minutes). A
  challenge reused or past TTL must be rejected as `invalid_argument` /
  verification failure — do not silently accept.
- **Session-bound.** Tie each challenge to the session/request that issued
  it so a challenge from one session can't be redeemed in another.
- **Concurrent options.** If the app requests options twice, invalidate the
  prior challenge or allow only the most recent. Avoid letting multiple
  outstanding challenges accumulate per device.

## Storage model: per-device, not per-user

Store restore keys keyed by **`(user_id, device_id)`**, not `user_id` alone.
A user can own several devices, each with its own active restore key.

```
user
 ├─ device A → restore_key_A  (active)
 ├─ device B → restore_key_B  (active)
 └─ device C → restore_key_C  (last_used_at old; TTL expiring)
```

### Replace on re-registration (primary cleanup)

When a device registers a **new** restore key for the same `device_id`
(i.e. the user signed in again and the app called create again), **replace**
that device's prior key. This caps growth at one active key per device and is
the main automatic cleanup mechanism.

### Multiple devices

Never assume 1:1 user↔key. A successful assertion identifies *which device's*
key was used (by credential id / `device_id`); update that row's
`last_used_at`. Do not delete another device's key because a different device
restored — see below.

### When to delete a restore key (server-side)

Do **not** delete on the app's `clearRestoreKey()` — that's a local,
per-device action and the server is not reliably notified (uninstall /
clear-data give no signal at all). Delete based on:

1. **Replace-on-re-registration** for the same `device_id` (above).
2. **TTL / usage**: delete when a key is unused past its TTL **and** the user
   is demonstrably active elsewhere. TTL must outlive the "sign out on old
   device → restore on new device" transition window, or you break in-flight
   restores.
3. **Explicit "revoke all sessions"** (a deliberate security action) → sweep
   that user's device keys.
4. **Account deletion** → delete all of the user's keys.

Do **not** delete on routine sign-out, and do **not** delete the old device's
key just because a new device restored — that breaks users who keep both
devices.

## Differentiate restore keys from passkeys

- Persist restore keys with a distinct **type/metadata** (e.g.
  `credential_type = "restore"`), separate from user-created passkeys.
- Do **not** show restore keys on passkey management UIs — they are
  system-managed and hidden from the user.
- Background restore may **skip explicit user verification**; your
  verification logic must allow restore-key assertions with `userVerification:
  discouraged` / absent UV, unlike typical passkey flows that assume UV.

## What to ensure (summary checklist)

- [ ] Four endpoints (create options/finish, assertion options/verify)
      returning valid WebAuthn JSON.
- [ ] `challenge` server-generated, stored, single-use, session-bound, TTL'd.
- [ ] `user.id` and `rp.id` present in create `requestJson`.
- [ ] Restore keys stored per `(user_id, device_id)`, distinct from passkeys.
- [ ] Replace a device's key on re-registration (≤1 active key per device).
- [ ] No deletion on `clearRestoreKey()`; deletion via replace / TTL / explicit
      revoke / account deletion.
- [ ] TTL survives the old→new device transition window.
- [ ] Allow restore assertions that skip explicit UV.
- [ ] No restore keys shown on passkey management UI.

## References

- [About Restore Credentials](https://developer.android.com/identity/sign-in/restore-credentials)
- [Implementation guide](https://developer.android.com/identity/sign-in/restore-credentials-implementation)
- [W3C PublicKeyCredentialCreationOptionsJSON](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson)
- [W3C PublicKeyCredentialRequestOptionsJSON](https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson)
- [Play Help — Zero-Tap Sign-In Restoration](https://support.google.com/googleplay/android-developer/answer/17492799#zero-tap_sign-in_restoration)
