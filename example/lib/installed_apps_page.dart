import 'package:flutter/material.dart';
import 'package:screen_time/screen_time.dart';

import 'app_monitoring_settings.dart';
import 'app_usage_page.dart';

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

  void onFloatingActionPressed(BuildContext context) {
    showModalBottomSheet(
      context: context,
      builder:
          (context) => Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              ListTile(
                leading: Icon(Icons.history),
                title: Text('App Usage'),
                onTap: () async {
                  final ctx = context;
                  final packagesName =
                      _selectedApp.map((app) => app.packageName ?? '').toList();
                  final result = await _screenTime.appUsageData(
                    packagesName: packagesName,
                  );

                  if (!ctx.mounted) return;
                  Navigator.push(
                    ctx,
                    MaterialPageRoute(
                      builder: (context) => AppUsagePage(apps: result),
                    ),
                  );
                },
              ),
              ListTile(
                leading: Icon(Icons.monitor),
                title: Text('App Monitoring'),
                onTap: () async {
                  Navigator.push(
                    context,
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
