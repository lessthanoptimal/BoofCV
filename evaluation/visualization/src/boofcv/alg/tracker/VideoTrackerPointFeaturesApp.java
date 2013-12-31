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

package boofcv.alg.tracker;

import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.gui.VideoProcessAppBase;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Runs a KLT tracker through a video sequence
 *
 * @author Peter Abeles
 */
// todo extract out base class for handling videos
public class VideoTrackerPointFeaturesApp<I extends ImageSingleBand, D extends ImageSingleBand>
		extends VideoProcessAppBase<I> implements MouseListener
{

	int maxFeatures = 400;
	int minFeatures = 150;

	PointTracker<I> tracker;

	ImagePanel gui = new ImagePanel();

	BufferedImage workImage;

	public VideoTrackerPointFeaturesApp(Class<I> imageType, Class<D> derivType) {
		super(1,imageType);

		PkltConfig config = new PkltConfig();
		config.templateRadius = 3;
		config.pyramidScaling = new int[]{1,2,4,8};

		ConfigFastHessian configFH = new ConfigFastHessian();
		configFH.maxFeaturesPerScale = 200;
		configFH.extractRadius = 4;
		configFH.detectThreshold = 15f;

		addAlgorithm(0,"KLT", FactoryPointTracker.klt(config, new ConfigGeneralDetector(maxFeatures, 1, 3),
				imageType, derivType));
		addAlgorithm(0,"ST-BRIEF", FactoryPointTracker.
				dda_ST_BRIEF(200, new ConfigGeneralDetector(maxFeatures, 3, 1), imageType, derivType));
		addAlgorithm(0,"ST-NCC", FactoryPointTracker.
				dda_ST_NCC(new ConfigGeneralDetector(maxFeatures, 3, 2), 5, imageType, derivType));
		addAlgorithm(0,"FH-SURF", FactoryPointTracker.
				dda_FH_SURF_Fast(configFH, null, null, imageType));
		addAlgorithm(0,"ST-SURF-KLT", FactoryPointTracker.
				combined_ST_SURF_KLT(new ConfigGeneralDetector(maxFeatures, 3, 1),
						config, 50, null, null, imageType, derivType));
		addAlgorithm(0,"FH-SURF-KLT", FactoryPointTracker.combined_FH_SURF_KLT(
				config, 50, configFH, null, null, imageType));

		gui.addMouseListener(this);
		gui.requestFocus();
		setMainGUI(gui);
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public void process( SimpleImageSequence<I> sequence ) {
		stopWorker();
		this.sequence = sequence;
		sequence.setLoop(true);
		doRefreshAll();
	}

	@Override
	public boolean getHasProcessedImage() {
		return workImage != null;
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( sequence == null )
			return;
		
		stopWorker();

		tracker = (PointTracker<I>)cookie;
		sequence.reset();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				I image = sequence.next();
				gui.setPreferredSize(new Dimension(image.width,image.height));
				workImage = new BufferedImage(image.width,image.height,BufferedImage.TYPE_INT_BGR);
				gui.setBufferedImage(workImage);
				revalidate();
				startWorkerThread();
			}});
	}

	void renderFeatures( BufferedImage orig , double trackerFPS ) {
		Graphics2D g2 = workImage.createGraphics();

		g2.drawImage(orig,0,0,orig.getWidth(),orig.getHeight(),null);
		for( PointTrack p : tracker.getActiveTracks(null) ) {
			int x = (int)p.x;
			int y = (int)p.y;

			VisualizeFeatures.drawPoint(g2,x,y,Color.blue);
		}

		for( PointTrack p : tracker.getNewTracks(null) ) {
			int x = (int)p.x;
			int y = (int)p.y;

			VisualizeFeatures.drawPoint(g2,x,y,Color.green);
		}

		g2.setColor(Color.WHITE);
		g2.fillRect(5,15,140,20);
		g2.setColor(Color.BLACK);
		g2.drawString(String.format("Tracker FPS: %3.1f",trackerFPS),10,30);
	}

	@Override
	protected void updateAlg(I frame, BufferedImage buffImage) {
		tracker.process(frame);

		if( tracker.getActiveTracks(null).size() < minFeatures ) {
			tracker.spawnTracks();
		}
	}

	@Override
	protected void handleRunningStatus(int status) {}

	@Override
	protected void updateAlgGUI(ImageSingleBand frame, BufferedImage imageGUI, double fps) {
		renderFeatures((BufferedImage)sequence.getGuiImage(),fps);
	}

	public static void main( String args[] ) {
		Class imageType = ImageFloat32.class;
		Class derivType = ImageFloat32.class;

//		Class imageType = ImageUInt8.class;
//		Class derivType = ImageSInt16.class;

		VideoTrackerPointFeaturesApp app = new VideoTrackerPointFeaturesApp(imageType, derivType);

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("Shake", "../data/applet/shake.mjpeg"));
		inputs.add(new PathLabel("Zoom", "../data/applet/zoom.mjpeg"));
		inputs.add(new PathLabel("Rotate", "../data/applet/rotate.mjpeg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Feature Tracker");
	}
}
