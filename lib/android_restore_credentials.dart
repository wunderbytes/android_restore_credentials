import 'android_restore_credentials_platform_interface.dart';

/// Public facade for the Android-only Restore Credentials plugin.
///
/// Wraps [AndroidRestoreCredentialsPlatform] so apps do not need to access the
/// platform interface directly. Only Android is supported; calls from other
/// platforms will surface a [PlatformException] (`unsupported_android_version`
/// or `unknown`) or [MissingPluginException].
class AndroidRestoreCredentials {
  /// Creates a restore key.
  ///
  /// [requestJson] is WebAuthn `PublicKeyCredentialCreationOptionsJSON` from
  /// the relying party. Returns `CreateRestoreCredentialResponse` JSON
  /// (attestation) for the RP to store.
  ///
  /// Tries cloud backup first; on E2EE/backup unavailable, retries locally
  /// unless [preferCloudBackup] is `false` (local only).
  ///
  /// Throws [PlatformException] with stable `code` values; see README.
  Future<String> createRestoreKey({
    required String requestJson,
    bool preferCloudBackup = true,
  }) {
    return AndroidRestoreCredentialsPlatform.instance.createRestoreKey(
      requestJson: requestJson,
      preferCloudBackup: preferCloudBackup,
    );
  }

  /// Retrieves a restore key.
  ///
  /// [requestJson] is WebAuthn `PublicKeyCredentialRequestOptionsJSON` from
  /// the RP. Returns [RestoreCredential] authentication JSON for the RP.
  ///
  /// Throws [PlatformException] with stable `code` values; see README.
  Future<String> getRestoreKey({required String requestJson}) {
    return AndroidRestoreCredentialsPlatform.instance.getRestoreKey(
      requestJson: requestJson,
    );
  }

  /// Deletes the restore key on this device
  /// (`TYPE_CLEAR_RESTORE_CREDENTIAL`).
  ///
  /// Throws [PlatformException] with stable `code` values; see README.
  Future<void> clearRestoreKey() {
    return AndroidRestoreCredentialsPlatform.instance.clearRestoreKey();
  }
}
