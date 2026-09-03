# android_restore_credentials_example

Android example app for the `android_restore_credentials` plugin.

The example demonstrates the **Tier 2 (foreground)** restoration path: on
first launch it calls `getRestoreKey` with fixture WebAuthn JSON. Three buttons
let you also `createRestoreKey` (after a simulated sign-in) and `clearRestoreKey`
(on sign-out). It does **not** talk to a real relying-party server — replace the
fixture JSON with options fetched from your RP.

Use FVM from the repository root (or this directory; FVM walks up to `.fvmrc`):

```sh
fvm flutter run
```

> Restore Credentials require Android 9 (API 28)+ and Google Play services
> 24220000+. On API 24–27 the plugin returns `unsupported_android_version`.

See the main repo [README](../README.md) for the host integration (two-tier)
guidance and `PlatformException` codes.
