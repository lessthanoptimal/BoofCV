/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.examples;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.tracker.pklt.PkltManagerConfig;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.video.VideoMjpegCodec;
import boofcv.io.wrapper.images.JpegByteImageSequence;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * <p>
 * Example of how to use the {@link ImagePointTracker} to track different types of point features.
 * ImagePointTracker hides much of the complexity involved in tracking point features and masks
 * the very different underlying structures used by these different trackers.  The default trackers
 * provided in BoofCV are general purpose trackers, that might not be the best tracker or utility
 * the underlying image features the best in all situations.
 * </p>
 *
 * @author Peter Abeles
 */
public class ExamplePointFeatureTracker< T extends ImageSingleBand, D extends ImageSingleBand>
{
	// type of input image
	Class<T> imageType;
	Class<D> derivType;

	// tracks point features inside the image
	ImagePointTracker<T> tracker;

	// displays the video sequence and tracked features
	ImagePanel gui = new ImagePanel();

	public ExamplePointFeatureTracker(Class<T> imageType) {
		this.imageType = imageType;
		this.derivType = GImageDerivativeOps.getDerivativeType(imageType);
	}

	/**
	 * Processes the sequence of images and displays the tracked features in a window
	 */
	public void process(SimpleImageSequence<T> sequence) {

		// Figure out how large the GUI window should be
		T frame = sequence.next();
		gui.setPreferredSize(new Dimension(frame.getWidth(),frame.getHeight()));
		ShowImages.showWindow(gui,"KTL Tracker");

		// process each frame in the image sequence
		while( sequence.hasNext() ) {
			frame = sequence.next();

			// tell the tracker to process the frame
			tracker.process(frame);

			// if there are too few tracks spawn more
			if( tracker.getActiveTracks().size() < 100 )
				tracker.spawnTracks();

			// visualize tracking results
			updateGUI(sequence);

			// wait for a fraction of a second so it doesn't process to fast
			BoofMiscOps.pause(100);
		}
	}

	/**
	 * Draw tracked features in blue, or red if they were just spawned.
	 */
	private void updateGUI(SimpleImageSequence<T> sequence) {
		BufferedImage orig = sequence.getGuiImage();
		Graphics2D g2 = orig.createGraphics();

		// draw active tracks as blue dots
		for( AssociatedPair p : tracker.getActiveTracks() ) {
			int x = (int)p.currLoc.x;
			int y = (int)p.currLoc.y;

			VisualizeFeatures.drawPoint(g2, x, y, Color.blue);
		}

		// draw tracks which have just been spawned green
		for( AssociatedPair p : tracker.getNewTracks() ) {
			int x = (int)p.currLoc.x;
			int y = (int)p.currLoc.y;

			VisualizeFeatures.drawPoint(g2, x, y, Color.green);
		}

		// tell the GUI to update
		gui.setBufferedImage(orig);
		gui.repaint();
	}

	/**
	 * A simple way to create a Kanade-Lucas-Tomasi (KLT) tracker.
	 */
	public void createKLT() {
		PkltManagerConfig<T, D> config = PkltManagerConfig.createDefault(imageType,derivType);
		config.maxFeatures = 200;
		config.featureRadius = 3;
		config.pyramidScaling = new int[]{1,2,4,8};

		tracker = FactoryPointSequentialTracker.klt(config);
	}

	/**
	 * Creates a SURF feature tracker.
	 */
	public void createSURF() {
		tracker = FactoryPointSequentialTracker.surf(200,80,3,imageType);
	}

	public static void main( String args[] ) throws FileNotFoundException {

		Class imageType = ImageFloat32.class;

		// loads an MJPEG video sequence
		VideoMjpegCodec codec = new VideoMjpegCodec();
		List<byte[]> data = codec.read(new FileInputStream("../applet/data/zoom.mjpeg"));
		SimpleImageSequence sequence = new JpegByteImageSequence(imageType,data,true);

		ExamplePointFeatureTracker app = new ExamplePointFeatureTracker(imageType);

		// Comment or un-comment to change the type of tracker being used
		app.createKLT();
//		app.createSURF();

		app.process(sequence);
	}
}
