# Implementation plan

Do this work only when a human asks to implement. Suggested PRs can be one
or several; order is fixed.

## Phase 0 — confirm library APIs

At implement time, open the matching `androidx.credentials` sources/docs for
the chosen stable version and confirm property names:

- `CreateRestoreCredentialResponse` JSON getter
- `RestoreCredential` JSON getter
- `ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL` constant

Pin that version in `android/build.gradle`.

## Phase 1 — Kotlin client

1. Add Gradle dependencies (credentials, play-services-auth, coroutines).
2. Add `RestoreCredentialsClient` with create (E2EE retry), get, clear.
3. Guard API 28+.
4. Unit-test retry / argument checks if injectable.

## Phase 2 — Method channel + Dart

1. Platform interface methods + `UnimplementedError` defaults.
2. Method channel invoke + argument maps.
3. Facade class `AndroidRestoreCredentials`.
4. Delete `getPlatformVersion` end-to-end (Dart, Kotlin, tests, example).
5. Document `PlatformException` codes in README (human docs, not only agent docs).

## Phase 3 — Tests

1. Update `test/android_restore_credentials_test.dart` fakes.
2. Update method channel tests with mocked `MethodChannel`.
3. Kotlin plugin tests for unknown method / missing args.
4. `fvm flutter analyze` + `fvm flutter test`.

## Phase 4 — Example (minimal)

Wire three buttons or a startup path using fixture JSON. Do not add a real
IdP. README snippet for hosts: create after login, get on first launch, clear
on logout.

Optional follow-up (separate, easy to skip): Kotlin `BackupAgent` helper +
example manifest **only in `example/android`**.

## Phase 5 — Human wrap-up

Paste `07-backend-reminders.md` to the developer. Point at Play Help and
Android implementation guide.

## Out of scope (until explicitly requested)

- Federated plugin (`*_android` / `_platform_interface` packages)
- Passkeys / passwords / Google ID options
- Generating WebAuthn JSON in Dart
- Changing `minSdk` to 28 (keep 24 + runtime check unless product says otherwise)
- iOS Keychain “equivalent”
- Publishing to pub.dev

## File touch list (expected)

- `android/build.gradle`
- `android/src/main/kotlin/.../RestoreCredentialsClient.kt` (new)
- `android/src/main/kotlin/.../AndroidRestoreCredentialsPlugin.kt`
- `android/src/test/kotlin/...` tests
- `lib/android_restore_credentials.dart`
- `lib/android_restore_credentials_platform_interface.dart`
- `lib/android_restore_credentials_method_channel.dart`
- `test/*.dart`
- `example/lib/main.dart` (+ maybe example Android BackupAgent later)
- `README.md`, `CHANGELOG.md`
