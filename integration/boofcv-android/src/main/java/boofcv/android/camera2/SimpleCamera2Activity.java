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
import android.content.res.Resources;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
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

	private CameraCaptureSession mPreviewSession;
	protected TextureView mTextureView;
	protected View mView;

	//######## START  Variables owned by lock
	private CameraOpen open = new CameraOpen();
	//######## END

	// width and height of the view the camera is displayed in
	protected int viewWidth,viewHeight;
	// ratio of image and screen density
	protected float cameraToDisplayDensity;

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

	protected DisplayMetrics displayMetrics;

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
	protected void onCreate(Bundle savedInstanceState) {
		if( verbose )
			Log.i(TAG,"onCreate()");
		super.onCreate(savedInstanceState);
		displayMetrics = Resources.getSystem().getDisplayMetrics();
	}

	@Override
	protected void onResume() {
		if( verbose )
			Log.i(TAG,"onResume()");
		super.onResume();

		// When attached to a change listener below it's possible for the activity to be shutdown and a change
		// in layout be broadcast after that. In that situation we don't want the camera to be opened!
		startBackgroundThread();
		// At this point in time the camera should be closed. It might not be due to rapid transitions between
		// onResume() and onPause(). Not sure why that happens but it's in the error reports.
		open.mLock.lock();
		try {
			switch (open.state) {
				// not sure how to recover from this. This means there's an asynch task that will execute sometime
				// in the future. It might have out of date information so I can't just let it be.
				case OPENING:
					throw new RuntimeException("Camera shouldn't be in opening state when starting onResume()");

				// It wants to be closed so let's just finish that
				case CLOSING:{
					if( verbose )
						Log.i(TAG, " camera is closing. Going to just close it now. device="+
								(open.mCameraDevice==null));
					if( open.mCameraDevice != null ) {
						open.closeCamera();
					}
				} break;

				case OPEN:
					throw new RuntimeException("Camera is opened. Was not cleaned up correctly onPause()");

				case CLOSED: // the state it should be in!
					break;

				default:
					throw new RuntimeException("New state was added and this needs to be updated. "+open.state);
			}
			// If everything went well above it's now in the opening state. This is set now because some of the options
			// might finish up later on. It's possible for a close request to come in before that has happened.
			open.state = CameraState.OPENING;
		} finally {
			open.mLock.unlock();
		}
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
		} else if( open.mCameraDevice == null ) {
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
	protected void onCameraResolutionChange( int cameraWidth , int cameraHeight ,
											 int orientation ) {
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
	protected boolean selectCamera( String id , CameraCharacteristics characteristics ) {
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
			Log.i(TAG,"openCamera( texture: "+widthTexture+"x"+heightTexture+") activity="+getClass().getSimpleName());

		if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
			throw new RuntimeException("Attempted to openCamera() when not in the main looper thread!");
		}

		if (isFinishing()) {
			return;
		}
		CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
		if( manager == null )
			throw new RuntimeException("Null camera manager");

		// The camera should be released here until a camera has been successfully initialized
		boolean releaseLock = true;
		try {
			if (!open.mLock.tryLock(2500, TimeUnit.MILLISECONDS)) {
				throw new RuntimeException("Time out waiting to lock camera opening.");
			}

			if( open.state == CameraState.CLOSING) {
				if( verbose )
					Log.d(TAG, "Close request was made after the open request. Aborting and closing. device="
					+(open.mCameraDevice==null));
				if( open.mCameraDevice != null ) {
					open.closeCamera();
				}
				open.state = CameraState.CLOSED;
				open.clearCamera();
				return;
			} else if( open.state == CameraState.CLOSED || open.state == CameraState.OPENING) {
				// These are the two states it should be in. if it wasn't opening before it is now
				open.state = CameraState.OPENING;
			} else {
				throw new RuntimeException("Unexpected state="+open.state);
			}

			if( mBackgroundHandler == null ) {
				if( verbose )
					Log.i(TAG,"Background handler is null. Aborting.");
				return;
			}

			if( open.mCameraDevice != null ) {
				throw new RuntimeException("Tried to open camera with one already open");
			}

			// Save the size of the component the camera feed is being displayed inside of
			this.viewWidth = widthTexture;
			this.viewHeight = heightTexture;
			this.cameraToDisplayDensity = 0;
			this.firstFrame = true;

			String[] cameras = manager.getCameraIdList();
			for( String cameraId : cameras ) {
				CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
				if(!selectCamera(cameraId,characteristics))
					continue;

				StreamConfigurationMap map = characteristics.
						get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
				if (map == null) {
					continue;
				}

				Size[] sizes = map.getOutputSizes(ImageFormat.YUV_420_888);
				int which = selectResolution(widthTexture, heightTexture,sizes);
				if( which < 0 || which >= sizes.length )
					continue;
				open.mCameraSize = sizes[which];
				open.cameraId = cameraId;
				open.mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

				this.cameraToDisplayDensity = displayDensityAdjusted();

				if( verbose )
					Log.i(TAG,"selected cameraId="+cameraId+" orientation="+open.mSensorOrientation+
							" res="+open.mCameraSize.getWidth()+"x"+open.mCameraSize.getHeight());

				open.mCameraCharacterstics = characteristics;
				onCameraResolutionChange(
						open.mCameraSize.getWidth(), open.mCameraSize.getHeight(),
						open.mSensorOrientation);

				open.mPreviewReader = ImageReader.newInstance(
						open.mCameraSize.getWidth(), open.mCameraSize.getHeight(),
						ImageFormat.YUV_420_888, 2);
				// Do the processing inside the the handler thread instead of the looper thread to avoid
				// grinding the UI to a halt
				open.mPreviewReader.setOnImageAvailableListener(onAvailableListener, mBackgroundHandler);
				configureTransform(widthTexture, heightTexture);
				manager.openCamera(cameraId, mStateCallback, null);
				releaseLock = false;
				return;
			}

			if( handleNoCameraSelected() ) {
				Toast.makeText(this, "No camera selected!", Toast.LENGTH_LONG).show();
				finish();
			}
		} catch (CameraAccessException e) {
			Toast.makeText(this, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
			if( handleCameraOpenException(e)) {
				finish();
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
			Log.e(TAG,"Null pointer in openCamera()");
			if( handleCameraOpenException(e)) {
				// Currently an NPE is thrown when the Camera2API is used but not supported on the
				// device this code runs.
				Toast.makeText(this, "Null pointer. Camera2 API not supported?", Toast.LENGTH_LONG).show();
				finish();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted while trying to lock camera opening.");
		} finally {
			if( releaseLock )
				open.mLock.unlock();
		}
	}

	/**
	 * Called if no camera was selected when trying to open a camera
	 * @return true means finish() the activity and show toast
	 */
	protected boolean handleNoCameraSelected() {
		return true;
	}

	/**
	 * An exception happened while trying to open the camera
	 * @return true means finish() the activity and show toast
	 */
	protected boolean handleCameraOpenException( Exception e ) {
		return true;
	}

	/**
	 * Re-opens the camera with the same settings at the specified resolution. It is assumed that you know
	 * what you're doing and that this is a valid resolution.
	 *
	 * WARNING: UNTESTED
	 */
	protected void reopenCameraAtResolution(int cameraWidth, int cameraHeight) {

		if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
			throw new RuntimeException("Attempted to reopenCameraAtResolution main looper thread!");
		}

		boolean releaseLock = true;
		open.mLock.lock();
		try {
			if (verbose)
				Log.i(TAG, "Reopening camera is null == " + (open.mCameraDevice == null)+" state="+open.state+
				" activity="+getClass().getSimpleName());

			if( open.state != CameraState.OPEN )
				throw new RuntimeException("BUG! Attempted to re-open camera when not open");

			if (null == open. mCameraDevice) {
				throw new RuntimeException("Can't re-open a closed camera");
			}
			closePreviewSession();
			open.mCameraSize = null;
			firstFrame = true;

			CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
			if (manager == null)
				throw new RuntimeException("Null camera manager");

			try {
				open.mPreviewReader = ImageReader.newInstance(
						cameraWidth, cameraHeight,
						ImageFormat.YUV_420_888, 2);
				// Do the processing inside the the handler thread instead of the looper thread to avoid
				// grinding the UI to a halt
				open.mPreviewReader.setOnImageAvailableListener(onAvailableListener, mBackgroundHandler);
				configureTransform(viewWidth, viewHeight);
				manager.openCamera(open.cameraId, mStateCallback, null);
				releaseLock = false;
			} catch (IllegalArgumentException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				finish();
			} catch (CameraAccessException e) {
				e.printStackTrace();
			}
		} finally {
			if(releaseLock)
				open.mLock.unlock();
		}
	}

	/**
	 * Closes the camera. Returns true if the camera was not already closed and it closed it
	 * @return
	 */
	protected boolean closeCamera() {
		if( verbose )
			Log.i(TAG,"closeCamera() activity="+getClass().getSimpleName());
		if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
			throw new RuntimeException("Attempted to close camera not on the main looper thread!");
		}

		boolean closed = false;

//		if( verbose ) {
//			StackTraceElement[] trace = new RuntimeException().getStackTrace();
//			for (int i = 0; i < Math.min(trace.length, 3); i++) {
//				System.out.println("[ " + i + " ] = " + trace[i].toString());
//			}
//		}

		// NOTE: Since open can only be called in the main looper this won't be enough to prevent
		// it from closing before it opens. That's why open.state exists
		open.mLock.lock();
		try {
			if( verbose )
				Log.i(TAG,"closeCamera: camera="+(open.mCameraDevice==null)+" state="+open.state);
			closePreviewSession();
			// close has been called while trying to open the camera!
			if( open.state == CameraState.OPENING ) {
				// If it's in this state that means an asych task is opening the camera. By changing the state
				// to closing it will not abort that process when the task is called.
				open.state = CameraState.CLOSING;
				if( open.mCameraDevice != null ) {
					throw new RuntimeException("BUG! Camera is opening and should be null until opened");
				}
			} else {
				if (null != open.mCameraDevice) {
					closed = true;
					open.closeCamera();
				}
				open.state = CameraState.CLOSED;
				open.clearCamera();
			}
		} finally {
			open.mLock.unlock();
		}
		return closed;
	}

	/**
	 * Start the camera preview.
	 */
	private void startPreview() {
		// Sanity check. Parts of this code assume it's on this thread. If it has been put into a handle
		// that's fine just be careful nothing assumes it's on the main looper
		if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
			throw new RuntimeException("Not on main looper! Modify code to remove assumptions");
		}

		if( verbose ) {
			Log.i(TAG,"startPreview()");
		}

		try {
			open.mLock.lock();

			if (null == open.mCameraDevice || null == open.mCameraSize) {
				Log.i(TAG,"  aborting startPreview. Camera not open yet.");
				return;
			}

			closePreviewSession();
			List<Surface> surfaces = new ArrayList<>();
			open.mPreviewRequestBuilder = open.mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

			if( mTextureView != null && mTextureView.isAvailable() ) {
				SurfaceTexture texture = mTextureView.getSurfaceTexture();
				assert texture != null;
				texture.setDefaultBufferSize(open.mCameraSize.getWidth(), open.mCameraSize.getHeight());

				// Display the camera preview into this texture
				Surface previewSurface = new Surface(texture);
				surfaces.add(previewSurface);
				open.mPreviewRequestBuilder.addTarget(previewSurface);
			}

			// This is where the image for processing is extracted from
			Surface readerSurface = open.mPreviewReader.getSurface();
			surfaces.add(readerSurface);
			open.mPreviewRequestBuilder.addTarget(readerSurface);

			configureCamera(open.mPreviewRequestBuilder);

			open.mCameraDevice.createCaptureSession(surfaces,
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
		} finally {
			open.mLock.unlock();
		}
	}

	/**
	 * Update the camera preview. {@link #startPreview()} needs to be called in advance.
	 */
	private void updatePreview() {
		if (null == open.mCameraDevice) {
			return;
		}
		open.mLock.lock();
		try {
			mPreviewSession.setRepeatingRequest(open.mPreviewRequestBuilder.build(), null, mBackgroundHandler);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		} finally {
			open.mLock.unlock();
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
		int cameraWidth,cameraHeight;
		try {
			open.mLock.lock();
			if (null == mTextureView || null == open.mCameraSize) {
				return;
			}
			cameraWidth = open.mCameraSize.getWidth();
			cameraHeight = open.mCameraSize.getHeight();
		} finally {
			open.mLock.unlock();
		}

		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		Matrix matrix = new Matrix();
		RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
		RectF bufferRect = new RectF(0, 0, cameraHeight, cameraWidth);// TODO why w/h swapped?
		float centerX = viewRect.centerX();
		float centerY = viewRect.centerY();
		if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
			bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
			matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
			float scale = Math.max(
					(float) viewHeight / cameraHeight,
					(float) viewWidth / cameraWidth);
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
				Log.i(TAG,"onLayoutChange() TL="+top+"x"+left+" view="+width+"x"+height+" mCameraSize="+(open.mCameraSize!=null));
			if( open.mCameraSize == null ) {
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
				Log.i(TAG,"onSurfaceTextureAvailable() view="+width+"x"+height+" mCameraSize="+(open.mCameraSize!=null));
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
				Log.i(TAG,"CameraDevice Callback onOpened() id="+cameraDevice.getId()+" camera="+open.state);
			if( !open.mLock.isLocked() )
				throw new RuntimeException("Camera not locked!");
			if( open.mCameraDevice != null )
				throw new RuntimeException("onOpen() and mCameraDevice is not null");

			boolean success = false;
			try {
				open.mCameraDevice = cameraDevice;
				if (open.state == CameraState.OPENING) {
					open.state = CameraState.OPEN;
					startPreview();
					if (null != mTextureView) {
						configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
					}
					success = true;
				} else if (open.state == CameraState.CLOSING) {
					// Closed was called when trying to open the camera
					// abort opening and immediately close it
					open.closeCamera();
				} else {
					open.mCameraDevice = null;
					throw new RuntimeException("Unexpected camera state "+open.state);
				}
			} finally {
				open.mLock.unlock();
			}
			if( success )
				onCameraOpened(cameraDevice);
		}

		@Override
		public void onDisconnected(@NonNull CameraDevice cameraDevice) {
			if( verbose )
				Log.i(TAG,"CameraDevice Callback onDisconnected() id="+cameraDevice.getId());

			boolean unexpected = !open.mLock.isLocked();
			if( unexpected ) {
				open.mLock.lock();
			}
			try {
				open.mCameraDevice = cameraDevice;
				open.closeCamera();
			} finally {
				open.mLock.unlock();
			}
			onCameraDisconnected(cameraDevice);
			if( unexpected) {
				// the camera disconnected and no request to disconnect it was made by
				// the application. not really sure what to do here. Restarting the activity
				// seems reasonable
				Log.e(TAG,"  Camera disconnection was unexpected. Restarting activity");
				recreate();
			}
		}

		@Override
		public void onError(@NonNull CameraDevice cameraDevice, int error) {
			if( verbose )
				Log.e(TAG,"CameraDevice Callback onError() error="+error);
			boolean unexpected = !open.mLock.isLocked();
			if( unexpected ) {
				open.mLock.lock();
			}
			try {
				open.mCameraDevice = cameraDevice;
				open.closeCamera();
			} finally {
				open.mLock.unlock();
			}
			// If the camera was locked that means it has an error when trying to open it
			if( unexpected )
				Log.e(TAG,"   No lock applied to the camera. Unexpected problem?");
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
		open.mLock.lock();
		try {
			SizeF sensorSize = null;
			float[] focalLengths = null;

			// This might be called before the camera is open
			if( open.mCameraCharacterstics != null ) {
				sensorSize = open.mCameraCharacterstics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
				focalLengths = open.mCameraCharacterstics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
			}

			if (focalLengths == null || sensorSize == null ) {
				// 60 degrees seems reasonable for a random guess
				double hfov = UtilAngle.radian(60);
				if( open.mCameraSize == null ) {
					Log.w(TAG,"Requesting FOV when the camera isn't open yet!");
					return new double[]{hfov, hfov};
				} else {

					double vfov = hfov * open.mCameraSize.getHeight() / open.mCameraSize.getWidth();
					return new double[]{hfov, vfov};
				}
			} else {
				double hfov = 2 * Math.atan(sensorSize.getWidth() / (2 * focalLengths[0]));
				double vfov = 2 * Math.atan(sensorSize.getHeight() / (2 * focalLengths[0]));
				return new double[]{hfov, vfov};
			}
		} finally {
			open.mLock.unlock();
		}
	}

	// This is run in the background handler and not the looper
	private ImageReader.OnImageAvailableListener onAvailableListener = imageReader -> {
		if( imageReader.getMaxImages() == 0 ) {
			Log.e(TAG,"No images available. Has image.close() not been called?");
			return;
		}

		Image image = imageReader.acquireLatestImage();
		if (image == null)
			return;
		try {
			// safely acquire the camera resolution
			int cameraWidth, cameraHeight, cameraOrientation;
			open.mLock.lock();
			try {
				if (open.mCameraSize == null || open.state != CameraState.OPEN)
					return;
				cameraWidth = open.mCameraSize.getWidth();
				cameraHeight = open.mCameraSize.getHeight();
				cameraOrientation = open.mSensorOrientation;
			} finally {
				open.mLock.unlock();
			}

			if (firstFrame) {
				firstFrame = false;
				canProcessImages = false;
				// sometimes we request a resolution and Android say's f-you and gives us something else even if it's
				// in the valid list. Re-adjust everything to what the actual resolution is
				if (cameraWidth != image.getWidth() || cameraHeight != image.getHeight()) {
					Log.e(TAG, "Android broke resolution contract. Actual=" + image.getWidth() + "x" + image.getHeight() +
							"  Expected=" + cameraWidth + "x" + cameraHeight);
					open.mLock.lock();
					try {
						if (open.mCameraSize == null)
							return;
						open.mCameraSize = new Size(image.getWidth(), image.getHeight());
					} finally {
						open.mLock.unlock();
					}
					runOnUiThread(() -> {
						configureTransform(viewWidth, viewHeight);
						onCameraResolutionChange(cameraWidth, cameraHeight, cameraOrientation);
						canProcessImages = true;
					});
				} else {
					canProcessImages = true;
				}
			}
			if (canProcessImages) {
				processFrame(image);
			}
		} catch( IllegalStateException e ) {
			// Looks like there are situations where a camera is closed and the images
			// freed but this function gets called anyways. if that happens any access to
			// the image will cause an IllegalStateException be thrown.
			if( verbose ) {
				Log.e(TAG,"OnImageAvailableListener exception="+e.getMessage());
			}
			handleOnImageAvailableException(e);
		} finally {
			// WARNING: It's not documented if Image is thread safe or not. it's implied that it because
			// Google's examples show it being closed and processed in a thread other than looper.
			image.close();
		}
	};

	/**
	 * An exception was thrown inside of OnImageAvailableListener. See code comments for details
	 */
	protected void handleOnImageAvailableException( RuntimeException e ) {}

	/**
	 * Some times the size of a font of stroke needs to be specified in the input image
	 * but then gets scaled to image resolution. This compensates for that.
	 */
	private float displayDensityAdjusted() {
		open.mLock.lock();
		try {
			if (open.mCameraSize == null)
				return displayMetrics.density;

			int rotation = getWindowManager().getDefaultDisplay().getRotation();
			int screenWidth = (rotation == 0 || rotation == 2) ? displayMetrics.widthPixels : displayMetrics.heightPixels;
			int cameraWidth = open.mSensorOrientation == 0 || open.mSensorOrientation == 180 ?
					open.mCameraSize.getWidth() : open.mCameraSize.getHeight();

			return displayMetrics.density * cameraWidth / screenWidth;
		} finally {
			open.mLock.unlock();
		}
	}

	/**
	 * All these variables are owned by the camera open lock
	 */
	static class CameraOpen
	{
		ReentrantLock mLock = new ReentrantLock();
		CameraState state = CameraState.CLOSED;
		CameraDevice mCameraDevice;
		Size mCameraSize; // size of camera preview
		String cameraId; // the camera that was selected to view
		int mSensorOrientation; // sensor's orientation
		// describes physical properties of the camera
		CameraCharacteristics mCameraCharacterstics;

		// Image reader for capturing the preview
		private ImageReader mPreviewReader;
		private CaptureRequest.Builder mPreviewRequestBuilder;


		public void closeCamera() {
			state = CameraState.CLOSED;
			mCameraDevice.close();
			mPreviewReader.close();
			// TODO do targets need to be removed from mPreviewRequestBuilder?

			clearCamera();
		}

		public void clearCamera() {
			mCameraCharacterstics = null;
			mCameraDevice = null;
			mCameraSize = null;
			cameraId = null;
			mSensorOrientation = 0;
			mPreviewReader = null;
			mPreviewRequestBuilder = null;
		}
	}

	protected enum CameraState {
		CLOSED,
		/**
		 * The camera enters into this state the second a request is made to open the camera. At this point
		 * none of the device isn't known.
		 */
		OPENING,
		OPEN,
		/**
		 * When in the closing state that means the camera was in the opening state when a close request was
		 * sent. At various points in the opening process it should see if its in this state. If the camera
		 * device is not null then the camera should be shut down. If null then just set the state to closed.
		 */
		CLOSING
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
}
