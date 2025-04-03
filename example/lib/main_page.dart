import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:screen_time/screen_time.dart';

import 'installed_apps_page.dart';

class MainPage extends StatefulWidget {
  const MainPage({super.key});

  @override
  State<MainPage> createState() => _MainPageState();
}

class _MainPageState extends State<MainPage> {
  final _screenTime = ScreenTime();
  String _lastResult = '';
  bool _isLoading = false;

  // @override
  // void initState() {
  //   super.initState();

  //   _executeMethod(_screenTime.scheduleBackgroundFetch);
  // }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Screen Time Plugin Example'),
        backgroundColor: Colors.blue,
        foregroundColor: Colors.white,
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Text(
                        'Method Results:',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: Colors.grey[200],
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child:
                            _isLoading
                                ? const Center(
                                  child: CircularProgressIndicator(),
                                )
                                : _lastResult.isEmpty
                                ? const Text('No method called yet')
                                : Text(
                                  const JsonEncoder.withIndent(
                                    '  ',
                                  ).convert(_lastResult),
                                  style: const TextStyle(
                                    fontFamily: 'monospace',
                                  ),
                                ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Text(
                        'Available Methods:',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 8),
                      _buildMethodButton(
                        'Request App Usage Permission',
                        () => _executeMethod(() async {
                          final result = await _screenTime.requestPermission();
                          return jsonEncode({'status': result.status});
                        }),
                      ),
                      const SizedBox(height: 8),
                      _buildMethodButton(
                        'Fetch Installed Application',
                        () async {
                          showDialog(
                            context: context,
                            builder:
                                (context) => const Center(
                                  child: CircularProgressIndicator(),
                                ),
                          );

                          final ctx = context;
                          final apps = await _screenTime.installedApps();

                          if (!ctx.mounted) return;
                          Navigator.pop(ctx);

                          Navigator.push(
                            ctx,
                            MaterialPageRoute(
                              builder:
                                  (context) =>
                                      InstalledAppsPage(installedApps: apps),
                            ),
                          );
                        },
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _executeMethod(dynamic Function() method) async {
    setState(() {
      _isLoading = true;
      _lastResult = '';
    });

    try {
      final result = await method();
      setState(() {
        _lastResult = result;
      });
    } on PlatformException catch (e) {
      setState(() {
        _lastResult = jsonEncode({
          'success': false,
          'error': 'PlatformException: ${e.message}',
          'details': e.details,
        });
      });
    } catch (e) {
      setState(() {
        _lastResult = jsonEncode({'success': false, 'error': e.toString()});
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Widget _buildMethodButton(String label, VoidCallback onPressed) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8.0),
      child: ElevatedButton(
        onPressed: _isLoading ? null : onPressed,
        style: ElevatedButton.styleFrom(
          padding: const EdgeInsets.symmetric(vertical: 12),
        ),
        child: Text(label),
      ),
    );
  }
}
