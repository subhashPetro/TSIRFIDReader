package com.example.tsl_rfid_plugin.RFID.inventory;
import static com.uk.tsl.rfid.DeviceListActivity.EXTRA_DEVICE_ACTION;
import static com.uk.tsl.rfid.DeviceListActivity.EXTRA_DEVICE_INDEX;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.util.Log;
import com.example.tsl_rfid_plugin.ModelBase;
import com.example.tsl_rfid_plugin.PluginResponseModel;
import com.example.tsl_rfid_plugin.WeakHandler;
import com.uk.tsl.rfid.DeviceListActivity;
import com.uk.tsl.rfid.asciiprotocol.AsciiCommander;
import com.uk.tsl.rfid.asciiprotocol.device.ConnectionState;
import com.uk.tsl.rfid.asciiprotocol.device.IAsciiTransport;
import com.uk.tsl.rfid.asciiprotocol.device.ObservableReaderList;
import com.uk.tsl.rfid.asciiprotocol.device.Reader;
import com.uk.tsl.rfid.asciiprotocol.device.ReaderManager;
import com.uk.tsl.rfid.asciiprotocol.device.TransportType;
import com.uk.tsl.rfid.asciiprotocol.responders.LoggerResponder;
import com.uk.tsl.rfid.devicelist.BuildConfig;
import com.uk.tsl.utils.Observable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RFIDUtility {

    final Context context;
    private static final String TAG = "RFIDUtility";
    private static final boolean D = BuildConfig.DEBUG;
    private Reader mReader = null;
    private ObservableReaderList mReaders;
    private Reader mLastUserDisconnectedReader = null;
    private boolean mIsSelectingReader = false;

    private InventoryModel mModel;

    public RFIDUtility(Context context) {
        this.context = context;
    }

    public void initRfidModel(){
        // Ensure the shared instance of AsciiCommander exists
        AsciiCommander.createSharedInstance(context);

        final AsciiCommander commander = getCommander();

        // Ensure that all existing responders are removed
        commander.clearResponders();

        // Add the LoggerResponder - this simply echoes all lines received from the reader to the log
        // and passes the line onto the next responder
        // This is ADDED FIRST so that no other responder can consume received lines before they are logged.
        commander.addResponder(new LoggerResponder());

        // Add responder to enable the synchronous commands
        commander.addSynchronousResponder();

        // Configure the ReaderManager when necessary
        ReaderManager.create(context);
        ReaderManager.sharedInstance().updateList();
        // Add observers for changes
        ReaderManager.sharedInstance().getReaderList().readerAddedEvent().addObserver(mAddedObserver);
        ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().addObserver(mUpdatedObserver);
        ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().addObserver(mRemovedObserver);

        mModel = new InventoryModel();
        mModel.setCommander(getCommander());
        // The handler for model messages
        GenericHandler mGenericModelHandler = new GenericHandler(this);
        mModel.setHandler(mGenericModelHandler);
        AutoSelectReader(true);

    }

    public List<Map> getDevicesData(){
       ArrayList<Reader> deviceList = ReaderManager.sharedInstance().getReaderList().list();
       Log.w("Fetch Devices length -", String.valueOf(deviceList.size()));
       List<Map> responseData = new ArrayList<>();
       if (deviceList.isEmpty()) {
           return new ArrayList<>();
       }
       for (int i =0; i<deviceList.size(); i++){
           Map deviceData = new HashMap<>();
           deviceData.put("displayName",deviceList.get(i).getDisplayName());
           deviceData.put("serialNumber",deviceList.get(i).getSerialNumber());
           deviceData.put("deviceMacId",deviceList.get(i).getDisplayInfoLine());
           deviceData.put("deviceProperties",deviceList.get(i).getDeviceProperties());
//           deviceData.put("deviceRegion",deviceList.get(i).getDeviceProperties().getRegion());
//           deviceData.put("maximumCarrierPower",deviceList.get(i).getDeviceProperties().getMaximumCarrierPower());
//           deviceData.put("minimumCarrierPower",deviceList.get(i).getDeviceProperties().getMinimumCarrierPower());
           responseData.add(deviceData);
       }
       return responseData;
    }

    private void onConnection(){

    }

    public PluginResponseModel connectDisconnect(
            String macId,
            boolean connect, boolean autoConnect) {
        try {
            if (connect && !macId.isEmpty()) {
                //Log.w("Array List - ",ReaderManager.sharedInstance().getReaderList().list())
                ArrayList<Reader> readerArrayList = ReaderManager.sharedInstance().getReaderList().list();
                int readerIndex = -1;
                for (int i = 0; i <= readerArrayList.size(); i++) {
                    Reader reader = readerArrayList.get(i);
                    String readerMacId = reader.getDisplayInfoLine();
                    if (readerMacId.contains(macId)) {
                        readerIndex = i;
                    }
                }
                if (readerIndex != -1) {
                    connect(readerIndex);
                }
            } else {
                if (mReader != null) {
                    mReader.disconnect();
                    mReader = null;
                }
            }
            return new PluginResponseModel(
                    "RFID Disconnected Successfully!",
                    true,
                    ""
            );
        }catch (Exception e){
            String msg = connect ? "Failed to connect RFID" : "RFID connected successfully!";
            return new PluginResponseModel(
                    msg,
                    false,
                    e.toString()
            );
        }
    }

    private void connect(int readerIndex){
        Reader chosenReader = ReaderManager.sharedInstance().getReaderList().list().get(readerIndex);
        // If already connected to a different reader then disconnect it
        if (mReader != null)
        {
            mReader.disconnect();
            mReader = null;
        }else{
            // Use the Reader found
            mReader = chosenReader;
            getCommander().setReader(mReader);
        }
    }


    public PluginResponseModel startRFID(){
        try {
            // Start the continuous inventory
            mModel.scanStart();

            return new PluginResponseModel(
                    "RFID Started Successfully!",
                    true,
                    ""
            );
        } catch (Exception e) {
            e.printStackTrace();
            return new PluginResponseModel(
                    "Failed to start RFID -"+e.toString(),
                    true,
                    ""
            );
        }
    }

    public PluginResponseModel stopRFID(){
        try {
            // Stop the continuous inventory
            mModel.scanStop();
            return new PluginResponseModel(
                    "RFID Stopped Successfully!",
                    true,
                    ""
            );
        } catch (Exception e) {
            e.printStackTrace();
            return new PluginResponseModel(
                    "Failed to stop RFID -"+e.toString(),
                    true,
                    ""
            );
        }
    }

    public void onDestroy()
    {

        // Remove observers for changes
        ReaderManager.sharedInstance().getReaderList().readerAddedEvent().removeObserver(mAddedObserver);
        ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().removeObserver(mUpdatedObserver);
        ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().removeObserver(mRemovedObserver);
    }

    public synchronized void onPause() {
        mModel.setEnabled(false);

        // Stop observing events from the AsciiCommander
        getCommander().stateChangedEvent().removeObserver(mConnectionStateObserver);

        // Disconnect from the reader to allow other Apps to use it
        // unless pausing when USB device attached or using the DeviceListActivity to select a Reader
        if( !mIsSelectingReader && !ReaderManager.sharedInstance().didCauseOnPause() && mReader != null )
        {
            mReader.disconnect();
        }

        ReaderManager.sharedInstance().onPause();
    }

    public synchronized void onResume() {

        mModel.setEnabled(true);

        // Observe events from the AsciiCommander
        getCommander().stateChangedEvent().addObserver(mConnectionStateObserver);

        // Remember if the pause/resume was caused by ReaderManager - this will be cleared when ReaderManager.onResume() is called
        boolean readerManagerDidCauseOnPause = ReaderManager.sharedInstance().didCauseOnPause();

        // The ReaderManager needs to know about Activity lifecycle changes
        ReaderManager.sharedInstance().onResume();

        // The Activity may start with a reader already connected (perhaps by another App)
        // Update the ReaderList which will add any unknown reader, firing events appropriately
        ReaderManager.sharedInstance().updateList();

        // Locate a Reader to use when necessary
        AutoSelectReader(!readerManagerDidCauseOnPause);

        mIsSelectingReader = false;

    }

    protected AsciiCommander getCommander()
    {
        return AsciiCommander.sharedInstance();
    }

    private static class GenericHandler extends WeakHandler<RFIDUtility>
    {
        public GenericHandler(RFIDUtility t)
        {
            super(t);
        }

        @Override
        public void handleMessage(Message msg, RFIDUtility t)
        {
            try {
                switch (msg.what) {
                    case ModelBase.BUSY_STATE_CHANGED_NOTIFICATION:
                        //TODO: process change in model busy state
                        break;

                    case ModelBase.MESSAGE_NOTIFICATION:
                        // Examine the message for prefix
                        String message = (String)msg.obj;
                        if( message.startsWith("ER:")) {
                            Log.e(TAG,message.substring(3));
                        }
                        else if( message.startsWith("BC:")) {
                            Log.e(TAG,message);
                        } else {
                            Log.e(TAG,message);
                        }
                        break;

                    default:
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG,e.toString());
            }

        }
    };

    private final Observable.Observer<String> mConnectionStateObserver = (observable, reason) ->
    {
        if (D) { Log.d(getClass().getName(), "AsciiCommander state changed - isConnected: " + getCommander().isConnected()); }

        if( getCommander().isConnected() )
        {

            mModel.resetDevice();
            mModel.updateConfiguration();
        }

    };

    Observable.Observer<Reader> mAddedObserver = new Observable.Observer<Reader>()
    {
        @Override
        public void update(Observable<? extends Reader> observable, Reader reader)
        {
            // See if this newly added Reader should be used
            AutoSelectReader(true);
        }
    };

    Observable.Observer<Reader> mUpdatedObserver = new Observable.Observer<Reader>()
    {
        @Override
        public void update(Observable<? extends Reader> observable, Reader reader)
        {
            // Is this a change to the last actively disconnected reader
            if( reader == mLastUserDisconnectedReader )
            {
                // Things have changed since it was actively disconnected so
                // treat it as new
                mLastUserDisconnectedReader = null;
            }

            // Was the current Reader disconnected i.e. the connected transport went away or disconnected
            if( reader == mReader && !reader.isConnected() )
            {
                // No longer using this reader
                mReader = null;

                // Stop using the old Reader
                getCommander().setReader(mReader);
            }
            else
            {
                // See if this updated Reader should be used
                // e.g. the Reader's USB transport connected
                AutoSelectReader(true);
            }
        }
    };

    Observable.Observer<Reader> mRemovedObserver = new Observable.Observer<Reader>()
    {
        @Override
        public void update(Observable<? extends Reader> observable, Reader reader)
        {
            // Is this a change to the last actively disconnected reader
            if( reader == mLastUserDisconnectedReader )
            {
                // Things have changed since it was actively disconnected so
                // treat it as new
                mLastUserDisconnectedReader = null;
            }

            // Was the current Reader removed
            if( reader == mReader)
            {
                mReader = null;

                // Stop using the old Reader
                getCommander().setReader(mReader);
            }
        }
    };

    private void AutoSelectReader(boolean attemptReconnect)
    {
        ObservableReaderList readerList = ReaderManager.sharedInstance().getReaderList();
        Reader usbReader = null;
        if( readerList.list().size() >= 1)
        {
            // Currently only support a single USB connected device so we can safely take the
            // first CONNECTED reader if there is one
            for (Reader reader : readerList.list())
            {
                if (reader.hasTransportOfType(TransportType.USB))
                {
                    usbReader = reader;
                    break;
                }
            }
        }

        if( mReader == null )
        {
            if( usbReader != null && usbReader != mLastUserDisconnectedReader)
            {
                // Use the Reader found, if any
                mReader = usbReader;
                getCommander().setReader(mReader);
            }
        }
        else
        {
            // If already connected to a Reader by anything other than USB then
            // switch to the USB Reader
            IAsciiTransport activeTransport = mReader.getActiveTransport();
            if ( activeTransport != null && activeTransport.type() != TransportType.USB && usbReader != null)
            {
                mReader.disconnect();

                mReader = usbReader;

                // Use the Reader found, if any
                getCommander().setReader(mReader);
            }
        }

        // Reconnect to the chosen Reader
        if( mReader != null
                && !mReader.isConnecting()
                && (mReader.getActiveTransport()== null || mReader.getActiveTransport().connectionStatus().value() == ConnectionState.DISCONNECTED))
        {
            // Attempt to reconnect on the last used transport unless the ReaderManager is cause of OnPause (USB device connecting)
            if( attemptReconnect )
            {
                if( mReader.allowMultipleTransports() || mReader.getLastTransportType() == null )
                {
                    // Reader allows multiple transports or has not yet been connected so connect to it over any available transport
                    mReader.connect();
                }
                else
                {
                    // Reader supports only a single active transport so connect to it over the transport that was last in use
                    mReader.connect(mReader.getLastTransportType());
                }
            }
        }
    }

}
