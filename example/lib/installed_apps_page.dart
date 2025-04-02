import 'package:flutter/material.dart';
import 'package:screen_time/screen_time.dart';

class InstalledAppsPage extends StatefulWidget {
  const InstalledAppsPage({super.key, required this.installedApps});

  final List<InstalledApp> installedApps;

  @override
  State<InstalledAppsPage> createState() => _InstalledAppsPageState();
}

class _InstalledAppsPageState extends State<InstalledAppsPage>
    with TickerProviderStateMixin {
  late TabController _tabController;

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
    );
  }

  Widget _buildAllAppsList() {
    return ListView.builder(
      itemCount: widget.installedApps.length,
      itemBuilder: (context, index) {
        final app = widget.installedApps[index];
        return ListTile(
          leading:
              app.iconInBytes != null ? Image.memory(app.iconInBytes!) : null,
          title: Text(app.appName ?? "Unknown"),
          subtitle: Text(app.category.name),
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
                    (app) => ListTile(
                      leading:
                          app.iconInBytes != null
                              ? Image.memory(app.iconInBytes!)
                              : const Icon(Icons.android),
                      title: Text(app.appName ?? "Unknown"),
                    ),
                  )
                  .toList(),
        );
      },
    );
  }
}
