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

package boofcv.examples;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.image.GrayF32;
import com.github.sarxos.webcam.Webcam;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Processes a video feed and tracks points using KLT
 *
 * @author Peter Abeles
 */
public class ExampleTrackingKlt {

	public static void main(String[] args) {

		// tune the tracker for the image size and visual appearance
		ConfigGeneralDetector configDetector = new ConfigGeneralDetector(-1,8,1);
		PkltConfig configKlt = new PkltConfig(3,new int[]{1,2,4,8});

		PointTracker<GrayF32> tracker = FactoryPointTracker.klt(configKlt,configDetector,GrayF32.class,null);

		// Open a webcam at a resolution close to 640x480
		Webcam webcam = UtilWebcamCapture.openDefault(640,480);

		// Create the panel used to display the image and feature tracks
		ImagePanel gui = new ImagePanel();
		gui.setPreferredSize(webcam.getViewSize());

		ShowImages.showWindow(gui,"KLT Tracker",true);

		int minimumTracks = 100;
		while( true ) {
			BufferedImage image = webcam.getImage();
			GrayF32 gray = ConvertBufferedImage.convertFrom(image,(GrayF32)null);

			tracker.process(gray);

			List<PointTrack> tracks = tracker.getActiveTracks(null);

			// Spawn tracks if there are too few
			if( tracks.size() < minimumTracks ) {
				tracker.spawnTracks();
				tracks = tracker.getActiveTracks(null);
				minimumTracks = tracks.size()/2;
			}

			// Draw the tracks
			Graphics2D g2 = image.createGraphics();

			for( PointTrack t : tracks ) {
				VisualizeFeatures.drawPoint(g2,(int)t.x,(int)t.y,Color.RED);
			}

			gui.setImageUI(image);
		}
	}
}
