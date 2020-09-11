package thesentinel.watcher;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.View;

import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import ca.uol.aig.fftpack.RealDoubleFFT;

import static android.graphics.Color.RED;
import static java.lang.Math.round;

public class SoundRecorderActivity extends Activity implements View.OnClickListener {

    private static final double[] CANCELLED = {100};
    int frequency = 8000;// = */44100;
    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    AudioRecord audioRecord;
    private RealDoubleFFT transformer;
    int blockSize = /*2048;// = */256;
    Button startStopButton, disconnectButton, triggerButton;
    boolean started = false;
    boolean CANCELLED_FLAG = false;
    double[][] cancelledResult = {{100}};
    int mPeakPos;
    double mHighestFreq;
    RecordAudio recordTask;
    ImageView imageViewDisplaySectrum;
    MyImageView imageViewScale;
    Bitmap bitmapDisplaySpectrum;

    Canvas canvasDisplaySpectrum;

    Paint paintSpectrumDisplay;
    Paint paintScaleDisplay;
    static SoundRecorderActivity mainActivity;
    LinearLayout main;
    int width;
    int height;
    int left_Of_BimapScale;
    int left_Of_DisplaySpectrum;
    private final static int ID_BITMAPDISPLAYSPECTRUM = 1;
    private final static int ID_IMAGEVIEWSCALE = 2;

    /* Sound Recorder */
    int countDetected = 0, countSuccess = 0;
    boolean sedangDiamati = false;
    long startTime = 0;
    MediaPlayer mp = null;

    /* Bluetooth */
    private BluetoothController bluetoothController;
    private ProgressDialog progress;
    private String address = null;

    private boolean alreadyTriggered = false;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Display display = getWindowManager().getDefaultDisplay();
        //Point size = new Point();
        //display.get(size);
        width = display.getWidth();
        height = display.getHeight();

