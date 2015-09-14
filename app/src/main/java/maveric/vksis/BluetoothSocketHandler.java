package maveric.vksis;

import android.bluetooth.BluetoothSocket;

/**
 * Created by Maveric on 11/09/2015.
 */
public class BluetoothSocketHandler {
    private static BluetoothSocket socket;

    public static synchronized BluetoothSocket getSocket(){
        return socket;
    }

    public static synchronized void setSocket(BluetoothSocket socket){
        BluetoothSocketHandler.socket = socket;
    }
}
