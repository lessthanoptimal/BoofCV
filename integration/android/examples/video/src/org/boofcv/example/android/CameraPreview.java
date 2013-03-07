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

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;

/**
 * Displays (or hides) the camera preview.  Adjusts the camera preview so that the displayed ratio is the same
 * as the input image ratio.
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
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void setCamera(Camera camera) {
		mCamera = camera;
		if (mCamera != null) {
			requestLayout();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width,height;
		if( hidden ) {
			// make the view small, effectively hiding it
			width=height=2;
		} else {
			// We purposely disregard child measurements so that the SurfaceView will center the camera
			// preview instead of stretching it.
			width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
			height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
		}
		setMeasuredDimension(width, height);
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
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (mCamera == null) {
			Log.d(TAG, "Camera is null.  Bug else where in code. ");
			return;
		}

		try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.setPreviewCallback(previewCallback);
			mCamera.startPreview();
		} catch (Exception e){
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {}
}
