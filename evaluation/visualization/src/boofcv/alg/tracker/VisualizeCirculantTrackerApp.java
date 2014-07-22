/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker;

import boofcv.abst.tracker.ConfigCirculantTracker;
import boofcv.alg.tracker.circulant.CirculantTracker;
import boofcv.factory.tracker.FactoryTrackerObjectAlgs;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.shapes.RectangleLength2D_F32;

import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class VisualizeCirculantTrackerApp<T extends ImageSingleBand>
	implements CirculantVisualizationPanel.Listener
{

	CirculantTracker<T> tracker;

	CirculantVisualizationPanel gui;

	T image;

	boolean paused;

	public VisualizeCirculantTrackerApp( Class<T> imageType ) {

		ConfigCirculantTracker config = new ConfigCirculantTracker();
		tracker = FactoryTrackerObjectAlgs.circulant(config,imageType);
		gui = new CirculantVisualizationPanel(this);
	}

	public void process( final SimpleImageSequence<T> sequence ) {

		if( !sequence.hasNext() )
			throw new IllegalArgumentException("Empty sequence");

		image = sequence.next();
		gui.setFrame((BufferedImage) sequence.getGuiImage());
		ShowImages.showWindow(gui, "Circulant Tracker");

//		tracker.initialize(image,273,156,358-273,293-156);

		paused = true;

		while( paused ) {
			Thread.yield();
		}

		int totalFrames = 0;
		long totalTime = 0;

		while( sequence.hasNext() ) {
			totalFrames++;

			image = sequence.next();
			gui.setFrame((BufferedImage)sequence.getGuiImage());

			long before = System.nanoTime();
			tracker.performTracking(image);
			long after = System.nanoTime();

			totalTime += after-before;
			System.out.println("FPS = "+(totalFrames)/(totalTime/2e9));

			gui.update(tracker);

			RectangleLength2D_F32 r = tracker.getTargetLocation();
			System.out.println("Target: " + r);
			gui.repaint();

			while( paused ) {
				Thread.yield();
			}
		}
		System.out.println("DONE");
	}

	@Override
	public void startTracking(int x0, int y0, int x1, int y1) {
		System.out.println(x0+","+y0+","+x1+","+y1);
		tracker.initialize(image,x0,y0,x1-x0,y1-y0);
		paused = false;
	}

	@Override
	public void togglePause() {
		paused = !paused;
	}


	public static void main( String args[] ) {
		VisualizeCirculantTrackerApp app = new VisualizeCirculantTrackerApp<ImageUInt8>(ImageUInt8.class);

//		String fileName = "../data/applet/tracking/track_peter.mjpeg";
		String fileName = "../data/applet/tracking/day_follow_car.mjpeg";

		SimpleImageSequence<ImageUInt8> sequence = DefaultMediaManager.INSTANCE.
				openVideo(fileName, ImageType.single(ImageUInt8.class));

		app.process(sequence);
	}
}
