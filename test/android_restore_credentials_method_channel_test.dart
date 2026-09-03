import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:android_restore_credentials/android_restore_credentials_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('android_restore_credentials');
  late MethodChannelAndroidRestoreCredentials platform;

  setUp(() {
    platform = MethodChannelAndroidRestoreCredentials();
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  void mockHandler(Future<Object?>? Function(MethodCall) handler) {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, handler);
  }

  test('createRestoreKey sends method, args, and returns String', () async {
    late String capturedMethod;
    late Map<dynamic, dynamic> capturedArgs;

    mockHandler((call) async {
      capturedMethod = call.method;
      capturedArgs = call.arguments as Map<dynamic, dynamic>;
      return 'create-response';
    });

    final result = await platform.createRestoreKey(
      requestJson: 'create-json',
      preferCloudBackup: false,
    );

    expect(result, 'create-response');
    expect(capturedMethod, 'createRestoreKey');
    expect(capturedArgs['requestJson'], 'create-json');
    expect(capturedArgs['preferCloudBackup'], false);
  });

  test('createRestoreKey defaults preferCloudBackup to true', () async {
    late Map<dynamic, dynamic> capturedArgs;

    mockHandler((call) async {
      capturedArgs = call.arguments as Map<dynamic, dynamic>;
      return 'create-response';
    });

    await platform.createRestoreKey(requestJson: 'create-json');

    expect(capturedArgs['preferCloudBackup'], true);
  });

  test('createRestoreKey throws PlatformException on null result', () async {
    mockHandler((call) async => null);

    expect(
      () => platform.createRestoreKey(requestJson: 'create-json'),
      throwsA(isA<PlatformException>()),
    );
  });

  test('getRestoreKey sends method, args, and returns String', () async {
    late String capturedMethod;
    late Map<dynamic, dynamic> capturedArgs;

    mockHandler((call) async {
      capturedMethod = call.method;
      capturedArgs = call.arguments as Map<dynamic, dynamic>;
      return 'get-response';
    });

    final result = await platform.getRestoreKey(requestJson: 'get-json');

    expect(result, 'get-response');
    expect(capturedMethod, 'getRestoreKey');
    expect(capturedArgs['requestJson'], 'get-json');
  });

  test('getRestoreKey throws PlatformException on null result', () async {
    mockHandler((call) async => null);

    expect(
      () => platform.getRestoreKey(requestJson: 'get-json'),
      throwsA(isA<PlatformException>()),
    );
  });

  test('clearRestoreKey sends method with no args', () async {
    late String capturedMethod;
    Object? capturedArgs;

    mockHandler((call) async {
      capturedMethod = call.method;
      capturedArgs = call.arguments;
      return null;
    });

    await platform.clearRestoreKey();

    expect(capturedMethod, 'clearRestoreKey');
    expect(capturedArgs, isNull);
  });
}
