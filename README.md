# Screen Time Plugin

A Flutter plugin for monitoring app usage in real-time on Android devices. This plugin provides detailed information about the currently active application and app usage statistics.

## Features

- Get detailed information about the currently active app
- Monitor app usage in real-time
- Configure monitoring intervals and lookback times
- Access app usage statistics

## Installation

Add the plugin to your `pubspec.yaml` file:

```yaml
dependencies:
  screen_time: ^1.0.0
```

## Setup

### Android

#### 1. Permission Information

The core permissions required by this plugin are already declared in the plugin's AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.GET_TASKS" />
```

You don't need to add these permissions to your app's AndroidManifest.xml as they're automatically merged during the build process.

#### 2. Implement the Accessibility Service

To monitor app usage in real-time, you need to implement an accessibility service in your app. Follow these steps:

##### a. Create accessibility service configuration

Create a file at `android/app/src/main/res/xml/accessibility_service_config.xml` with the following content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/accessibility_service_description"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="50"
    android:canRetrieveWindowContent="true"
    android:canPerformGestures="false" />
```

##### b. Add service description

Add the following string to `android/app/src/main/res/values/strings.xml`:

```xml
<string name="accessibility_service_description">This service monitors app usage in real-time and provides detailed information about the apps you use.</string>
```

##### c. Register the service

Add the following service declaration to your app's `AndroidManifest.xml` within the `<application>` tag:

```xml
<service
    android:name="com.solusibejo.screen_time.service.AppMonitoringService"
    android:label="App Monitoring Service"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="false">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

## Usage

### Request Permissions

Before using the plugin, request the necessary permissions:

```dart
final ScreenTime screenTime = ScreenTime();
final permissionStatus = await screenTime.requestPermission();

if (permissionStatus.status) {
  // Permission granted, proceed with usage
} else {
  // Handle permission denied
}
```

### Open Accessibility Settings

To enable the accessibility service, guide the user to the system accessibility settings:

```dart
await screenTime.openAccessibilitySettings();
```

### Check if Service is Enabled

```dart
final isEnabled = await screenTime.isAppMonitoringServiceEnabled();
```

### Configure the Service

You can configure the monitoring interval and lookback time:

```dart
await screenTime.configureAppMonitoringService(
  interval: UsageInterval.daily,
  lookbackTimeMs: 10000, // 10 seconds
);
```

### Monitor App Usage

```dart
final result = await screenTime.monitoringAppUsage(
  startHour: 0,
  startMinute: 0,
  endHour: 23,
  endMinute: 59,
  usageInterval: UsageInterval.daily,
  lookbackTimeMs: 10000,
);

if (result.status) {
  // Access current foreground app information
  final currentApp = result.currentForegroundApp;
  print('Current app: ${currentApp?["appName"]}');
}
```

### Get App Usage Data

```dart
final DateTime now = DateTime.now();
final DateTime yesterday = now.subtract(const Duration(days: 1));

final List<AppUsage> usageData = await screenTime.appUsageData(
  startTime: yesterday,
  endTime: now,
  usageInterval: UsageInterval.daily,
);

for (final app in usageData) {
  print('${app.appName}: ${app.usageTime} ms');
}
```

## Example

See the [example](https://github.com/chandrabezzo/screen_time/tree/main/example) directory for a complete sample app demonstrating how to use this plugin.

## Customization

You can customize the accessibility service by modifying the `accessibility_service_config.xml` file. For more advanced customization, refer to the [Android Accessibility Service documentation](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService).

## License

This project is licensed under the MIT License - see the LICENSE file for details.
