package cordova.plugin.tgsiwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;


import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class echoes a string called from JavaScript.
 */
public class TgsiWifiPlugin extends CordovaPlugin implements WifiP2pManager.ConnectionInfoListener, Handler.Callback {

    private final IntentFilter intentFilter = new IntentFilter();
    private Context context;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private P2pBroadcastReceiver receiver = null;
    private DnsSdTxtRecordListener dnsTxtListener;
    private WifiP2pDnsSdServiceRequest serviceRequest;

    private static final String TAG = TgsiWifiPlugin.class.getSimpleName();
    private final List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();

    private Handler chatHandler;
    private ChatManager chatManager;
    private Timer timer;



    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        //  Activity context
        context = cordova.getActivity().getApplicationContext();

        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(context, context.getMainLooper(), null);

        LOG.d("TAG", "ENABLED");
        chatHandler = new Handler(this);
        //enabledWifi();
        //registerReceiver();
        //timer = new Timer();
    }

    private void enabledWifi(final String wifiFlag) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        boolean flag = Boolean.parseBoolean(wifiFlag);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(flag);
        } else {
			wifiManager.setWifiEnabled(flag);
		}
    }


    private void registerReceiver() {
        LOG.d("TAG", "registerReceiver");
        receiver = new P2pBroadcastReceiver(manager, channel, this);
        cordova.getActivity().registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        LOG.d("TAG", "onResume");
        receiver = new P2pBroadcastReceiver(manager, channel, this);
        cordova.getActivity().registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        cordova.getActivity().unregisterReceiver(receiver);
    }

    @Override
    public void onStop() {
        removeGroup();
        super.onStop();
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            LOG.d("B", action);
            String message = args.getString(0);
            LOG.d("C", action);
            this.coolMethod(message, callbackContext);
            LOG.d("D", action);
        } else if (action.equals("registerService")) {
            String peerName = args.getString(0);
            this.registerService(peerName, callbackContext);
        } else if (action.equals("startSearching")) {
            startSearching(callbackContext);
        } else if (action.equals("getPeerList")) {
            this.getPeerList(callbackContext);
        } else if (action.equals("connect")) {
            String deviceAdd = args.getString(0);
            this.connect(deviceAdd, callbackContext);
        } else if (action.equals("sendMessage")) {
            String message = args.getString(0);
            this.sendMessage(message, callbackContext);
        } else if (action.equals("stopServices")) {
            this.stopServices(callbackContext);
        } else if (action.equals("enabledWifi")){
			String flag = args.getString(0);
			this.enabledWifi(flag);
		}
		else {
            return false;
        }
        return true;
    }


    private void registerService(final String peerName, final CallbackContext callbackContext) {

		timer = new Timer();
		registerReceiver();
        manager.clearLocalServices(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                register(peerName, callbackContext);
            }

            @Override
            public void onFailure(int arg0) {
                JSONObject response = new JSONObject();
                try {
                    response.put(SystemConstant.RESPONSE_MESSAGE, "Error service registration");
                    LOG.d(TAG, "registerService-error: Error in clearing of local service");
                    callbackContext.error(response);
                } catch (JSONException exception) {
                    LOG.d(TAG, "registerService-error: " + exception.getMessage());
                }
            }
        });

    }


    private void register(String peerName, final CallbackContext callbackContext) {

        //  Create a string map containing information about your service.
        Map record = new HashMap();
        record.put("peerName", peerName);
        record.put("available", "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance(SystemConstant.SERVICE_NAME, SystemConstant.SERVICE_TYPE, record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        manager.addLocalService(channel, serviceInfo, new ActionListener() {
            @Override
            public void onSuccess() {
                JSONObject response = new JSONObject();
                try {
                    response.put(SystemConstant.RESPONSE_MESSAGE, "Success");
                    callbackContext.success(response);
                } catch (JSONException exception) {
                    LOG.d(TAG, "addLocalService-success: " + exception.getMessage());
                }
            }

            @Override
            public void onFailure(int arg0) {
                JSONObject response = new JSONObject();
                try {
                    response.put(SystemConstant.RESPONSE_MESSAGE, "Error service registration");
                    callbackContext.error(response);

                } catch (JSONException exception) {
                    LOG.d(TAG, "addLocalService-error: " + exception.getMessage());
                }
            }
        });
    }




    private void startSearching(final CallbackContext callbackContext) {

        search();
        callbackContext.success();
    }


    private void search () {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                cordova.getThreadPool().execute(
                        new Runnable() {
                            public void run() {
                                discoverService();
                            }
                        }
                );

                cordova.getThreadPool().execute(
                        new Runnable() {
                            public void run() {
                                discoverPeers();
                            }
                        }
                );
            }
        };

        timer.scheduleAtFixedRate(timerTask, 0, 60 * 1000 );
    }

    private void stopTimer () {
        if (timer != null) {
            timer.cancel();
        }
    }


    private void discoverService() {

        prepareDiscoveryService();
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        manager.removeServiceRequest(channel, serviceRequest,
                new ActionListener() {
                    @Override
                    public void onSuccess() {
                        LOG.d(TAG, "removeServiceRequest: " + "SUCCESS");
                        manager.addServiceRequest(channel,
                                serviceRequest,
                                new ActionListener() {
                                    @Override
                                    public void onSuccess() {
                                        LOG.d(TAG, "addServiceRequest: " + "SUCCESS");
                                        manager.discoverServices(channel, new ActionListener() {

                                            @Override
                                            public void onSuccess() {
                                                LOG.d(TAG, "discoverServices: " + "SUCCESS");
                                            }

                                            @Override
                                            public void onFailure(int code) {
                                                LOG.d(TAG, "discoverServices: " + "ERROR");
                                                if (code == WifiP2pManager.P2P_UNSUPPORTED) {
                                                    Log.d(TAG, "P2P isn't supported on this device.");
                                                } else if(code == WifiP2pManager.BUSY) {
                                                    Log.d(TAG, "Service is busy.");
                                                } else if(code == WifiP2pManager.ERROR) {
                                                    Log.d(TAG, "Internal Error.");
                                                }

                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(int code) {
                                        LOG.d(TAG, "addServiceRequest: " + "ERROR");
                                    }
                                });
                    }

                    @Override
                    public void onFailure(int code) {
                        LOG.d(TAG, "removeServiceRequest: " + "ERROR");
                    }
                });

    }

    private void prepareDiscoveryService() {
        dnsTxtListener = new TxtListener();

        DnsSdServiceResponseListener servListener = new DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice resourceType) {
                HashMap<String, String> peerList;
                peerList = ((TxtListener) dnsTxtListener).getPeerList();
                LOG.d(TAG, "discoverService: " + peerList.size());
                if (instanceName.equalsIgnoreCase(SystemConstant.SERVICE_NAME)) {
                    resourceType.deviceName = peerList
                            .containsKey(resourceType.deviceAddress) ? peerList
                            .get(resourceType.deviceAddress) : resourceType.deviceName;
                    peers.add(resourceType);
                }
            }
        };
        manager.setDnsSdResponseListeners(channel, servListener, dnsTxtListener);
    }


    private void discoverPeers() {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                LOG.d(TAG, "discoverPeers: " + "SUCCESS");
            }

            @Override
            public void onFailure(int reasonCode) {
                LOG.d(TAG, "discoverPeers: " + "ERROR");
            }
        });


    }


    private void getPeerList(final CallbackContext callbackContext) {
        List<WifiP2pDevice> servicePeer = getServicePeerList();
        List<WifiP2pDevice> peerList = receiver.getPeerList();
//        List<Peer> refreshList = new ArrayList<Peer>();
        JSONArray refreshList = new JSONArray();
        String serviceDeviceAdd;
        String deviceAdd;
        Peer peer;
        try {
            for (WifiP2pDevice device : peerList) {
                deviceAdd = device.deviceAddress;
                for (WifiP2pDevice serviceDevice : servicePeer) {
                    serviceDeviceAdd = serviceDevice.deviceAddress;
                    if (serviceDeviceAdd.equals(deviceAdd)) {
                        LOG.d(TAG, "getPeerList: " + device.deviceName + " : " + device.deviceAddress);
                        JSONObject obj = new JSONObject();
                        obj.put("deviceName", serviceDevice.deviceName);
                        obj.put("deviceAdd", serviceDevice.deviceAddress);
                        refreshList.put(obj);
                        break;
                    }
                }
            }
        } catch (JSONException exception) {
            LOG.d(TAG, "discoverPeers: " + exception.getMessage());
        }

        JSONArray jsonArray = new JSONArray(servicePeer);
        LOG.d(TAG, "getPeerList: " + jsonArray.toString());

        JSONArray jsonArray1 = new JSONArray(peerList);
        LOG.d(TAG, "getPeerList: " + jsonArray1.toString());
        LOG.d(TAG, "getPeerList: " + refreshList.toString());

        callbackContext.success(refreshList);
    }


    private List<WifiP2pDevice> getServicePeerList() {
        return peers;

    }


    private void connect(String deviceAdd, final CallbackContext callbackContext) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = deviceAdd;
        config.wps.setup = WpsInfo.PBC;

        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
                JSONObject response = new JSONObject();
                try {
                    response.put(SystemConstant.RESPONSE_MESSAGE, "SUCCESS");
                    callbackContext.success(response);

                } catch (JSONException exception) {
                    LOG.d(TAG, "connect-success: " + exception.getMessage());
                }
            }

            @Override
            public void onFailure(int reason) {
                JSONObject response = new JSONObject();
                try {
                    response.put(SystemConstant.RESPONSE_MESSAGE, "Error connecting to peer.");
                    callbackContext.error(response);

                } catch (JSONException exception) {
                    LOG.d(TAG, "connect-error: " + exception.getMessage());
                }
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {

        Thread chatThread = null;


        // After the group negotiation, we can determine the group owner
        // (server).
        if (info.groupFormed && info.isGroupOwner) {
            try {
                chatThread = new GroupOwnerSocketHandler(chatHandler);
                chatThread.start();
                cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadUrl("javascript:connectToChat()");
                    }
                });
            } catch (IOException e) {
                Log.d(TAG,
                        "Failed to create a server thread - " + e.getMessage());
                return;
            }
        } else if (info.groupFormed) {
            chatThread = new ClientSocketHandler(chatHandler,
                    info.groupOwnerAddress);
            chatThread.start();
            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    webView.loadUrl("javascript:collaborate()");
                }
            });
        }
		
		stopTimer();
    }

    private void sendMessage(final String message, final CallbackContext callbackContext) {
        chatManager.write(message.getBytes());
    }

    private void receiveMessage(final String message) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl("javascript:receiveMessage('" + message + "')");
            }
        });
    }

    private void stopServices(final CallbackContext callbackContext) {
        cordova.getActivity().unregisterReceiver(receiver);
        removeGroup();
        callbackContext.success();
        stopTimer();
    }

    private void removeGroup() {
        if (manager != null && channel != null) {
            manager.removeGroup(channel, new ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                }

                @Override
                public void onSuccess() {
                }

            });
        }

    }


    private void coolMethod(final String message, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String sample = "From Java";
                webView.loadUrl("javascript:myFun('" + sample + "')");
            }
        });


//        LOG.d("A", "a");
//        cordova.getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(cordova.getActivity().getApplicationContext(), message, Toast.LENGTH_LONG).show();
//            }
//        });
//        try {
//            JSONObject entry = new JSONObject();
//            entry.put("ok", "Success");
//            callbackContext.success(entry);
//            LOG.d("A", "b");
//
//
//        } catch (JSONException exception) {
//
//        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case SystemConstant.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(TAG, readMessage);
                receiveMessage(readMessage);
                break;
            case SystemConstant.MY_HANDLE:
                chatManager = (ChatManager) msg.obj;
        }
        return true;
    }
}
