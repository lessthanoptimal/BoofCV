/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.color.ColorFormat;
import boofcv.android.ConvertBitmap;
import boofcv.android.ConvertCameraImage;
import boofcv.misc.MovingAverage;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray_I8;
import pabeles.concurrency.GrowArray;

import java.util.ArrayDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Extension of {@link SimpleCamera2Activity} which adds visualization and hooks for image
 * processing. Video frames are automatically converted into a format which can be processed by
 * BoofCV routines Optionally multiple threads can be generated so that video frames are
 * processed concurrently. The input image is automatically converted into a Bitmap image if
 * requested. If multiple threads are being used the user can toggle if they want visualization
 * to be shown if an old image finished being processed after a newer one.
 *
 * Must call {@link #startCamera(ViewGroup, TextureView)} in onCreate().
 *
 * Customize how images are processed with the following functions:
 * <ul>
 *     <li>{@link #setThreadPoolSize}: Changes the number of threads available for processing a video stream. Useful
 *     when using expensive image processing routines.</li>
 *     <li>{@link #setImageType}: Specifies image format passed to processImage() function/li>
 * </ul>
 *
 * Useful variables
 * <ul>
 *     <li><b>imageToView</b>: Matrix that converts a pixel in video frame to view frame</li>
 *     <li><b>displayView</b>: Where visualizations are rendered in.</li>
 *     <li><b>targetResolution</b>: Specifies how many pixels you want in the video frame</li>
 *     <li><b>stretchToFill</b>: true to stretch the video frame to fill the entire view</li>
 *     <li><b>bitmapMode</b>: Species how and if the bitmap should be drawn to the screen</li>
 *     <li><b>visualizeOnlyMostRecent</b>: If more than one thread is enabled should it show old results</li>
 * </ul>
 *
 * Configuration variables
 * <ul>
 *     <li>targetResolution</li>
 *     <li>showBitmap</li>
 *     <li>stretchToFill</li>
 *     <li>visualizeOnlyMostRecent</li>
 *     <li>renderBitmapImage</li>
 * </ul>
 *
 * @see SimpleCamera2Activity
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class VisualizeCamera2Activity extends SimpleCamera2Activity {

	private static final String TAG = "VisualizeCamera2";

	protected TextureView textureView; // used to display camera preview directly to screen
	protected DisplayView displayView; // used to render visuals

	// Data used for converting from Image to a BoofCV image type
	private final BoofImage boofImage = new BoofImage();

	// Storage for bitmap and workspace. The bitmap is used to render images to the display. There are different modes
	// that can be used for handling threading. See BitmapMode for a description
	protected final ReentrantLock bitmapLock = new ReentrantLock();
	protected BitmapMode bitmapMode = BitmapMode.DOUBLE_BUFFER;
	protected Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
	protected Bitmap bitmapWork = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
	protected DogArray_I8 bitmapTmp = new DogArray_I8();
	//---- END LOCK BITMAP

	protected LinkedBlockingQueue threadQueue = new LinkedBlockingQueue();
	protected ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 1, 50,
			TimeUnit.MILLISECONDS, threadQueue);

	/**
	 * Stores the transform from the video image to the view it is displayed on. Handles
	 * All those annoying rotations.
	 */
	protected Matrix imageToView = new Matrix();
	protected Matrix viewToImage = new Matrix();

	// number of pixels it searches for when choosing camera resolution
	protected int targetResolution = 640*480;

	// if true it will sketch the bitmap to fill the view
	protected boolean stretchToFill = false;

	// If an old thread takes longer to finish than a new thread it won't be visualized
	protected boolean visualizeOnlyMostRecent = true;
	protected volatile long timeOfLastUpdated;

	//START Lock for timing structures
	protected static final int TIMING_WARM_UP = 3; // number of frames that must be processed before it starts
	protected final Object lockTiming = new Object();
	protected int totalConverted; // counter for frames converted since data type was set
	protected final MovingAverage periodConvert = new MovingAverage(0.8); // milliseconds
	//END

	protected VisualizeCamera2Activity() {}

	/**
	 * Configures how it visualizes. By default it will render a bitmap to the main view. This can be disabled by
	 * setting the bitmapMode to BitmapMode.NONE and overriding {@link #renderBitmapImage(BitmapMode, ImageBase)}.
	 *
	 * @param bitmapMode How the memory
	 */
	protected VisualizeCamera2Activity( BitmapMode bitmapMode ) {
		this.bitmapMode = bitmapMode;
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate(savedInstanceState);
		if (verbose)
			Log.i(TAG, "onCreate()");

		// Free up more screen space
		android.app.ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.hide();
		}
	}

	/**
	 * Specifies the number of threads in the thread-pool. If this is set to a value greater than
	 * one you better know how to write concurrent code or else you're going to have a bad time.
	 */
	public void setThreadPoolSize( int threads ) {
		if (threads <= 0)
			throw new IllegalArgumentException("Number of threads must be greater than 0");
		if (verbose)
			Log.i(TAG, "setThreadPoolSize(" + threads + ")");

		threadPool.setCorePoolSize(threads);
		threadPool.setMaximumPoolSize(threads);
	}

	/**
	 * Starts the camera.
	 *
	 * @param layout Where the visualization overlay will be placed inside of
	 * @param view If not null then this will be used to display the camera preview.
	 */
	protected void startCamera( @NonNull ViewGroup layout, @Nullable TextureView view ) {
		if (verbose)
			Log.i(TAG, "startCamera(layout , view=" + (view != null) + ")");
		displayView = new DisplayView(this);
		layout.addView(displayView, layout.getChildCount(),
				new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		if (view == null) {
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
	protected int selectResolution( int widthTexture, int heightTexture, Size[] resolutions ) {
		// just wanted to make sure this has been cleaned up
		timeOfLastUpdated = 0;

		// select the resolution here
		int bestIndex = -1;
		double bestAspect = Double.MAX_VALUE;
		double bestArea = 0;

		for (int i = 0; i < resolutions.length; i++) {
			Size s = resolutions[i];
			int width = s.getWidth();
			int height = s.getHeight();

			double aspectScore = Math.abs(width*height - targetResolution);

			if (aspectScore < bestAspect) {
				bestIndex = i;
				bestAspect = aspectScore;
				bestArea = width*height;
			} else if (Math.abs(aspectScore - bestArea) <= 1e-8) {
				bestIndex = i;
				double area = width*height;
				if (area > bestArea) {
					bestArea = area;
				}
			}
		}

		return bestIndex;
	}

	@Override
	protected void onCameraResolutionChange( int cameraWidth, int cameraHeight, int sensorOrientation ) {
		// pre-declare bitmap image used for display
		if (bitmapMode != BitmapMode.NONE) {
			bitmapLock.lock();
			try {
				if (bitmap.getWidth() != cameraWidth || bitmap.getHeight() != cameraHeight) {
					bitmap = Bitmap.createBitmap(cameraWidth, cameraHeight, Bitmap.Config.ARGB_8888);
					if (bitmapMode == BitmapMode.DOUBLE_BUFFER) {
						bitmapWork = Bitmap.createBitmap(cameraWidth, cameraHeight, Bitmap.Config.ARGB_8888);
					}
				}
			} finally {
				bitmapLock.unlock();
			}
		}
		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		if (verbose)
			Log.i(TAG, "camera rotation = " + sensorOrientation + " display rotation = " + rotation);

		videoToDisplayMatrix(cameraWidth, cameraHeight, sensorOrientation,
				viewWidth, viewHeight, rotation*90, stretchToFill, imageToView);
		if (!imageToView.invert(viewToImage)) {
			throw new RuntimeException("WTF can't invert the matrix?");
		}
	}

	protected static void videoToDisplayMatrix( int cameraWidth, int cameraHeight, int cameraRotation,
												int displayWidth, int displayHeight, int displayRotation,
												boolean stretchToFill, Matrix imageToView ) {
		// Compute transform from bitmap to view coordinates
		int rotatedWidth = cameraWidth;
		int rotatedHeight = cameraHeight;

		int offsetX = 0, offsetY = 0;

		boolean needToRotateView = (0 == displayRotation || 180 == displayRotation) !=
				(cameraRotation == 0 || cameraRotation == 180);

		if (needToRotateView) {
			rotatedWidth = cameraHeight;
			rotatedHeight = cameraWidth;
			offsetX = (rotatedWidth - rotatedHeight)/2;
			offsetY = (rotatedHeight - rotatedWidth)/2;
		}

		imageToView.reset();
		float scale = Math.min(
				(float)displayWidth/rotatedWidth,
				(float)displayHeight/rotatedHeight);
		if (scale == 0) {
			Log.e(TAG, "displayView has zero width and height");
			return;
		}

		imageToView.postRotate(-displayRotation + cameraRotation, cameraWidth/2, cameraHeight/2);
		imageToView.postTranslate(offsetX, offsetY);
		imageToView.postScale(scale, scale);
		if (stretchToFill) {
			imageToView.postScale(
					displayWidth/(rotatedWidth*scale),
					displayHeight/(rotatedHeight*scale));
		} else {
			imageToView.postTranslate(
					(displayWidth - rotatedWidth*scale)/2,
					(displayHeight - rotatedHeight*scale)/2);
		}
	}

	/**
	 * Same as {@link #setImageType(ImageType, ColorFormat)} but defaults to {@link ColorFormat#RGB RGB}.
	 *
	 * @param type type of image you wish th convert the into to
	 */
	protected void setImageType( ImageType type ) {
		this.setImageType(type, ColorFormat.RGB);
	}

	/**
	 * Changes the type of image the camera frame is converted to
	 */
	protected void setImageType( ImageType type, ColorFormat colorFormat ) {
		synchronized (boofImage.imageLock) {
			boofImage.colorFormat = colorFormat;
			if (!boofImage.imageType.isSameType(type)) {
				boofImage.imageType = type;
				boofImage.stackImages.clear();
			}

			synchronized (lockTiming) {
				totalConverted = 0;
				periodConvert.reset();
			}
		}
	}

	@Override
	protected void processFrame( Image image ) {
		// If there's a thread pending to go into the pool wait until it has been cleared out
		if (threadQueue.size() > 0)
			return;

		ImageBase converted;

		// When the image is removed from the stack it's no longer controlled by this class
		synchronized (boofImage.imageLock) {
			if (boofImage.stackImages.isEmpty()) {
				converted = boofImage.imageType.createImage(1, 1);
			} else {
				converted = boofImage.stackImages.pop();
			}
		}
		// below is an expensive operation so care is taken to do it safely outside of locks
		// We are now safe to modify the image. I don't believe this function can be invoked multiple times at once
		// so the convert work space should be safe from modifications
		long before = System.nanoTime();
		ConvertCameraImage.imageToBoof(image, boofImage.colorFormat, converted, boofImage.convertWork);
		long after = System.nanoTime();
//			Log.i(TAG,"processFrame() image="+image.getWidth()+"x"+image.getHeight()+
//					"  boof="+converted.width+"x"+converted.height);

		// record how long it took to convert the image for diagnostic reasons
		synchronized (lockTiming) {
			totalConverted++;
			if (totalConverted >= TIMING_WARM_UP) {
				periodConvert.update((after - before)*1e-6);
			}
		}

		threadPool.execute(() -> processImageOuter(converted));
	}

	/**
	 * Where all the image processing happens. If the number of threads is greater than one then
	 * this function can be called multiple times before previous calls have finished.
	 *
	 * WARNING: If the image type can change this must be before processing it.
	 *
	 * @param image The image which is to be processed. The image is owned by this function until
	 * it returns. After that the image and all it's data will be recycled. DO NOT
	 * SAVE A REFERENCE TO IT.
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
		if (!visualizeOnlyMostRecent || startTime > timeOfLastUpdated) {
			timeOfLastUpdated = startTime;

			// Copy this frame
			renderBitmapImage(bitmapMode, image);

			// Update the visualization
			runOnUiThread(() -> displayView.invalidate());
		}

		// Put the image into the stack if the image type has not changed
		synchronized (boofImage.imageLock) {
			if (boofImage.imageType.isSameType(image.getImageType()))
				boofImage.stackImages.add(image);
		}
	}

	/**
	 * Renders the bitmap image to output. If you don't wish to have this behavior then override this function.
	 * Jsut make sure you respect the bitmap mode or the image might not render as desired or you could crash the app.
	 */
	protected void renderBitmapImage( BitmapMode mode, ImageBase image ) {
		switch (mode) {
			case UNSAFE: {
				if (image.getWidth() == bitmap.getWidth() && image.getHeight() == bitmap.getHeight())
					ConvertBitmap.boofToBitmap(image, bitmap, bitmapTmp);
			}
			break;

			case DOUBLE_BUFFER: {
				// TODO if there are multiple processing threads bad stuff will happen here. Need one work buffer
				// per thread
				// convert the image. this can be a slow operation
				if (image.getWidth() == bitmapWork.getWidth() && image.getHeight() == bitmapWork.getHeight())
					ConvertBitmap.boofToBitmap(image, bitmapWork, bitmapTmp);

				// swap the two buffers. If it's locked don't swap. This will skip a frame but will not cause
				// a significant slow down
				if (bitmapLock.tryLock()) {
					try {
						Bitmap tmp = bitmapWork;
						bitmapWork = bitmap;
						bitmap = tmp;
					} finally {
						bitmapLock.unlock();
					}
				}
			}
			break;

			case NONE:
				break;
		}
	}

	/**
	 * Renders the visualizations. Override and invoke super to add your own
	 */
	protected void onDrawFrame( SurfaceView view, Canvas canvas ) {

		// Code below is usefull when debugging display issues
//		Paint paintFill = new Paint();
//		paintFill.setColor(Color.RED);
//		paintFill.setStyle(Paint.Style.FILL);
//		Paint paintBorder = new Paint();
//		paintBorder.setColor(Color.BLUE);
//		paintBorder.setStyle(Paint.Style.STROKE);
//		paintBorder.setStrokeWidth(6*displayMetrics.density);
//
//		Rect r = new Rect(0,0,view.getWidth(),view.getHeight());
//		canvas.drawRect(r,paintFill);
//		canvas.drawRect(r,paintBorder);
		switch (bitmapMode) {
			case UNSAFE:
				canvas.drawBitmap(this.bitmap, imageToView, null);
				break;

			case DOUBLE_BUFFER:
				bitmapLock.lock();
				try {
					canvas.drawBitmap(this.bitmap, imageToView, null);
				} finally {
					bitmapLock.unlock();
				}
				break;

			case NONE:
				break;
		}
	}

	/**
	 * Custom view for visualizing results
	 */
	public class DisplayView extends SurfaceView implements SurfaceHolder.Callback {

		SurfaceHolder mHolder;

		public DisplayView( Context context ) {
			super(context);
			mHolder = getHolder();

			// configure it so that its on top and will be transparent
			setZOrderOnTop(true);    // necessary
			mHolder.setFormat(PixelFormat.TRANSPARENT);

			// if this is not set it will not draw
			setWillNotDraw(false);
		}

		@Override public void onDraw( Canvas canvas ) {onDrawFrame(this, canvas);}

		@Override public void surfaceCreated( SurfaceHolder surfaceHolder ) {}

		@Override public void surfaceChanged( SurfaceHolder surfaceHolder, int i, int i1, int i2 ) {}

		@Override public void surfaceDestroyed( SurfaceHolder surfaceHolder ) {}
	}

	/**
	 * Data related to converting between Image and BoofCV data types
	 *
	 * All class data is owned by the lock
	 */
	protected static class BoofImage {
		protected final Object imageLock = new Object();
		protected ImageType imageType = ImageType.SB_U8;
		protected ColorFormat colorFormat = ColorFormat.RGB;
		/**
		 * Images available for use, When inside the stack they must not be referenced anywhere else.
		 * When removed they are owned by the thread in which they were removed.
		 */
		protected ArrayDeque<ImageBase> stackImages = new ArrayDeque<>();
		protected GrowArray<DogArray_I8> convertWork = new GrowArray<>(DogArray_I8::new);
	}

	/**
	 * How processing of the bitmap is managed when a new frame arrives
	 */
	public enum BitmapMode {
		/**
		 * Don't render a bitmap to the display
		 */
		NONE,
		/**
		 * A single buffer and no lock to prevent one thread from read and another writing at the same time. Fastest
		 * and lowest memory but will result in multiple artifacts
		 */
		UNSAFE,
		/**
		 * Two buffers are used to avoid read and writing from occurring at the same time. No artifacts but
		 * uses more memory.
		 */
		DOUBLE_BUFFER
	}
}
