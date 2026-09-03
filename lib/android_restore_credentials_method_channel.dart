import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'android_restore_credentials_platform_interface.dart';

/// An implementation of [AndroidRestoreCredentialsPlatform] that uses method
/// channels.
class MethodChannelAndroidRestoreCredentials
    extends AndroidRestoreCredentialsPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('android_restore_credentials');

  @override
  Future<String> createRestoreKey({
    required String requestJson,
    bool preferCloudBackup = true,
  }) async {
    final result = await methodChannel.invokeMethod<String>(
      'createRestoreKey',
      <String, Object>{
        'requestJson': requestJson,
        'preferCloudBackup': preferCloudBackup,
      },
    );
    if (result == null) {
      throw _missingResult('createRestoreKey');
    }
    return result;
  }

  @override
  Future<String> getRestoreKey({required String requestJson}) async {
    final result = await methodChannel.invokeMethod<String>(
      'getRestoreKey',
      <String, Object>{'requestJson': requestJson},
    );
    if (result == null) {
      throw _missingResult('getRestoreKey');
    }
    return result;
  }

  @override
  Future<void> clearRestoreKey() async {
    await methodChannel.invokeMethod<void>('clearRestoreKey');
  }

  PlatformException _missingResult(String method) => PlatformException(
        code: 'unknown',
        message: '$method returned null',
      );
}
