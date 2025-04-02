import 'package:screen_time/src/model/request_permission_model.dart';

import 'screen_time_platform_interface.dart';
import 'src/model/app_usage.dart';
import 'src/model/installed_app.dart';
import 'src/model/monitoring_app_usage.dart';
import 'src/model/usage_interval.dart';

export 'src/const/method_name.dart';
export 'src/const/argument.dart';
export 'src/model/permission_status.dart';
export 'src/model/installed_app.dart';
export 'src/model/app_category.dart';
export 'src/model/app_usage.dart';
export 'src/model/monitoring_app_usage.dart';
export 'src/model/request_permission_model.dart';
export 'src/model/usage_interval.dart';

class ScreenTime {
  Future<List<InstalledApp>> installedApps() {
    return ScreenTimePlatform.instance.installedApps();
  }

  /// Request Screen Time permission from the user.
  ///
  /// Parameters:
  /// - `interval`: The interval to use for usage stats queries (DAILY, WEEKLY, MONTHLY, YEARLY, BEST)
  ///
  /// Returns a [RequestPermissionModel] with the following keys:
  /// - `status`: The current authorization status `true` is requested `false`: failed to request.
  /// - `error`: Error message if failed to request.
  Future<RequestPermissionModel> requestPermission({
    UsageInterval interval = UsageInterval.daily,
  }) async {
    return await ScreenTimePlatform.instance
        .requestPermission(interval: interval);
  }

  /// Fetch app usage data from the device.
  ///
  /// Returns a map with the following keys:
  /// - `status`: Whether the data was successfully fetched.
  /// - `data`: Map of app bundle IDs to usage data.
  /// - `error`: Error message if the data fetch failed.
  Future<List<AppUsage>> appUsageData({
    DateTime? startTime,
    DateTime? endTime,
    UsageInterval usageInterval = UsageInterval.daily,
  }) {
    return ScreenTimePlatform.instance.appUsageData(
      startTime: startTime,
      endTime: endTime,
      usageInterval: usageInterval,
    );
  }

  /// Start monitoring app usage with the specified schedule.
  ///
  /// Parameters:
  /// - `startHour`: The hour to start monitoring (0-23).
  /// - `startMinute`: The minute to start monitoring (0-59).
  /// - `endHour`: The hour to end monitoring (0-23).
  /// - `endMinute`: The minute to end monitoring (0-59).
  ///
  /// Returns a map with the following keys:
  /// - `status`: Whether monitoring was successfully started.
  /// - `monitoringActive`: Whether monitoring is currently active.
  /// - `schedule`: Map containing schedule details (startTime, endTime, frequency).
  /// - `timestamp`: When monitoring was started.
  /// - `error`: Error message if monitoring failed to start.
  Future<MonitoringAppUsage> monitoringAppUsage({
    required int startHour,
    required int startMinute,
    required int endHour,
    required int endMinute,
    UsageInterval usageInterval = UsageInterval.daily,
    int lookbackTimeMs = 10000, // Default: 10 seconds
  }) {
    return ScreenTimePlatform.instance.monitoringAppUsage(
      startHour: startHour,
      startMinute: startMinute,
      endHour: endHour,
      endMinute: endMinute,
      usageInterval: usageInterval,
      lookbackTimeMs: lookbackTimeMs,
    );
  }

  /// Stream app usage data in real-time.
  ///
  /// This method returns a Stream that emits events whenever the foreground app changes.
  /// It uses the native AppMonitoringService to provide real-time updates.
  ///
  /// Parameters:
  /// - `usageInterval`: The interval to use for usage stats queries (DAILY, WEEKLY, MONTHLY, YEARLY, BEST)
  /// - `lookbackTimeMs`: How far back in time to look for app usage data (in milliseconds)
  ///
  /// Returns a Stream of [Map<String, dynamic>] containing the foreground app data.
  Stream<Map<String, dynamic>> streamAppUsage({
    UsageInterval usageInterval = UsageInterval.daily,
    int lookbackTimeMs = 10000, // Default: 10 seconds
  }) {
    return ScreenTimePlatform.instance.streamAppUsage(
      usageInterval: usageInterval,
      lookbackTimeMs: lookbackTimeMs,
    );
  }

  /// Opens the system accessibility settings screen
  /// This allows users to enable the app monitoring service
  ///
  /// Returns `true` if the settings screen was opened successfully,
  /// `false` otherwise
  Future<bool> openAccessibilitySettings() {
    return ScreenTimePlatform.instance.openAccessibilitySettings();
  }

  /// Checks if the app monitoring service is enabled
  ///
  /// Returns `true` if the service is enabled, `false` otherwise
  Future<bool> isAppMonitoringServiceEnabled() {
    return ScreenTimePlatform.instance.isAppMonitoringServiceEnabled();
  }

  /// Configures the app monitoring service with the specified interval and lookback time
  ///
  /// Parameters:
  /// - `interval`: The interval to use for usage stats queries (DAILY, WEEKLY, MONTHLY, YEARLY, BEST)
  /// - `lookbackTimeMs`: How far back in time to look for app usage data (in milliseconds)
  ///
  /// Returns `true` if the service was configured successfully, `false` otherwise
  Future<bool> configureAppMonitoringService({
    UsageInterval interval = UsageInterval.daily,
    int lookbackTimeMs = 10000, // Default: 10 seconds
  }) {
    return ScreenTimePlatform.instance.configureAppMonitoringService(
      interval: interval,
      lookbackTimeMs: lookbackTimeMs,
    );
  }
}
