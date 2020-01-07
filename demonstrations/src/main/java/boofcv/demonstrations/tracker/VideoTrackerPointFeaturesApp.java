/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.tracker;

import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.MovingAverage;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.FastQueue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs a KLT tracker through a video sequence
 *
 * @author Peter Abeles
 */
public class VideoTrackerPointFeaturesApp<I extends ImageGray<I>>
		extends DemonstrationBase implements TrackerPointControlPanel.Listener
{
	PointTracker<I> tracker;

	VisualizePanel gui = new VisualizePanel();
	// synchronized when manipulating
	final FastQueue<PointTrack> tracksGui = new FastQueue<>(PointTrack.class,true);
	final FastQueue<PointTrack> spawnedGui = new FastQueue<>(PointTrack.class,true);

	TrackerPointControlPanel controlPanel = new TrackerPointControlPanel(this);
	Class<I> imageType;

	MovingAverage processingTime = new MovingAverage();

	// slow down a video
	int delay = 0;

	boolean drawFilled=true;

	public VideoTrackerPointFeaturesApp(List<PathLabel> examples,
										Class<I> imageType ) {
		super(examples, ImageType.single(imageType));
		this.imageType = imageType;
		this.allowImages = false;

		gui.setPreferredSize(new Dimension(400,400));

		add(BorderLayout.WEST, controlPanel);
		add(BorderLayout.CENTER, gui);

		setAlgorithm(0);

		// toggle rendering mode when mouse clicked
		gui.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				drawFilled = !drawFilled;
			}
		});
	}

	public void setAlgorithm( int which ) {
		int featRadius = controlPanel.featWidth/2;

		PkltConfig config = new PkltConfig();
		config.templateRadius = featRadius;
		config.pyramidScaling = new int[]{1, 2, 4, 8};

		ConfigFastHessian configFH = new ConfigFastHessian();
		configFH.maxFeaturesPerScale = 200;
		configFH.extractRadius = 4;
		configFH.detectThreshold = 15f;

		int maxFeatures = controlPanel.maxFeatures;

		switch( which ) {
			case 0: tracker = FactoryPointTracker.klt(config,
					new ConfigGeneralDetector(maxFeatures, featRadius+2, 3),imageType,null); break;

			case 1: tracker = FactoryPointTracker.dda_ST_BRIEF(
					200, new ConfigGeneralDetector(maxFeatures, 3, 1),
					imageType, null); break;

			case 2: tracker = FactoryPointTracker.dda_ST_NCC(new ConfigGeneralDetector(
					maxFeatures, featRadius+2, 2), 5, imageType, null); break;

			case 3: tracker = FactoryPointTracker.dda_FH_SURF_Fast(
					configFH, null, null, imageType); break;

			case 4: tracker = FactoryPointTracker.combined_ST_SURF_KLT(
					new ConfigGeneralDetector(maxFeatures, featRadius+2, 1),
					config, 50, null, null, imageType, null); break;

			case 5: tracker = FactoryPointTracker.combined_FH_SURF_KLT(
					config, 50, configFH, null, null, imageType); break;
		}
		processingTime.reset();
	}

	@Override
	public void openExample(Object o) {
		delay = 0;
		if (o instanceof PathLabel) {
			if (((PathLabel) o).label.equals("Shake")) {
				delay = 100;
			} else if (((PathLabel) o).label.equals("Zoom")) {
				delay = 100;
			} else if (((PathLabel) o).label.equals("Rotate")) {
				delay = 100;
			}
		}

		super.openExample(o);
	}

	@Override
	protected void configureVideo(int which, SimpleImageSequence sequence) {
		super.configureVideo(which, sequence);
		sequence.setLoop(true);
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		super.handleInputChange(source, method, width, height);
		gui.setPreferredSize(new Dimension(width,height));
		BoofSwingUtil.invokeNowOrLater(()->{
			controlPanel.setImageSize(width,height);
			controlPanel.resetPaused();
		});
	}

	@Override
	public void handleAlgorithmUpdated() {
		synchronized (this) {
			setAlgorithm(controlPanel.algorithm);
		}
	}

	@Override
	public void handlePause(boolean paused) {
		super.streamPaused = paused;
	}

	public class VisualizePanel extends ImagePanel {
		Ellipse2D.Double ellipse = new Ellipse2D.Double();

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			synchronized (tracksGui) {
				for (PointTrack p : tracksGui.toList() ) {
					int red = (int) (2.5 * (p.featureId % 100));
					int green = (int) ((255.0 / 150.0) * (p.featureId % 150));
					int blue = (int) (p.featureId % 255);

					double x = offsetX + scale*p.x;
					double y = offsetY + scale*p.y;

					if( drawFilled )
						VisualizeFeatures.drawPoint(g2, x,y, 5, new Color(red, green, blue),true,ellipse );
					else {
						g2.setColor(new Color(red, green, blue));
						VisualizeFeatures.drawCircle(g2, x, y, 5, ellipse);
					}
				}

				for (PointTrack p : spawnedGui.toList() ) {
					double x = offsetX + scale*p.x;
					double y = offsetY + scale*p.y;
					VisualizeFeatures.drawPoint(g2, x, y, 5,Color.green,true,ellipse);
				}
			}
		}
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase input) {
		PointTracker<I> tracker;
		synchronized (this) {
			tracker = this.tracker;
		}

		long time0 = System.nanoTime();
		tracker.process((I)input);

		if (tracker.getActiveTracks(null).size() < controlPanel.minFeatures) {
			tracker.spawnTracks();
		}
		long time1 = System.nanoTime();
		processingTime.update((time1-time0)*1e-6);

		List<PointTrack> active = tracker.getActiveTracks(null);
		List<PointTrack> spawned = tracker.getNewTracks(null);

		int count = active.size();

		SwingUtilities.invokeLater(()->{
			controlPanel.setTime(processingTime.getAverage());
			controlPanel.setTrackCount(count);
		});

		synchronized (tracksGui) {
			tracksGui.reset();
			spawnedGui.reset();

			for (int i = 0; i < active.size(); i++) {
				tracksGui.grow().set(active.get(i));
			}

			for (int i = 0; i < spawned.size(); i++) {
				spawnedGui.grow().set(spawned.get(i));
			}
		}

		gui.setImageRepaint(buffered);

		// some older videos are too fast if not paused
		if( delay > 0 )
			BoofMiscOps.sleep(delay);
	}

	public static void main(String args[]) {
		//		Class type = GrayF32.class;
		Class type = GrayU8.class;

		List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("Chipmunk", UtilIO.pathExample("tracking/chipmunk.mjpeg")));
		examples.add(new PathLabel("Shake", UtilIO.pathExample("shake.mjpeg")));
		examples.add(new PathLabel("Zoom", UtilIO.pathExample("zoom.mjpeg")));
		examples.add(new PathLabel("Rotate", UtilIO.pathExample("rotate.mjpeg")));
		examples.add(new PathLabel("Driving Snow", UtilIO.pathExample("tracking/snow_follow_car.mjpeg")));
		examples.add(new PathLabel("Driving Night", UtilIO.pathExample("tracking/night_follow_car.mjpeg")));

		SwingUtilities.invokeLater(()->{
			VideoTrackerPointFeaturesApp app = new VideoTrackerPointFeaturesApp(examples,type);
			app.openFile(new File(examples.get(0).getPath()));
			app.display( "Feature Tracker");
		});
	}
}

