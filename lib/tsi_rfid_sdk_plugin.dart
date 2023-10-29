import 'dart:async';
import 'package:rfiid_reader/permission_manager.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:tsl_rfid_plugin/tsi_event_handler.dart';
import 'package:zebra_print_plugin/zebra_print_plugin.dart';

class TSIRfidSdkPlugin {
  static const MethodChannel _channel =
      MethodChannel('com.example.tsl_rfid_plugin/plugin');
  static const EventChannel _eventChannel =
      EventChannel('com.example.tsl_rfid_plugin/event_channel');
  static TSIRFIDEventHandler? _handler;

  static Future<String?> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  bool _initialize =false;
  bool _hasPermission =false;

  TSIRfidSdkPlugin._private();

  static TSIRfidSdkPlugin get getInstance => TSIRfidSdkPlugin._private();

  ///
  Future<dynamic> init() async {
   bool have = await BluetoothPermissionManager.checkAndRequestPermissions();
   var res = await _channel.invokeMethod('init');
   _hasPermission = have;
   _initialize = true;
  }

  ///
  Future<dynamic> onRead() async {
    return _channel.invokeMethod('startRead');
  }

  Future<dynamic> write() async {
    return _channel.invokeMethod('write');
  }

  ///connect device
  Future<dynamic> connect({required String hostName}) async {
  try {
    var result = await _channel.invokeMethod(
      'connect', 
      {"hostName": hostName},
    );
    return result;
  } catch (e) {
    if (kDebugMode) {
      print("Exception on connect device");
    }
    return {
      "error": e,
      "message": "Error on connect device",
    };
  }
}


  ///disconnect device
  Future<dynamic> disconnect() async {
    return _channel.invokeMethod('disconnect');
  }


  ///start RFID
  Future<dynamic> startRFID({required String hostName}) async {
    try {
      var result = await _channel.invokeMethod(
        'startRFID',
        {"hostName": hostName},
      );
      return result;
    } catch (e) {
      if (kDebugMode) {
        print("Exception on start rfid device");
      }
      return {
        "error": e,
        "message": "Error on start rfid device",
      };
    }
  }

  ///start RFID
  Future<dynamic> stopRFID({required String hostName}) async {
    try {
      var result = await _channel.invokeMethod(
        'stopRFID',
        {"hostName": hostName},
      );
      return result;
    } catch (e) {
      if (kDebugMode) {
        print("Exception on stop rfid device");
      }
      return {
        "error": e,
        "message": "Error on stop rfid device",
      };
    }
  }

  ///get paired devices list
  Future<dynamic> getPairedDevices() async {
    try {
      var result = await _channel.invokeMethod(
        'getReadersList',
      );
      return result;
    } catch (e) {
      var data = ZebraPrintPlugin().getAllPairedZQDevices();
      if (kDebugMode) {
        print("Exception on get paired device");
      }
      return data;
    }
  }

  /// Sets the engine event handler.
  ///
  /// After setting the engine event handler, you can listen for engine events and receive the statistics of the corresponding [RtcEngine] instance.
  ///
  /// **Parameter** [handler] The event handler.
  void setEventHandler(TSIRFIDEventHandler handler) {
    _handler = handler;
  }

  StreamSubscription<dynamic>? _sink;
  Future<void> _addEventChannelHandler() async {
    _sink ??= _eventChannel.receiveBroadcastStream().listen((event) {
      final eventMap = Map<String, dynamic>.from(event);
      final eventName = eventMap['eventName'] as String;
      // final data = List<dynamic>.from(eventMap['data']);
      _handler?.process(eventName, eventMap);
    });
  }

  ///dispose device
  Future<dynamic> dispose() async {
    _sink = null;
    return _channel.invokeMethod('dispose');
  }
}
