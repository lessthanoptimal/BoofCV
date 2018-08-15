package org.boofcv.video;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import boofcv.android.camera2.SimpleCamera2Activity;

public class ExposureActivity extends SimpleCamera2Activity
        implements View.OnClickListener
{
    public static final String TAG = "Exposure";
    // The application forces there to be at most 7 selectable levels to make this more hardware
    // independent.
    int level = 3, numLevels=7;
    int minEV,maxEV;

    TextView textView;
    long timeLastClick = 0;

    public ExposureActivity() {
        setVisible(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This will steam the feed to a texture view. No image
        // processing or bitmaps required
        setContentView(R.layout.exposure);
        TextureView view = findViewById(R.id.camera_view);
        view.setOnClickListener(this);

        textView = findViewById(R.id.text_view_id);
        textView.setText("Touch screen to change exposure");

        startCameraTexture(view);
    }

    @Override
    protected void configureCamera(CameraDevice device, CameraCharacteristics characteristics, CaptureRequest.Builder captureRequestBuilder) {
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        // Divide the total exposure compensation needed by the size of the exposure compensation steps to be able to pass in the number of steps
        Range<Integer> range = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        if( range != null ) {
            minEV = range.getLower();
            maxEV = range.getUpper();
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, computeEV());
        } else {
            Toast.makeText(this,"Can't adjust camera's EV", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void processFrame(Image image) {
//        Log.i("Exposure","got frame");
    }

    @Override
    public void onClick(View v) {
        // Cool down to prevent it from trying to change too quickly
        if( System.currentTimeMillis() < timeLastClick )
            return;
        timeLastClick = System.currentTimeMillis()+250;

        // Cycle through all possible values
        level = (level+1)%numLevels;
        int ev = computeEV();
        changeCameraConfiguration();

        // Display info to the user
        String message = String.format(Locale.getDefault(),"min=%3d max=%3d current=%3d",minEV,maxEV,ev);
        textView.setText(message);
    }

    private int computeEV() {
        return (int)Math.round((maxEV-minEV)*(level/(double)(numLevels-1)))+minEV;
    }
}
