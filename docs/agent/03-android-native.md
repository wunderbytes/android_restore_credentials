# Planned Android native layer

## Dependencies (`android/build.gradle` library)

Prefer latest **stable** `androidx.credentials` ≥ 1.5.0. Google’s skill samples
alpha; this plugin should not pin alpha unless stable lacks Restore Credentials
(it does not — 1.5.0+).

```groovy
implementation("androidx.credentials:credentials:<stable>")
implementation("androidx.credentials:credentials-play-services-auth:<stable>")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:<stable>")
```

`credentials-play-services-auth` is required on API ≤ 33.

Add `androidx.credentials` restore exception imports from
`androidx.credentials.exceptions` and
`androidx.credentials.exceptions.restorecredential`.

## Kotlin client (new file)

`RestoreCredentialsClient(context: Context)` wrapping `CredentialManager.create(context)`.

### createRestoreKey

```kotlin
val request = CreateRestoreCredentialRequest(
    requestJson = requestJson,
    isCloudBackupEnabled = preferCloudBackup,
)
credentialManager.createCredential(context, request)
```

If `preferCloudBackup` is true and `E2eeUnavailableException` is thrown, retry
with `isCloudBackupEnabled = false`. Return
`CreateRestoreCredentialResponse` JSON (`responseJson` / registration JSON —
use the field the library exposes on `CreateRestoreCredentialResponse`).

Use **suspend** `createCredential`, not the Activity-UI callback APIs.

### getRestoreKey

```kotlin
val option = GetRestoreCredentialOption(requestJson)
val getRequest = GetCredentialRequest(listOf(option)) // ONLY this option
val response = credentialManager.getCredential(context, getRequest)
val credential = response.credential as RestoreCredential
// return credential.authenticationResponseJson (confirm exact property name at impl time)
```

Never mix other `CredentialOption` subclasses.

### clearRestoreKey

```kotlin
val clearRequest = ClearCredentialStateRequest(
    ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL,
)
credentialManager.clearCredentialState(clearRequest)
```

Do not call typeless `ClearCredentialStateRequest()` for this method.

## Plugin class

`AndroidRestoreCredentialsPlugin` stays `FlutterPlugin` + `MethodCallHandler`.
On attach, construct `RestoreCredentialsClient(applicationContext)`.

Dispatch:

- Parse arguments; missing/blank `requestJson` → error `invalid_argument`
- `api < 28` → `unsupported_android_version`
- Launch a coroutine (`CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`
  plus `withContext(Dispatchers.IO)` for Credential Manager)
- Map exceptions per [04-error-handling.md](04-error-handling.md)
- Cancel the scope in `onDetachedFromEngine`

## Manifest

Plugin `AndroidManifest.xml` stays empty of backup/agent. No new permissions
are documented as required for Restore Credentials themselves.

## Tests

Keep/extend `AndroidRestoreCredentialsPluginTest` with Mockito: method routing
only, or extract client and unit-test E2EE retry with a fake `CredentialManager`
if feasible. Prefer testing retry logic in a small wrapper that is injectable.
