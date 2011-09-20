/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker;

import boofcv.abst.feature.tracker.PointSequentialTracker;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.SingleImageInput;
import boofcv.alg.tracker.pklt.PkltManagerConfig;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.video.VideoListManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Runs a KLT tracker through a video sequence
 *
 * @author Peter Abeles
 */
public class VideoTrackFeaturesApp<I extends ImageBase, D extends ImageBase>
		extends SelectAlgorithmImagePanel implements ProcessInput
{

	long framePeriod = 100;
	int maxFeatures = 130;
	int minFeatures = 90;

	PointSequentialTracker<I> tracker;
	SimpleImageSequence<I> sequence;

	ImagePanel gui = new ImagePanel();

	volatile boolean requestStop = false;
	volatile boolean isRunning = false;

	BufferedImage workImage;

	public VideoTrackFeaturesApp( Class<I> imageType , Class<D> derivType ) {
		super(1);

		PkltManagerConfig<I, D> config =
				PkltManagerConfig.createDefault(imageType,derivType);
		config.maxFeatures = maxFeatures;
		config.minFeatures = minFeatures;
		config.featureRadius = 3;
		config.pyramidScaling = new int[]{1,2,4,8};

		addAlgorithm(0,"KLT", FactoryPointSequentialTracker.klt(config));

		setMainGUI(gui);
	}

	public void process( SimpleImageSequence<I> sequence ) {
		stopWorker();
		this.sequence = sequence;
		doRefreshAll();
	}

	@Override
	public boolean getHasProcessedImage() {
		return isRunning;
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( sequence == null )
			return;
		
		tracker = (PointSequentialTracker<I>)cookie;

		stopWorker();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				I image = sequence.next();
				workImage = new BufferedImage(image.width,image.height,BufferedImage.TYPE_INT_BGR);
				gui.setBufferedImage(workImage);
				gui.setPreferredSize(new Dimension(image.width,image.height));
				revalidate();
				new WorkThread().start();
			}});
	}

	private void stopWorker() {
		requestStop = true;
		while( isRunning ) {
			Thread.yield();
		}
		requestStop = false;
	}

	@Override
	public void changeImage(String name, int index) {
		VideoListManager manager = getInputManager();
		process(manager.loadSequence(index));
	}

	void renderFeatures( BufferedImage orig , long trackingPeriodMilli ) {
		Graphics2D g2 = workImage.createGraphics();

		g2.drawImage(orig,0,0,orig.getWidth(),orig.getHeight(),null);
		for( AssociatedPair p : tracker.getActiveTracks() ) {
			int x = (int)p.currLoc.x;
			int y = (int)p.currLoc.y;

			VisualizeFeatures.drawPoint(g2,x,y,Color.red);
		}

		for( AssociatedPair p : tracker.getNewTracks() ) {
			int x = (int)p.currLoc.x;
			int y = (int)p.currLoc.y;

			VisualizeFeatures.drawPoint(g2,x,y,Color.blue);
		}

		double trackerFPS = 1e9/trackingPeriodMilli;
		g2.setColor(Color.WHITE);
		g2.fillRect(5,15,140,20);
		g2.setColor(Color.BLACK);
		g2.drawString(String.format("Tracker FPS: %3.1f",trackerFPS),10,30);
	}


	private class WorkThread extends Thread
	{
		@Override
		public void run() {
			isRunning = true;

			long totalTrackerTime = 0;
			long totalFrames = 0;

			while( requestStop == false ) {
				long startTime = System.currentTimeMillis();
				// periodically reset the FPS
				if( totalFrames > 20 ) {
					totalFrames = 0;
					totalTrackerTime = 0;
				}

				if( sequence.hasNext() ) {
					I frame = sequence.next();

					long startTracker = System.nanoTime();
					((SingleImageInput<I>)tracker).process(frame);

					if( tracker.getActiveTracks().size() < minFeatures ) {
						tracker.spawnTracks();
					}
					totalTrackerTime += System.nanoTime()-startTracker;
					totalFrames++;


					// render the features
					renderFeatures(sequence.getGuiImage(),totalTrackerTime/totalFrames);
					gui.repaint();

				}
				while( System.currentTimeMillis()-startTime < framePeriod ) {
					Thread.yield();
				}
			}

			isRunning = false;
		}
	}

	public static void main( String args[] ) {
		VideoTrackFeaturesApp app = new VideoTrackFeaturesApp(ImageFloat32.class, ImageFloat32.class);

		VideoListManager manager = new VideoListManager(ImageFloat32.class);
		manager.add("Snow","../applet/data/snow_rail");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Feature Tracker");
	}
}
