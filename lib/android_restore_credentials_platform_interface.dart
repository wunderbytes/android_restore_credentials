import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'android_restore_credentials_method_channel.dart';

/// Platform interface for the Android-only Restore Credentials plugin.
///
/// Only Android is supported. Hosts of multi-platform apps should guard calls
/// with `defaultTargetPlatform == TargetPlatform.android`.
abstract class AndroidRestoreCredentialsPlatform extends PlatformInterface {
  /// Constructs a AndroidRestoreCredentialsPlatform.
  AndroidRestoreCredentialsPlatform() : super(token: _token);

  static final Object _token = Object();
  static AndroidRestoreCredentialsPlatform _instance =
      MethodChannelAndroidRestoreCredentials();

  /// The default instance of [AndroidRestoreCredentialsPlatform] to use.
  ///
  /// Defaults to [MethodChannelAndroidRestoreCredentials].
  static AndroidRestoreCredentialsPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [AndroidRestoreCredentialsPlatform]
  /// when they register themselves.
  static set instance(AndroidRestoreCredentialsPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

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
    throw UnimplementedError('createRestoreKey() has not been implemented.');
  }

  /// Retrieves a restore key.
  ///
  /// [requestJson] is WebAuthn `PublicKeyCredentialRequestOptionsJSON` from
  /// the RP. Returns [RestoreCredential] authentication JSON for the RP.
  ///
  /// Throws [PlatformException] with stable `code` values; see README.
  Future<String> getRestoreKey({required String requestJson}) {
    throw UnimplementedError('getRestoreKey() has not been implemented.');
  }

  /// Deletes the restore key on this device
  /// (`TYPE_CLEAR_RESTORE_CREDENTIAL`).
  ///
  /// Throws [PlatformException] with stable `code` values; see README.
  Future<void> clearRestoreKey() {
    throw UnimplementedError('clearRestoreKey() has not been implemented.');
  }
}
