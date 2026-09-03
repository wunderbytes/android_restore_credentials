// This is a basic Flutter widget test for the example app.

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:android_restore_credentials_example/main.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  // The example app calls getRestoreKey on first launch. Stub the channel so
  // the integration doesn't blow up with MissingPluginException on non-Android
  // hosts.
  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
      const MethodChannel('android_restore_credentials'),
      (call) async {
        if (call.method == 'getRestoreKey') return 'assertion-json';
        return null;
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(const MethodChannel('android_restore_credentials'), null);
  });

  testWidgets('shows Restore Credentials example title', (WidgetTester tester) async {
    await tester.pumpWidget(const MyApp());

    expect(find.text('Restore Credentials example'), findsOneWidget);
  });
}
