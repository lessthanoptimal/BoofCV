/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;

/**
 * Displays camera preview.  Android forces the camera preview to be displayed at all times.  This gets
 * around that restriction by making the preview to be 2x2 pixel big .
 *
 * @author Peter Abeles
 */
public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback {
	private final String TAG = "CameraPreview";

	SurfaceView mSurfaceView;
	SurfaceHolder mHolder;
	Camera mCamera;
	Camera.PreviewCallback previewCallback;
	boolean hidden;

	public CameraPreview(Context context, Camera.PreviewCallback previewCallback, boolean hidden ) {
		super(context);
		this.previewCallback = previewCallback;
		this.hidden = hidden;

		mSurfaceView = new SurfaceView(context);
		addView(mSurfaceView);

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = mSurfaceView.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void setCamera(Camera camera) {
		mCamera = camera;
		if (mCamera != null) {
			// We purposely disregard child measurements so that the SurfaceView will center the camera
			// preview instead of stretching it.
			startPreview();
			// Need to adjust the layout for the new camera
			requestLayout();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if( hidden ) {
			this.setMeasuredDimension(2, 2);
		} else {
			// We purposely disregard child measurements because want it to act as a
			// wrapper to a SurfaceView that centers the camera preview instead
			// of stretching it.
			final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
			final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
			setMeasuredDimension(width, height);
		}

	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if( mCamera == null )
			return;

		if (changed && getChildCount() > 0) {
			final View child = getChildAt(0);

			final int width = r - l;
			final int height = b - t;

			Camera.Size size = mCamera.getParameters().getPreviewSize();
			int previewWidth = size.width;
			int previewHeight = size.height;

			// Center the child SurfaceView within the parent.
			if (width * previewHeight > height * previewWidth) {
				final int scaledChildWidth = previewWidth * height / previewHeight;
				l = (width - scaledChildWidth) / 2;
				t = 0;
				r = (width + scaledChildWidth) / 2;
				b = height;
			} else {
				final int scaledChildHeight = previewHeight * width / previewWidth;
				l = 0;
				t = (height - scaledChildHeight) / 2;
				r = width;
				b = (height + scaledChildHeight) / 2;
			}
			child.layout(l,t,r,b);
//			Log.w("BoofCV","onLayout did stuff");
		} else {
//			Log.w("BoofCV","onLayout nothing done");
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		try {
			if (mCamera != null) {
				mCamera.setPreviewDisplay(holder);
			}
		} catch (IOException exception) {
			Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		if (mCamera != null) {
			mCamera.stopPreview();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		if( mCamera == null )
			return;

		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.

		if (mHolder.getSurface() == null){
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e){
			// ignore: tried to stop a non-existent preview
		}

		// start preview with new settings
		startPreview();
	}

	private void startPreview() {
		try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.setPreviewCallback(previewCallback);
			mCamera.startPreview();
		} catch (Exception e){
			Log.e(TAG, "Error starting camera preview: " + e.getMessage());
		}
	}

}

