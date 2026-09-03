# Agent documentation index

Planning and implementation notes for Restore Credentials in this plugin.
**Status: planned, not implemented.** Dart still exposes only `getPlatformVersion`.

| Doc | Contents |
| --- | --- |
| [00-overview.md](00-overview.md) | Product goal, Play requirement, what this plugin is / is not |
| [01-architecture.md](01-architecture.md) | Two-tier restore, plugin vs host app, Kotlin vs Dart |
| [02-dart-api.md](02-dart-api.md) | Planned `createRestoreKey` / `getRestoreKey` / `clearRestoreKey` |
| [03-android-native.md](03-android-native.md) | `androidx.credentials` mapping, Gradle, contexts |
| [04-error-handling.md](04-error-handling.md) | Exceptions → `PlatformException` codes |
| [05-host-app-integration.md](05-host-app-integration.md) | When hosts call APIs, BackupAgent, `allowBackup` |
| [06-implementation-plan.md](06-implementation-plan.md) | Ordered work, files, acceptance checks |
| [07-backend-reminders.md](07-backend-reminders.md) | RP/server notes to show humans (never implement here) |
| [08-testing.md](08-testing.md) | Unit / example / device testing |

Canonical Android client skill (Google): [android/skills identity/restore-credentials](https://github.com/android/skills/blob/main/identity/restore-credentials/SKILL.md).

Canonical APIs:

- [About Restore Credentials](https://developer.android.com/identity/sign-in/restore-credentials)
- [Implementation guide](https://developer.android.com/identity/sign-in/restore-credentials-implementation)
- [CreateRestoreCredentialRequest](https://developer.android.com/reference/androidx/credentials/CreateRestoreCredentialRequest)
- [GetRestoreCredentialOption](https://developer.android.com/reference/androidx/credentials/GetRestoreCredentialOption)
