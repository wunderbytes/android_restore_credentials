## 0.1.0

* Android-only Restore Credentials: `createRestoreKey`, `getRestoreKey`,
  and `clearRestoreKey` via `androidx.credentials` 1.6.0.
* Removed template `getPlatformVersion` API end-to-end.
* Kotlin `RestoreCredentialsClient` (public) so host `BackupAgent` code can call
  the same client without a Flutter engine. E2EE cloud-backup retry on
  `E2eeUnavailableException`. API 28+ guard.
* Example app demonstrates the Tier-2 foreground restoration path. Tier-1
  background restore is documented via a README snippet (a functional
  `BackupAgent` needs host-specific Kotlin backend calls, out of scope for the
  plugin example).
* Stable `PlatformException` codes; see README.
