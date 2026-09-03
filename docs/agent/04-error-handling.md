# Error handling

Map Android exceptions to `PlatformException(code, message, details)`.
Use **stable string codes** so Dart and hosts can switch on `code`.

| Code | Typical cause |
| --- | --- |
| `invalid_argument` | Missing/empty JSON; `IllegalArgumentException` (invalid JSON / `user.id`) |
| `unsupported_android_version` | SDK &lt; 28 |
| `create_dom` | `CreateRestoreCredentialDomException` (JSON not WebAuthn creation options) |
| `e2ee_unavailable` | `E2eeUnavailableException` **after** retry disabled, or if caller passed `preferCloudBackup: false` and cloud was not attempted—normally create retries and should not surface this if local create succeeds |
| `no_credential` | `NoCredentialException` on get (nothing to restore) |
| `get_interrupted` / `get_canceled` | User/system cancellation if it can occur on silent get |
| `get_failed` | Other `GetCredentialException` |
| `create_failed` | Other `CreateCredentialException` |
| `clear_failed` | `ClearCredentialException` |
| `unexpected_type` | Get succeeded but credential is not `RestoreCredential` |
| `unknown` | Anything else |

On create, **do not** fail the first `E2eeUnavailableException` when
`preferCloudBackup == true`; retry locally. Only map `e2ee_unavailable` if the
retry also fails for that reason (should be rare).

Include `e.javaClass.simpleName` and `e.message` in `message` or `details` for
support logs. Do not log `requestJson` (may contain user ids / challenges).

Dart: optional thin wrappers later (`RestoreCredentialsException`); v0.1 can
rethrow `PlatformException`.
