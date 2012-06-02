/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm;

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.sfm.StereoVisualOdometry;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.io.wrapper.images.MjpegStreamSequence;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.se.Se3_F64;

import java.awt.*;
import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class VisualizeStereoVisualOdometry
{

	public static void main( String args[] ) throws FileNotFoundException {

		MediaManager media = DefaultMediaManager.INSTANCE;

		StereoParameters stereoParam = BoofMiscOps.loadXML("stereo.xml");

		SimpleImageSequence<ImageFloat32> videoLeft =
				new MjpegStreamSequence<ImageFloat32>("/home/pja/temp/left.mjpeg",ImageFloat32.class);
		SimpleImageSequence<ImageFloat32> videoRight =
				new MjpegStreamSequence<ImageFloat32>("/home/pja/temp/right.mjpeg",ImageFloat32.class);

		ImagePointTracker<ImageFloat32> tracker =
				FactoryPointSequentialTracker.klt(300,new int[]{1,2,4,8},3,3,2,ImageFloat32.class,ImageFloat32.class);

		StereoDisparitySparse<ImageFloat32> disparity =
				FactoryStereoDisparity.regionSparseWta(3,130,2,2,40,-1,true,ImageFloat32.class);

		StereoVisualOdometry<ImageFloat32> alg = FactoryVisualOdometry.stereoSimple(300,1.5,tracker,stereoParam,
				disparity,ImageFloat32.class);

		int frameNumber = 0;

		ImagePanel gui = new ImagePanel();
		gui.setPreferredSize(new Dimension(640,480));
		ShowImages.showWindow(gui,"Left Video");


		while( videoLeft.hasNext() ) {
			ImageFloat32 left = videoLeft.next();
			ImageFloat32 right = videoRight.next();

			long before = System.nanoTime();
			boolean worked = alg.process(left,right);
			long after = System.nanoTime();

			if( worked ) {
				Se3_F64 pose = alg.getCameraToWorld();

				System.out.println(frameNumber+"   location: "+pose.getT()+"  ms = "+(after-before)/1e6);
			} else {
				System.out.println(frameNumber+"   failed");
			}

			gui.setBufferedImage(videoLeft.getGuiImage());
			gui.repaint();
			frameNumber++;
		}
		System.out.println("Done");
	}
}
