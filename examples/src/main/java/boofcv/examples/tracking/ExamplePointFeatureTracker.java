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

package boofcv.examples.tracking;

import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;

/**
 * <p>
 * Example of how to use the {@link boofcv.abst.tracker.PointTracker} to track different types of point features.
 * ImagePointTracker hides much of the complexity involved in tracking point features and masks
 * the very different underlying structures used by these different trackers. The default trackers
 * provided in BoofCV are general purpose trackers, that might not be the best tracker or utility
 * the underlying image features the best in all situations.
 * </p>
 *
 * @author Peter Abeles
 */
public class ExamplePointFeatureTracker<T extends ImageGray<T>, D extends ImageGray<D>> {
	// type of input image
	Class<T> imageType;
	Class<D> derivType;

	// tracks point features inside the image
	PointTracker<T> tracker;

	// displays the video sequence and tracked features
	ImagePanel gui = new ImagePanel();

	int pause;

	public ExamplePointFeatureTracker( Class<T> imageType, int pause ) {
		this.imageType = imageType;
		this.derivType = GImageDerivativeOps.getDerivativeType(imageType);
		this.pause = pause;
	}

	/**
	 * Processes the sequence of images and displays the tracked features in a window
	 */
	public void process( SimpleImageSequence<T> sequence ) {

		// Figure out how large the GUI window should be
		T frame = sequence.next();
		gui.setPreferredSize(new Dimension(frame.getWidth(), frame.getHeight()));
		ShowImages.showWindow(gui, "KTL Tracker", true);

		// process each frame in the image sequence
		while (sequence.hasNext()) {
			frame = sequence.next();

			// tell the tracker to process the frame
			tracker.process(frame);

			// if there are too few tracks spawn more
			if (tracker.getActiveTracks(null).size() < 130)
				tracker.spawnTracks();

			// visualize tracking results
			updateGUI(sequence);

			// wait for a fraction of a second so it doesn't process to fast
			BoofMiscOps.pause(pause);
		}
	}

	/**
	 * Draw tracked features in blue, or red if they were just spawned.
	 */
	private void updateGUI( SimpleImageSequence<T> sequence ) {
		BufferedImage orig = sequence.getGuiImage();
		Graphics2D g2 = orig.createGraphics();

		// draw tracks with semi-unique colors so you can track individual points with your eyes
		for (PointTrack p : tracker.getActiveTracks(null)) {
			int red = (int)(2.5*(p.featureId%100));
			int green = (int)((255.0/150.0)*(p.featureId%150));
			int blue = (int)(p.featureId%255);
			VisualizeFeatures.drawPoint(g2, (int)p.pixel.x, (int)p.pixel.y, new Color(red, green, blue));
		}

		// draw tracks which have just been spawned green
		for (PointTrack p : tracker.getNewTracks(null)) {
			VisualizeFeatures.drawPoint(g2, (int)p.pixel.x, (int)p.pixel.y, Color.green);
		}

		// tell the GUI to update
		gui.setImage(orig);
		gui.repaint();
	}

	/**
	 * A simple way to create a Kanade-Lucas-Tomasi (KLT) tracker.
	 */
	public void createKLT() {
		ConfigPKlt configKlt = new ConfigPKlt();
		configKlt.templateRadius = 3;
		configKlt.pyramidLevels = ConfigDiscreteLevels.levels(4);

		ConfigPointDetector configDetector = new ConfigPointDetector();
		configDetector.type = PointDetectorTypes.SHI_TOMASI;
		configDetector.general.maxFeatures = 600;
		configDetector.general.radius = 6;
		configDetector.general.threshold = 1;


		tracker = FactoryPointTracker.klt(configKlt, configDetector, imageType, derivType);
	}

	/**
	 * Creates a SURF feature tracker.
	 */
	public void createSURF() {
		ConfigFastHessian configDetector = new ConfigFastHessian();
		configDetector.maxFeaturesPerScale = 250;
		configDetector.extract.radius = 3;
		configDetector.initialSampleStep = 2;
		tracker = FactoryPointTracker.dda_FH_SURF_Fast(configDetector, null, null, imageType);
	}

	public static void main( String[] args ) throws FileNotFoundException {
		Class imageType = GrayF32.class;

		MediaManager media = DefaultMediaManager.INSTANCE;

		int pause;
		SimpleImageSequence sequence =
				media.openVideo(UtilIO.pathExample("zoom.mjpeg"), ImageType.single(imageType));
		pause = 100;
//				media.openCamera(null,640,480,ImageType.single(imageType)); pause = 5;
		sequence.setLoop(true);

		ExamplePointFeatureTracker app = new ExamplePointFeatureTracker(imageType, pause);

		// Comment or un-comment to change the type of tracker being used
		app.createKLT();
//		app.createSURF();

		app.process(sequence);
	}
}
