/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.gui.image.ShowImages;
import boofcv.gui.tracker.TrackerObjectQuadPanel;
import boofcv.io.MediaManager;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import georegression.struct.shapes.Quadrilateral_F64;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Demonstration on how to use the high level {@link TrackerObjectQuad} interface for tracking objects in a
 * video sequence.  This interface allows the target to be specified using an arbitrary quadrilateral.  Specific
 * implementations might not support that shape, so they instead will track an approximation of it.  The
 * interface also allows information on target visibility to be returned.  As is usually the case, tracker
 * specific information is lost in the high level interface and you should consider using the trackers
 * directly if more control is needed.
 *
 * This is an active area of research and all of the trackers eventually diverge given a long enough sequence.
 *
 * @author Peter Abeles
 */
public class ExampleTrackerObjectQuad {

	public static void main(String[] args) {
		MediaManager media = DefaultMediaManager.INSTANCE;
		String fileName = UtilIO.pathExample("tracking/wildcat_robot.mjpeg");

		// Create the tracker.  Comment/Uncomment to change the tracker.
		TrackerObjectQuad tracker =
				FactoryTrackerObjectQuad.circulant(null, GrayU8.class);
//				FactoryTrackerObjectQuad.sparseFlow(null,GrayU8.class,null);
//				FactoryTrackerObjectQuad.tld(null,GrayU8.class);
//				FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(), ImageType.pl(3, GrayU8.class));
//				FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(true),ImageType.pl(3,GrayU8.class));

				// Mean-shift likelihood will fail in this video, but is excellent at tracking objects with
				// a single unique color.  See ExampleTrackerMeanShiftLikelihood
//				FactoryTrackerObjectQuad.meanShiftLikelihood(30,5,255, MeanShiftLikelihoodType.HISTOGRAM,ImageType.pl(3,GrayU8.class));

		SimpleImageSequence video = media.openVideo(fileName, tracker.getImageType());

		// specify the target's initial location and initialize with the first frame
		Quadrilateral_F64 location = new Quadrilateral_F64(211.0,162.0,326.0,153.0,335.0,258.0,215.0,249.0);
		ImageBase frame = video.next();
		tracker.initialize(frame,location);

		// For displaying the results
		TrackerObjectQuadPanel gui = new TrackerObjectQuadPanel(null);
		gui.setPreferredSize(new Dimension(frame.getWidth(),frame.getHeight()));
		gui.setImageUI((BufferedImage)video.getGuiImage());
		gui.setTarget(location,true);
		ShowImages.showWindow(gui,"Tracking Results", true);

		// Track the object across each video frame and display the results
		long previous = 0;
		while( video.hasNext() ) {
			frame = video.next();

			boolean visible = tracker.process(frame,location);

			gui.setImageUI((BufferedImage) video.getGuiImage());
			gui.setTarget(location, visible);
			gui.repaint();

			// shoot for a specific frame rate
			long time = System.currentTimeMillis();
			BoofMiscOps.pause(Math.max(0,80-(time-previous)));
			previous = time;
		}
	}
}
