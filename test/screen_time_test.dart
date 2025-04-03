import 'package:flutter_test/flutter_test.dart';
import 'package:screen_time/screen_time.dart';
import 'package:screen_time/screen_time_platform_interface.dart';
import 'package:screen_time/screen_time_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockScreenTimePlatform
    with MockPlatformInterfaceMixin
    implements ScreenTimePlatform {
  @override
  Future<List<InstalledApp>> installedApps({
    bool ignoreSystemApps = true,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<List<AppUsage>> appUsageData({
    DateTime? startTime,
    DateTime? endTime,
    UsageInterval usageInterval = UsageInterval.daily,
    List<String>? packagesName,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<bool> configureAppMonitoringService(
      {UsageInterval interval = UsageInterval.daily,
      int lookbackTimeMs = 10000}) {
    throw UnimplementedError();
  }

  @override
  Future<MonitoringAppUsage> monitoringAppUsage({
    int startHour = 0,
    int startMinute = 0,
    int endHour = 23,
    int endMinute = 59,
    UsageInterval usageInterval = UsageInterval.daily,
    int lookbackTimeMs = 10000,
    List<String>? packagesName,
  }) {
    throw UnimplementedError();
  }

  @override
  Stream<Map<String, dynamic>> streamAppUsage(
      {UsageInterval usageInterval = UsageInterval.daily,
      int lookbackTimeMs = 10000}) {
    throw UnimplementedError();
  }

  @override
  Future<ScreenTimePermissionStatus> permissionStatus(
      {ScreenTimePermissionType permissionType =
          ScreenTimePermissionType.appUsage}) {
    throw UnimplementedError();
  }

  @override
  Future<bool> requestPermission(
      {UsageInterval interval = UsageInterval.daily,
      ScreenTimePermissionType permissionType =
          ScreenTimePermissionType.appUsage}) {
    throw UnimplementedError();
  }
}

void main() {
  final ScreenTimePlatform initialPlatform = ScreenTimePlatform.instance;

  test('$MethodChannelScreenTime is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelScreenTime>());
  });

  test('getPlatformVersion', () async {
    // ScreenTime screenTimePlugin = ScreenTime();
    MockScreenTimePlatform fakePlatform = MockScreenTimePlatform();
    ScreenTimePlatform.instance = fakePlatform;

    // expect(await screenTimePlugin.getPlatformVersion(), '42');
  });
}
