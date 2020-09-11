package thesentinel.watcher;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import static java.lang.Math.round;

public class SoundRecorderActivityBackup extends AppCompatActivity {

    /* Sound Recorder */
    private SoundRecorderController soundRecorderController;
    private TextView amplitudeValue;
    private ConstraintLayout constraintLayout;
    private static double MAX_AMPLITUDE_THRESHOLD = 80.0;

    /* Bluetooth */
    private BluetoothController bluetoothController;
    private Button btnDisconnect, btnSendMsg;
    private EditText msg;
    private ProgressDialog progress;
    private String address = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_recorder_backup);

        /* Toolbar */
        initializeLayout();

        /* Sound Recorder */
        //initializeSoundRecorder();

        /* Bluetooth */
        initializeBlutooth();
    }

    /** SOUND RECORDER **/
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (soundRecorderController.checkRecordingPermission()) {
            soundRecorderController.startRecording();
        } else {
            soundRecorderController.getPermission();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("DEBUG","SoundRecorderActivity::onPause");
        soundRecorderController.stopRecording();
    }

    public void updateAmplitude(double amplitudeDb) {
        if (amplitudeDb > 0 && amplitudeDb < 1000) {
            amplitudeValue.setText(String.valueOf(round(amplitudeDb)) + " dB");

            boolean triggered = amplitudeDb > MAX_AMPLITUDE_THRESHOLD;

            if (triggered) {
                actionTriggered();
            } else {
                constraintLayout.setBackground(getResources().getDrawable(R.drawable.bg));
            }
        }
    }

    private void sendMsg(String s) {
        bluetoothController.sendMsg(s);
    }

    private void actionTriggered() {
        String s = msg.getText().toString();
        this.sendMsg(s);
        Toast.makeText(this.getApplicationContext(), "Triggered!", Toast.LENGTH_SHORT);

        Intent i = new Intent(getApplicationContext(),DetectedActivity.class);
        i.putExtra("latlng", String.valueOf(msg));
        startActivity(i);
    }

    public void showProgress() {
        progress = ProgressDialog.show(SoundRecorderActivityBackup.this, "Connecting...", "Please wait!!!");  //show a progress dialog
    }

    public void dismissProgress() {
        progress.dismiss();
    }

    /** INITIALIZATION **/
    private void initializeLayout() {
        constraintLayout = (ConstraintLayout) findViewById(R.id.constraintLayout);
        msg = (EditText) findViewById(R.id.msg);
        btnSendMsg = (Button) findViewById(R.id.btnTrigger);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        amplitudeValue = (TextView) findViewById(R.id.amplitudeValue);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothController.Disconnect();
                finish();
            }
        });
    }

    private void initializeBlutooth() {
        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceListActivity.EXTRA_ADDRESS); //receive the address of the bluetooth device
        bluetoothController = new BluetoothController(address, new SoundRecorderActivity());
        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                bluetoothController.Disconnect();
                finish();
            }
        });

        btnSendMsg.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                actionTriggered();
            }
        });

        bluetoothController.connectBT(); //Call the class to connect
    }

    private void initializeSoundRecorder(){
        soundRecorderController = new SoundRecorderController(this);
        if (soundRecorderController.checkRecordingPermission()) {
            soundRecorderController.startRecording();
        } else {
            soundRecorderController.getPermission();
        }
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
}
