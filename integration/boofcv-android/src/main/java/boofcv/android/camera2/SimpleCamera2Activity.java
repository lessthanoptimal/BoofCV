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

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
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
 *     <li>{@link #onCameraOpened}</li>
 *     <li>{@link #onCameraDisconnected}</li>
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
public abstract class SimpleCamera2Activity extends Activity {
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

	// Is this the first frame being processed. Sanity checks are done on the first frame
	private volatile boolean firstFrame;
	private volatile boolean canProcessImages;

	// If true there will be verbose output to Log
	protected boolean verbose = true;

	/**
	 * An additional thread for running tasks that shouldn't block the UI.
	 */
	private HandlerThread mBackgroundThread;
	private Handler mBackgroundHandler;

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
		view.addOnLayoutChangeListener(mViewLayoutChangeListener);
	}

	protected void startCamera() {
		if( verbose )
			Log.i(TAG,"startCamera()");
		this.mView = null;
		this.mTextureView =null;
		runOnUiThread(()->openCamera(0,0));
	}

	/**
	 * Starts a background thread and its {@link Handler}.
	 */
	private void startBackgroundThread() {
		mBackgroundThread = new HandlerThread("CameraBackground");
		mBackgroundThread.start();
		mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
	}

	/**
	 * Stops the background thread and its {@link Handler}.
	 */
	private void stopBackgroundThread() {
		mBackgroundThread.quitSafely();
		try {
			mBackgroundThread.join();
			mBackgroundThread = null;
			mBackgroundHandler = null;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onResume() {
		if( verbose )
			Log.i(TAG,"onResume()");
		super.onResume();

		// When attached to a change listener below it's possible for the activity to but shutdown and a change
		// in layout be broadcast after that. In that situation we don't want the camera to be opened!
		startBackgroundThread();
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
				mView.addOnLayoutChangeListener(mViewLayoutChangeListener);
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
		stopBackgroundThread();
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
	 * Called when the camera's resolution has changed. This function can be called more than once
	 * each time a camera is opened, e.g. requested resolution does not match actual.
	 */
	protected void onCameraResolutionChange( int cameraWidth , int cameraHeight ) {
		if( verbose )
			Log.i(TAG,"onCameraResolutionChange( "+cameraWidth+" , "+cameraHeight+")");
	}

	/**
	 * Override to do custom configuration of the camera's settings. By default the camera
	 * is put into auto mode.
	 *
	 * @param captureRequestBuilder used to configure the camera
	 */
	protected void configureCamera( CaptureRequest.Builder captureRequestBuilder ) {
		if( verbose )
			Log.i(TAG,"configureCamera() default function");
		captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
		captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
	}

	/**
	 * By default this will select the backfacing camera. override to change the camera it selects.
	 */
	protected boolean selectCamera( CameraCharacteristics characteristics ) {
		if( verbose )
			Log.i(TAG,"selectCamera() default function");
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
	protected void openCamera(int widthTexture, int heightTexture) {
		if( verbose )
			Log.i(TAG,"openCamera( texture: "+widthTexture+"x"+heightTexture+")");

		if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
			throw new RuntimeException("Attempted to openCamera() when not in the main looper thread!");
		}

		if( mBackgroundHandler == null ) {
			if( verbose )
				Log.i(TAG,"Background handler is null. Aborting. Activity should be shutdown already");
			return;
		}

		if( mCameraDevice != null ) {
			throw new RuntimeException("Tried to open camera with one already open");
		}

		if (isFinishing()) {
			return;
		}
		CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
		if( manager == null )
			throw new RuntimeException("Null camera manager");

		// Save the size of the component the camera feed is being displayed inside of
		this.viewWidth = widthTexture;
		this.viewHeight = heightTexture;
		this.firstFrame = true;

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
					// Do the processing inside the the handler thread instead of the looper thread to avoid
					// grinding the UI to a halt
					mPreviewReader.setOnImageAvailableListener(onAvailableListener, mBackgroundHandler);
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

	/**
	 * Re-opens the camera with the same settings at the specified resolution. It is assumed that you know
	 * what you're doing and that this is a valid resolution.
	 *
	 * WARNING: UNTESTED
	 */
	protected void reopenCameraAtResolution(int cameraWidth, int cameraHeight) {

		mCameraOpenCloseLock.lock();
		if( verbose )
			Log.i(TAG,"  camera is null == "+(mCameraDevice==null));
		closePreviewSession();
		if (null != mCameraDevice) {
			mCameraDevice.close();
			mCameraDevice = null;
		}
		mCameraSize = null;
		mCameraCharacterstics = null;
		firstFrame = true;

		CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
		if( manager == null )
			throw new RuntimeException("Null camera manager");

		try {
			mPreviewReader = ImageReader.newInstance(
					cameraWidth,cameraHeight,
					ImageFormat.YUV_420_888, 2);
			// Do the processing inside the the handler thread instead of the looper thread to avoid
			// grinding the UI to a halt
			mPreviewReader.setOnImageAvailableListener(onAvailableListener, mBackgroundHandler);
			configureTransform(viewWidth, viewHeight);
			manager.openCamera(cameraId, mStateCallback, null);
		} catch( IllegalArgumentException e ) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			finish();
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Closes the camera. Returns true if the camera was not already closed and it closed it
	 * @return
	 */
	protected boolean closeCamera() {
		if( verbose )
			Log.i(TAG,"closeCamera()");
		if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
			throw new RuntimeException("Attempted to close camera not on the main looper thread!");
		}

		boolean closed = false;

		if( verbose ) {
			StackTraceElement[] trace = new RuntimeException().getStackTrace();
			for (int i = 0; i < Math.min(trace.length, 3); i++) {
				System.out.println("[ " + i + " ] = " + trace[i].toString());
			}
		}
		try {
			mCameraOpenCloseLock.lock();
			if( verbose )
				Log.i(TAG,"  camera is null == "+(mCameraDevice==null));
			closePreviewSession();
			if (null != mCameraDevice) {
				closed = true;
				mCameraDevice.close();
				mCameraDevice = null;
			}
			mCameraSize = null;
			mCameraCharacterstics = null;
		} finally {
			mCameraOpenCloseLock.unlock();
		}
		return closed;
	}

	/**
	 * Start the camera preview.
	 */
	private void startPreview() {
		if (null == mCameraDevice || null == mCameraSize) {
			return;
		}
		// Sanity check. Parts of this code assume it's on this thread. If it has been put into a handle
		// that's fine just be careful nothing assumes it's on the main looper
		if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
			throw new RuntimeException("Not on main looper! Modify code to remove assumptions");
		}

		if( verbose ) {
			Log.i(TAG,"startPreview()");
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
							Log.i(TAG,"CameraCaptureSession.onConfigureFailed()");
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
			mPreviewSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
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

	private View.OnLayoutChangeListener mViewLayoutChangeListener
			= new View.OnLayoutChangeListener() {

		@Override
		public void onLayoutChange(View view, int left, int top, int right, int bottom,
								   int leftWas, int topWas, int rightWas, int bottomWas)
		{
			int width = right-left;
			int height = bottom-top;
			if( verbose )
				Log.i(TAG,"onLayoutChange() TL="+top+"x"+left+" view="+width+"x"+height+" mCameraSize="+(mCameraSize!=null));
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
			if( verbose )
				Log.i(TAG,"onSurfaceTextureAvailable() view="+width+"x"+height+" mCameraSize="+(mCameraSize!=null));
			openCamera(width, height);
		}

		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
												int width, int height) {
			if( verbose )
				Log.i(TAG,"onSurfaceTextureSizeChanged() view="+width+"x"+height);
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
			onCameraOpened(cameraDevice);
		}

		@Override
		public void onDisconnected(@NonNull CameraDevice cameraDevice) {
			if( verbose )
				Log.i(TAG,"CameraDevice Callback onDisconnected() id="+cameraDevice.getId());
			mCameraDevice = null;
			mCameraOpenCloseLock.unlock();
			cameraDevice.close();
			onCameraDisconnected(cameraDevice);
		}

		@Override
		public void onError(@NonNull CameraDevice cameraDevice, int error) {
//			if( verbose )
			Log.e(TAG,"CameraDevice Callback onError() error="+error);
			mCameraDevice = null;
			mCameraOpenCloseLock.unlock();
			cameraDevice.close();
			finish();
		}
	};

	/**
	 * Invoked when the camera has been opened
	 */
	protected void onCameraOpened( @NonNull CameraDevice cameraDevice ){}

	/**
	 * Invoked when the camera has been disconnected
	 */
	protected void onCameraDisconnected( @NonNull CameraDevice cameraDevice ){}

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

	// This is run in the background handler and not the looper
	private ImageReader.OnImageAvailableListener onAvailableListener = imageReader -> {
		Image image = imageReader.acquireLatestImage();
		if (image == null)
			return;

		if( firstFrame ) {
			firstFrame = false;
			canProcessImages = false;
			// sometimes we request a resolution and Android say's f-you and gives us something else even if it's
			// in the valid list. Re-adjust everything to what the actual resolution is
			if( mCameraSize.getWidth() != image.getWidth() || mCameraSize.getHeight() != image.getHeight() ) {
				Log.e(TAG,"Android broke resolution contract. Actual="+image.getWidth()+"x"+image.getHeight()+
				"  Expected="+mCameraSize.getWidth()+"x"+mCameraSize.getHeight());
				mCameraSize = new Size(image.getWidth(), image.getHeight());
				runOnUiThread(() -> {
					configureTransform(viewWidth, viewHeight);
					onCameraResolutionChange(mCameraSize.getWidth(), mCameraSize.getHeight());
					canProcessImages = true;
				});
			} else {
				canProcessImages = true;
			}
		}
		if( canProcessImages ) {
			processFrame(image);
			// WARNING: It's not documented if Image is thread safe or not. it's implied that it because
			// Google's examples show it being closed and processed in a thread other than looper.
		}
		image.close();
	};

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
}
