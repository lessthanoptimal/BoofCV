/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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


package org.boofcv.example.android;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.FrameLayout;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.android.ConvertBitmap;
import boofcv.android.ConvertNV21;
import boofcv.android.VisualizeImageData;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;

import java.util.List;

/**
 * Demonstration of how to process a video stream on an Android device using BoofCV.  Most of the code below
 * is deals with handling Android and all of its quirks.  Video streams can be accessed in Android by processing
 * a camera preview.  Data from a camera preview comes in an NV21 image format, which needs to be converted.
 * After it has been converted it needs to be processed and then displayed.  Note that several locks are required
 * to avoid the three threads (GUI, camera preview, and processing) from interfering with each other.
 *
 * @author Peter Abeles
 */
public class VideoActivity extends Activity implements Camera.PreviewCallback {

	// camera and display objects
	private Camera mCamera;
	private Visualization mDraw;
	private CameraPreview mPreview;

	// computes the image gradient
	private ImageGradient<ImageUInt8,ImageSInt16> gradient = FactoryDerivative.three(ImageUInt8.class, ImageSInt16.class);

	// Two images are needed to store the converted preview image to prevent a thread conflict from occurring
	private ImageUInt8 gray1,gray2;
	private ImageSInt16 derivX,derivY;

	// Android image data used for displaying the results
	private Bitmap output;
	// temporary storage that's needed when converting from BoofCV to Android image data types
	private byte[] storage;

	// Thread where image data is processed
	private ThreadProcess thread;

	// Object used for synchronizing gray images
	private final Object lockGray = new Object();
	// Object used for synchronizing output image
	private final Object lockOutput = new Object();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.video);

		// Used to visualize the results
		mDraw = new Visualization(this);

		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(this,this,true);

		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);

		preview.addView(mPreview);
		preview.addView(mDraw);
	}

	@Override
	protected void onResume() {
		super.onResume();
		setUpAndConfigureCamera();
	}

	@Override
	protected void onPause() {
		super.onPause();

		// stop the camera preview and all processing
		if (mCamera != null){
			mPreview.setCamera(null);
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;

			thread.stopThread();
			thread = null;
		}
	}

	/**
	 * Sets up the camera if it is not already setup.
	 */
	private void setUpAndConfigureCamera() {
		// Open and configure the camera
		mCamera = Camera.open();

		Camera.Parameters param = mCamera.getParameters();

		// Select the preview size closest to 320x240
		// Smaller images are recommended because some computer vision operations are very expensive
		List<Camera.Size> sizes = param.getSupportedPreviewSizes();
		Camera.Size s = sizes.get(closest(sizes,320,240));
		param.setPreviewSize(s.width,s.height);
		mCamera.setParameters(param);

		// declare image data
		gray1 = new ImageUInt8(s.width,s.height);
		gray2 = new ImageUInt8(s.width,s.height);
		derivX = new ImageSInt16(s.width,s.height);
		derivY = new ImageSInt16(s.width,s.height);
		output = Bitmap.createBitmap(s.width,s.height,Bitmap.Config.ARGB_8888 );
		storage = ConvertBitmap.declareStorage(output, storage);

		// start image processing thread
		thread = new ThreadProcess();
		thread.start();

		// Start the video feed by passing it to mPreview
		mPreview.setCamera(mCamera);
	}

	/**
	 * Goes through the size list and selects the one which is the closest specified size
	 */
	public static int closest( List<Camera.Size> sizes , int width , int height ) {
		int best = -1;
		int bestScore = Integer.MAX_VALUE;

		for( int i = 0; i < sizes.size(); i++ ) {
			Camera.Size s = sizes.get(i);

			int dx = s.width-width;
			int dy = s.height-height;

			int score = dx*dx + dy*dy;
			if( score < bestScore ) {
				best = i;
				bestScore = score;
			}
		}

		return best;
	}

	/**
	 * Called each time a new image arrives in the data stream.
	 */
	@Override
	public void onPreviewFrame(byte[] bytes, Camera camera) {

		// convert from NV21 format into gray scale
		synchronized (lockGray) {
			ConvertNV21.nv21ToGray(bytes,gray1.width,gray1.height,gray1);
		}

		// Can only do trivial amounts of image processing inside this function or else bad stuff happens.
		// To work around this issue most of the processing has been pushed onto a thread and the call below
		// tells the thread to wake up and process another image
		thread.interrupt();
	}

	/**
	 * Draws on top of the video stream for visualizing computer vision results
	 */
	private class Visualization extends SurfaceView {

		Activity activity;

		public Visualization(Activity context ) {
			super(context);
			this.activity = context;

			// This call is necessary, or else the
			// draw method will not be called.
			setWillNotDraw(false);
		}

		@Override
		protected void onDraw(Canvas canvas){

			synchronized ( lockOutput ) {
				int w = canvas.getWidth();
				int h = canvas.getHeight();

				// fill the window and center it
				double scaleX = w/(double)output.getWidth();
				double scaleY = h/(double)output.getHeight();

				double scale = Math.min(scaleX,scaleY);
				double tranX = (w-scale*output.getWidth())/2;
				double tranY = (h-scale*output.getHeight())/2;

				canvas.translate((float)tranX,(float)tranY);
				canvas.scale((float)scale,(float)scale);

				// draw the image
				canvas.drawBitmap(output,0,0,null);
			}
		}
	}

	/**
	 * External thread used to do more time consuming image processing
	 */
	private class ThreadProcess extends Thread {

		// true if a request has been made to stop the thread
		volatile boolean stopRequested = false;
		// true if the thread is running and can process more data
		volatile boolean running = true;

		/**
		 * Blocks until the thread has stopped
		 */
		public void stopThread() {
			stopRequested = true;
			while( running ) {
				thread.interrupt();
				Thread.yield();
			}
		}

		@Override
		public void run() {

			while( !stopRequested ) {

				// Sleep until it has been told to wake up
				synchronized ( Thread.currentThread() ) {
					try {
						wait();
					} catch (InterruptedException ignored) {}
				}

				// process the most recently converted image by swapping image buffered
				synchronized (lockGray) {
					ImageUInt8 tmp = gray1;
					gray1 = gray2;
					gray2 = tmp;
				}

				// process the image and compute its gradient
				gradient.process(gray2,derivX,derivY);

				// render the output in a synthetic color image
				synchronized ( lockOutput ) {
					VisualizeImageData.colorizeGradient(derivX,derivY,-1,output,storage);
				}
				mDraw.postInvalidate();
			}
			running = false;
		}
	}
}
