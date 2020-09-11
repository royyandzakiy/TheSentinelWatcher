package thesentinel.watcher;

import android.content.Intent;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class DetectedActivity extends AppCompatActivity {

    TextView latlngValue;
    String latlng;
//    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detected);

        Intent i = getIntent();
        if (i.hasExtra("latlng")){
            Bundle j = getIntent().getExtras();
            latlng = j.getString("latlng");
        } else {
            latlng = "-6.9,107.8";
        }
        latlngValue = (TextView) findViewById(R.id.latlngValue);
        latlngValue.setText(String.valueOf(latlng));

//        mediaPlayer = MediaPlayer.create(getApplicationContext(), getResources(R.raw.alarm));
//
//        mediaPlayer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
