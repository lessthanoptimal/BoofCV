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

package boofcv.gui.tracker;

import boofcv.alg.tracker.tld.TldConfig;
import boofcv.alg.tracker.tld.TldTracker;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.shapes.RectangleCorner2D_F64;

import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class VisualizeTldTrackerApp<T extends ImageSingleBand,D extends ImageSingleBand>
		implements TldVisualizationPanel.Listener
{

	TldTracker<T,D> tracker;

	TldVisualizationPanel gui = new TldVisualizationPanel(this);

	T image;

	boolean paused;

	public VisualizeTldTrackerApp( Class<T> imageType ) {

		TldConfig<T,D> config = new TldConfig<T, D>(imageType);
		tracker = new TldTracker<T, D>(config);

	}

	public void process( final SimpleImageSequence<T> sequence ) {

		if( !sequence.hasNext() )
			throw new IllegalArgumentException("Empty sequence");

		image = sequence.next();
		gui.setFrame((BufferedImage) sequence.getGuiImage());
		ShowImages.showWindow(gui,"TLD Tracker");

//		tracker.initialize(image,77,203,77+51,203+83);

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
			boolean success = tracker.track(image);
			long after = System.nanoTime();

			totalTime += after-before;
			System.out.println("FPS = "+(totalFrames)/(totalTime/2e9));

			gui.update(tracker,success);

			if( !success ) {
				System.out.println("No rectangle found");
			} else {
				RectangleCorner2D_F64 r = tracker.getTargetRegion();
				System.out.println("Target: "+r);
			}
			gui.repaint();

			while( paused ) {
				Thread.yield();
			}
		}
		System.out.println("DONE");
	}

	@Override
	public void startTracking(int x0, int y0, int x1, int y1) {
		tracker.initialize(image,x0,y0,x1,y1);
		paused = false;
	}

	@Override
	public void togglePause() {
		paused = !paused;
	}


	public static void main( String args[] ) {
		VisualizeTldTrackerApp app = new VisualizeTldTrackerApp(ImageUInt8.class);

//		String fileName = "/home/pja/Downloads/multi_face_turning/motinas_multi_face_turning.avi";
		String fileName = "/home/pja/Downloads/david_indoor/david_indoor.avi";

//		SimpleImageSequence<ImageUInt8> sequence =
//				new XugglerSimplified<ImageUInt8>(fileName, ImageDataType.single(ImageUInt8.class));
//
//		app.process(sequence);
	}
}
