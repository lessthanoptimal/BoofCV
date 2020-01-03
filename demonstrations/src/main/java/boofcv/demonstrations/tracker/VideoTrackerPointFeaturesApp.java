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
import boofcv.alg.tracker.klt.ConfigPKlt;
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
import org.ddogleg.struct.GrowQueue_F64;

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
	long frameIdGui = -1;
	final FastQueue<PointTrack> tracksGui = new FastQueue<>(PointTrack.class,true);

	long selectedTrackID = -1;

	TrackerPointControlPanel controlPanel = new TrackerPointControlPanel(this);
	Class<I> imageType;

	MovingAverage processingTime = new MovingAverage();

	// track duration 50% and 95%
	volatile double duration50,duration95;
	// track duration work space
	GrowQueue_F64 storageDuration = new GrowQueue_F64();

	public VideoTrackerPointFeaturesApp(List<PathLabel> examples,
										Class<I> imageType ) {
		super(examples, ImageType.single(imageType));
		this.imageType = imageType;
		this.allowImages = false;

		gui.setPreferredSize(new Dimension(400,400));

		add(BorderLayout.WEST, controlPanel);
		add(BorderLayout.CENTER, gui);

		gui.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				double x = gui.offsetX + e.getX()/gui.scale;
				double y = gui.offsetY + e.getY()/gui.scale;
				handleMouseClick(x,y);
			}
		});

		setAlgorithm(0);
	}

	public void setAlgorithm( int which ) {

		final int maxFeatures = controlPanel.maxFeatures;

		controlPanel.controlKlt.detector.maxFeatures = maxFeatures;
		controlPanel.controlsGeneric.detector.maxFeatures = maxFeatures;


		ConfigFastHessian configFH = new ConfigFastHessian();
		configFH.maxFeaturesPerScale = 200;
		configFH.extractRadius =  controlPanel.controlsGeneric.detector.radius;
		configFH.detectThreshold = 15f;

		switch( which ) {
			case 0: tracker = FactoryPointTracker.klt(controlPanel.controlKlt.klt,controlPanel.controlKlt.detector,
					imageType,null); break;

			case 1: tracker = FactoryPointTracker.dda_ST_BRIEF(
					200, new ConfigGeneralDetector(maxFeatures, 3, 1),
					imageType, null); break;

			case 2: tracker = FactoryPointTracker.dda_ST_NCC(new ConfigGeneralDetector(
					maxFeatures, controlPanel.controlsGeneric.detector.radius, 2), 5, imageType, null); break;

			case 3: tracker = FactoryPointTracker.dda_FH_SURF_Fast(
					configFH, null, null, imageType); break;

			case 4: tracker = FactoryPointTracker.combined_ST_SURF_KLT(
					new ConfigGeneralDetector(maxFeatures,  controlPanel.controlsGeneric.detector.radius, 1),
					controlPanel.controlKlt.klt, 50, null, null, imageType, null); break;

			case 5: tracker = FactoryPointTracker.combined_FH_SURF_KLT(
					controlPanel.controlKlt.klt, 50, configFH, null, null, imageType); break;
		}
		processingTime.reset();
	}

	@Override
	public void openExample(Object o) {
		int delay = 0;
		if (o instanceof PathLabel) {
			if (((PathLabel) o).label.equals("Shake")) {
				delay = 100;
			} else if (((PathLabel) o).label.equals("Zoom")) {
				delay = 100;
			} else if (((PathLabel) o).label.equals("Rotate")) {
				delay = 100;
			}
		}
		int _delay = delay;
		BoofSwingUtil.invokeNowOrLater(()->{
			controlPanel.setDelay(_delay);
		});

		super.openExample(o);
	}

	@Override
	protected void configureVideo(int which, SimpleImageSequence sequence) {
		super.configureVideo(which, sequence);
		sequence.setLoop(true);
		controlPanel.setPauseState(false);
		streamPaused = false;
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		super.handleInputChange(source, method, width, height);
		gui.setPreferredSize(new Dimension(width,height));
		BoofSwingUtil.invokeNowOrLater(()->{
			controlPanel.setImageSize(width,height);
			controlPanel.setPauseState(false);
			streamPaused = false;
		});
	}

	@Override
	public void handleAlgorithmUpdated() {
		synchronized (this) {
			setAlgorithm(controlPanel.algorithm);
		}
	}

	@Override
	public void handleVisualizationUpdated() {
		gui.repaint();
	}

	@Override
	public void handlePause(boolean paused) {
		super.streamPaused = paused;
	}

	/**
	 * Show info on the selected feature and
	 * @param x
	 * @param y
	 */
	public void handleMouseClick( double x , double y ) {
		String text = String.format("Clicked %4.1f %4.1f\n",x,y);
		double tol = 10*10;
		selectedTrackID = -1;
		synchronized (tracksGui) {
			double bestDistance = tol;
			PointTrack best = null;
			for (PointTrack p : tracksGui.toList()) {
				double d = p.distance2(x,y);
				if( d <= bestDistance ) {
					best = p;
					bestDistance = d;
				}
			}


			if( best != null ) {
				selectedTrackID = best.featureId;
				text += stringPointInfo(best);
			} else {
				selectedTrackID = -1;
			}
		}

		gui.repaint();
		controlPanel.textArea.setText(text);
	}

	private String stringPointInfo(PointTrack best) {
		String text = "Track ID "+best.featureId+"\n";
		text += "Set      "+best.setId+"\n";
		text += "Duration "+(frameIdGui-best.spawnFrameID)+"\n";
		text += String.format("Location %7.2f %7.2f",best.x,best.y);
		return text;
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
					long duration = frameIdGui-p.spawnFrameID;

					if( duration < controlPanel.minDuration )
						continue;

					int red = (int) (2.5 * (p.featureId % 100));
					int green = (int) ((255.0 / 150.0) * (p.featureId % 150));
					int blue = (int) (p.featureId % 255);

					double x = offsetX + scale*p.x;
					double y = offsetY + scale*p.y;

					if( duration == 0 ) {
						VisualizeFeatures.drawPoint(g2, x,y, 5, Color.GREEN,true,ellipse );
					} else if( controlPanel.fillCircles )
						VisualizeFeatures.drawPoint(g2, x,y, 5, new Color(red, green, blue),true,ellipse );
					else {
						g2.setColor(new Color(red, green, blue));
						VisualizeFeatures.drawCircle(g2, x, y, 5, ellipse);
					}
					// Visually indidate that the user has clicked on this feature
					if( p.featureId == selectedTrackID ) {
						controlPanel.textArea.setText(stringPointInfo(p));
						g2.setColor(Color.CYAN);
						for (int i = 0; i < 3; i++) {
							VisualizeFeatures.drawCircle(g2, x, y, 7+i*2, ellipse);
						}
					}
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

		// Compute track duration statistics
		long trackerFrameID = tracker.getFrameID();
		storageDuration.resize(active.size());
		for (int i = 0; i < active.size(); i++) {
			storageDuration.data[i] = trackerFrameID-active.get(i).spawnFrameID;
		}
		storageDuration.sort();
		this.duration50 = storageDuration.getFraction(0.5);
		this.duration95 = storageDuration.getFraction(0.95);

		int count = active.size();

		SwingUtilities.invokeLater(()->{
			controlPanel.setTime(processingTime.getAverage());
			controlPanel.setTrackCount(count);
			controlPanel.setDuration(duration50,duration95);
		});

		synchronized (tracksGui) {
			tracksGui.reset();
			frameIdGui = tracker.getFrameID();

			for (int i = 0; i < active.size(); i++) {
				tracksGui.grow().set(active.get(i));
			}
		}

		gui.setImageRepaint(buffered);

		// some older videos are too fast if not paused
		if( controlPanel.delayMS > 0 )
			BoofMiscOps.sleep(controlPanel.delayMS);

		if( controlPanel.step ) {
			controlPanel.step = false;
			this.streamPaused = true;
			SwingUtilities.invokeLater(()-> this.controlPanel.setPauseState(true));
		}
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

