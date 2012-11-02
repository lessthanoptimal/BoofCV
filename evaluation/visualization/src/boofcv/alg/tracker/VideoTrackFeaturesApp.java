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

package boofcv.alg.tracker;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.alg.tracker.pklt.PkltManagerConfig;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
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
public class VideoTrackFeaturesApp<I extends ImageSingleBand, D extends ImageSingleBand>
		extends VideoProcessAppBase<I> implements MouseListener
{

	int maxFeatures = 130;
	int minFeatures = 90;

	ImagePointTracker<I> tracker;

	ImagePanel gui = new ImagePanel();

	BufferedImage workImage;

	public VideoTrackFeaturesApp( Class<I> imageType , Class<D> derivType ) {
		super(1,imageType);

		PkltManagerConfig<I, D> config =
				PkltManagerConfig.createDefault(imageType,derivType);
		config.maxFeatures = maxFeatures;
		config.featureRadius = 3;
		config.pyramidScaling = new int[]{1,2,4,8};

		addAlgorithm(0,"KLT", FactoryPointSequentialTracker.klt(config));
		addAlgorithm(0,"BRIEF", FactoryPointSequentialTracker.dda_ShiTomasi_BRIEF(300, 200, 1, 1, imageType));
		addAlgorithm(0,"SURF", FactoryPointSequentialTracker.dda_FH_SURF(300, 200, 2, imageType));
		addAlgorithm(0,"NCC", FactoryPointSequentialTracker.dda_ShiTomasi_NCC(500, 5, 5, 20, imageType, derivType));


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

		tracker = (ImagePointTracker<I>)cookie;
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
		for( PointTrack p : tracker.getActiveTracks() ) {
			int x = (int)p.x;
			int y = (int)p.y;

			VisualizeFeatures.drawPoint(g2,x,y,Color.blue);
		}

		for( PointTrack p : tracker.getNewTracks() ) {
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

		if( tracker.getActiveTracks().size() < minFeatures ) {
			tracker.spawnTracks();
		}
	}

	@Override
	protected void updateAlgGUI(ImageSingleBand frame, BufferedImage imageGUI, double fps) {
		renderFeatures((BufferedImage)sequence.getGuiImage(),fps);
	}

	public static void main( String args[] ) {
		Class imageType = ImageFloat32.class;
		Class derivType = ImageFloat32.class;

//		Class imageType = ImageUInt8.class;
//		Class derivType = ImageSInt16.class;

		VideoTrackFeaturesApp app = new VideoTrackFeaturesApp(imageType, derivType);

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
