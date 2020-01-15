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
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.misc.MovingAverage;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	final Map<Long, Point2D_F64> tracksPrev = new HashMap<>();

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
				gui.requestFocus();
			}
		});

		// Step video when space is pressed
		gui.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode()==KeyEvent.VK_SPACE){
					controlPanel.buttonStep.doClick();
				}
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
		int period = 33;
		if (o instanceof PathLabel) {
			// These examples were made before mp4 capability was added and were kept small by reducing the frame rate
			switch (((PathLabel) o).label) {
				case "Shake":
				case "Zoom":
				case "Rotate":
					period = 150;
					break;
			}
		}
		int _period = period;
		BoofSwingUtil.invokeNowOrLater(() -> controlPanel.setVideoPeriod(_period));

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
		this.streamPeriod = controlPanel.videoPeriod;
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
		super.streamPeriod = controlPanel.videoPeriod;
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

		double maxVelocity=0;
		int red,green,blue;


		// Used for computing log scale flow colors
		double logBase = 0.0;
		double logScale = 25.0;
		double maxLog = Math.log(logScale+logBase);

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final TrackerPointControlPanel.Colorization colorization = controlPanel.colorization;
			final boolean fillCircles = controlPanel.fillCircles;
			final int minDuration = controlPanel.minDuration;

			synchronized (tracksGui) {
				// Find max velocity of features which are drawn
				if( colorization != TrackerPointControlPanel.Colorization.TRACK_ID ) {
					computeMaxFlowVelocity(minDuration);
				}

				for (PointTrack p : tracksGui.toList() ) {
					long duration = frameIdGui-p.spawnFrameID;

					if( duration < minDuration )
						continue;

					switch( colorization ) {
						case TRACK_ID: {
							red = (int) (2.5 * (p.featureId % 100));
							green = (int) ((255.0 / 150.0) * (p.featureId % 150));
							blue = (int) (p.featureId % 255);
						} break;

						case FLOW: {
							colorizeFlow(p,tracksPrev.get(p.featureId));
						} break;

						case FLOW_LOG: {
							colorizeFlowLog(p,tracksPrev.get(p.featureId));
						} break;

						default: throw new RuntimeException("BUG");
					}

					double x = offsetX + scale*p.x;
					double y = offsetY + scale*p.y;

					if( duration == 0 ) {
						VisualizeFeatures.drawPoint(g2, x,y, 5, Color.GREEN,true,ellipse );
					} else if( fillCircles ) {
						VisualizeFeatures.drawPoint(g2, x, y, 5, new Color(red, green, blue), true, ellipse);
					} else {
						g2.setColor(new Color(red, green, blue));
						VisualizeFeatures.drawCircle(g2, x, y, 5, ellipse);
					}
					// Visually indicates that the user has clicked on this feature
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

		private void computeMaxFlowVelocity( int minDuration ) {
			for (PointTrack p : tracksGui.toList() ) {
				long duration = frameIdGui-p.spawnFrameID;

				if( duration < minDuration )
					continue;

				Point2D_F64 prev = tracksPrev.get(p.featureId);
				if( prev != null ) {
					// velocity Sq is much faster to compute than Euclidean distance
					maxVelocity = Math.max(maxVelocity, prev.distance2(p));
				}
			}
			maxVelocity = Math.sqrt(maxVelocity);
		}

		private void colorizeFlow( PointTrack p , Point2D_F64 prev ) {
			if( prev == null ) {
				red = blue = 0;
				green = 0xFF;
			} else {
				red = blue = green = 0;
				double dx = p.x-prev.x;
				double dy = p.y-prev.y;

				if( dx > 0 ) {
					red = Math.min(255,(int)(255*dx/maxVelocity));
				} else {
					green = Math.min(255,(int)(-255*dx/maxVelocity));
				}
				if( dy > 0 ) {
					blue = Math.min(255,(int)(255*dy/maxVelocity));
				} else {
					int v = Math.min(255,(int)(-255*dy/maxVelocity));
					red += v;
					green += v;
					if( red > 255 ) red = 255;
					if( green > 255 ) green = 255;
				}
			}
		}

		private void colorizeFlowLog( PointTrack p , Point2D_F64 prev ) {
			if( prev == null ) {
				red = blue = 0;
				green = 0xFF;
			} else {
				red = blue = green = 0;
				double dx = p.x-prev.x;
				double dy = p.y-prev.y;

				if( dx > 0 ) {
					red = Math.max(0,(int)(255*Math.log(logBase+logScale*dx/maxVelocity)/maxLog));
				} else {
					green = Math.max(0,(int)(255*Math.log(logBase-logScale*dx/maxVelocity)/maxLog));
				}
				if( dy > 0 ) {
					blue = Math.max(0,(int)(255*Math.log(logBase+logScale*dy/maxVelocity)/maxLog));
				} else {
					int v = Math.max(0,(int)(255*Math.log(logBase-logScale*dy/maxVelocity)/maxLog));
					red += v;
					green += v;
					if( red > 255 ) red = 255;
					if( green > 255 ) green = 255;
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
			controlPanel.setFrame((int)tracker.getFrameID());
			controlPanel.setTime(processingTime.getAverage());
			controlPanel.setTrackCount(count);
			controlPanel.setDuration(duration50,duration95);
		});

		synchronized (tracksGui) {
			tracksPrev.clear();
			for (int i = 0; i < tracksGui.size(); i++) {
				PointTrack t = tracksGui.get(i);
				tracksPrev.put(t.featureId,t.copy());
			}

			tracksGui.reset();
			frameIdGui = tracker.getFrameID();

			for (int i = 0; i < active.size(); i++) {
				tracksGui.grow().set(active.get(i));
			}
		}

		gui.setImageRepaint(buffered);

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
		examples.add(new PathLabel("Driving City 1", UtilIO.pathExample("tracking/dashcam01.mp4")));
		examples.add(new PathLabel("Driving City 2", UtilIO.pathExample("tracking/dashcam02.mp4")));
		examples.add(new PathLabel("Driving Snow", UtilIO.pathExample("tracking/snow_follow_car.mjpeg")));
		examples.add(new PathLabel("Driving Night", UtilIO.pathExample("tracking/night_follow_car.mjpeg")));

		SwingUtilities.invokeLater(()->{
			VideoTrackerPointFeaturesApp app = new VideoTrackerPointFeaturesApp(examples,type);
			app.openFile(new File(examples.get(0).getPath()));
			app.display( "Feature Tracker");
		});
	}
}

