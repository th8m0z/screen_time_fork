import 'package:flutter/material.dart';
import 'package:screen_time/screen_time.dart';
import 'package:screen_time_example/duration_ext.dart';

import 'app_monitoring_settings.dart';
import 'app_usage_page.dart';

/// Enum for blocking action options
enum BlockingAction { stop, pause, cancel }

class InstalledAppsPage extends StatefulWidget {
  const InstalledAppsPage({super.key, required this.installedApps});

  final List<InstalledApp> installedApps;

  @override
  State<InstalledAppsPage> createState() => _InstalledAppsPageState();
}

class _InstalledAppsPageState extends State<InstalledAppsPage>
    with TickerProviderStateMixin {
  late TabController _tabController;
  final _screenTime = ScreenTime();
  final _selectedApp = <InstalledApp>{};

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Installed Apps'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [Tab(text: 'All Apps'), Tab(text: 'By Category')],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [_buildAllAppsList(), _buildCategorizedAppsList()],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => onFloatingActionPressed(context),
        child: const Icon(Icons.add),
      ),
    );
  }

  void onFloatingActionPressed(BuildContext context) async {
    final ctx = context;
    final appUsagePermission = await _screenTime.permissionStatus(
      permissionType: ScreenTimePermissionType.appUsage,
    );
    final drawOverlayPermission = await _screenTime.permissionStatus(
      permissionType: ScreenTimePermissionType.drawOverlay,
    );
    final notificationPermission = await _screenTime.permissionStatus(
      permissionType: ScreenTimePermissionType.notification,
    );
    final accessibiltySettingsPermission = await _screenTime.permissionStatus(
      permissionType: ScreenTimePermissionType.accessibilitySettings,
    );

    final isOnBlocking = await _screenTime.isOnBlockingApps;

    if (!ctx.mounted) return;
    showModalBottomSheet(
      context: ctx,
      builder:
          (modalContext) => Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              ListTile(
                leading: Icon(Icons.history),
                title: Text(
                  appUsagePermission == ScreenTimePermissionStatus.approved
                      ? 'App Usage'
                      : 'Need Request App Usage Permission',
                ),
                onTap: () async {
                  final appUsageContext = modalContext;
                  if (appUsagePermission ==
                      ScreenTimePermissionStatus.approved) {
                    final packagesName =
                        _selectedApp
                            .map((app) => app.packageName ?? '')
                            .toList();
                    final result = await _screenTime.appUsageData(
                      packagesName: packagesName,
                    );

                    if (!appUsageContext.mounted) return;
                    Navigator.push(
                      appUsageContext,
                      MaterialPageRoute(
                        builder: (context) => AppUsagePage(apps: result),
                      ),
                    );
                  } else {
                    Navigator.pop(modalContext);
                  }
                },
              ),
              ListTile(
                leading: Icon(Icons.monitor),
                title: Text(
                  (accessibiltySettingsPermission ==
                          ScreenTimePermissionStatus.approved)
                      ? 'App Monitoring'
                      : 'Need Request App Monitoring Permission',
                ),
                onTap: () async {
                  if (accessibiltySettingsPermission ==
                      ScreenTimePermissionStatus.approved) {
                    final appMonitoringContext = modalContext;
                    Navigator.push(
                      appMonitoringContext,
                      MaterialPageRoute(
                        builder:
                            (context) => AppMonitoringSettingsScreen(
                              packagesName:
                                  _selectedApp
                                      .map((app) => app.packageName ?? '')
                                      .toList(),
                            ),
                      ),
                    );
                  } else {
                    Navigator.pop(modalContext);
                  }
                },
              ),
              ListTile(
                leading: Icon(Icons.block),
                title: Text(
                  (appUsagePermission == ScreenTimePermissionStatus.approved &&
                          drawOverlayPermission ==
                              ScreenTimePermissionStatus.approved &&
                          notificationPermission ==
                              ScreenTimePermissionStatus.approved)
                      ? (isOnBlocking)
                          ? 'Block Apps Options'
                          : 'Block Apps'
                      : 'Need Usage Stat, Request Draw Overlay, and Notification',
                ),
                onTap: () async {
                  final modalCtx = modalContext;
                  if (appUsagePermission ==
                          ScreenTimePermissionStatus.approved &&
                      drawOverlayPermission ==
                          ScreenTimePermissionStatus.approved &&
                      notificationPermission ==
                          ScreenTimePermissionStatus.approved) {
                    if (isOnBlocking) {
                      // Show options when blocking is active: Stop or Pause
                      if (!modalCtx.mounted) return;
                      final action = await _showBlockingOptionsDialog(modalCtx);

                      if (action == BlockingAction.stop) {
                        await _screenTime.unblockApps();
                      } else if (action == BlockingAction.pause) {
                        // Show pause duration selection dialog
                        if (!modalCtx.mounted) return;
                        final pauseDuration = await _showDurationPickerDialog(
                          modalCtx,
                          title: 'Select Pause Duration',
                        );

                        if (pauseDuration != null) {
                          await _screenTime.pauseBlockApps(
                            pauseDuration: pauseDuration,
                            notificationTitle: 'Blocking Resumed',
                            notificationText:
                                'App blocking has resumed after pause',
                            showNotification: true,
                          );
                        }
                      }
                    } else {
                      // Show duration selection dialog for new blocking
                      if (!modalCtx.mounted) return;
                      final selectedDuration = await _showDurationPickerDialog(
                        modalCtx,
                        title: 'Select Block Duration',
                      );

                      // If user cancels the dialog, selectedDuration will be null
                      if (selectedDuration != null) {
                        final apps =
                            _selectedApp
                                .map((app) => app.packageName ?? '')
                                .toList();
                        await _screenTime.blockApps(
                          packagesName: apps,
                          duration: selectedDuration,
                          layoutName: 'block_overlay', // Custom overlay layout
                          notificationTitle: 'Screen Time Example Active',
                          notificationText:
                              'Blocking ${apps.length} apps for ${selectedDuration.inString} now.',
                        );
                      }
                    }

                    if (!modalCtx.mounted) return;
                    Navigator.pop(modalCtx);
                  } else {
                    Navigator.pop(modalContext);
                  }
                },
              ),
              ListTile(
                leading: Icon(Icons.monitor),
                title: Text(
                  (accessibiltySettingsPermission ==
                          ScreenTimePermissionStatus.approved)
                      ? 'App Monitoring'
                      : 'Need Request App Monitoring Permission',
                ),
                onTap: () async {
                  if (accessibiltySettingsPermission ==
                      ScreenTimePermissionStatus.approved) {
                    final appMonitoringContext = modalContext;
                    Navigator.push(
                      appMonitoringContext,
                      MaterialPageRoute(
                        builder:
                            (context) => AppMonitoringSettingsScreen(
                              packagesName:
                                  _selectedApp
                                      .map((app) => app.packageName ?? '')
                                      .toList(),
                            ),
                      ),
                    );
                  } else {
                    Navigator.pop(modalContext);
                  }
                },
              ),
            ],
          ),
    );
  }

  Widget _buildAllAppsList() {
    return ListView.builder(
      itemCount: widget.installedApps.length,
      itemBuilder: (context, index) {
        final app = widget.installedApps[index];
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Row(
            children: [
              Checkbox(
                value: _selectedApp.contains(app),
                onChanged: (isSelected) {
                  setState(() {
                    if (isSelected == true) {
                      _selectedApp.add(app);
                    } else {
                      _selectedApp.remove(app);
                    }
                  });
                },
              ),
              app.iconInBytes != null
                  ? Image.memory(app.iconInBytes!, width: 60, height: 60)
                  : const SizedBox.shrink(),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(app.appName ?? "Unknown"),
                    Text(
                      app.packageName ?? "-",
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              Text(app.category.name),
              const SizedBox(width: 16),
            ],
          ),
        );
      },
    );
  }

  /// Shows a dialog for selecting duration for app blocking
  Future<Duration?> _showDurationPickerDialog(
    BuildContext context, {
    String title = 'Select Blocking Duration',
  }) async {
    int hours = 1;
    int minutes = 0;

    return showDialog<Duration>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(title),
          content: StatefulBuilder(
            builder: (BuildContext context, StateSetter setState) {
              return Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text('Hours:'),
                      Row(
                        children: [
                          IconButton(
                            icon: Icon(Icons.remove),
                            onPressed:
                                hours > 0
                                    ? () => setState(() => hours--)
                                    : null,
                          ),
                          Text('$hours'),
                          IconButton(
                            icon: Icon(Icons.add),
                            onPressed: () => setState(() => hours++),
                          ),
                        ],
                      ),
                    ],
                  ),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text('Minutes:'),
                      Row(
                        children: [
                          IconButton(
                            icon: Icon(Icons.remove),
                            onPressed:
                                minutes > 0
                                    ? () => setState(() => minutes -= 1)
                                    : null,
                          ),
                          Text('$minutes'),
                          IconButton(
                            icon: Icon(Icons.add),
                            onPressed:
                                minutes < 55
                                    ? () => setState(() => minutes += 1)
                                    : null,
                          ),
                        ],
                      ),
                    ],
                  ),
                ],
              );
            },
          ),
          actions: <Widget>[
            TextButton(
              child: Text('Cancel'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            TextButton(
              child: Text('OK'),
              onPressed: () {
                Navigator.of(
                  context,
                ).pop(Duration(hours: hours, minutes: minutes));
              },
            ),
          ],
        );
      },
    );
  }

  /// Shows a dialog with options for managing active app blocking
  Future<BlockingAction?> _showBlockingOptionsDialog(
    BuildContext context,
  ) async {
    return showDialog<BlockingAction>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text('Block Apps Options'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('What would you like to do with the current app blocking?'),
            ],
          ),
          actions: <Widget>[
            TextButton(
              child: Text('Cancel'),
              onPressed: () {
                Navigator.of(context).pop(BlockingAction.cancel);
              },
            ),
            TextButton(
              child: Text('Pause Blocking'),
              onPressed: () {
                Navigator.of(context).pop(BlockingAction.pause);
              },
            ),
            TextButton(
              child: Text('Stop Blocking'),
              onPressed: () {
                Navigator.of(context).pop(BlockingAction.stop);
              },
            ),
          ],
        );
      },
    );
  }

  Widget _buildCategorizedAppsList() {
    // Group apps by category
    final Map<AppCategory, List<InstalledApp>> categorizedApps = {};

    for (var app in widget.installedApps) {
      if (!categorizedApps.containsKey(app.category)) {
        categorizedApps[app.category] = [];
      }
      categorizedApps[app.category]!.add(app);
    }

    // Sort categories
    final sortedCategories =
        categorizedApps.keys.toList()..sort((a, b) => a.name.compareTo(b.name));

    return ListView.builder(
      itemCount: sortedCategories.length,
      itemBuilder: (context, index) {
        final category = sortedCategories[index];
        final appsInCategory = categorizedApps[category]!;

        return ExpansionTile(
          title: Text(category.name),
          subtitle: Text('${appsInCategory.length} apps'),
          children:
              appsInCategory
                  .map(
                    (app) => Row(
                      children: [
                        Checkbox(
                          value: _selectedApp.contains(app),
                          onChanged: (isSelected) {
                            setState(() {
                              if (isSelected == true) {
                                _selectedApp.add(app);
                              } else {
                                _selectedApp.remove(app);
                              }
                            });
                          },
                        ),
                        app.iconInBytes != null
                            ? Image.memory(
                              app.iconInBytes!,
                              width: 60,
                              height: 60,
                            )
                            : const SizedBox.shrink(),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(app.appName ?? "Unknown"),
                              Text(
                                app.packageName ?? "-",
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                        Text(app.category.name),
                        const SizedBox(width: 16),
                      ],
                    ),
                  )
                  .toList(),
        );
      },
    );
  }
}
