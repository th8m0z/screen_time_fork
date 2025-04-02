class BaseMonitoringAppUsage {
  final bool status;
  final MonitoringAppUsage data;

  BaseMonitoringAppUsage({
    required this.status,
    required this.data,
  });

  factory BaseMonitoringAppUsage.fromJson(Map<String, dynamic> json) =>
      BaseMonitoringAppUsage(
        status: json["status"],
        data: MonitoringAppUsage.fromJson(json["data"]),
      );

  Map<String, dynamic> toJson() => {
        "status": status,
        "data": data.toJson(),
      };
}

class MonitoringAppUsage {
  final String startTime;
  final String endTime;
  final String frequency;
  final Map<String, dynamic>? currentForegroundApp;

  MonitoringAppUsage({
    required this.startTime,
    required this.endTime,
    required this.frequency,
    this.currentForegroundApp,
  });

  factory MonitoringAppUsage.fromJson(Map<String, dynamic> json) =>
      MonitoringAppUsage(
        startTime: json["startTime"],
        endTime: json["endTime"],
        frequency: json["frequency"],
        currentForegroundApp: json["currentForegroundApp"] != null
            ? Map<String, dynamic>.from(json["currentForegroundApp"])
            : null,
      );

  Map<String, dynamic> toJson() => {
        "startTime": startTime,
        "endTime": endTime,
        "frequency": frequency,
        "currentForegroundApp": currentForegroundApp,
      };
}
