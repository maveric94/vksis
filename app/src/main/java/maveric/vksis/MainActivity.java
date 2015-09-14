package maveric.vksis;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;


public class MainActivity extends Activity {

    private final SocketThread mSocketThread = new SocketThread();

    private final int DO_UPDATE_TEXT = 1;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            final int op = msg.what;
            switch (op) {
                case DO_UPDATE_TEXT :  doUpdate((byte[])msg.obj); break;
            }

        }
    };
    private class SocketThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public SocketThread() {
            mmSocket = BluetoothSocketHandler.getSocket();
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            ArrayList<Byte> packet = new ArrayList<>();
            byte readByte;

            while (true) {
                packet.clear();
                try {
                    do {
                        readByte = (byte)mmInStream.read();
                    }
                    while (readByte != 0x7E);

                    packet.add((byte)0x7E);

                    do {
                        readByte = (byte)mmInStream.read();
                        packet.add(readByte);
                    }
                    while(readByte != 0x7E);
                    // Send the obtained bytes to the UI activity
                    Message msg = new Message();

                    msg.what = DO_UPDATE_TEXT;
                    msg.obj = Packet.extractFromPacket(toPrimitives(packet.toArray(new Byte[packet.size()])));
                    mHandler.sendMessage(msg);
                }
                catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(Packet.makePacket(bytes));
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
        private byte[] toPrimitives(Byte[] oBytes)
        {
            byte[] bytes = new byte[oBytes.length];

            for(int i = 0; i < oBytes.length; i++) {
                bytes[i] = oBytes[i];
            }

            return bytes;
        }
    }
    private void doUpdate(byte[] buffer) {
        TextView textView = (TextView)findViewById(R.id.messages);
        textView.append(new String(buffer, Charset.forName("UTF-8")) + "\n");
        final int scrollAmount = textView.getLayout().getLineTop(textView.getLineCount()) - textView.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0)
            textView.scrollTo(0, scrollAmount);
        else
            textView.scrollTo(0, 0);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSocketThread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
    public void sendMessage(View view) {
        EditText editText = (EditText)findViewById(R.id.edit_message);
        TextView textView = (TextView)findViewById(R.id.messages);
        String text = editText.getText().toString();
        if (text.length() == 0) {
            return;
        }
        mSocketThread.write(text.getBytes(Charset.forName("UTF-8")));
        editText.getText().clear();

        textView.append(text + "\n");
        final int scrollAmount = textView.getLayout().getLineTop(textView.getLineCount()) - textView.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0)
            textView.scrollTo(0, scrollAmount);
        else
            textView.scrollTo(0, 0);

    }
    protected void onDestroy() {
        super.onDestroy();
        try {
            BluetoothSocketHandler.getSocket().close();
        } catch (IOException e) { }
    }

}
