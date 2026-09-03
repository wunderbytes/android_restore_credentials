// Basic Flutter integration test for the Android Restore Credentials plugin.
//
// Since integration tests run in a full Flutter application, they can interact
// with the host side of a plugin implementation, unlike Dart unit tests.
// For more information about Flutter integration tests, please see
// https://flutter.dev/to/integration-testing

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'package:android_restore_credentials/android_restore_credentials.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('createRestoreKey surfaces invalid_argument on blank JSON',
      (WidgetTester tester) async {
    final plugin = AndroidRestoreCredentials();

    expect(
      () => plugin.createRestoreKey(requestJson: ''),
      throwsA(
        isA<PlatformException>().having((e) => e.code, 'code', 'invalid_argument'),
      ),
    );
  });

  testWidgets('getRestoreKey surfaces invalid_argument on blank JSON',
      (WidgetTester tester) async {
    final plugin = AndroidRestoreCredentials();

    expect(
      () => plugin.getRestoreKey(requestJson: ''),
      throwsA(
        isA<PlatformException>().having((e) => e.code, 'code', 'invalid_argument'),
      ),
    );
  });
}
