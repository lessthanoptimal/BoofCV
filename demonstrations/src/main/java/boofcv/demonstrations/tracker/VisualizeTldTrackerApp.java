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

package boofcv.demonstrations.tracker;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.tracker.tld.ConfigTld;
import boofcv.alg.tracker.tld.TldTracker;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.image.ShowImages;
import boofcv.gui.tracker.TldVisualizationPanel;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.shapes.Rectangle2D_F64;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Shows TLD track data
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class VisualizeTldTrackerApp<T extends ImageGray<T>, D extends ImageGray<D>>
		implements TldVisualizationPanel.Listener {

	TldTracker<T, D> tracker;

	TldVisualizationPanel gui = new TldVisualizationPanel(this);

	T image;

	boolean paused;

	public VisualizeTldTrackerApp( Class<T> imageType ) {

		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);

		InterpolatePixelS<T> interpolate = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);
		ImageGradient<T, D> gradient = FactoryDerivative.sobel(imageType, derivType);

		tracker = new TldTracker<>(new ConfigTld(), interpolate, gradient, imageType, derivType);
	}

	public void process( final SimpleImageSequence<T> sequence ) {

		if (!sequence.hasNext())
			throw new IllegalArgumentException("Empty sequence");

		image = sequence.next();
		gui.setFrame((BufferedImage)sequence.getGuiImage());
		ShowImages.showWindow(gui, "TLD Tracker", true);

//		tracker.initialize(image,274,159,356,292);
//		gui.turnOffSelect();

		paused = true;

		while (paused) {
			BoofMiscOps.sleep(10);
		}

		int totalFrames = 0;
		long totalTime = 0;

		while (sequence.hasNext()) {
			totalFrames++;

			image = sequence.next();
			gui.setFrame((BufferedImage)sequence.getGuiImage());

			long before = System.nanoTime();
			boolean success = tracker.track(image);
			long after = System.nanoTime();

			totalTime += after - before;
			System.out.println("FPS = " + totalFrames/(totalTime/2e9));

			gui.update(tracker, success);

			if (!success) {
				System.out.println("No rectangle found");
			} else {
				Rectangle2D_F64 r = tracker.getTargetRegion();
				System.out.println("Target: " + r);
			}
			gui.repaint();

			while (paused) {
				BoofMiscOps.sleep(10);
			}
		}
		System.out.println("DONE");
	}

	@Override
	public void startTracking( int x0, int y0, int x1, int y1 ) {
		tracker.initialize(image, x0, y0, x1, y1);
		paused = false;
	}

	@Override
	public void togglePause() {
		paused = !paused;
	}

	public static void main( String[] args ) {
		var app = new VisualizeTldTrackerApp(GrayU8.class);

		String fileName = UtilIO.pathExample("tracking/track_book.mjpeg");

		SimpleImageSequence<GrayU8> sequence =
				DefaultMediaManager.INSTANCE.openVideo(fileName, ImageType.single(GrayU8.class));

		app.process(Objects.requireNonNull(sequence));
	}
}
