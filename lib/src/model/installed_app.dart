import 'dart:convert';

import 'package:flutter/foundation.dart';

import 'app_category.dart';

class BaseInstalledApp {
  final bool status;
  final List<InstalledApp> data;
  final String? error;

  BaseInstalledApp({
    required this.status,
    required this.data,
    this.error,
  });

  factory BaseInstalledApp.fromJson(Map<String, dynamic> json) =>
      BaseInstalledApp(
        status: json["status"],
        data: List<InstalledApp>.from(
            json["data"].map((x) => InstalledApp.fromJson(x))),
        error: json["error"],
      );

  Map<String, dynamic> toJson() => {
        "status": status,
        "data": data.map((x) => x.toJson()).toList(),
        "error": error,
      };
}

class InstalledApp {
  final String? appName;
  final bool enabled;
  final AppCategory category;
  final String? versionName;
  final int? versionCode;
  final Uint8List? iconInBytes;

  InstalledApp({
    this.appName,
    this.enabled = false,
    this.category = AppCategory.undefined,
    this.versionName,
    this.versionCode,
    this.iconInBytes,
  });

  factory InstalledApp.fromJson(Map<String, dynamic> json) => InstalledApp(
        appName: json["appName"],
        enabled: json["enabled"],
        category: AppCategory.values.firstWhere(
          (element) => element.name == json["category"],
          orElse: () => AppCategory.undefined,
        ),
        versionName: json["versionName"],
        versionCode: json["versionCode"],
        iconInBytes: (json["appIcon"] != null)
            ? base64Decode(json["appIcon"].replaceAll("\n", ""))
            : null,
      );

  Map<String, dynamic> toJson() => {
        "appName": appName,
        "enabled": enabled,
        "category": category,
        "versionName": versionName,
        "versionCode": versionCode,
        "appIcon": iconInBytes,
      };
}
