package cordova.plugin.tgsiwifi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alagrazon on 3/27/2017.
 */

public class P2pBroadcastReceiver extends BroadcastReceiver {
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private TgsiWifiPlugin tgsiWifiPlugin;
    private static final String TAG = P2pBroadcastReceiver.class.getSimpleName();

    private PeerListener peerListListener = new PeerListener();
    private List<WifiP2pDevice> peerList = new ArrayList<WifiP2pDevice>();

    /**
     * @param manager  WifiP2pManager system service
     * @param channel  Wifi p2p channel
     */
    public P2pBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                TgsiWifiPlugin tgsiWifiPlugin) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.tgsiWifiPlugin = tgsiWifiPlugin;
    }

    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(P2pBroadcastReceiver.TAG, action);
        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                manager.requestConnectionInfo(channel,
                        (WifiP2pManager.ConnectionInfoListener) tgsiWifiPlugin);
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
                .equals(action)) {

            WifiP2pDevice device = (WifiP2pDevice) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.d(TAG, "Device status -" + device.status);

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if (manager != null) {
                manager.requestPeers(channel, peerListListener);
            }

        }
    }

    public List<WifiP2pDevice> getPeerList() {
        peerList = peerListListener.getPeers();
        return peerList;
    }


}


