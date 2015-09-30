package maveric.vksis;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.io.IOException;
import java.util.UUID;


public class StartActivity extends Activity {

    private static final String NAME = "Server";
    private static final UUID MY_UUID = UUID.fromString("afa2d36c-5865-11e5-885d-feff819cdc9f");

    private final int REQUEST_ENABLE_BT = 1;
    private final int REQUEST_ENABLE_DISCOVERABILITY = 2;
    private final int BT_DISCOVERABLE_DURATION = 60
            ;

    private ArrayAdapter<String> mArrayAdapter;


    private BluetoothAdapter mBluetoothAdapter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            }
        }
    };

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            BluetoothSocketHandler.setSocket(mmSocket);
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            startActivity(intent);
            Packet.bytesPerPacket = Integer.parseInt(((EditText)findViewById(R.id.bytes_per_packet)).getText().toString());
            /*try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) { }
            // If a connection was accepted
            if (socket != null) {
                // Do work to manage the connection (in a separate thread)
                BluetoothSocketHandler.setSocket(socket);
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                startActivity((intent));
                /*try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    break;
                }*/
                Packet.bytesPerPacket = Integer.parseInt(((EditText)findViewById(R.id.bytes_per_packet)).getText().toString());


            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        byte[] test = {49, 50, 51};
        //byte[] processed = Packet.makePacket(test);
        //byte[] reverce = Packet.extractFromPacket(processed);
        CRC8.updateChecksumm(test);
        byte a = CRC8.getValue();

        test[0] ^= 0x01;
        CRC8.clearValue();
        CRC8.updateChecksumm(test);
        byte b = CRC8.getValue();



        final ListView listview = (ListView) findViewById(R.id.listview);

        mArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listview.setAdapter(mArrayAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                mBluetoothAdapter.cancelDiscovery();
                final String item = (String) parent.getItemAtPosition(position);
                ConnectThread thread = new ConnectThread(mBluetoothAdapter.getRemoteDevice(item.split("\n")[1]));
                thread.start();
                unregisterReceiver(mReceiver);
            }

        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            throw new UnsupportedOperationException();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT)
            if (resultCode == RESULT_OK)
                return;
            else
                throw new UnsupportedOperationException();

        else if (requestCode == REQUEST_ENABLE_DISCOVERABILITY) {
            if (resultCode == BT_DISCOVERABLE_DURATION) {
                AcceptThread thread = new AcceptThread();
                thread.start();
            }
            else
                throw new UnsupportedOperationException();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void searchDevices(View view) {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        boolean res = mBluetoothAdapter.startDiscovery();
        if (!res)
        {
            throw new UnsupportedOperationException();
        }
    }
    public void enableDiscoverability(View view) {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, BT_DISCOVERABLE_DURATION);
        startActivityForResult(discoverableIntent, REQUEST_ENABLE_DISCOVERABILITY);
    }

}
