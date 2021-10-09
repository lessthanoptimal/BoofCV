/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.ddogleg.struct.DogArray;

import java.util.List;
import java.util.Locale;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.android.camera2.VisualizeCamera2Activity;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.misc.MovingAverage;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

/**
 * Demonstrates how to detect a QR Code and visualize the results.
 *
 * @author Peter Abeles
 * @see VisualizeCamera2Activity
 * @see boofcv.android.camera2.SimpleCamera2Activity
 */
public class QrCodeActivity extends VisualizeCamera2Activity {
	// QR Code detector. Use default configuration
	private final QrCodeDetector<GrayU8> detector = FactoryFiducial.qrcode(null, GrayU8.class);

	// Used to display text info on the display
	private final Paint paintText = new Paint();

	private final Paint colorDetected = new Paint();

	// Storage for bounds of found QR Codes
	private final DogArray<Polygon2D_F64> foundQR = new DogArray<>(Polygon2D_F64::new);
	private String message = ""; // most recently decoded QR code

	// Used to compute average time in the detector
	private final MovingAverage timeDetection = new MovingAverage();

	// where the decoded QR's message is printed
	private TextView textMessageView;

	// work space for display
	Path path = new Path();

	public QrCodeActivity() {
		// The default behavior for selecting the camera's resolution is to
		// find the resolution which comes the closest to having this many
		// pixels.
		targetResolution = 1024*768;
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate(savedInstanceState);

		// Java 1.8 issues with older SDK versions
		BoofConcurrency.USE_CONCURRENT = android.os.Build.VERSION.SDK_INT >= 24;

		setContentView(R.layout.qrcode);
		FrameLayout surface = findViewById(R.id.camera_frame);
		textMessageView = findViewById(R.id.qrmessage);

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

		// Color that detected QR will be painted
		colorDetected.setARGB(0xA0, 0, 0xFF, 0);
		colorDetected.setStyle(Paint.Style.FILL);

		// The camera stream will now start after this function is called.
		startCamera(surface, null);
	}

	/**
	 * This function is invoked in its own thread and can take as long as you want.
	 */
	@Override
	protected void processImage( ImageBase image ) {
		// Detect the QR Code
		// GrayU8 image was specified in onCreate()
		long time0 = System.nanoTime();
		detector.process((GrayU8)image);
		long time1 = System.nanoTime();
		timeDetection.update((time1 - time0)*1e-6);

		// Create a copy of what we will visualize here. In general you want a copy because
		// the UI and image processing is done on two different threads
		synchronized (foundQR) {
			foundQR.reset();
			List<QrCode> found = detector.getDetections();
			for (int i = 0; i < found.size(); i++) {
				QrCode qr = found.get(i);
				foundQR.grow().setTo(qr.bounds);
				message = qr.message;
			}
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
		canvas.drawText(String.format(Locale.getDefault(),
						"detector: %4.1f (ms)", timeDetection.getAverage()),
				180, 170, paintText);

		// This line is magical and will save you hours/days of anguish
		// What it does is correctly convert the coordinate system from
		// image pixels that were processed into display coordinates
		// taking in account rotations and weird CCD layouts
		canvas.concat(imageToView);

		// Draw the bounding squares around the QR Codes
		synchronized (foundQR) {
			for (int foundIdx = 0; foundIdx < foundQR.size(); foundIdx++) {
				renderPolygon(foundQR.get(foundIdx), path, canvas, colorDetected);
			}
			if (foundQR.size() > 0)
				textMessageView.setText(message);
		}

		// Pro tip: Run in app fast or release mode for a dramatic speed up!
		// In Android Studio expand "Build Variants" tab on left.
	}

	public static void renderPolygon( Polygon2D_F64 s, Path path, Canvas canvas, Paint paint ) {
		path.reset();
		for (int j = 0; j < s.size(); j++) {
			Point2D_F64 p = s.get(j);
			if (j == 0)
				path.moveTo((float)p.x, (float)p.y);
			else
				path.lineTo((float)p.x, (float)p.y);
		}
		Point2D_F64 p = s.get(0);
		path.lineTo((float)p.x, (float)p.y);
		path.close();
		canvas.drawPath(path, paint);
	}
}
