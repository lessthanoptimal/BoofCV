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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Size;
import android.view.*;
import boofcv.android.ConvertBitmap;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Extension of {@link SimpleCamera2Activity} which adds visualization and hooks for image
 * processing. Video frames are automatically converted into a format which can be processed by
 * computer vision. Optionally multiple threads can be generated so that video frames are
 * processed concurrently. The input image is automatically converted into a Bitmap image if
 * requested. If multiple threads are being used the user can toggle if they want visualization
 * to be shown if an old image finished being processed after a newer one.
 *
 * Must call {@link #startCamera(ViewGroup, TextureView)} in the constructor.
 *
 * To customize it's behavior override the following functions:
 * <ul>
 *     <li>{@link #setThreadPoolSize}</li>
 *     <li>{@link #setImageType}</li>
 * </ul>
 *
 * Configuration variables
 * <ul>
 *     <li>targetResolution</li>
 *     <li>showBitmap</li>
 *     <li>stretchToFill</li>
 *     <li>visualizeOnlyMostRecent</li>
 * </ul>
 *
 * @see SimpleCamera2Activity
 */
public abstract class VisualizeCamera2Activity extends SimpleCamera2Activity {

    private static final String TAG = "VisualizeCamera2";

    protected TextureView textureView; // used to display camera preview directly to screen
    protected DisplayView displayView; // used to render visuals

    //---- START Owned By imagLock
    protected final Object imageLock = new Object();
    protected ImageType imageType = ImageType.single(GrayU8.class);
    protected Stack<ImageBase> stackImages = new Stack<>(); // images which are available for use
    byte[] convertWork = new byte[1]; // work space for converting images
    //---- END

    //---- START owned by bitmapLock
    protected final Object bitmapLock = new Object();
    protected Bitmap bitmap = Bitmap.createBitmap(1,1, Bitmap.Config.ARGB_8888);
    protected byte[] bitmapTmp =  new byte[1];
    //---- END

    LinkedBlockingQueue threadQueue = new LinkedBlockingQueue();
    ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1,1,50, TimeUnit.MILLISECONDS,
            threadQueue);

    Matrix imageToView = new Matrix();

    // number of pixels it searches for when choosing camera resolution
    protected int targetResolution = 640*480;

    // If true the bitmap will be shown. otherwise the original preview image will be
    protected boolean showBitmap = true;

    // if true it will sketch the bitmap to fill the view
    protected boolean stretchToFill = false;

    // If an old thread takes longer to finish than a new thread it won't be visualized
    protected boolean visualizeOnlyMostRecent = true;
    protected volatile long timeOfLastUpdated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if( verbose )
            Log.i(TAG,"onCreate()");

        // Free up more screen space
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if( actionBar != null ) {
            actionBar.hide();
        }
    }

    /**
     * Specifies the number of threads in the thread-pool. If this is set to a value greater than
     * one you better know how to write concurrent code or else you're going to have a bad time.
     */
    public void setThreadPoolSize( int threads ) {
        if( threads <= 0 )
            throw new IllegalArgumentException("Number of threads must be greater than 0");
        if( verbose )
            Log.i(TAG,"setThreadPoolSize("+threads+")");

        threadPool.setCorePoolSize(threads);
        threadPool.setMaximumPoolSize(threads);
    }

    /**
     *
     * @param layout Where the visualization overlay will be placed inside of
     * @param view If not null then this will be used to display the camera preview.
     */
    protected void startCamera(@NonNull ViewGroup layout, @Nullable TextureView view ) {
        if( verbose )
            Log.i(TAG,"startCamera(layout,view="+(view!=null)+")");
        displayView = new DisplayView(this);
        layout.addView(displayView,layout.getChildCount(),
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if( view == null ) {
            super.startCameraView(displayView);
        } else {
            this.textureView = view;
            super.startCameraTexture(textureView);
        }
    }

    @Override
    protected void startCameraTexture( @Nullable TextureView view ) {
        throw new RuntimeException("Call the other start camera function");
    }
    @Override
    protected void startCameraView( @Nullable View view ) {
        throw new RuntimeException("Call the other start camera function");
    }
    @Override
    protected void startCamera() {
        throw new RuntimeException("Call the other start camera function");
    }

    /**
     * Selects a resolution which has the number of pixels closest to the requested value
     */
    @Override
    protected int selectResolution( int widthTexture, int heightTexture, Size[] resolutions  ) {
        // just wanted to make sure this has been cleaned up
        timeOfLastUpdated = 0;

        // select the resolution here
        int bestIndex = -1;
        double bestAspect = Double.MAX_VALUE;
        double bestArea = 0;

        for( int i = 0; i < resolutions.length; i++ ) {
            Size s = resolutions[i];
            int width = s.getWidth();
            int height = s.getHeight();

            double aspectScore = Math.abs(width*height-targetResolution);

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

    @Override
    protected void onCameraResolutionChange(int width, int height) {
        // predeclare bitmap image used for display
        if( showBitmap ) {
            synchronized (bitmapLock) {
                if (bitmap.getWidth() != width || bitmap.getHeight() != height)
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmapTmp = ConvertBitmap.declareStorage(bitmap, bitmapTmp);
            }
        }
        // Compute transform from bitmap to view coordinates
        int rotatedWidth = width;
        int rotatedHeight = height;

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int offsetX=0,offsetY=0;

        if (Surface.ROTATION_0 == rotation || Surface.ROTATION_180 == rotation) {
            rotatedWidth = height;
            rotatedHeight = width;
            offsetX = (rotatedWidth-rotatedHeight)/2;
            offsetY = (rotatedHeight-rotatedWidth)/2;
        }

        imageToView.reset();
        float scale = Math.min(
                (float)displayView.getWidth()/rotatedWidth,
                (float)displayView.getHeight()/rotatedHeight);
        if( scale == 0 ) {
            Log.e(TAG,"displayView has zero width and height");
            return;
        }


        imageToView.postRotate(-90*rotation + mSensorOrientation, width/2, height/2);
        imageToView.postTranslate(offsetX,offsetY);
        imageToView.postScale(scale,scale);
        if( stretchToFill ) {
            imageToView.postScale(
                    displayView.getWidth()/(rotatedWidth*scale),
                    displayView.getHeight()/(rotatedHeight*scale));
        } else {
            imageToView.postTranslate(
                    (displayView.getWidth() - rotatedWidth*scale) / 2,
                    (displayView.getHeight() - rotatedHeight*scale) / 2);
        }

        Log.i(TAG,imageToView.toString());
        Log.i(TAG,"scale = "+scale);
        Log.i(TAG,"camera resolution cam="+width+"x"+height+" view="+displayView.getWidth()+"x"+displayView.getHeight());
    }

    /**
     * Changes the type of image the camera frame is converted to
     */
    protected void setImageType( ImageType type ) {
        synchronized (imageLock){
            this.imageType = type;
            // todo check to see if the type is the same or not before clearing
            this.stackImages.clear();
        }
    }

    @Override
    protected void processFrame(Image image) {
        // If there's a thread pending to go into the pool wait until it has been cleared out
        if( threadQueue.size() > 0 )
            return;

        synchronized (imageLock) {
            ImageBase converted;
            if( stackImages.empty()) {
                converted = imageType.createImage(1,1);
            } else {
                converted = stackImages.pop();
            }
            convertWork = ConvertYuv420_888.declareWork(image, convertWork);
            ConvertYuv420_888.yuvToBoof(image,converted, convertWork);

            threadPool.execute(()->processImageOuter(converted));
        }
    }

    /**
     * Where all the image processing happens. If the number of threads is greater than one then
     * this function can be called multiple times before previous calls have finished.
     *
     * WARNING: If the image type can change this must be before processing it.
     *
     * @param image The image which is to be processed. The image is owned by this function until
     *              it returns. After that the image and all it's data will be recycled. DO NOT
     *              SAVE A REFERENCE TO IT.
     */
    protected abstract void processImage( ImageBase image );

    /**
     * Internal function which manages images and invokes {@link #processImage}.
     */
    private void processImageOuter( ImageBase image ) {
        long startTime = System.currentTimeMillis();

        // this image is owned by only this process and no other. So no need to lock it while
        // processing
        processImage(image);

        // If an old image finished being processes after a more recent one it won't be visualized
        if( !visualizeOnlyMostRecent || startTime > timeOfLastUpdated ) {
            timeOfLastUpdated = startTime;

            // Copy this frame
            if (showBitmap) {
                synchronized (bitmapLock) {
                    ConvertBitmap.boofToBitmap(image, bitmap, bitmapTmp);
                }
            }

            // Update the visualization
            runOnUiThread(() -> displayView.invalidate());
        }

        // Put the image into the stack if the image type has not changed
        synchronized (imageLock) {
            // TODO replace with imageType.isSameType() on next release
            if( imageType.getFamily() != image.getImageType().getFamily() )
                return;
            if( imageType.getDataType() != image.getImageType().getDataType() )
                return;
            if( imageType.getNumBands() != image.getImageType().getNumBands() )
                return;
            stackImages.add(image);
        }
    }

    /**
     * Renders the visualizations. Override and invoke super to add your own
     */
    protected void onDrawFrame( SurfaceView view , Canvas canvas ) {
        if( showBitmap ) {
            synchronized (bitmapLock) {
                canvas.drawBitmap(bitmap, imageToView, null);
            }
        }
    }

    /**
     * Custom view for visualizing results
     */
    public class DisplayView extends SurfaceView implements SurfaceHolder.Callback {

        SurfaceHolder mHolder;

        public DisplayView(Context context) {
            super(context);
            mHolder = getHolder();

            // configure it so that its on top and will be transparent
            setZOrderOnTop(true);    // necessary
            mHolder.setFormat(PixelFormat.TRANSPARENT);

            // if this is not set it will not draw
            setWillNotDraw(false);
        }


        @Override
        public void onDraw(Canvas canvas) {
            onDrawFrame(this,canvas);
        }

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {}

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {}

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {}
    }
}
