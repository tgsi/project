package cordova.plugin.tgsiwifi;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by alagrazon on 3/28/2017.
 */

public class PeerListener implements WifiP2pManager.PeerListListener {

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {

        Collection<WifiP2pDevice> refreshedPeers = peerList.getDeviceList();
        if (!refreshedPeers.equals(peers)) {
            peers.clear();
            peers.addAll(refreshedPeers);
        }
    }

    public List<WifiP2pDevice> getPeers() {
        return peers;
    }

    public void setPeers(List<WifiP2pDevice> peers) {
        this.peers = peers;
    }
}
