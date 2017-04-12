package cordova.plugin.tgsiwifi;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by alagrazon on 3/29/2017.
 */

public class ChatManager implements Runnable {



    private Socket socket = null;
    private Handler handler;
    private InputStream iStream;
    private OutputStream oStream;
    private static final String TAG = ChatManager.class.getSimpleName();


    public ChatManager(Socket mSocket, Handler handler) {
        this.socket = mSocket;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {

            iStream = socket.getInputStream();
            oStream = socket.getOutputStream();
            byte[] buffer = new byte[1024];
            int bytes;
            handler.obtainMessage(SystemConstant.MY_HANDLE, this)
                    .sendToTarget();

            while (true) {
                try {
                    // Read from the InputStream
                    bytes = iStream.read(buffer);
                    if (bytes == -1) {
                        break;
                    }

                    // Send the obtained bytes to the UI Activity
                    Log.d(TAG, "Rec:" + String.valueOf(buffer));
                    handler.obtainMessage(SystemConstant.MESSAGE_READ,
                            bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
				mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean write(byte[] buffer) {
        boolean isSuccess = false;
        try {
            oStream.write(buffer);
            isSuccess = true;
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
        return isSuccess;
    }
}
