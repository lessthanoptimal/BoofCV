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

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.tracker.tld.TldParameters;
import boofcv.alg.tracker.tld.TldTracker;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.image.ShowImages;
import boofcv.gui.tracker.TldVisualizationPanel;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.shapes.Rectangle2D_F64;

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

		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);

		InterpolatePixelS<T> interpolate = FactoryInterpolation.bilinearPixelS(imageType);
		ImageGradient<T,D> gradient =  FactoryDerivative.sobel(imageType, derivType);

		tracker = new TldTracker<T, D>(new TldParameters(),interpolate,gradient,imageType,derivType);
	}

	public void process( final SimpleImageSequence<T> sequence ) {

		if( !sequence.hasNext() )
			throw new IllegalArgumentException("Empty sequence");

		image = sequence.next();
		gui.setFrame((BufferedImage) sequence.getGuiImage());
		ShowImages.showWindow(gui,"TLD Tracker");

//		tracker.initialize(image,274,159,356,292);
//		gui.turnOffSelect();

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
				Rectangle2D_F64 r = tracker.getTargetRegion();
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

		String fileName = "../data/applet/tracking/track_book.mjpeg";

		SimpleImageSequence<ImageUInt8> sequence =
				DefaultMediaManager.INSTANCE.openVideo(fileName,ImageType.single(ImageUInt8.class));

		app.process(sequence);
	}
}
