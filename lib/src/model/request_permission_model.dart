class RequestPermissionModel {
  final bool status;
  final String? error;

  RequestPermissionModel({
    this.status = false,
    this.error,
  });

  factory RequestPermissionModel.fromJson(Map<String, dynamic> json) =>
      RequestPermissionModel(
        status: json["status"] ?? false,
        error: json["error"],
      );

  Map<String, dynamic> toJson() => {
        "status": status,
        "error": error,
      };
}
