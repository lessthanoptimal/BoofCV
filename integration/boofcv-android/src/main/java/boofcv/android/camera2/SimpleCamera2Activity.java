/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.android.camera2;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;
import georegression.metric.UtilAngle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Activity for collecting images from single camera on an Android device using the camera2 API.
 *
 * To start the camera invoke {@link #startCamera} inside your Activity's onCreate function.
 *
 * To customize it's behavior override the following functions:
 * <ul>
 *     <li>{@link #selectResolution}</li>
 *     <li>{@link #onCameraResolutionChange}</li>
 *     <li>{@link #configureCamera}</li>
 *     <li>{@link #selectCamera}</li>
 *     <li>{@link #processFrame}</li>
 * </ul>
 *
 * Configuration variables
 * <ul>
 *     <li>verbose</li>
 * </ul>
 *
 * Specify the following permissions and features in AndroidManifest.xml
 * <pre>
 * {@code
 * <uses-permission android:name="android.permission.CAMERA" />
 * <uses-feature android:name="android.hardware.camera2.full" />
 * }</pre>
 *
 * @author Peter Abeles
 */
public abstract class SimpleCamera2Activity extends AppCompatActivity {
    private static final String TAG = "SimpleCamera2";

    private CameraDevice mCameraDevice;
    private CameraCaptureSession mPreviewSession;
    protected TextureView mTextureView;
    protected View mView;
    // size of camera preview
    protected Size mCameraSize;

    protected int mSensorOrientation;

    // the camera that was selected to view
    protected String cameraId;
    // describes physical properties of the camera
    protected CameraCharacteristics mCameraCharacterstics;

    private ReentrantLock mCameraOpenCloseLock = new ReentrantLock();

    // Image reader for capturing the preview
    private ImageReader mPreviewReader;
    private CaptureRequest.Builder mPreviewRequestBuilder;

    // width and height of the view the camera is displayed in
    protected int viewWidth,viewHeight;

    // If true there will be verbose outpput to Log
    protected boolean verbose = true;

    /**
     * After this function is called the camera will be start. It might not start immediately
     * and there can be a delay.
     * @param view The view the camera is displayed inside or null if not displayed
     */
    protected void startCameraTexture( TextureView view ) {
        if( verbose )
            Log.i(TAG,"startCamera(TextureView="+(view!=null)+")");
        this.mTextureView = view;
        this.mView = null;
        this.mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    protected void startCameraView( View view) {
        if( verbose )
            Log.i(TAG,"startCamera(View="+(view!=null)+")");
        this.mView = view;
        this.mTextureView = null;
        view.addOnLayoutChangeListener(mViewLayoutChangeListeneer);
    }

    protected void startCamera() {
        if( verbose )
            Log.i(TAG,"startCamera()");
        this.mView = null;
        this.mTextureView =null;
        runOnUiThread(()->openCamera(0,0));
    }

    @Override
    protected void onResume() {
        if( verbose )
            Log.i(TAG,"onResume()");
        super.onResume();

        // TODO this hasn't been well tested yet
        if( mTextureView != null ) {
            if (mTextureView.isAvailable()) {
                openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            } else {
                mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            }
        } else if( mView != null ) {
            if( mView.getWidth() != 0 && mView.getHeight() != 0 ) {
                openCamera(mView.getWidth(), mView.getHeight());
            } else {
                mView.addOnLayoutChangeListener(mViewLayoutChangeListeneer);
            }
        } else if( mCameraSize == null ) {
            startCamera();
        }
    }

    @Override
    protected void onPause() {
        if( verbose )
            Log.i(TAG,"onPause()");
        closeCamera();
        super.onPause();
    }

    /**
     * Selects the camera resolution from the list of possible values. By default it picks the
     * resolution which best fits the texture's aspect ratio. If there's a tie the area is
     * maximized.
     *
     * @param widthTexture Width of the texture the preview is displayed inside of. <= 0 if no view
     * @param heightTexture Height of the texture the preview is displayed inside of. <= 0 if no view
     * @param resolutions array of possible resolutions
     * @return index of the resolution
     */
    protected int selectResolution( int widthTexture, int heightTexture, Size[] resolutions  ) {
        int bestIndex = -1;
        double bestAspect = Double.MAX_VALUE;
        double bestArea = 0;

        double textureAspect = widthTexture > 0 ? widthTexture/(double)heightTexture:0;

        for( int i = 0; i < resolutions.length; i++ ) {
            Size s = resolutions[i];
            int width = s.getWidth();
            int height = s.getHeight();

            double aspectScore = widthTexture > 0 ? Math.abs(width - height*textureAspect)/width:1;

            if( aspectScore < bestAspect ) {
                bestIndex = i;
                bestAspect = aspectScore;
                bestArea = width*height;
            } else if( Math.abs(aspectScore-bestArea) <= 1e-8 ) {
                bestIndex = i;
                double area = width*height;
                if( area > bestArea ) {
                    bestArea = area;
                }
            }
        }

        return bestIndex;
    }

    /**
     * Called when the camera's resolution has changed.
     */
    protected void onCameraResolutionChange( int width , int height ) {
        if( verbose )
            Log.i(TAG,"onCameraResolutionChange( "+width+" , "+height+")");

    }

    /**
     * Override to do custom configuration of the camera's settings. By default the camera
     * is put into auto mode.
     *
     * @param captureRequestBuilder used to configure the camera
     */
    protected void configureCamera( CaptureRequest.Builder captureRequestBuilder ) {
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
    }

    /**
     * By default this will select the backfacing camera. override to change the camera it selects.
     */
    protected boolean selectCamera( CameraCharacteristics characteristics ) {
        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        return facing == null || facing != CameraCharacteristics.LENS_FACING_FRONT;
    }

    /**
     * Process a single frame from the video feed. Image is automatically
     * closed after this function exists. No need to invoke image.close() manually.
     *
     * All implementations of this function must run very fast. Less than 5 milliseconds is a good
     * rule of thumb. If longer than that then you should spawn a thread and process the
     * image inside of that.
     */
    protected abstract void processFrame( Image image );

    /**
     * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
    @SuppressWarnings("MissingPermission")
    private void openCamera(int widthTexture, int heightTexture) {
        if( verbose )
            Log.i(TAG,"openCamera( texture: "+widthTexture+" , "+heightTexture+")");
        if (isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if( manager == null )
            throw new RuntimeException("Null camera manager");

        // Save the size of the component the camera feed is being displayed inside of
        this.viewWidth = widthTexture;
        this.viewHeight = heightTexture;

        try {
            if( verbose )
                Log.d(TAG, "before tryAcquire mCameraOpenCloseLock");
            if (!mCameraOpenCloseLock.tryLock(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            String[] cameras = manager.getCameraIdList();
            for( String cameraId : cameras ) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                if(!selectCamera(characteristics))
                    continue;

                StreamConfigurationMap map = characteristics.
                        get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
                int which = selectResolution(widthTexture, heightTexture,sizes);
                if( which < 0 || which >= sizes.length )
                    continue;
                mCameraSize = sizes[which];
                this.cameraId = cameraId;
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

                if( verbose )
                    Log.i(TAG,"selected cameraId="+cameraId+" orientation="+mSensorOrientation);

                mCameraCharacterstics = characteristics;
                onCameraResolutionChange( mCameraSize.getWidth(), mCameraSize.getHeight() );
                try {
                    mPreviewReader = ImageReader.newInstance(
                            mCameraSize.getWidth(), mCameraSize.getHeight(),
                            ImageFormat.YUV_420_888, 2);
                    mPreviewReader.setOnImageAvailableListener(onAvailableListener, null);
                    configureTransform(widthTexture, heightTexture);
                    manager.openCamera(cameraId, mStateCallback, null);
                } catch( IllegalArgumentException e ) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                }
                return;
            }
            mCameraOpenCloseLock.unlock();
            throw new RuntimeException("No camera selected!");

        } catch (CameraAccessException e) {
            Toast.makeText(this, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e(TAG,"Null point in openCamera()");
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Toast.makeText(this,"Null pointer. Camera2 API not supported?",Toast.LENGTH_LONG).show();
            finish();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    private void closeCamera() {
        if( verbose )
            Log.i(TAG,"closeCamera()");
        try {
            mCameraOpenCloseLock.lock();
            closePreviewSession();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            mCameraSize = null;
            mCameraCharacterstics = null;
        } finally {
            mCameraOpenCloseLock.unlock();
        }
    }

    /**
     * Start the camera preview.
     */
    private void startPreview() {
        if (null == mCameraDevice || null == mCameraSize) {
            return;
        }
        try {
            closePreviewSession();
            List<Surface> surfaces = new ArrayList<>();
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            if( mTextureView != null && mTextureView.isAvailable() ) {
                SurfaceTexture texture = mTextureView.getSurfaceTexture();
                assert texture != null;
                texture.setDefaultBufferSize(mCameraSize.getWidth(), mCameraSize.getHeight());

                // Display the camera preview into this texture
                Surface previewSurface = new Surface(texture);
                surfaces.add(previewSurface);
                mPreviewRequestBuilder.addTarget(previewSurface);
            }

            // This is where the image for processing is extracted from
            Surface readerSurface = mPreviewReader.getSurface();
            surfaces.add(readerSurface);
            mPreviewRequestBuilder.addTarget(readerSurface);

            configureCamera(mPreviewRequestBuilder);

            mCameraDevice.createCaptureSession(surfaces,
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mPreviewSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(SimpleCamera2Activity.this, "Failed", Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            mPreviewSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mCameraSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mCameraSize.getHeight(), mCameraSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mCameraSize.getHeight(),
                    (float) viewWidth / mCameraSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private void closePreviewSession() {
        if( verbose )
            Log.i(TAG,"closePreviewSession");

        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    private View.OnLayoutChangeListener mViewLayoutChangeListeneer
            = new View.OnLayoutChangeListener() {

        @Override
        public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                   int leftWas, int topWas, int rightWas, int bottomWas)
        {
            int width = right-left;
            int height = bottom-top;
            if( mCameraSize == null ) {
                openCamera(width,height);
            }
            view.removeOnLayoutChangeListener(this);
        }
    };

    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    };

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            if( verbose )
                Log.i(TAG,"CameraDevice Callback onOpened() id="+cameraDevice.getId());
            mCameraDevice = cameraDevice;
            startPreview();
            mCameraOpenCloseLock.unlock();
            if (null != mTextureView) {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            if( verbose )
                Log.i(TAG,"CameraDevice Callback onDisconnected() id="+cameraDevice.getId());
            mCameraOpenCloseLock.unlock();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            if( verbose )
                Log.e(TAG,"CameraDevice Callback onError() error="+error);
            mCameraOpenCloseLock.unlock();
            cameraDevice.close();
            mCameraDevice = null;
            finish();
        }
    };

    /**
     * Estimates the camera's horizontal and vertical FOV by picking a nominal value.
     * Determining the actual FOV is a much more complex process.
     */
    public double[] cameraNominalFov() {
        SizeF sensorSize = mCameraCharacterstics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        float[] focalLengths = mCameraCharacterstics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);

        if( focalLengths == null || sensorSize == null ) {
            double hfov = UtilAngle.radian(60);
            double vfov = hfov*mCameraSize.getHeight()/mCameraSize.getWidth();
            return new double[]{hfov,vfov};
        } else {
            double hfov = 2 * Math.atan(sensorSize.getWidth() / (2 * focalLengths[0]));
            double vfov = 2 * Math.atan(sensorSize.getHeight() / (2 * focalLengths[0]));
            return new double[]{hfov,vfov};
        }
    }

    private ImageReader.OnImageAvailableListener onAvailableListener = imageReader -> {
        Image image = imageReader.acquireLatestImage();
        if (image == null)
            return;

        processFrame(image);
        image.close();
    };

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
