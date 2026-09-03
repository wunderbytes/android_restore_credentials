# Testing plan

## Dart (`fvm flutter test`)

- Fake `AndroidRestoreCredentialsPlatform`: create returns a JSON string,
  get returns a JSON string, clear completes.
- Method channel: mock binary messenger; assert method names and args
  (`requestJson`, `preferCloudBackup`).
- Default platform interface throws `UnimplementedError` until overridden.

## Kotlin

- Missing `requestJson` → `invalid_argument` (plugin handler).
- Unknown method → `notImplemented`.
- E2EE retry: fake manager throws `E2eeUnavailableException` once, succeeds
  on `isCloudBackupEnabled = false`.
- Get with a non-restore credential type → `unexpected_type` (if injectable).

Emulator/device (manual, later):

- API 28+ with Play services ≥ 24220000 (version 24.22 or newer)
- Create with screen lock + backup on (cloud path)
- Create with backup/E2EE off (retry path)
- Get with no key → `no_credential`
- Clear then get → `no_credential`

BackupAgent path needs a device-to-device or cloud restore lab; do not block
the first implementation PR on that.

## Analyze

`fvm flutter analyze` from repo root must stay clean.
