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

package boofcv.examples.tracking;

import boofcv.abst.tracker.ConfigComaniciu2003;
import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.gui.image.ShowImages;
import boofcv.gui.tracker.TrackerObjectQuadPanel;
import boofcv.io.MediaManager;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.shapes.Quadrilateral_F64;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Mean-shift based trackers use color information to perform a local search.  The Comaniciu based trackers match
 * the color histogram directly, are more robust to objects with complex distributions, and can be configured to
 * estimate the object's scale.  Likelihood based trackers use color to compute a likelihood
 * function, which mean-shift is then run on.  The likelihood based trackers are very fast but only work well
 * when the target is composed of a single color.
 *
 * @author Peter Abeles
 */
public class ExampleTrackerMeanShift {
	public static void main(String[] args) {
		MediaManager media = DefaultMediaManager.INSTANCE;
		String fileName = "../data/applet/tracking/balls_blue_red.mjpeg";
		Quadrilateral_F64 location = new Quadrilateral_F64(394,247,474,244,475,325,389,321);

		ImageType<MultiSpectral<ImageUInt8>> imageType = ImageType.ms(3,ImageUInt8.class);

		SimpleImageSequence<MultiSpectral<ImageUInt8>> video = media.openVideo(fileName, imageType);

		// Create the tracker.  Comment/Uncomment to change the tracker.
		TrackerObjectQuad<MultiSpectral<ImageUInt8>> tracker =
				FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(), imageType);
//				FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(true),imageType);
//				FactoryTrackerObjectQuad.meanShiftLikelihood(30,5,255, MeanShiftLikelihoodType.HISTOGRAM,imageType);

		// specify the target's initial location and initialize with the first frame

		MultiSpectral<ImageUInt8> frame = video.next();
		tracker.initialize(frame,location);

		// For displaying the results
		TrackerObjectQuadPanel gui = new TrackerObjectQuadPanel(null);
		gui.setPreferredSize(new Dimension(frame.getWidth(),frame.getHeight()));
		gui.setBackGround((BufferedImage)video.getGuiImage());
		gui.setTarget(location,true);
		ShowImages.showWindow(gui, "Tracking Results");

		// Track the object across each video frame and display the results
		while( video.hasNext() ) {
			frame = video.next();

			boolean visible = tracker.process(frame,location);

			gui.setBackGround((BufferedImage) video.getGuiImage());
			gui.setTarget(location,visible);
			gui.repaint();

			BoofMiscOps.pause(20);
		}
	}
}
