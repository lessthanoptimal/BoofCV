/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.android.gui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Looper;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

/**
 *
 * <p>
 * Activity for processing and displaying video results.  Use of this class can save you a considerable amount of
 * effort if you wish to display processed video frames as an image.  The actual processing of image data is
 * done by a class provided to it via {@link #setProcessing}.
 * </p>
 *
 * <p>
 * Shutting down and restarting the camera as the activity transitions to different
 * phases in its life cycle is correctly handled here.  The display can also be customized.
 * User interaction and additional display widgets can be added to the activity.  To access the main content
 * view call {@link #getViewContent} and to access the video display call {@link #getViewPreview},
 * </p>
 *
 * <p>
 * If you wish to use this class and not display a preview that is also possible.  Simply set {@link #hidePreview}
 * to false.  To display the FPS which the video sequence is processed at pass in true to {@link #setShowFPS(boolean)},
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class VideoDisplayActivity extends Activity implements Camera.PreviewCallback {

	protected Camera mCamera;
	protected Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
	private Visualization mDraw;
	private CameraPreview mPreview;
	protected VideoProcessing processing;

	// Used to inform the user that its doing some calculations
	ProgressDialog progressDialog;
	protected final Object lockProgress = new Object();

	boolean hidePreview = true;
	boolean showFPS = false;

	LinearLayout contentView;
	FrameLayout preview;

	// number of degrees the preview image neesd to be rotated
	protected int previewRotation;

	public VideoDisplayActivity() {
	}

	public VideoDisplayActivity(boolean hidePreview) {
		this.hidePreview = hidePreview;
	}

	/**
	 * Changes the CV algorithm running.  Should only be called from a GUI thread.
	 */
	public void setProcessing( VideoProcessing processing ) {
		if( this.processing != null ) {
			// kill the old process
			this.processing.stopProcessing();
		}

		if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
			throw new RuntimeException("Not called from a GUI thread. Bad stuff could happen");
		}

		this.processing = processing;
		// if the camera is null then it will be initialized when the camera is initialized
		if( processing != null && mCamera != null ) {
			processing.init(mDraw,mCamera,mCameraInfo,previewRotation);
		}
	}

	/**
	 * The parent view for this activity
	 */
	public LinearLayout getViewContent() {
		return contentView;
	}

	/**
	 * The view containing the camera preview.  The first child in the parent view's list.
	 */
	public FrameLayout getViewPreview() {
		return preview;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);


		contentView = new LinearLayout(this);
		contentView.setOrientation(LinearLayout.VERTICAL);
		LayoutParams contentParam = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		preview = new FrameLayout(this);
		LayoutParams frameLayoutParam = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT,1);
		contentView.addView(preview, frameLayoutParam);

		mDraw = new Visualization(this);

		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(this,this,hidePreview);

		preview.addView(mPreview);
		preview.addView(mDraw);

		setContentView(contentView, contentParam);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if( mCamera != null )
			throw new RuntimeException("Bug, camera should not be initialized already");

		setUpAndConfigureCamera();
	}

	@Override
	protected void onPause() {
		super.onPause();

		hideProgressDialog();

		if( processing != null ) {
			VideoProcessing p = processing;
			processing = null;
			p.stopProcessing();
		}

		if (mCamera != null){
			mPreview.setCamera(null);
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}

	/**
	 * Sets up the camera if it is not already setup.
	 */
	private void setUpAndConfigureCamera() {
		// Open and configure the camera
		mCamera = openConfigureCamera(mCameraInfo);
		setCameraDisplayOrientation(mCameraInfo,mCamera);

		// Create an instance of Camera
		mPreview.setCamera(mCamera);

		if( processing != null ) {
			processing.init(mDraw,mCamera,mCameraInfo,previewRotation);
		}
	}

	/**
	 * Open the camera and configure it.
	 *
	 * @return camera
	 */
	protected abstract Camera openConfigureCamera( Camera.CameraInfo cameraInfo);

	public void setCameraDisplayOrientation(Camera.CameraInfo info,
											Camera camera) {

		int rotation = this.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0: degrees = 0; break;
			case Surface.ROTATION_90: degrees = 90; break;
			case Surface.ROTATION_180: degrees = 180; break;
			case Surface.ROTATION_270: degrees = 270; break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(result);

		previewRotation = result;
	}

	@Override
	public void onPreviewFrame(byte[] bytes, Camera camera) {
		if( processing != null )
			processing.convertPreview(bytes,camera);
	}

	/**
	 * Draws on top of the video stream for visualizing results from vision algorithms
	 */
	private class Visualization extends SurfaceView {

		private Paint textPaint = new Paint();

		double history[] = new double[10];
		int historyNum = 0;

		Activity activity;

		long previous = 0;

		public Visualization(Activity context ) {
			super(context);
			this.activity = context;

			// Create out paint to use for drawing
			textPaint.setARGB(255, 200, 0, 0);
			textPaint.setTextSize(60);
			// This call is necessary, or else the
			// draw method will not be called.
			setWillNotDraw(false);
		}

		@Override
		protected void onDraw(Canvas canvas){

			canvas.save();
			if( processing != null )
				processing.onDraw(canvas);

			// Draw how fast it is running
			long current = System.currentTimeMillis();
			long elapsed = current - previous;
			previous = current;
			history[historyNum++] = 1000.0/elapsed;
			historyNum %= history.length;

			double meanFps = 0;
			for( int i = 0; i < history.length; i++ ) {
				meanFps += history[i];
			}
			meanFps /= history.length;

			// work around an issue in marshmallow
			try {
				canvas.restore();
			} catch( IllegalStateException e ) {
				if( !e.getMessage().contains("Underflow in restore - more restores than saves"))
					throw e;
			}
			if( showFPS )
				canvas.drawText(String.format("FPS = %5.2f",meanFps), 50, 50, textPaint);
		}
	}

	/**
	 * Displays an indeterminate progress dialog.   If the dialog is already open this will change the message being
	 * displayed.  Function blocks until the dialog has been declared.
	 *
	 * @param message Text shown in dialog
	 */
	protected void setProgressMessage(final String message) {
		runOnUiThread(new Runnable() {
			public void run() {
				synchronized ( lockProgress ) {
					if( progressDialog != null ) {
						// a dialog is already open, change the message
						progressDialog.setMessage(message);
						return;
					}
					progressDialog = new ProgressDialog(VideoDisplayActivity.this);
					progressDialog.setMessage(message);
					progressDialog.setIndeterminate(true);
					progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				}

				// don't show the dialog until 1 second has passed
				long showTime = System.currentTimeMillis()+1000;
				while( showTime > System.currentTimeMillis() ) {
					Thread.yield();
				}
				// if it hasn't been dismissed, show the dialog
				synchronized ( lockProgress ) {
					if( progressDialog != null )
						progressDialog.show();
				}
			}});

		// block until the GUI thread has been called
		while( progressDialog == null  ) {
			Thread.yield();
		}
	}

	/**
	 * Dismisses the progress dialog.  Can be called even if there is no progressDialog being shown.
	 */
	protected void hideProgressDialog() {
		// do nothing if the dialog is already being displayed
		synchronized ( lockProgress ) {
			if( progressDialog == null )
				return;
		}

		if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
			// if inside the UI thread just dismiss the dialog and avoid a potential locking condition
			synchronized ( lockProgress ) {
				progressDialog.dismiss();
				progressDialog = null;
			}
		} else {
			runOnUiThread(new Runnable() {
				public void run() {
					synchronized ( lockProgress ) {
						progressDialog.dismiss();
						progressDialog = null;
					}
				}});

			// block until dialog has been dismissed
			while( progressDialog != null  ) {
				Thread.yield();
			}
		}
	}

	public boolean isShowFPS() {
		return showFPS;
	}

	public void setShowFPS(boolean showFPS) {
		this.showFPS = showFPS;
	}
}