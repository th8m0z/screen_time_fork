import 'dart:async';
import 'dart:isolate';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:screen_time/src/model/monitoring_app_usage.dart';
import 'package:screen_time/src/model/screen_time_permission_status.dart';

import 'screen_time_platform_interface.dart';
import 'src/const/argument.dart';
import 'src/const/method_name.dart';
import 'src/model/app_usage.dart';
import 'src/model/installed_app.dart';
import 'src/model/screen_time_permission_type.dart';
import 'src/model/usage_interval.dart';

/// An implementation of [ScreenTimePlatform] that uses method channels.
class MethodChannelScreenTime extends ScreenTimePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('screen_time');

  /// The event channel used for streaming app usage data
  @visibleForTesting
  final eventChannel = const EventChannel('screen_time/app_usage_stream');

  @override
  Future<List<InstalledApp>> installedApps({
    bool ignoreSystemApps = true,
  }) async {
    final result = await methodChannel
        .invokeMethod<Map<Object?, Object?>>(MethodName.installedApps, {
      Argument.ignoreSystemApps: ignoreSystemApps,
    });

    return await Isolate.run(() async {
      final map = await _convertToStringDynamicMap(result);
      final response = BaseInstalledApp.fromJson(map);
      if (response.status) {
        return response.data;
      } else {
        debugPrint(map.toString());
        return <InstalledApp>[];
      }
    });
  }

  @override
  Future<bool> requestPermission({
    UsageInterval interval = UsageInterval.daily,
    ScreenTimePermissionType permissionType = ScreenTimePermissionType.appUsage,
  }) async {
    return await methodChannel
            .invokeMethod<bool>(MethodName.requestPermission, {
          Argument.interval: interval.name,
          Argument.permissionType: permissionType.name,
        }) ??
        false;
  }

  @override
  Future<ScreenTimePermissionStatus> permissionStatus({
    ScreenTimePermissionType permissionType = ScreenTimePermissionType.appUsage,
  }) async {
    final result =
        await methodChannel.invokeMethod<String>(MethodName.permissionStatus, {
              Argument.permissionType: permissionType.name,
            }) ??
            ScreenTimePermissionStatus.notDetermined.name;
    return ScreenTimePermissionStatus.values.byName(result);
  }

  @override
  Future<List<AppUsage>> appUsageData({
    DateTime? startTime,
    DateTime? endTime,
    UsageInterval usageInterval = UsageInterval.daily,
    List<String>? packagesName,
  }) async {
    final arguments = <Object?, Object?>{};
    if (startTime != null) {
      arguments[Argument.startTimeInMillisecond] =
          startTime.millisecondsSinceEpoch;
    }
    if (endTime != null) {
      arguments[Argument.endTimeInMillisecond] = endTime.millisecondsSinceEpoch;
    }

    if (packagesName != null) {
      arguments[Argument.packagesName] = packagesName;
    }

    final result = await methodChannel.invokeMethod<Map<Object?, Object?>>(
        MethodName.appUsageData, arguments);

    return await Isolate.run(() async {
      final map = await _convertToStringDynamicMap(result);
      final response = BaseAppUsage.fromJson(map);
      if (response.status) {
        return response.data;
      } else {
        debugPrint(map.toString());
        return <AppUsage>[];
      }
    });
  }

  @override
  Future<bool> blockApps({
    List<String> packagesName = const <String>[],
    required Duration duration,
  }) async {
    final arguments = <Object?, Object?>{
      Argument.packagesName: packagesName,
      Argument.duration: duration.inMilliseconds,
    };

    return await methodChannel.invokeMethod<bool>(
            MethodName.blockApps, arguments) ??
        false;
  }

  @override
  Future<bool> get isOnBlockingApps async =>
      await methodChannel.invokeMethod<bool>(MethodName.isOnBlockingApps) ??
      false;

  @override
  Future<bool> unblockApps({
    List<String> packagesName = const <String>[],
  }) async {
    final arguments = <Object?, Object?>{
      Argument.packagesName: packagesName,
    };

    return await methodChannel.invokeMethod<bool>(
            MethodName.unblockApps, arguments) ??
        false;
  }

  @override
  Future<MonitoringAppUsage> monitoringAppUsage({
    int startHour = 0,
    int startMinute = 0,
    int endHour = 23,
    int endMinute = 59,
    UsageInterval usageInterval = UsageInterval.daily,
    int lookbackTimeMs = 10000, // Default: 10 seconds
    List<String>? packagesName,
  }) async {
    final arguments = <Object?, Object?>{
      Argument.startHour: startHour,
      Argument.startMinute: startMinute,
      Argument.endHour: endHour,
      Argument.endMinute: endMinute,
      Argument.lookbackTimeMs: lookbackTimeMs,
      Argument.interval: usageInterval.name,
    };
    if (packagesName != null) {
      arguments[Argument.packagesName] = packagesName;
    }
    final result = await methodChannel.invokeMethod<Map<Object?, Object?>>(
        MethodName.monitoringAppUsage, arguments);
    final map = await _convertToStringDynamicMap(result);
    final response = BaseMonitoringAppUsage.fromJson(map);
    return response.data;
  }

  @override
  Future<bool> configureAppMonitoringService({
    UsageInterval interval = UsageInterval.daily,
    int lookbackTimeMs = 10000, // Default: 10 seconds
  }) async {
    final result = await methodChannel.invokeMethod<bool>(
      MethodName.configureAppMonitoringService,
      {
        Argument.interval: interval.name,
        Argument.lookbackTimeMs: lookbackTimeMs,
      },
    );
    return result ?? false;
  }

  @override
  Stream<Map<String, dynamic>> streamAppUsage({
    UsageInterval usageInterval = UsageInterval.daily,
    int lookbackTimeMs = 10000,
  }) {
    // First configure the service with the specified parameters
    configureAppMonitoringService(
      interval: usageInterval,
      lookbackTimeMs: lookbackTimeMs,
    );

    // Return the stream from the event channel
    return eventChannel.receiveBroadcastStream({
      Argument.interval: usageInterval.name,
      Argument.lookbackTimeMs: lookbackTimeMs,
    }).map((dynamic event) {
      // Convert the event data to the expected type
      if (event is Map) {
        return _convertNestedMap(event);
      } else {
        throw PlatformException(
          code: 'INVALID_EVENT',
          message: 'Invalid event format received from native platform',
        );
      }
    });
  }

  /// Helper method to convert the result from native code to the expected Dart type
  Future<Map<String, dynamic>> _convertToStringDynamicMap(
      Map<Object?, Object?>? result) async {
    if (result == null) {
      final error = result?['error'];
      throw Exception(error);
    }

    return await Isolate.run(() {
      final Map<String, dynamic> convertedMap = {};

      result.forEach((key, value) {
        if (key is String) {
          if (value is Map) {
            // Recursively convert nested maps
            convertedMap[key] = _convertNestedMap(value);
          } else if (value is List) {
            // Convert lists
            convertedMap[key] = _convertList(value);
          } else {
            // Direct assignment for primitive types
            convertedMap[key] = value;
          }
        }
      });

      return convertedMap;
    });
  }

  /// Helper method to convert nested maps
  dynamic _convertNestedMap(Map<dynamic, dynamic> map) {
    final convertedMap = <String, dynamic>{};

    map.forEach((key, value) {
      if (key is String) {
        if (value is Map) {
          convertedMap[key] = _convertNestedMap(value);
        } else if (value is List) {
          convertedMap[key] = _convertList(value);
        } else {
          convertedMap[key] = value;
        }
      }
    });

    return convertedMap;
  }

  /// Helper method to convert lists
  List<dynamic> _convertList(List<dynamic> list) {
    return list.map((item) {
      if (item is Map) {
        return _convertNestedMap(item);
      } else if (item is List) {
        return _convertList(item);
      } else {
        return item;
      }
    }).toList();
  }
}
