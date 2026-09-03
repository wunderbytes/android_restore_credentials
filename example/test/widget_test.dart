// This is a basic Flutter widget test for the example app.

import 'package:flutter_test/flutter_test.dart';

import 'package:android_restore_credentials_example/main.dart';

void main() {
  testWidgets('shows Restore Credentials example title', (WidgetTester tester) async {
    await tester.pumpWidget(const MyApp());

    expect(find.text('Restore Credentials example'), findsOneWidget);
  });
}
