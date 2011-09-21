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

package boofcv.alg.geo.d2;

import boofcv.abst.feature.tracker.PointSequentialTracker;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.d2.stabilization.PointImageStabilization;
import boofcv.alg.tracker.pklt.PkltManagerConfig;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.video.VideoListManager;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.distance.StatisticalDistance;
import boofcv.numerics.fitting.modelset.distance.StatisticalDistanceModelMatcher;
import boofcv.numerics.fitting.modelset.lmeds.LeastMedianOfSquares;
import boofcv.numerics.fitting.modelset.ransac.SimpleInlierRansac;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.affine.Affine2D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Runs a KLT tracker through a video sequence
 *
 * @author Peter Abeles
 */
// todo add control panel
// show feature locations
// number of frame changes
// frame period

	
public class VideoStabilizePointApp<I extends ImageBase, D extends ImageBase>
		extends SelectAlgorithmImagePanel implements ProcessInput
{

	long framePeriod = 150;
	int maxFeatures = 250;
	int minFeatures = 150;
	static int thresholdChange = 80;
	static int thresholdReset = 30;
	static double thresholdDistance = 50;

	int videoIndex;
	Class<I> imageType;

	EvaluateImageStabilization<I> evaluator = new EvaluateImageStabilization<I>();

	PointSequentialTracker<I> tracker;
	ModelMatcher<Affine2D_F64,AssociatedPair> modelMatcher;
	SimpleImageSequence<I> sequence;

	PointImageStabilization<I> stabilizer;

	DisplayPanel gui = new DisplayPanel();

	volatile boolean requestStop = false;
	volatile boolean isRunning = false;

	BufferedImage workImage;

	public VideoStabilizePointApp( Class<I> imageType , Class<D> derivType ) {
		super(2);

		this.imageType = imageType;

		PkltManagerConfig<I, D> config =
				PkltManagerConfig.createDefault(imageType,derivType);
		config.maxFeatures = maxFeatures;
		config.minFeatures = minFeatures;
		config.featureRadius = 3;
		config.pyramidScaling = new int[]{1,2,4,8};

		addAlgorithm(0,"KLT", FactoryPointSequentialTracker.klt(config));

		ModelFitterAffine2D modelFitter = new ModelFitterAffine2D();
		DistanceAffine2DSq distance = new DistanceAffine2DSq();
//		DistanceAffine2D distance = new DistanceAffine2D();
		Affine2DCodec codec = new Affine2DCodec();

		int numSample =  modelFitter.getMinimumPoints();

		addAlgorithm(1,"RANSAC",
				new SimpleInlierRansac<Affine2D_F64,AssociatedPair>(123123,
				modelFitter,distance,30,numSample,numSample,10000,2.0));

		addAlgorithm(1,"LMedS",
				new LeastMedianOfSquares<Affine2D_F64,AssociatedPair>(123123,
				numSample,25,1.1,0.6,modelFitter,distance));

		addAlgorithm(1,"StatDist",
				new StatisticalDistanceModelMatcher<Affine2D_F64,AssociatedPair>(25,0.001,0.001,1.2,
				numSample, StatisticalDistance.PERCENTILE,0.9,modelFitter,distance,codec));

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
		stopWorker();
		tracker = (PointSequentialTracker<I>)cookies[0];
		modelMatcher = (ModelMatcher<Affine2D_F64,AssociatedPair>)cookies[1];
		startEverything();
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( sequence == null || modelMatcher == null )
			return;

		stopWorker();

		switch( indexFamily ) {
			case 0:
				tracker = (PointSequentialTracker<I>)cookie;
				break;
			case 1:
				modelMatcher = (ModelMatcher<Affine2D_F64,AssociatedPair>)cookie;
				break;
		}

		// restart the video
		sequence.reset();

		startEverything();
	}

	private void startEverything() {
		stabilizer = new PointImageStabilization<I>(
				imageType,tracker,modelMatcher,thresholdChange,thresholdReset,thresholdDistance);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				I image = sequence.next();
				workImage = new BufferedImage(image.width,image.height,BufferedImage.TYPE_INT_BGR);
				gui.setPreferredSize(new Dimension(image.width*2+10,image.height));
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
		this.videoIndex = index;
		VideoListManager manager = getInputManager();
		process(manager.loadSequence(index));
	}

	private class DisplayPanel extends JPanel
	{
		BufferedImage orig;
		BufferedImage stabilized;
		double trackerFPS;

		public void setImages( BufferedImage orig , BufferedImage stabilized )
		{
			this.orig = orig;
			this.stabilized = stabilized;
		}

		public void setTrackerFPS(double trackerFPS) {
			this.trackerFPS = trackerFPS;
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			if( orig == null || stabilized == null )
				return;

			Graphics2D g2 = (Graphics2D)g;

			int w = getWidth()/2-10;
			int h = getHeight();


			double scaleX = w/(double)orig.getWidth();
			double scaleY = h/(double)orig.getHeight();

			double scale = Math.min(scaleX,scaleY);
			if( scale > 1 ) scale = 1;

			int scaledWidth = (int)(scale*orig.getWidth());
			int scaledHeight = (int)(scale*orig.getHeight());

			//  draw unstabilized image on right
			g2.drawImage(orig,0,0,scaledWidth,scaledHeight,0,0,orig.getWidth(),orig.getHeight(),null);

			// draw stabilized on right
			g2.drawImage(stabilized,scaledWidth+10,0,scaledWidth*2+10,scaledHeight,0,0,orig.getWidth(),orig.getHeight(),null);

			g2.setColor(Color.WHITE);
			g2.fillRect(5,15,140,20);
			g2.setColor(Color.BLACK);
			g2.drawString(String.format("Stabilize FPS: %3.1f",trackerFPS),10,30);
		}
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
					stabilizer.process(frame);
					I stabilizedImage = stabilizer.getStabilizedImage();
					totalTrackerTime += System.nanoTime()-startTracker;
					totalFrames++;

					ConvertBufferedImage.convertTo(stabilizedImage,workImage);
					gui.setTrackerFPS(1e9/(totalTrackerTime/(double)totalFrames));

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							gui.setImages(sequence.getGuiImage(),workImage);
							gui.repaint();
						}});

				}
				while( System.currentTimeMillis()-startTime < framePeriod ) {
					synchronized (this) {
						try {
							wait(System.currentTimeMillis()-startTime-10);
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}

			isRunning = false;
		}
	}

	public static void main( String args[] ) {
		VideoStabilizePointApp app = new VideoStabilizePointApp(ImageFloat32.class, ImageFloat32.class);

		VideoListManager manager = new VideoListManager(ImageFloat32.class);
		manager.add("Smaller","/home/pja/2011_09_20/parking_wobble.wmv");
		manager.add("Snow","/home/pja/2011_09_20/MAQ00687.MP4");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Feature Tracker");
	}
}
