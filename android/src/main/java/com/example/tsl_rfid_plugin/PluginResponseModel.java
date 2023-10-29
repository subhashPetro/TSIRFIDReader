package com.example.tsl_rfid_plugin;


import java.util.HashMap;
import java.util.Map;

public class PluginResponseModel{
    final String message;
    final boolean success;
    final String data;

    public PluginResponseModel(String message, boolean success, String data) {
        this.message = message;
        this.success = success;
        this.data = data;
    }

    Map toMap(){
        Map<String, String> codes = new HashMap<String, String>();
        codes.put("message",message);
        codes.put("success", String.valueOf(success));
        codes.put("data",data);
        return codes;
    }
}
