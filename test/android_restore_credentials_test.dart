import 'package:flutter_test/flutter_test.dart';
import 'package:android_restore_credentials/android_restore_credentials.dart';
import 'package:android_restore_credentials/android_restore_credentials_platform_interface.dart';
import 'package:android_restore_credentials/android_restore_credentials_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockAndroidRestoreCredentialsPlatform
    with MockPlatformInterfaceMixin
    implements AndroidRestoreCredentialsPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final AndroidRestoreCredentialsPlatform initialPlatform = AndroidRestoreCredentialsPlatform.instance;

  test('$MethodChannelAndroidRestoreCredentials is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelAndroidRestoreCredentials>());
  });

  test('getPlatformVersion', () async {
    AndroidRestoreCredentials androidRestoreCredentialsPlugin = AndroidRestoreCredentials();
    MockAndroidRestoreCredentialsPlatform fakePlatform = MockAndroidRestoreCredentialsPlatform();
    AndroidRestoreCredentialsPlatform.instance = fakePlatform;

    expect(await androidRestoreCredentialsPlugin.getPlatformVersion(), '42');
  });
}
