import 'dart:convert';

import 'package:flutter/foundation.dart';

import 'app_category.dart';

class BaseAppUsage {
  final bool status;
  final List<AppUsage> data;
  final String? error;

  BaseAppUsage({
    required this.status,
    required this.data,
    this.error,
  });

  factory BaseAppUsage.fromJson(Map<String, dynamic> json) => BaseAppUsage(
        status: json["status"],
        data:
            List<AppUsage>.from(json["data"].map((x) => AppUsage.fromJson(x))),
        error: json["error"],
      );

  Map<String, dynamic> toJson() => {
        "status": status,
        "data": data.map((x) => x.toJson()).toList(),
        "error": error,
      };
}

class AppUsage {
  final String? packageName;
  final DateTime? lastTimeUsed;
  final DateTime? firstTime;
  final DateTime? lastTime;
  final Duration? usageTime;


  AppUsage({
   
    this.packageName,
    this.lastTimeUsed,
    this.firstTime,
    this.lastTime,
    this.usageTime,
  });

  factory AppUsage.fromJson(Map<String, dynamic> json) => AppUsage(
        packageName: json["packageName"],
        lastTimeUsed: (json["lastTimeUsed"] != null)
            ? DateTime.fromMillisecondsSinceEpoch(json["lastTimeUsed"]!)
            : null,
        firstTime: (json["firstTime"] != null)
            ? DateTime.fromMillisecondsSinceEpoch(json["firstTime"]!)
            : null,
        lastTime: (json["lastTime"] != null)
            ? DateTime.fromMillisecondsSinceEpoch(json["lastTime"]!)
            : null,
        usageTime: (json["usageTime"] != null)
            ? Duration(milliseconds: json["usageTime"]!)
            : null,
      );

  Map<String, dynamic> toJson() => { 
        "packageName": packageName,
        "lastTimeUsed": lastTimeUsed?.toIso8601String(),
        "firstTime": firstTime?.toIso8601String(),
        "lastTime": lastTime?.toIso8601String(),
        "usageTime": (usageTime != null) ? usageTime!.inMilliseconds : null,
      };
}
