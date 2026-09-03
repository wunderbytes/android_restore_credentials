import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:android_restore_credentials/android_restore_credentials_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelAndroidRestoreCredentials platform = MethodChannelAndroidRestoreCredentials();
  const MethodChannel channel = MethodChannel('android_restore_credentials');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
