# Backend reminders (do not implement in this repo)

Copy these to the human after client-side implementation. Do **not** write
RP/server code as part of this plugin unless explicitly asked.

Adapted from Google’s Restore Credentials client skill (2026-08-21).

1. **Differentiate restore credentials from passkeys in storage.**  
   Standard WebAuthn often assumes user verification. Restore keys are
   system-managed and hidden. Use a distinct credential type or metadata.
   Do not show them on passkey management screens. Background restore may
   skip explicit UV.

2. **Prevent orphaned keys.**  
   Uninstall / clear-data deletes the **local** key and does not call the
   server. Plan cleanup: e.g. replace old keys on new registration, TTL
   unused keys, at most one key per user per device.

3. **Balance lifespan and TTL.**  
   If the user restores, then signs out on the **old** device, local clear
   must not immediately invalidate the key the **new** device still needs.
   TTL should survive that transition; delete based on registration/usage
   rules, not only on client clear.

4. **Support multiple devices.**  
   Schema: multiple active restore credentials per user (e.g. per
   device id), not 1:1 user↔key.

Same RP stack as passkeys can verify restore keys, but **persist them as a
separate class**.
