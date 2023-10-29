package com.example.tsl_rfid_plugin;
import android.content.Context;

import androidx.annotation.NonNull;

import com.example.tsl_rfid_plugin.RFID.inventory.RFIDUtility;

import java.util.List;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** TslRfidPlugin */
public class TslRfidPlugin implements FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {

  private MethodChannel channel;
  private Context context;

  private EventChannel eventChannel;

  private EventChannel.EventSink sink = null;

  private final String TAG = "TSIRfidSdkPlugin";

  private RFIDUtility rfidUtility;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "com.example.tsl_rfid_plugin/plugin");
    context = flutterPluginBinding.getApplicationContext();
    channel.setMethodCallHandler(this);
    eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "com.example.tsl_rfid_plugin/event_channel");
    eventChannel.setStreamHandler(this);
    rfidUtility = new RFIDUtility(context);
    rfidUtility.initRfidModel();
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "getPlatformVersion":
        result.success("Android " + android.os.Build.VERSION.RELEASE);
        break;
      case "connect":
        String hostName = call.argument("hostName");
        PluginResponseModel responseModel = rfidUtility.connectDisconnect(
                hostName,true,false );
        result.success(responseModel.toMap());
        break;
      case "getReadersList":
        try {
          List deviceList = rfidUtility.getDevicesData();
          result.success(deviceList);
        }catch (Exception e){
          List deviceList = rfidUtility.getDevicesData();
          result.success(deviceList);
          Log.w(TAG,e.toString());
        }
        break;
      case "startRFID":
        PluginResponseModel startRes = rfidUtility.startRFID();
        result.success(startRes.toMap());
        break;
      case "stopRFID":
        PluginResponseModel responseModel1 = rfidUtility.stopRFID();
        result.success(responseModel1.toMap());
        break;

      case "disconnect":
        PluginResponseModel disconnect = rfidUtility.connectDisconnect(
                "",false,false );
        break;
      default:
        result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
    eventChannel.setStreamHandler(null);
  }


  @Override
  public void onListen(Object arguments, EventChannel.EventSink events) {
    Log.w(TAG, "adding listener");
    sink = events;

  }

  @Override
  public void onCancel(Object arguments) {
    Log.w(TAG, "cancelling listener");
    sink = null;
  }

}


