# android_restore_credentials_example

Android example app for the `android_restore_credentials` plugin.

The example demonstrates the **Tier 2 (foreground)** restoration path: on
first launch, `lib/main.dart` calls `getRestoreKey` with fixture WebAuthn JSON.
Three buttons let you also `createRestoreKey` (after a simulated sign-in) and
`clearRestoreKey` (on sign-out). It does **not** talk to a real relying-party
server — replace the fixture JSON with options fetched from your RP.

> **Tier 1 (background) is intentionally not included here.** A functional
> `BackupAgent` requires host-specific Kotlin backend calls (fetch options,
> POST the assertion, persist the session) that depend on your RP API and are
> out of scope for a plugin example. The plugin *does* expose the public
> Kotlin `RestoreCredentialsClient` so hosts can write their own
> `BackupAgent.onRestoreFinished` — see the "Host integration" section of the
> main [README](../README.md) for a Kotlin snippet.

Use FVM from the repository root (or this directory; FVM walks up to `.fvmrc`):

```sh
fvm flutter run
```

> Restore Credentials require Android 9 (API 28)+ and Google Play services
> 24220000+. On API 24–27 the plugin returns `unsupported_android_version`.

See the main repo [README](../README.md) for the host integration (two-tier)
guidance and `PlatformException` codes.
