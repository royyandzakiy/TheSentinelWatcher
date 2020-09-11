package thesentinel.watcher;

import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;

import static java.lang.Math.round;

public class SoundRecorderController {
    private MediaRecorder mRecorder;
    private SoundRecorderActivityBackup activity;
    private Thread recordingThread;

    public SoundRecorderController(SoundRecorderActivityBackup activity) {
        this.activity = activity;
    }

    public synchronized void startRecording() {

    }

    public synchronized void stopRecording() {

    }

    public void getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(
                    new String[]{android.Manifest.permission.RECORD_AUDIO}, 13);
        }
    }

    public boolean checkRecordingPermission()
    {
        int result  = activity.getApplicationContext().checkCallingOrSelfPermission(android.Manifest.permission.RECORD_AUDIO);
        return (result == PackageManager.PERMISSION_GRANTED);
    }
}
