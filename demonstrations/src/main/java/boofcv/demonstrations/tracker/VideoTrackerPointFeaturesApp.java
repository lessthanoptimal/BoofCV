/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.demonstrations.tracker.TrackerPointControlPanel.Marker;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeOpticalFlow;
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
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;

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

import static java.util.Objects.requireNonNull;

/**
 * Runs a KLT tracker through a video sequence
 *
 * @author Peter Abeles
 */
public class VideoTrackerPointFeaturesApp<I extends ImageGray<I>>
		extends DemonstrationBase implements TrackerPointControlPanel.Listener {
	PointTracker<I> tracker;

	VisualizePanel gui = new VisualizePanel();
	// synchronized when manipulating
	long frameIdGui = -1;
	final DogArray<PointTrack> tracksGui = new DogArray<>(PointTrack::new);
	final Map<Long, Point2D_F64> tracksPrev = new HashMap<>();

	long selectedTrackID = -1;

	TrackerPointControlPanel controlPanel = new TrackerPointControlPanel(this);
	Class<I> imageType;

	MovingAverage processingTime = new MovingAverage();

	// track duration 50% and 95%
	volatile double duration50, duration95;
	// track duration work space
	DogArray_F64 storageDuration = new DogArray_F64();

	public VideoTrackerPointFeaturesApp( List<PathLabel> examples,
										 Class<I> imageType ) {
		super(examples, ImageType.single(imageType));
		this.imageType = imageType;
		this.allowImages = false;

		gui.setPreferredSize(new Dimension(400, 400));

		add(BorderLayout.WEST, controlPanel);
		add(BorderLayout.CENTER, gui);

		gui.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed( MouseEvent e ) {
				double x = gui.offsetX + e.getX()/gui.scale;
				double y = gui.offsetY + e.getY()/gui.scale;
				handleMouseClick(x, y);
				gui.requestFocus();
			}
		});

		// Step video when space is pressed
		gui.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed( KeyEvent e ) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					controlPanel.buttonStep.doClick();
				}
			}
		});

		tracker = controlPanel.controlTracker.createTracker(super.getImageType(0));
	}

	@Override
	public void openExample( Object o ) {
		int period = 33;
		if (o instanceof PathLabel) {
			// These examples were made before mp4 capability was added and were kept small by reducing the frame rate
			switch (((PathLabel)o).label) {
				case "Shake", "Zoom", "Rotate" -> period = 150;
			}
		}
		int _period = period;
		BoofSwingUtil.invokeNowOrLater(() -> controlPanel.setVideoPeriod(_period));

		super.openExample(o);
	}

	@Override
	protected void configureVideo( int which, SimpleImageSequence sequence ) {
		super.configureVideo(which, sequence);
		sequence.setLoop(true);
		tracker.reset();
		BoofSwingUtil.invokeNowOrLater(() -> {
			controlPanel.setPauseState(false);
			streamPaused = false;
		});
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		super.handleInputChange(source, method, width, height);
		this.streamPeriod = controlPanel.videoPeriod;
		gui.setPreferredSize(new Dimension(width, height));
		BoofSwingUtil.invokeNowOrLater(() -> controlPanel.setImageSize(width, height));
	}

	@Override
	public void handleAlgorithmUpdated() {
		synchronized (this) {
			tracker = controlPanel.controlTracker.createTracker(super.getImageType(0));
		}
	}

	@Override
	public void handleVisualizationUpdated() {
		super.streamPeriod = controlPanel.videoPeriod;
		gui.repaint();
	}

	@Override
	public void handlePause( boolean paused ) {
		super.streamPaused = paused;
	}

	/**
	 * Show info on the selected feature and
	 */
	public void handleMouseClick( double x, double y ) {
		String text = String.format("Clicked %4.1f %4.1f\n", x, y);
		double tol = 10*10;
		selectedTrackID = -1;
		synchronized (tracksGui) {
			double bestDistance = tol;
			PointTrack best = null;
			for (PointTrack p : tracksGui.toList()) {
				double d = p.pixel.distance2(x, y);
				if (d <= bestDistance) {
					best = p;
					bestDistance = d;
				}
			}

			if (best != null) {
				selectedTrackID = best.featureId;
				text += stringPointInfo(best);
			} else {
				selectedTrackID = -1;
			}
		}

		gui.repaint();
		controlPanel.textArea.setText(text);
	}

	private String stringPointInfo( PointTrack best ) {
		String text = "Track ID " + best.featureId + "\n";
		text += "Set      " + best.detectorSetId + "\n";
		text += "Duration " + (frameIdGui - best.spawnFrameID) + "\n";
		text += String.format("Location %7.2f %7.2f", best.pixel.x, best.pixel.y);
		return text;
	}

	public class VisualizePanel extends ImagePanel {
		Ellipse2D.Double ellipse = new Ellipse2D.Double();
		VisualizeOpticalFlow visualizeFlow = new VisualizeOpticalFlow();

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);
			BoofSwingUtil.antialiasing(g);
			Graphics2D g2 = (Graphics2D)g;

			final TrackerPointControlPanel.Colorization colorization = controlPanel.colorization;
			final Marker markerType = controlPanel.markerType;
			final int minDuration = controlPanel.minDuration;

			boolean computeMaxFlow = colorization != TrackerPointControlPanel.Colorization.TRACK_ID ||
					markerType == Marker.Line;

			synchronized (tracksGui) {
				// Find max velocity of features which are drawn
				if (computeMaxFlow) {
					computeMaxFlowVelocity(minDuration);
				}

				for (PointTrack p : tracksGui.toList()) {
					long duration = frameIdGui - p.spawnFrameID;

					if (duration < minDuration)
						continue;

					switch (colorization) {
						case TRACK_ID -> {
							int rgb = VisualizeFeatures.trackIdToRgb(p.featureId);
							visualizeFlow.red = (rgb >> 16) & 0xFF;
							visualizeFlow.green = (rgb >> 8) & 0xFF;
							visualizeFlow.blue = rgb & 0xFF;
						}
						case FLOW -> visualizeFlow.computeColor(p.pixel, requireNonNull(tracksPrev.get(p.featureId)), false);
						case FLOW_LOG -> visualizeFlow.computeColor(p.pixel, requireNonNull(tracksPrev.get(p.featureId)), true);
						default -> throw new RuntimeException("BUG");
					}

					double x = offsetX + scale*p.pixel.x;
					double y = offsetY + scale*p.pixel.y;

					Stroke strokeBefore = g2.getStroke();
					if (duration == 0) {
						VisualizeFeatures.drawPoint(g2, x, y, 5, Color.GREEN, true, ellipse);
					} else {
						switch (markerType) {
							case Dot -> VisualizeFeatures.drawPoint(g2, x, y, 5, visualizeFlow.createColor(), true, ellipse);
							case Circle -> {
								g2.setColor(visualizeFlow.createColor());
								VisualizeFeatures.drawCircle(g2, x, y, 5, ellipse);
							}
							case Line -> {
								Point2D_F64 prev = tracksPrev.get(p.featureId);
								if (prev == null) {
									// this is a bug... show something so that we know there's a bug
									VisualizeFeatures.drawPoint(g2, x, y, 5, visualizeFlow.createColor(), true, ellipse);
								} else {
									visualizeFlow.drawLine(x, y, offsetX + scale*prev.x, offsetY + scale*prev.y, g2);
								}
							}
						}
					}
					g2.setStroke(strokeBefore);
					// Visually indicates that the user has clicked on this feature
					if (p.featureId == selectedTrackID) {
						controlPanel.textArea.setText(stringPointInfo(p));
						g2.setColor(Color.CYAN);
						for (int i = 0; i < 3; i++) {
							VisualizeFeatures.drawCircle(g2, x, y, 7 + i*2, ellipse);
						}
					}
				}
			}
		}

		private void computeMaxFlowVelocity( int minDuration ) {
			double maxVelocity = 0;
			for (PointTrack p : tracksGui.toList()) {
				long duration = frameIdGui - p.spawnFrameID;

				if (duration < minDuration)
					continue;

				Point2D_F64 prev = tracksPrev.get(p.featureId);
				if (prev != null) {
					// velocity Sq is much faster to compute than Euclidean distance
					maxVelocity = Math.max(maxVelocity, prev.distance2(p.pixel));
				}
			}
			visualizeFlow.maxVelocity = Math.sqrt(maxVelocity);
		}
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase input ) {
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
		processingTime.update((time1 - time0)*1e-6);

		List<PointTrack> active = tracker.getActiveTracks(null);

		// Compute track duration statistics
		long trackerFrameID = tracker.getFrameID();
		storageDuration.resize(active.size());
		for (int i = 0; i < active.size(); i++) {
			storageDuration.data[i] = trackerFrameID - active.get(i).spawnFrameID;
		}
		storageDuration.sort();
		this.duration50 = storageDuration.getFraction(0.5);
		this.duration95 = storageDuration.getFraction(0.95);

		int count = active.size();

		SwingUtilities.invokeLater(() -> {
			controlPanel.setFrame((int)tracker.getFrameID());
			controlPanel.setTime(processingTime.getAverage());
			controlPanel.setTrackCount(count);
			controlPanel.setDuration(duration50, duration95);
		});

		synchronized (tracksGui) {
			tracksPrev.clear();
			for (int i = 0; i < tracksGui.size(); i++) {
				PointTrack t = tracksGui.get(i);
				tracksPrev.put(t.featureId, t.pixel.copy());
			}

			tracksGui.reset();
			frameIdGui = tracker.getFrameID();

			for (int i = 0; i < active.size(); i++) {
				tracksGui.grow().setTo(active.get(i));
			}
		}

		gui.setImageRepaint(buffered);

		if (controlPanel.step) {
			controlPanel.step = false;
			this.streamPaused = true;
			SwingUtilities.invokeLater(() -> this.controlPanel.setPauseState(true));
		}
	}

	public static void main( String[] args ) {
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

		SwingUtilities.invokeLater(() -> {
			var app = new VideoTrackerPointFeaturesApp(examples, type);
			app.openFile(new File(examples.get(0).getPath()));
			app.display("Feature Tracker");
		});
	}
}
