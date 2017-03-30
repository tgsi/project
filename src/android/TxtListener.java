package cordova.plugin.tgsiwifi;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by alagrazon on 3/28/2017.
 */

public class TxtListener implements DnsSdTxtRecordListener{

    private HashMap<String, String> peerList = new HashMap<String, String>();

    @Override
    public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
        this.peerList.put(srcDevice.deviceAddress, txtRecordMap.get("peerName"));
    }

    public HashMap<String, String> getPeerList() {
        return this.peerList;
    }


}
