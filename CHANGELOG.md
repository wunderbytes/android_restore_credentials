## 0.1.0

* Android-only Restore Credentials: `createRestoreKey`, `getRestoreKey`,
  and `clearRestoreKey` via `androidx.credentials` 1.6.0.
* Removed template `getPlatformVersion` API end-to-end.
* Kotlin `RestoreCredentialsClient` (public) so host `BackupAgent` code can call
  the same client without a Flutter engine. E2EE cloud-backup retry on
  `E2eeUnavailableException`. API 28+ guard.
* Stable `PlatformException` codes; see README.
