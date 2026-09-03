import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:android_restore_credentials/android_restore_credentials.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _plugin = AndroidRestoreCredentials();
  String _status = 'Idle';

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Restore Credentials example')),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(_status, textAlign: TextAlign.center),
              const SizedBox(height: 16),
              if (!kIsWeb &&
                  defaultTargetPlatform == TargetPlatform.android) ...[
                ElevatedButton(
                  onPressed: _create,
                  child: const Text('Create restore key'),
                ),
                ElevatedButton(
                  onPressed: _get,
                  child: const Text('Get restore key'),
                ),
                ElevatedButton(
                  onPressed: _clear,
                  child: const Text('Clear restore key'),
                ),
              ] else
                const Text('Restore Credentials only run on Android.'),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _create() async {
    try {
      final response = await _plugin.createRestoreKey(
        requestJson: _fixtureCreateJson,
      );
      setState(() => _status = 'Created: $response');
    } on PlatformException catch (e) {
      setState(() => _status = 'Create failed: ${e.code} ${e.message}');
    }
  }

  Future<void> _get() async {
    try {
      final response = await _plugin.getRestoreKey(
        requestJson: _fixtureGetJson,
      );
      setState(() => _status = 'Got: $response');
    } on PlatformException catch (e) {
      setState(() => _status = 'Get failed: ${e.code} ${e.message}');
    }
  }

  Future<void> _clear() async {
    try {
      await _plugin.clearRestoreKey();
      setState(() => _status = 'Cleared');
    } on PlatformException catch (e) {
      setState(() => _status = 'Clear failed: ${e.code} ${e.message}');
    }
  }
}

/// Fixture WebAuthn JSON for the example only. A real host fetches this from
/// its relying-party server.
const _fixtureCreateJson = '''
{
  "challenge": "dGVzdA",
  "rp": {"name": "Example", "id": "example.com"},
  "user": {"id": "dXNlcg", "name": "user", "displayName": "User"},
  "pubKeyCredParams": [{"type": "public-key", "alg": -7}]
}
''';

const _fixtureGetJson = '''
{
  "challenge": "dGVzdA",
  "rpId": "example.com"
}
''';
