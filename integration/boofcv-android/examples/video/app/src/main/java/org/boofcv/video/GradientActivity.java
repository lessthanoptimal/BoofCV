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
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.util.Locale;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.android.VisualizeImageData;
import boofcv.android.camera2.VisualizeCamera2Activity;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Demonstrates how to use the visualize activity. A video stream is opened and the image gradient
 * is found. The gradient is then rendered into a format which can be visualized and displayed
 * on the Android device's screen.
 *
 * This greatly simplifies the process of capturing and visualizing image data from a camera.
 * Internally it uses the camera 2 API. You can customize its behavior by overriding
 * different internal functions. For more details, see the JavaDoc of it's parent classes.
 *
 * @author Peter Abeles
 * @see VisualizeCamera2Activity
 * @see boofcv.android.camera2.SimpleCamera2Activity
 */
public class GradientActivity extends VisualizeCamera2Activity {
	// Storage for the gradient
	private GrayS16 derivX = new GrayS16(1, 1);
	private GrayS16 derivY = new GrayS16(1, 1);

	// Storage for image gradient. In general you will want to precompute data structures due
	// to the expense of garbage collection
	private ImageGradient<GrayU8, GrayS16> gradient = FactoryDerivative.three(GrayU8.class, GrayS16.class);

	// Used to display text info on the display
	private Paint paintText = new Paint();

	public GradientActivity() {
		// The default behavior for selecting the camera's resolution is to
		// find the resolution which comes the closest to having this many
		// pixels.
		targetResolution = 640*480;
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate(savedInstanceState);

		// Java 1.8 issues with older SDK versions
		BoofConcurrency.USE_CONCURRENT = android.os.Build.VERSION.SDK_INT >= 24;

		setContentView(R.layout.gradient);
		FrameLayout surface = findViewById(R.id.camera_frame);

		// By calling this function you are telling the camera library that you wish to process
		// images in a gray scale format. The video stream is typically in YUV420. Color
		// image formats are supported as RGB, YUV, ... etc, color spaces.
		setImageType(ImageType.single(GrayU8.class));

		// Configure paint used to display FPS
		paintText.setStrokeWidth(4*displayMetrics.density);
		paintText.setTextSize(14*displayMetrics.density);
		paintText.setTextAlign(Paint.Align.LEFT);
		paintText.setARGB(0xFF, 0xFF, 0xB0, 0);
		paintText.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

		// The camera stream will now start after this function is called.
		startCamera(surface, null);
	}

	/**
	 * This is where you specify custom camera settings. See {@link boofcv.android.camera2.SimpleCamera2Activity}'s
	 * JavaDoc for more functions which you can override.
	 *
	 * @param captureRequestBuilder Used to configure the camera.
	 */
	@Override
	protected void configureCamera( CameraDevice device, CameraCharacteristics characteristics, CaptureRequest.Builder captureRequestBuilder ) {
		captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
		captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
	}

	/**
	 * During camera initialization this function is called once after the resolution is known.
	 * This is a good function to override and predeclare data structres which are dependent
	 * on the video feeds resolution.
	 */
	@Override
	protected void onCameraResolutionChange( int width, int height, int sensorOrientation ) {
		super.onCameraResolutionChange(width, height, sensorOrientation);

		derivX.reshape(width, height);
		derivY.reshape(width, height);
	}

	/**
	 * This function is invoked in its own thread and can take as long as you want.
	 */
	@Override
	protected void processImage( ImageBase image ) {
		// The data type of 'image' was specified in onCreate() function
		// The line below will compute the gradient and store it in two images. One for the
		// gradient along the x-axis and the other along the y-axis
		gradient.process((GrayU8)image, derivX, derivY);
	}

	/**
	 * Override the default behavior and colorize gradient instead of converting input image.
	 */
	@Override
	protected void renderBitmapImage( BitmapMode mode, ImageBase image ) {
		switch (mode) {
			case UNSAFE: { // this application is configured to use double buffer and could ignore all other modes
				VisualizeImageData.colorizeGradient(derivX, derivY, -1, bitmap, bitmapTmp);
			}
			break;

			case DOUBLE_BUFFER: {
				VisualizeImageData.colorizeGradient(derivX, derivY, -1, bitmapWork, bitmapTmp);

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
		}
	}

	/**
	 * Demonstrates how to draw visuals
	 */
	@Override
	protected void onDrawFrame( SurfaceView view, Canvas canvas ) {
		super.onDrawFrame(view, canvas);

		// Display info on the image being process and how fast input camera
		// stream (probably in YUV420) is converted into a BoofCV format
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		canvas.drawText(String.format(Locale.getDefault(),
						"%d x %d Convert: %4.1f (ms)",
						width, height, periodConvert.getAverage()),
				0, 120, paintText);

		// Pro tip: Run in app fast or release mode for a dramatic speed up!
		// In Android Studio expand "Build Variants" tab on left.
	}
}
