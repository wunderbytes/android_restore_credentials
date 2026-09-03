import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'android_restore_credentials_method_channel.dart';

abstract class AndroidRestoreCredentialsPlatform extends PlatformInterface {
  /// Constructs a AndroidRestoreCredentialsPlatform.
  AndroidRestoreCredentialsPlatform() : super(token: _token);

  static final Object _token = Object();

  static AndroidRestoreCredentialsPlatform _instance = MethodChannelAndroidRestoreCredentials();

  /// The default instance of [AndroidRestoreCredentialsPlatform] to use.
  ///
  /// Defaults to [MethodChannelAndroidRestoreCredentials].
  static AndroidRestoreCredentialsPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [AndroidRestoreCredentialsPlatform] when
  /// they register themselves.
  static set instance(AndroidRestoreCredentialsPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
