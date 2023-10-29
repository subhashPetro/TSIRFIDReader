// ignore_for_file: use_build_context_synchronously

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:tsl_rfid_plugin/tsi_rfid_sdk_plugin.dart';

void main() {
  runApp(const MaterialApp(home: MyApp()));
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  List<dynamic> _device = [];

  Map<String,dynamic>? currentConnection;
  String? batteryLevel;

  bool _working = false;
  bool _connecting = false;

  @override
  void initState() {
     channelTest();
    super.initState();
  }

  void setListener() {
    if (currentConnection != null) {
      // _rfiidReaderPlugin.readDataStream(currentConnection!)?.listen((event) {
      //   print("listen event - $event");
      // });
    }
  }

  Future<void> channelTest() async {

    await TSIRfidSdkPlugin.getInstance.init();
    final res = await TSIRfidSdkPlugin.getInstance.getPairedDevices();
    print("devices - $res");
    if (res != null) {
      setState(() {
        _device = res;
      });
    } else {
      _device = [];
    }
  }

  // Platform messages are asynchronous, so we initialize in an async method.

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Plugin example app'),
      ),
      body: _working
          ? const Center(
        child: CircularProgressIndicator(),
      )
          : Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: 20,
          vertical: 20,
        ),
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.only(
                bottom: 20,
              ),
              child:
              Text('Running on device version : $_platformVersion\n'),
            ),
            const Padding(
              padding: EdgeInsets.only(
                bottom: 20,
              ),
              child: Text('Available devices'),
            ),
            Padding(
              padding: const EdgeInsets.only(
                bottom: 20,
              ),
              child: Column(
                children: _device
                    .map((e) => ListTile(
                  onTap: () async {
                    final res =
                    await TSIRfidSdkPlugin.getInstance.connect(
                      hostName: e['deviceMacId'],
                    );

                    print("connected - $res");
                    if (res ?? false) {
                      setState(() {
                        currentConnection = e;
                      });
                    } else {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text(
                              "Failed to connect device, please make sure your device is ready connect"),
                        ),
                      );
                    }
                  },
                  title: Text(
                    e.toString(),
                    style: Theme.of(context)
                        .textTheme
                        .displaySmall
                        ?.copyWith(
                      color: Colors.lightBlue,
                      fontSize: 16,
                    ),
                  ),
                ))
                    .toList(),
              ),
            ),
            Visibility(
              visible: currentConnection != null,
              child: Padding(
                padding: const EdgeInsets.only(
                  bottom: 20,
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Connected device - $currentConnection',
                      style: Theme.of(context)
                          .textTheme
                          .bodyMedium
                          ?.copyWith(
                        color: Colors.red,
                      ),
                    ),
                    TextButton(
                      onPressed: () async {
                        if (currentConnection != null) {
                          await TSIRfidSdkPlugin.getInstance.disconnect();
                        }
                        setState(() {
                          currentConnection = null;
                        });
                      },
                      child: const Text("Disconnect"),
                    )
                  ],
                ),
              ),
            ),
            Visibility(
              visible: batteryLevel != null,
              child: Padding(
                padding: const EdgeInsets.only(
                  bottom: 20,
                ),
                child: Text(
                  'Remaining charging - $batteryLevel',
                  style: Theme.of(context)
                      .textTheme
                      .bodyMedium
                      ?.copyWith(color: Colors.red),
                ),
              ),
            ),
            if (currentConnection != null)
              Padding(
                padding: const EdgeInsets.symmetric(
                  vertical: 20,
                ),
                child: Row(
                  children: [
                    TextButton(
                      onPressed: () {
                        TSIRfidSdkPlugin.getInstance.startRFID(
                          hostName: currentConnection!['deviceMacId'],
                        );
                      },
                      child: const Text("Start"),
                    ),
                    TextButton(
                      onPressed: () async {
                        final result =
                        await TSIRfidSdkPlugin.getInstance.stopRFID(
                          hostName: currentConnection!['deviceMacId'],
                        );
                        print("read result - $result");
                      },
                      child: const Text("Stop"),
                    ),
                  ],
                ),
              ),
            // currentConnection != null
            //     ? StreamBuilder(
            //   stream: _rfiidReaderPlugin
            //       .readDataStream(currentConnection!),
            //   builder: (_, snapshot) {
            //     if (snapshot.connectionState ==
            //         ConnectionState.waiting) {
            //       return const Text('waiting');
            //     } else {
            //       return Text(snapshot.data.toString() ?? '');
            //     }
            //   },
            // )
            //     : const SizedBox()
          ],
        ),
      ),
    );
  }
}