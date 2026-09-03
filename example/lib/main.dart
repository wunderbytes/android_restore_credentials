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
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    // Tier 2 — foreground restoration: attempt a silent get on first launch
    // to cover the case where background (BackupAgent) restore did not run,
    // backup is off, or restore is independent of app-data backup.
    _restoreOnFirstLaunch();
  }

  Future<void> _restoreOnFirstLaunch() async {
    if (!mounted) return;
    if (!kIsWeb && defaultTargetPlatform != TargetPlatform.android) {
      setState(() => _status = 'Restore Credentials only run on Android.');
      return;
    }
    setState(() => _status = 'Checking for a restore key on first launch…');
    try {
      final assertion = await _plugin.getRestoreKey(requestJson: _fixtureGetJson);
      if (!mounted) return;
      // In a real app you would POST `assertion` to your RP to verify and
      // establish a session. Here we just display it.
      setState(() => _status = 'Restored (first launch): $assertion');
    } on PlatformException catch (e) {
      if (!mounted) return;
      // `no_credential` is the normal "nothing to restore" case.
      setState(() => _status = 'No restore key on first launch (${e.code}).');
    }
  }

  @override
  Widget build(BuildContext context) {
    final onAndroid = !kIsWeb && defaultTargetPlatform == TargetPlatform.android;
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Restore Credentials example')),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(_status, textAlign: TextAlign.center),
                const SizedBox(height: 16),
                if (onAndroid) ...[
                  ElevatedButton(
                    onPressed: _busy ? null : _create,
                    child: const Text('Create restore key (after sign-in)'),
                  ),
                  ElevatedButton(
                    onPressed: _busy ? null : _get,
                    child: const Text('Get restore key'),
                  ),
                  ElevatedButton(
                    onPressed: _busy ? null : _clear,
                    child: const Text('Clear restore key (on sign-out)'),
                  ),
                ] else
                  const Text('Restore Credentials only run on Android.'),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _run(String label, Future<String?> Function() action) async {
    setState(() {
      _busy = true;
      _status = '$label…';
    });
    try {
      final result = await action();
      if (!mounted) return;
      setState(() => _status = '$label: $result');
    } on PlatformException catch (e) {
      if (!mounted) return;
      setState(() => _status = '$label failed: ${e.code} ${e.message}');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _create() => _run(
        'Created',
        () async => _plugin.createRestoreKey(requestJson: _fixtureCreateJson),
      );

  Future<void> _get() => _run(
        'Got',
        () async => _plugin.getRestoreKey(requestJson: _fixtureGetJson),
      );

  Future<void> _clear() async {
    setState(() {
      _busy = true;
      _status = 'Clearing…';
    });
    try {
      await _plugin.clearRestoreKey();
      if (!mounted) return;
      setState(() => _status = 'Cleared');
    } on PlatformException catch (e) {
      if (!mounted) return;
      setState(() => _status = 'Clear failed: ${e.code} ${e.message}');
    } finally {
      if (mounted) setState(() => _busy = false);
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
