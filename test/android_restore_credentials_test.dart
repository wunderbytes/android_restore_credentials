import 'package:flutter_test/flutter_test.dart';
import 'package:android_restore_credentials/android_restore_credentials.dart';
import 'package:android_restore_credentials/android_restore_credentials_platform_interface.dart';
import 'package:android_restore_credentials/android_restore_credentials_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockAndroidRestoreCredentialsPlatform
    with MockPlatformInterfaceMixin
    implements AndroidRestoreCredentialsPlatform {
  String? createRequestJson;
  bool? createPreferCloudBackup;
  String? getRequestJson;
  int clearCalls = 0;

  @override
  Future<String> createRestoreKey({
    required String requestJson,
    bool preferCloudBackup = true,
  }) async {
    createRequestJson = requestJson;
    createPreferCloudBackup = preferCloudBackup;
    return 'create-response';
  }

  @override
  Future<String> getRestoreKey({required String requestJson}) async {
    getRequestJson = requestJson;
    return 'get-response';
  }

  @override
  Future<void> clearRestoreKey() async {
    clearCalls += 1;
  }
}

void main() {
  final AndroidRestoreCredentialsPlatform initialPlatform =
      AndroidRestoreCredentialsPlatform.instance;

  test('$MethodChannelAndroidRestoreCredentials is the default instance', () {
    expect(initialPlatform, isA<MethodChannelAndroidRestoreCredentials>());
  });

  test('createRestoreKey forwards args and returns response', () async {
    final fake = MockAndroidRestoreCredentialsPlatform();
    AndroidRestoreCredentialsPlatform.instance = fake;

    final plugin = AndroidRestoreCredentials();
    final result = await plugin.createRestoreKey(
      requestJson: 'create-json',
      preferCloudBackup: false,
    );

    expect(result, 'create-response');
    expect(fake.createRequestJson, 'create-json');
    expect(fake.createPreferCloudBackup, false);
  });

  test('getRestoreKey forwards args and returns response', () async {
    final fake = MockAndroidRestoreCredentialsPlatform();
    AndroidRestoreCredentialsPlatform.instance = fake;

    final plugin = AndroidRestoreCredentials();
    final result = await plugin.getRestoreKey(requestJson: 'get-json');

    expect(result, 'get-response');
    expect(fake.getRequestJson, 'get-json');
  });

  test('clearRestoreKey forwards to platform', () async {
    final fake = MockAndroidRestoreCredentialsPlatform();
    AndroidRestoreCredentialsPlatform.instance = fake;

    final plugin = AndroidRestoreCredentials();
    await plugin.clearRestoreKey();

    expect(fake.clearCalls, 1);
  });

  test('default platform interface throws UnimplementedError', () {
    final unimplemented = _UnimplementedPlatform();
    AndroidRestoreCredentialsPlatform.instance = unimplemented;

    expect(
      () => unimplemented.createRestoreKey(requestJson: 'x'),
      throwsA(isA<UnimplementedError>()),
    );
    expect(
      () => unimplemented.getRestoreKey(requestJson: 'x'),
      throwsA(isA<UnimplementedError>()),
    );
    expect(
      () => unimplemented.clearRestoreKey(),
      throwsA(isA<UnimplementedError>()),
    );
  });
}

class _UnimplementedPlatform extends AndroidRestoreCredentialsPlatform {}
