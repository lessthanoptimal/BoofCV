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


package org.boofcv.video;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.android.ConvertBitmap;
import boofcv.android.VisualizeImageData;
import boofcv.android.camera2.VisualizeCamera2Activity;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Demonstrates how to use the visualize activity. This greatly simplifies
 * the process of capturing and visualizing image data from a camera.
 * Internally it uses the camera 2 API. You can customize its behavior by overriding
 * different internal functions.
 *
 * @see VisualizeCamera2Activity
 * @see boofcv.android.camera2.SimpleCamera2Activity
 *
 * @author Peter Abeles
 */
public class VideoActivity extends VisualizeCamera2Activity
{
	// Storage for the gradient
	private GrayS16 derivX = new GrayS16(1,1);
	private GrayS16 derivY = new GrayS16(1,1);

	// computes the image gradient
	private ImageGradient<GrayU8,GrayS16> gradient = FactoryDerivative.three(GrayU8.class, GrayS16.class);

	public VideoActivity() {
		// The default behavior for selecting the camera's resolution is to
		// find the resolution which comes the closest to having this many
		// pixels.
		targetResolution = 640*480;
		// This behavior can be changed as well as which camera is selected
		// by overriding functions defined in SimpleCamera2Activity

		// Tell the visualization activity that we want to handle
		// rendering the bitmap. If this is set to false it will overwrite our
		// changes
		super.showBitmap = false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.video);
		FrameLayout surface = findViewById(R.id.camera_frame);

		// Tell it that a gray scale image is being processed. It will
		// automatically convert the video frame into this format
		// RGB is also supported
		setImageType(ImageType.single(GrayU8.class));

		// The bitmap modified below will be draw in the surface provided
		// to start camera. A camera preview can also be shown by providing
		// a texture view.
		startCamera(surface,null);
	}

	/**
	 * This is called when the video resolution is known. Data structures
	 * can be initialized now.
	 */
	@Override
	protected void onCameraResolutionChange( int width , int height ) {
		super.onCameraResolutionChange(width, height);

		derivX.reshape(width, height);
		derivY.reshape(width, height);

		// If showBitmap was set to false you wouldn't need to do this
		synchronized (bitmapLock) {
			if (bitmap.getWidth() != width || bitmap.getHeight() != height)
				bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			bitmapTmp = ConvertBitmap.declareStorage(bitmap, bitmapTmp);
		}
	}

	/**
	 * This function is invoked in its own thread and can take as long as you want.
	 */
	@Override
	protected void processImage(ImageBase image) {
		// The image type for image was specified in onResume()
		gradient.process((GrayU8)image,derivX,derivY);

		// the bitmap will be rendered onto surface previded earlier
		synchronized (bitmapLock) {
			VisualizeImageData.colorizeGradient(derivX,derivY,-1, bitmap, bitmapTmp);
		}
	}

	/**
	 * Invoked in the UI thread and must run very fast for a good user experience
	 */
	@Override
	protected void onDrawFrame(SurfaceView view , Canvas canvas ) {
		super.onDrawFrame(view, canvas);

		// The imageToView variable specifies the transform from a video frame pixel
		// to a pixel in the view it's being displayed in. Sensor and camera rotations
		// as well as scaling along x and y axises are all handled.
		synchronized (bitmapLock) {
			canvas.drawBitmap(bitmap, imageToView, null);
		}
	}

}
