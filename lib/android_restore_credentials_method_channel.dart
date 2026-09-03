import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'android_restore_credentials_platform_interface.dart';

/// An implementation of [AndroidRestoreCredentialsPlatform] that uses method channels.
class MethodChannelAndroidRestoreCredentials extends AndroidRestoreCredentialsPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('android_restore_credentials');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
