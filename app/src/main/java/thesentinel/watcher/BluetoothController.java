package thesentinel.watcher;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class BluetoothController {

    // Button btnOn, btnOff, btnDis;
    ImageButton On, Off, Discnt, Abt;
    String address = null;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private SoundRecorderActivity activity;

    protected BluetoothController(String address, SoundRecorderActivity activity)
    {
        if (address == null) {
            activity.finish();
        }
        this.address = address;
        this.activity = activity;
        // new ConnectBT().execute(); //Call the class to connect
    }

    public void Disconnect()
    {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
                btSocket = null;
            }
            catch (IOException e)
            { msg("Error");}
        }
        Log.d("DEBUG", "disconnect aman bos");

    }

    public void sendMsg(String msg) {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write(msg.toString().getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    private void msg(String s) {
        Toast.makeText(activity.getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    public void connectBT() {
        new ConnectBT().execute();
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            activity.showProgress();
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if ((btSocket == null || !isBtConnected) && address != null)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
            }
            activity.dismissProgress();

            if (!ConnectSuccess) {
                activity.finish();
            }
        }
    }
}