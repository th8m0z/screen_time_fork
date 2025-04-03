import 'dart:async';
import 'package:flutter/material.dart';
import 'package:screen_time/screen_time.dart';

class AppMonitoringSettingsScreen extends StatefulWidget {
  const AppMonitoringSettingsScreen({super.key, this.packagesName});

  final List<String>? packagesName;

  @override
  State<AppMonitoringSettingsScreen> createState() =>
      _AppMonitoringSettingsScreenState();
}

class _AppMonitoringSettingsScreenState
    extends State<AppMonitoringSettingsScreen> {
  final ScreenTime _screenTime = ScreenTime();
  bool _isServiceEnabled = false;
  UsageInterval _selectedInterval = UsageInterval.daily;
  int _lookbackTime = 10; // seconds

  // Monitoring state
  bool _isMonitoring = false;
  Map<String, dynamic>? _currentAppData;
  final List<Map<String, dynamic>> _appHistory = [];
  String? _lastPackageName;
  StreamSubscription<Map<String, dynamic>>? _appUsageSubscription;

  @override
  void initState() {
    super.initState();
    _checkServiceStatus();
  }

  @override
  void dispose() {
    _stopMonitoring();
    super.dispose();
  }

  Future<void> _checkServiceStatus() async {
    final isEnabled = await _screenTime.isAppMonitoringServiceEnabled();
    setState(() {
      _isServiceEnabled = isEnabled;
    });
  }

  Future<void> _openAccessibilitySettings() async {
    await _screenTime.openAccessibilitySettings();
    // Wait a bit before checking status again
    await Future.delayed(const Duration(seconds: 3));
    await _checkServiceStatus();
  }

  Future<void> _configureService(BuildContext context) async {
    final ctx = context;
    final success = await _screenTime.configureAppMonitoringService(
      interval: _selectedInterval,
      lookbackTimeMs: _lookbackTime * 1000,
    );

    if (!ctx.mounted) return;
    ScaffoldMessenger.of(ctx).showSnackBar(
      SnackBar(
        content: Text(
          success
              ? 'Service configured successfully'
              : 'Failed to configure service',
        ),
      ),
    );

    // After configuring, start monitoring if service is enabled
    if (success && _isServiceEnabled) {
      _startMonitoring();
    }
  }

  // Start monitoring app usage with streaming approach
  Future<void> _startMonitoring() async {
    if (_isMonitoring) return;

    try {
      // Initialize with current data
      final result = await _screenTime.monitoringAppUsage(
        startHour: 0,
        startMinute: 0,
        endHour: 23,
        endMinute: 59,
        usageInterval: _selectedInterval,
        lookbackTimeMs: _lookbackTime * 1000,
        packagesName: widget.packagesName,
      );

      // Update UI with initial data
      setState(() {
        _isMonitoring = true;
        if (result.currentForegroundApp != null) {
          _currentAppData = result.currentForegroundApp;
          _addToHistory(result.currentForegroundApp!);
        }
      });

      // Start streaming updates
      _appUsageSubscription = _screenTime.streamAppUsage().listen(
        (appData) {
          if (mounted) {
            setState(() {
              _currentAppData = appData;
              _addToHistory(appData);
            });
          }
        },
        onError: (error) {
          debugPrint('Stream error: $error');
        },
      );
    } catch (e) {
      debugPrint('Error starting monitoring: $e');
      setState(() {
        _isMonitoring = false;
      });
    }
  }

  void _stopMonitoring() {
    setState(() {
      _isMonitoring = false;
    });
    _appUsageSubscription?.cancel();
  }

  void _addToHistory(Map<String, dynamic> appData) {
    // Only add to history if it's a different app than the last one
    final packageName = appData['packageName'] as String?;
    if (packageName != null && packageName != _lastPackageName) {
      _lastPackageName = packageName;

      // Add timestamp if not present
      final appWithTimestamp = Map<String, dynamic>.from(appData);
      if (!appWithTimestamp.containsKey('timestamp')) {
        appWithTimestamp['timestamp'] = DateTime.now().millisecondsSinceEpoch;
      }

      setState(() {
        _appHistory.insert(0, appWithTimestamp);
        // Limit history size
        if (_appHistory.length > 20) {
          _appHistory.removeLast();
        }
      });
    }
  }

  // Get a color based on the index for visual differentiation
  Color _getColorForIndex(int index) {
    final colors = [
      Colors.blue,
      Colors.red,
      Colors.green,
      Colors.orange,
      Colors.purple,
      Colors.teal,
      Colors.pink,
      Colors.indigo,
    ];
    return colors[index % colors.length];
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('App Monitoring Settings')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Service status
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'App Monitoring Service',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Icon(
                          _isServiceEnabled ? Icons.check_circle : Icons.error,
                          color: _isServiceEnabled ? Colors.green : Colors.red,
                        ),
                        const SizedBox(width: 8),
                        Text(
                          _isServiceEnabled
                              ? 'Service is enabled'
                              : 'Service is disabled',
                          style: TextStyle(
                            color:
                                _isServiceEnabled ? Colors.green : Colors.red,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        ElevatedButton(
                          onPressed: _openAccessibilitySettings,
                          child: const Text('Open Accessibility Settings'),
                        ),
                        IconButton(
                          onPressed: () => _checkServiceStatus(),
                          icon: Icon(Icons.refresh),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 24),

            // Service configuration
            if (_isServiceEnabled) ...[
              const Text(
                'Configure Monitoring',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 16),

              // Interval selection
              const Text('Usage Stats Interval:'),
              DropdownButton<UsageInterval>(
                value: _selectedInterval,
                onChanged: (UsageInterval? newValue) {
                  if (newValue != null) {
                    setState(() {
                      _selectedInterval = newValue;
                    });
                  }
                },
                items:
                    UsageInterval.values.map<DropdownMenuItem<UsageInterval>>((
                      UsageInterval value,
                    ) {
                      return DropdownMenuItem<UsageInterval>(
                        value: value,
                        child: Text(value.name),
                      );
                    }).toList(),
              ),

              const SizedBox(height: 16),

              // Lookback time slider
              Text(
                'Lookback Time: ${_lookbackTime.toStringAsFixed(1)} seconds',
              ),
              Slider(
                value: _lookbackTime.toDouble(),
                min: 1.0,
                max: 60.0,
                divisions: 59,
                label: _lookbackTime.toStringAsFixed(1),
                onChanged: (double value) {
                  setState(() {
                    _lookbackTime = value.toInt();
                  });
                },
              ),

              const SizedBox(height: 16),

              // Apply button
              ElevatedButton(
                onPressed: () => _configureService(context),
                child: const Text('Apply Configuration'),
              ),

              const SizedBox(height: 24),

              // Monitoring controls
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Monitoring Controls',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 16),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          ElevatedButton(
                            onPressed: _isMonitoring ? null : _startMonitoring,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.green,
                              foregroundColor: Colors.white,
                            ),
                            child: const Text('Start Monitoring'),
                          ),
                          ElevatedButton(
                            onPressed: _isMonitoring ? _stopMonitoring : null,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.red,
                              foregroundColor: Colors.white,
                            ),
                            child: const Text('Stop Monitoring'),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Center(
                        child: Text(
                          _isMonitoring
                              ? 'Monitoring active'
                              : 'Monitoring inactive',
                          style: TextStyle(
                            color: _isMonitoring ? Colors.green : Colors.grey,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],

            // Current app section
            if (_isMonitoring && _currentAppData != null) ...[
              const SizedBox(height: 24),
              const Text(
                'Current Foreground App',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Card(
                child: ListTile(
                  leading: CircleAvatar(
                    backgroundColor: Colors.blue,
                    child: Text(
                      (_currentAppData!['appName'] as String? ?? 'U').isNotEmpty
                          ? (_currentAppData!['appName'] as String)
                              .substring(0, 1)
                              .toUpperCase()
                          : 'U',
                      style: const TextStyle(color: Colors.white),
                    ),
                  ),
                  title: Text(
                    _currentAppData!['appName'] as String? ?? 'Unknown',
                  ),
                  subtitle: Text(
                    _currentAppData!['packageName'] as String? ?? 'Unknown',
                  ),
                  trailing:
                      _currentAppData!.containsKey('timestamp')
                          ? Text(
                            _formatTimestamp(
                              _currentAppData!['timestamp'] as int,
                            ),
                          )
                          : null,
                ),
              ),
            ],

            // App history section
            if (_appHistory.isNotEmpty) ...[
              const SizedBox(height: 24),
              const Text(
                'App Usage History',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              ListView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: _appHistory.length,
                itemBuilder: (context, index) {
                  final appData = _appHistory[index];
                  return Card(
                    margin: const EdgeInsets.only(bottom: 8.0),
                    child: ListTile(
                      leading: CircleAvatar(
                        backgroundColor: _getColorForIndex(index),
                        child: Text(
                          (appData['appName'] as String? ?? 'U').isNotEmpty
                              ? (appData['appName'] as String)
                                  .substring(0, 1)
                                  .toUpperCase()
                              : 'U',
                          style: const TextStyle(color: Colors.white),
                        ),
                      ),
                      title: Text(appData['appName'] as String? ?? 'Unknown'),
                      subtitle: Text(
                        appData['packageName'] as String? ?? 'Unknown',
                      ),
                      trailing:
                          appData.containsKey('timestamp')
                              ? Text(
                                _formatTimestamp(appData['timestamp'] as int),
                              )
                              : null,
                    ),
                  );
                },
              ),
            ],
          ],
        ),
      ),
    );
  }

  String _formatTimestamp(int timestamp) {
    final dateTime = DateTime.fromMillisecondsSinceEpoch(timestamp);
    return '${dateTime.hour.toString().padLeft(2, '0')}:${dateTime.minute.toString().padLeft(2, '0')}:${dateTime.second.toString().padLeft(2, '0')}';
  }
}
