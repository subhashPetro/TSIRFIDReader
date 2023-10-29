class PluginResponseModel {
  final String message;
  final bool success;
  final String data;

  PluginResponseModel(
      {required this.message, required this.success, required this.data});

  Map<String, dynamic> toMap() {
    return {
      "message": message,
      "success": success,
      "data": data,
    };
  }
}