        //blockSize = 256;
    }



    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
        //left_Of_BimapScale = main.getC.getLeft();
        MyImageView  scale = (MyImageView)main.findViewById(ID_IMAGEVIEWSCALE/*ID_IMAGEVIEWSCALE*/);
        ImageView bitmap = (ImageView)main.findViewById(ID_BITMAPDISPLAYSPECTRUM);
        left_Of_BimapScale = scale.getLeft();
        left_Of_DisplaySpectrum = bitmap.getLeft();
    }

    private void sendBluetoothMessage(String msg) {
        // bluetoothController.sendMsg(msg);
    }

    public void watcherTriggered() {

        // String s = msg.getText().toString();
        String msg = "3#-6.89,107.61";
        // this.sendBluetoothMessage(msg);

        String latlng = msg.substring(2);
        main.setBackgroundColor(Color.RED);
        if (!alreadyTriggered) {
//        Intent i = new Intent(getApplicationContext(),DetectedActivity.class);
//        i.putExtra("latlng", String.valueOf(latlng));
//            startActivity(i);

            mp = MediaPlayer.create(SoundRecorderActivity.this, R.raw.alarm);
            mp.start();
            alreadyTriggered = true;
            Toast.makeText(getApplicationContext(), "DETECTED!", Toast.LENGTH_SHORT).show();
        }
    }

    private class RecordAudio extends AsyncTask<Void, double[], Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            int bufferSize = AudioRecord.getMinBufferSize(frequency,
                    channelConfiguration, audioEncoding);
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT, frequency,
                    channelConfiguration, audioEncoding, bufferSize);
            int bufferReadResult;
            short[] buffer = new short[blockSize];
            double[] toTransform = new double[blockSize];
            try {
                audioRecord.startRecording();
            } catch (IllegalStateException e) {
                Log.e("Recording failed", e.toString());

            }
            while (started) {

                if (isCancelled() || (CANCELLED_FLAG == true)) {

                    started = false;
                    publishProgress(cancelledResult);
                    Log.d("doInBackground", "Cancelling the RecordTask");
                    break;
                } else {
                    bufferReadResult = audioRecord.read(buffer, 0, blockSize);

                    for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                        toTransform[i] = (double) buffer[i] / 32768.0; // signed 16 bit
                    }

                    transformer.ft(toTransform);

                    publishProgress(toTransform);

                }

            }
            return true;
        }

        @Override
        protected void onProgressUpdate(double[]...progress) {
            //Log.e("RecordingProgress", "Displaying in progress");
            double mMaxFFTSample = 150.0;

            //Log.d("Test:", Integer.toString(progress[0].length));
            if(progress[0].length == 1 ){

                Log.d("FFTSpectrumAnalyzer", "onProgressUpdate: Blackening the screen");
                canvasDisplaySpectrum.drawColor(Color.BLACK);
                imageViewDisplaySectrum.invalidate();

            }

            else {
                if (width > 512) {
                    Log.d("DEBUG", "width > 512");
                    for (int i = 0; i < progress[0].length; i++) {
                        int x = 2 * i;
                        int downy = (int) (150 - (progress[0][i] * 10));
                        int upy = 150;
                        if(downy < mMaxFFTSample)
                        {
                            // kasih batas rentang frekuensi yang akan diamati
                            mMaxFFTSample = downy;
                            //mMag = mMaxFFTSample;
                            mPeakPos = i;
                        }

                        /* SENTINEL */
                        //boolean batas_0 = x >= 30 && x <= 50;
                        boolean batas_0 = false;
                        boolean batas_1 = x >= 80 && x <= 100;
                        int thresholdAmplitudo = 155;
                        long durasi = System.currentTimeMillis() - startTime;

                        if (countSuccess > 3) {
                            watcherTriggered();
                        }

                        if (durasi > 6000) {
                            countSuccess = 0;
                        }

                        if (sedangDiamati && durasi >= 3000) {
                            if (countDetected > 40) {
                                countSuccess += 1;
                            }
                            // waktu habis, RESET
                            sedangDiamati = false;
                            countDetected = 0;
                        }

                        if (batas_0  || batas_1) {
                            Log.d("DEBUG", "countSuccess: " +countSuccess+ ", countDetected: " + countDetected + ", durasi: " +durasi+ ", downy: " + downy);
                            if (downy >= thresholdAmplitudo) {
                                //Log.d("DEBUG", "batas_0("+batas_0+"), batas_1("+batas_1+"): ada (" + downy + ")");
                                if (!sedangDiamati) {
                                    sedangDiamati = true;
                                    startTime = System.currentTimeMillis();
                                } else {
                                    countDetected += 1;
                                }
                            } else {
                                //Log.d("DEBUG", "batas_0("+batas_0+"), batas_1("+batas_1+"): tidak ada (" + downy + ")");
                            }
                        }

                        // LOW
                        if (x == 30) {
                            canvasDisplaySpectrum.drawLine(x, 250, x, upy, paintSpectrumDisplay);
                        } else if (x == 50) {
                            canvasDisplaySpectrum.drawLine(x, 250, x, upy, paintSpectrumDisplay);


                        } else if (x == 80) {
                            canvasDisplaySpectrum.drawLine(x, 250, x, upy, paintSpectrumDisplay);
                        } else if (x == 100) {
                            canvasDisplaySpectrum.drawLine(x, 250, x, upy, paintSpectrumDisplay);

                        // OTHERS
                        } else {
                            canvasDisplaySpectrum.drawLine(x, downy, x, upy, paintSpectrumDisplay);
                        }

                        // Log.d("DEBUG", "x: " + x + ", downy: " + downy + ", upy: " + upy + ", mMaxFFTSample: " + mMaxFFTSample);
                    }

                    imageViewDisplaySectrum.invalidate();
                } else {
                    for (int i = 0; i < progress[0].length; i++) {
                        int x = i;
                        int downy = (int) (150 - (progress[0][i] * 10));
                        int upy = 150;
                        if(downy < mMaxFFTSample)
                        {
                            mMaxFFTSample = downy;
                            //mMag = mMaxFFTSample;
                            mPeakPos = i;
                        }
                        canvasDisplaySpectrum.drawLine(x, downy, x, upy, paintSpectrumDisplay);
                    }


                    imageViewDisplaySectrum.invalidate();
                }
            }
        }

        private void duration() {
            boolean stopwatchStarted = false;
            long startTime = 0;
            if (!stopwatchStarted) {
                startTime = System.currentTimeMillis();
            }

            // trigger untuk mulai amati
            // periksa titik dengan threshold radius x == 10
             // kenali pola berdasarkan kemunculan pada *rentang_frequency* tertentu selama berturut2 dg *amplitudo* tertentu tiap *periode* tertentu
            // selama 5 detik menghasilkan lebih dr 5 kemunculan
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            try{
                audioRecord.stop();
            }
            catch(IllegalStateException e){
                Log.e("Stop failed", e.toString());
            }

            canvasDisplaySpectrum.drawColor(Color.BLACK);
            imageViewDisplaySectrum.invalidate();
               /* mHighestFreq = (((1.0 * frequency) / (1.0 * blockSize)) * mPeakPos)/2;
                String str = "Frequency for Highest amplitude: " + mHighestFreq;
                Toast.makeText(getApplicationContext(), str , Toast.LENGTH_LONG).show();*/

        }
    }

    protected void onCancelled(Boolean result){

        try{
            audioRecord.stop();
        }
        catch(IllegalStateException e){
            Log.e("Stop failed", e.toString());

        }
        //recordTask.cancel(true);

        Log.d("FFTSpectrumAnalyzer","onCancelled: New Screen");
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

    }

    public void onClick(View v) {
        if (started == true) {
            //started = false;
            CANCELLED_FLAG = true;
            //recordTask.cancel(true);
            try{
                audioRecord.stop();
            }
            catch(IllegalStateException e){
                Log.e("Stop failed", e.toString());

            }
            startStopButton.setText("Start");
            //show the frequency that has the highest amplitude...
            mHighestFreq = (((1.0 * frequency) / (1.0 * blockSize)) * mPeakPos)/2;
            String str = "Frequency for Highest amplitude: " + mHighestFreq;
            Toast.makeText(getApplicationContext(), str , Toast.LENGTH_LONG).show();

            canvasDisplaySpectrum.drawColor(Color.BLACK);

        }

        else {
            countDetected = 0;
            countSuccess = 0;
            started = true;
            CANCELLED_FLAG = false;
            startStopButton.setText("Stop");
            recordTask = new RecordAudio();
            recordTask.execute();
        }

    }
    static SoundRecorderActivity getMainActivity(){

        return mainActivity;
    }

    public void onStop(){
        super.onStop();
        	started = false;
            startStopButton.setText("Start");
        if(recordTask != null){
            recordTask.cancel(true);
        }
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        if (mp != null) {
            try {
                mp.stop();
            } finally {
                mp.release();
                mp = null;
            }
        }
    }

    public void onStart(){

        super.onStart();
        main = new LinearLayout(this);
        main.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        main.setBackground(getResources().getDrawable(R.drawable.bg));
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        transformer = new RealDoubleFFT(blockSize);

        imageViewDisplaySectrum = new ImageView(this);
        if(width > 512){
            bitmapDisplaySpectrum = Bitmap.createBitmap((int)512,(int)300,Bitmap.Config.ARGB_8888);
        }
        else{
            bitmapDisplaySpectrum = Bitmap.createBitmap((int)256,(int)150,Bitmap.Config.ARGB_8888);
        }
        LinearLayout.LayoutParams layoutParams_imageViewScale = null;
        //Bitmap scaled = Bitmap.createScaledBitmap(bitmapDisplaySpectrum, 320, 480, true);
        canvasDisplaySpectrum = new Canvas(bitmapDisplaySpectrum);
        //canvasDisplaySpectrum = new Canvas(scaled);
        paintSpectrumDisplay = new Paint();
        paintSpectrumDisplay.setColor(Color.GREEN);
        imageViewDisplaySectrum.setImageBitmap(bitmapDisplaySpectrum);
        if(width >512){
            //imageViewDisplaySectrum.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams layoutParams_imageViewDisplaySpectrum=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ((ViewGroup.MarginLayoutParams) layoutParams_imageViewDisplaySpectrum).setMargins(100, 600, 0, 0);
            imageViewDisplaySectrum.setLayoutParams(layoutParams_imageViewDisplaySpectrum);
            layoutParams_imageViewScale= new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            //layoutParams_imageViewScale.gravity = Gravity.CENTER_HORIZONTAL;
            ((ViewGroup.MarginLayoutParams) layoutParams_imageViewScale).setMargins(100, 20, 0, 0);

        }

        else if ((width >320) && (width<512)){
            LinearLayout.LayoutParams layoutParams_imageViewDisplaySpectrum=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ((ViewGroup.MarginLayoutParams) layoutParams_imageViewDisplaySpectrum).setMargins(60, 250, 0, 0);
            //layoutParams_imageViewDisplaySpectrum.gravity = Gravity.CENTER_HORIZONTAL;
            imageViewDisplaySectrum.setLayoutParams(layoutParams_imageViewDisplaySpectrum);

            //imageViewDisplaySectrum.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
            layoutParams_imageViewScale=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ((ViewGroup.MarginLayoutParams) layoutParams_imageViewScale).setMargins(60, 20, 0, 100);
            //layoutParams_imageViewScale.gravity = Gravity.CENTER_HORIZONTAL;
        }

        else if (width < 320){
            	/*LinearLayout.LayoutParams layoutParams_imageViewDisplaySpectrum=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                ((MarginLayoutParams) layoutParams_imageViewDisplaySpectrum).setMargins(30, 100, 0, 100);
                imageViewDisplaySectrum.setLayoutParams(layoutParams_imageViewDisplaySpectrum);*/
            imageViewDisplaySectrum.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
            layoutParams_imageViewScale=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            //layoutParams_imageViewScale.gravity = Gravity.CENTER;
        }
        imageViewDisplaySectrum.setId(ID_BITMAPDISPLAYSPECTRUM);
        main.addView(imageViewDisplaySectrum);

        imageViewScale = new MyImageView(this);
        imageViewScale.setLayoutParams(layoutParams_imageViewScale);
        imageViewScale.setId(ID_IMAGEVIEWSCALE);

        //imageViewScale.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        main.addView(imageViewScale);

        startStopButton = new Button(this);
        startStopButton.setText("Start");
        startStopButton.setOnClickListener(this);
        startStopButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));

        disconnectButton = new Button(this);
        disconnectButton.setText("Disconnect");
        disconnectButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));

        triggerButton = new Button(this);
        triggerButton.setText("Trigger");
        triggerButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));

        main.addView(startStopButton);
        main.addView(triggerButton);
        main.addView(disconnectButton);

        setContentView(main);

        mainActivity = this;

        /* Initialize Layout */
        initializeLayout();

        /* Initialize Bluetooth */
         // initializeBlutooth();
    }

    private void initializeLayout() {
        // add toolbar later...
        triggerButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "TRIGGERED!", Toast.LENGTH_SHORT).show();
                watcherTriggered();
            }
        });
    }

    private void initializeBlutooth() {
        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceListActivity.EXTRA_ADDRESS); //receive the address of the bluetooth device
        bluetoothController = new BluetoothController(address, this);
        disconnectButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                 bluetoothController.Disconnect();
                 finish();
            }
        });

        bluetoothController.connectBT(); //Call the class to connect
    }

    public void showProgress() {
        progress = ProgressDialog.show(SoundRecorderActivity.this, "Connecting...", "Please wait!!!");  //show a progress dialog
    }

    public void dismissProgress() {
        progress.dismiss();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(recordTask != null){
        recordTask.cancel(true);
        }
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if(recordTask != null){
            recordTask.cancel(true);
        }
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    //Custom Imageview Class
    public class MyImageView extends android.support.v7.widget.AppCompatImageView {
        Paint paintScaleDisplay;
        Bitmap bitmapScale;
        //Canvas canvasScale;
        public MyImageView(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
            if(width >512){
                bitmapScale = Bitmap.createBitmap((int)512,(int)50,Bitmap.Config.ARGB_8888);
            }
            else{
                bitmapScale =  Bitmap.createBitmap((int)256,(int)50,Bitmap.Config.ARGB_8888);
            }

            paintScaleDisplay = new Paint();
            paintScaleDisplay.setColor(Color.WHITE);
            paintScaleDisplay.setStyle(Paint.Style.FILL);

            //canvasScale = new Canvas(bitmapScale);

            setImageBitmap(bitmapScale);
            invalidate();
        }
        @Override
        protected void onDraw(Canvas canvas)
        {
            // TODO Auto-generated method stub
            super.onDraw(canvas);

            if(width > 512){
                //canvasScale.drawLine(0, 30,  512, 30, paintScaleDisplay);
                canvas.drawLine(0, 30,  512, 30, paintScaleDisplay);
                for(int i = 0,j = 0; i< 512; i=i+128, j++){
                    for (int k = i; k<(i+128); k=k+16){
                        //canvasScale.drawLine(k, 30, k, 25, paintScaleDisplay);
                        canvas.drawLine(k, 30, k, 25, paintScaleDisplay);
                    }
                    //canvasScale.drawLine(i, 40, i, 25, paintScaleDisplay);
                    canvas.drawLine(i, 40, i, 25, paintScaleDisplay);
                    String text = Integer.toString(j) + " KHz";
                    //canvasScale.drawText(text, i, 45, paintScaleDisplay);
                    canvas.drawText(text, i, 45, paintScaleDisplay);
                }
                canvas.drawBitmap(bitmapScale, 0, 0, paintScaleDisplay);
            }
            else if ((width >320) && (width<512)){
                //canvasScale.drawLine(0, 30, 0 + 256, 30, paintScaleDisplay);
                canvas.drawLine(0, 30, 0 + 256, 30, paintScaleDisplay);
                for(int i = 0,j = 0; i<256; i=i+64, j++){
                    for (int k = i; k<(i+64); k=k+8){
                        //canvasScale.drawLine(k, 30, k, 25, paintScaleDisplay);
                        canvas.drawLine(k, 30, k, 25, paintScaleDisplay);
                    }
                    //canvasScale.drawLine(i, 40, i, 25, paintScaleDisplay);
                    canvas.drawLine(i, 40, i, 25, paintScaleDisplay);
                    String text = Integer.toString(j) + " KHz";
                    //canvasScale.drawText(text, i, 45, paintScaleDisplay);
                    canvas.drawText(text, i, 45, paintScaleDisplay);
                }
                canvas.drawBitmap(bitmapScale, 0, 0, paintScaleDisplay);
            }

            else if (width <320){
                //canvasScale.drawLine(0, 30,  256, 30, paintScaleDisplay);
                canvas.drawLine(0, 30,  256, 30, paintScaleDisplay);
                for(int i = 0,j = 0; i<256; i=i+64, j++){
                    for (int k = i; k<(i+64); k=k+8){
                        //canvasScale.drawLine(k, 30, k, 25, paintScaleDisplay);
                        canvas.drawLine(k, 30, k, 25, paintScaleDisplay);
                    }
                    //canvasScale.drawLine(i, 40, i, 25, paintScaleDisplay);
                    canvas.drawLine(i, 40, i, 25, paintScaleDisplay);
                    String text = Integer.toString(j) + " KHz";
                    //canvasScale.drawText(text, i, 45, paintScaleDisplay);
                    canvas.drawText(text, i, 45, paintScaleDisplay);
                }
                canvas.drawBitmap(bitmapScale, 0, 0, paintScaleDisplay);
            }
        }
    }
}
    
