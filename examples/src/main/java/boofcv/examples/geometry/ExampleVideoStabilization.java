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

package boofcv.examples.geometry;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.abst.sfm.d2.PlToGrayMotion2D;
import boofcv.alg.sfm.d2.StitchingFromMotion2D;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.sfm.FactoryMotion2D;
import boofcv.gui.image.ImageGridPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.homography.Homography2D_F64;

import java.awt.image.BufferedImage;

/**
 * Example of how to stabilizing a video sequence using StitchingFromMotion2D.  Video stabilization is almost
 * the same as creating a video mosaic and the code in this example is a tweaked version of the mosaic example.
 * The differences are that the output size is the same as the input image size and that the origin is never changed.
 *
 * @author Peter Abeles
 */
public class ExampleVideoStabilization {
	public static void main( String args[] ) {

		// Configure the feature detector
		ConfigGeneralDetector confDetector = new ConfigGeneralDetector();
		confDetector.threshold = 10;
		confDetector.maxFeatures = 300;
		confDetector.radius = 2;

		// Use a KLT tracker
		PointTracker<GrayF32> tracker = FactoryPointTracker.klt(new int[]{1,2,4,8},confDetector,3,
				GrayF32.class,GrayF32.class);

		// This estimates the 2D image motion
		// An Affine2D_F64 model also works quite well.
		ImageMotion2D<GrayF32,Homography2D_F64> motion2D =
				FactoryMotion2D.createMotion2D(200,3,2,30,0.6,0.5,false,tracker,new Homography2D_F64());

		// wrap it so it output color images while estimating motion from gray
		ImageMotion2D<Planar<GrayF32>,Homography2D_F64> motion2DColor =
				new PlToGrayMotion2D<>(motion2D, GrayF32.class);

		// This fuses the images together
		StitchingFromMotion2D<Planar<GrayF32>,Homography2D_F64>
				stabilize = FactoryMotion2D.createVideoStitch(0.5, motion2DColor,ImageType.pl(3,GrayF32.class));

		// Load an image sequence
		MediaManager media = DefaultMediaManager.INSTANCE;
		String fileName = UtilIO.pathExample("shake.mjpeg");
		SimpleImageSequence<Planar<GrayF32>> video =
				media.openVideo(fileName, ImageType.pl(3, GrayF32.class));

		Planar<GrayF32> frame = video.next();

		// The output image size is the same as the input image size
		stabilize.configure(frame.width, frame.height, null);
		// process the first frame
		stabilize.process(frame);

		// Create the GUI for displaying the results + input image
		ImageGridPanel gui = new ImageGridPanel(1,2);
		gui.setImage(0,0,new BufferedImage(frame.width,frame.height,BufferedImage.TYPE_INT_RGB));
		gui.setImage(0,1,new BufferedImage(frame.width,frame.height,BufferedImage.TYPE_INT_RGB));
		gui.autoSetPreferredSize();

		ShowImages.showWindow(gui,"Example Stabilization", true);

		// process the video sequence one frame at a time
		while( video.hasNext() ) {
			if( !stabilize.process(video.next()) )
				throw new RuntimeException("Don't forget to handle failures!");

			// display the stabilized image
			ConvertBufferedImage.convertTo(frame,gui.getImage(0, 0),true);
			ConvertBufferedImage.convertTo(stabilize.getStitchedImage(), gui.getImage(0, 1),true);

			gui.repaint();

			// throttle the speed just in case it's on a fast computer
			BoofMiscOps.pause(50);
		}
	}
}
