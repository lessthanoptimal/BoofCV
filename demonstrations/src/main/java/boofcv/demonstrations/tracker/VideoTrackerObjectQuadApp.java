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

import boofcv.abst.tracker.*;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.tracker.TrackerObjectQuadPanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.struct.image.*;
import georegression.struct.shapes.Quadrilateral_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Let's the user select a region in the image and tracks it using different algorithms.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class VideoTrackerObjectQuadApp<I extends ImageGray<I>>
		extends DemonstrationBase
		implements TrackerObjectQuadPanel.Listener, TrackerQuadInfoPanel.Listener, ActionListener {
	Class<I> imageClass;
	TrackerObjectQuad tracker;

	TrackerObjectQuadPanel videoPanel;
	TrackerQuadInfoPanel infoBar;

	// which tracking algorithm has the user selected
	int whichAlg;

	I gray;

	// current location of target in the image
	Quadrilateral_F64 target = new Quadrilateral_F64();
	// default location of target (if available) at the first frame
	Quadrilateral_F64 targetDefault = new Quadrilateral_F64();

	// has a region been selected by the user or a default was found
	boolean targetSelected = false;
	// Does the tracker need to be initialized?
	boolean initializeTracker = false;

	// was tracking successful?
	boolean success;
	// Is this the very first frame processed by the tracker
	boolean firstFrame = true;

	// has which tracker being used been changed by the user?
	boolean trackerChanged;
	double FPS = 0;

	// GUI component which lets the user select which algorithm to run
	JComboBox<String> selectAlgorithm;
	boolean hasDefaultRect = false;

	public VideoTrackerObjectQuadApp( List<PathLabel> examples, Class<I> imageType ) {
		super(examples, ImageType.pl(3, imageType));
		this.allowImages = false;
		this.imageClass = imageType;

		gray = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		videoPanel = new TrackerObjectQuadPanel(this);
		infoBar = new TrackerQuadInfoPanel(this);

		add(infoBar, BorderLayout.WEST);
		add(videoPanel, BorderLayout.CENTER);

		selectAlgorithm = new JComboBox<String>();
		selectAlgorithm.addItem("Circulant");
		selectAlgorithm.addItem("TLD");
		selectAlgorithm.addItem("Mean-Shift Region Fixed");
		selectAlgorithm.addItem("Mean-Shift Region Scale");
		selectAlgorithm.addItem("Mean-Shift Pixel");
		selectAlgorithm.addItem("Sparse Flow Tracker");
		selectAlgorithm.addActionListener(this);
		selectAlgorithm.setMaximumSize(selectAlgorithm.getPreferredSize());
		menuBar.add(selectAlgorithm);
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		super.handleInputChange(source, method, width, height);

		if (!(method == InputMethod.VIDEO || method == InputMethod.WEBCAM))
			throw new IllegalArgumentException("Must be a video or webcam!");

		// paused the video or webcam so that user can select
		setPaused(method == InputMethod.VIDEO);

		if (!hasDefaultRect) {
			videoPanel.setMode(TrackerObjectQuadPanel.Mode.IDLE);
			targetSelected = false;
		} else {
			resetTracker();
		}

		trackerChanged = true;
		firstFrame = true;
		FPS = 0;

		infoBar.setPlay(false);

		// override default speed
		setMaxFPS(infoBar.getMaxFPS());
		infoBar.setImageSize(width, height);

		videoPanel.setPreferredSize(new Dimension(width, height));
		videoPanel.setMaximumSize(new Dimension(width, height));
	}

	@Override
	public void processImage( int sourceID, long frameID, final BufferedImage buffered, ImageBase frame ) {

		if (trackerChanged) {
			trackerChanged = false;
			createNewTracker();
			initializeTracker = true;
		}

		boolean grayScale = false;

		if (tracker.getImageType().getFamily() == ImageType.Family.GRAY) {
			gray.reshape(frame.width, frame.height);
			GConvertImage.average((Planar<I>)frame, gray);
			grayScale = true;
		}

		if (targetSelected) {
			if (initializeTracker) {
				initializeTracker = false;
				if (grayScale)
					success = tracker.initialize(gray, target);
				else
					success = tracker.initialize(frame, target);
			} else {
				long before = System.nanoTime();
				if (grayScale)
					success = tracker.process(gray, target);
				else
					success = tracker.process(frame, target);
				long after = System.nanoTime();

				// update the algorithm FPS estimate using a rolling average
				double elapsed = (after - before)*1e-9;
				double decay = 0.98;
				if (FPS == 0)
					FPS = 1.0/elapsed;
				else
					FPS = decay*FPS + (1.0 - decay)*(1.0/elapsed);
			}
		}

		SwingUtilities.invokeLater(() -> updateGUI(buffered));
	}

	private void createNewTracker() {
		ImageType imageType = getImageType(0);

		if (whichAlg == 0)
			tracker = FactoryTrackerObjectQuad.circulant(new ConfigCirculantTracker(), imageClass);
		else if (whichAlg == 1)
			tracker = FactoryTrackerObjectQuad.tld(new ConfigTrackerTld(false), imageClass);
		else if (whichAlg == 2) {
			ConfigComaniciu2003 config = new ConfigComaniciu2003();
			config.scaleChange = 0;
			tracker = FactoryTrackerObjectQuad.meanShiftComaniciu2003(config, imageType);
		} else if (whichAlg == 3) {
			ConfigComaniciu2003 config = new ConfigComaniciu2003();
			config.scaleChange = 0.05f;
			tracker = FactoryTrackerObjectQuad.meanShiftComaniciu2003(config, imageType);
		} else if (whichAlg == 4) {
			tracker = FactoryTrackerObjectQuad.meanShiftLikelihood(30, 5, 256,
					MeanShiftLikelihoodType.HISTOGRAM, imageType);
		} else if (whichAlg == 5) {
			tracker = FactoryTrackerObjectQuad.sparseFlow(null, imageClass, null);
		} else
			throw new RuntimeException("Unknown algorithm");
	}

	protected void updateGUI( BufferedImage imageGUI ) {
		if (firstFrame) {
			videoPanel.setImageUI(imageGUI);
			infoBar.setFPS(0);
			infoBar.setTracking("");
			firstFrame = false;
		} else {
			videoPanel.setImageUI(imageGUI);
			if (targetSelected)
				videoPanel.setTarget(target, success);
			infoBar.setFPS(FPS);
			if (success) {
				infoBar.setTracking("FOUND");
			} else {
				infoBar.setTracking("?");
			}
		}
		videoPanel.repaint();
	}

	@Override
	public void selectedTarget( Quadrilateral_F64 target ) {
		System.out.println(target.a.x + " " + target.a.y + " " + target.b.x + " " + target.b.y + " " + target.c.x + " " + target.c.y + " " + target.d.x + " " + target.d.y);
		this.target.setTo(target);
		targetSelected = true;
		initializeTracker = true;
		if (infoBar.autoStart)
			streamPaused = false;
	}

	@Override
	public void pauseTracker() {
		setPaused(true);
	}

	@Override
	public void togglePause() {
		setPaused(!streamPaused);
	}

	@Override
	public void reprocessInput() {
		super.reprocessInput();
		if (inputMethod == InputMethod.VIDEO) {
			// reset the target to the default and pause the video
			resetTracker();
			setPaused(true);
		}
	}

	private void resetTracker() {
		if (hasDefaultRect) {
			videoPanel.setDefaultTarget(targetDefault);
			target.setTo(targetDefault);
			targetSelected = true;
		} else {
			videoPanel.setMode(TrackerObjectQuadPanel.Mode.IDLE);
			targetSelected = false;
		}
		initializeTracker = true;
	}

	@Override
	public void setMaxFPS( double fps ) {
		if (fps == 0)
			streamPeriod = 0;
		else
			streamPeriod = (long)(1000/fps);
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		if (e.getSource() == selectAlgorithm) {
			whichAlg = selectAlgorithm.getSelectedIndex();
			trackerChanged = true;
		}
	}

	@Override
	public void openWebcam() {
		hasDefaultRect = false;
		super.openWebcam();
	}

	@Override
	public void openFile( File file ) {
		hasDefaultRect = false;
		String videoName = file.getPath();
		String path = videoName.substring(0, videoName.lastIndexOf('.'));
		try {
			parseQuad(path + "_rect.txt");
			hasDefaultRect = true;
		} catch (FileNotFoundException e) {
			System.out.println("Can't find predefined region for " + file.getName());
		}

		super.openFile(file);
	}

	private void setPaused( boolean paused ) {
		streamPaused = paused;
		infoBar.setPlay(!streamPaused);
	}

	private void parseQuad( String fileName ) throws FileNotFoundException {
		BufferedReader in = UtilIO.openBufferedReader(fileName);

		try {
			String w[] = in.readLine().split(" ");

			if (w.length != 8)
				throw new RuntimeException("Unexpected number of variables in rectangle: " + w.length);

			targetDefault.a.x = Double.parseDouble(w[0]);
			targetDefault.a.y = Double.parseDouble(w[1]);
			targetDefault.b.x = Double.parseDouble(w[2]);
			targetDefault.b.y = Double.parseDouble(w[3]);
			targetDefault.c.x = Double.parseDouble(w[4]);
			targetDefault.c.y = Double.parseDouble(w[5]);
			targetDefault.d.x = Double.parseDouble(w[6]);
			targetDefault.d.y = Double.parseDouble(w[7]);

			in.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * 0 = running
	 * 1 = paused
	 * 2 = finished
	 */
//	@Override
//	protected void handleRunningStatus(int status) {
//		if( status == 0 ) {
//			infoBar.setPlay(false);
//		} else if( status == 1 ) {
//			infoBar.setPlay(true);
//		}
//	}
	public static void main( String[] args ) {
//		Class type = GrayF32.class;
		Class type = GrayU8.class;

//		app.setBaseDirectory(UtilIO.pathExample("");
//		app.loadInputData(UtilIO.pathExample("tracking/file_list.txt");

		List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("WildCat", UtilIO.pathExample("tracking/wildcat_robot.mjpeg")));
		examples.add(new PathLabel("Tree", UtilIO.pathExample("tracking/tree.mjpeg")));
		examples.add(new PathLabel("Book", UtilIO.pathExample("tracking/track_book.mjpeg")));
		examples.add(new PathLabel("Face", UtilIO.pathExample("tracking/track_peter.mjpeg")));
		examples.add(new PathLabel("Chipmunk", UtilIO.pathExample("tracking/chipmunk.mjpeg")));
		examples.add(new PathLabel("Balls", UtilIO.pathExample("tracking/balls_blue_red.mjpeg")));
		examples.add(new PathLabel("Driving Snow", UtilIO.pathExample("tracking/snow_follow_car.mjpeg")));
		examples.add(new PathLabel("Driving Night", UtilIO.pathExample("tracking/night_follow_car.mjpeg")));

		VideoTrackerObjectQuadApp app = new VideoTrackerObjectQuadApp(examples, type);

		app.openFile(new File(examples.get(0).getPath()));

		app.display("Tracking Rectangle");
	}
}
