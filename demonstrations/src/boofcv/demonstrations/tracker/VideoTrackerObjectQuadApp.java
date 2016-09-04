/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.gui.image.ShowImages;
import boofcv.gui.tracker.TrackerObjectQuadPanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.shapes.Quadrilateral_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Let's the user select a region in the image and tracks it using different algorithms.
 *
 * @author Peter Abeles
 */
// TODO Add controller for MAX FPS
	// TODO fix pause button state
	// TODO add back ability to select tracker
	// TODO fix no image on webpage problem
public class VideoTrackerObjectQuadApp<I extends ImageGray>
		extends DemonstrationBase<Planar<I>>
		implements TrackerObjectQuadPanel.Listener  , TrackerQuadInfoPanel.Listener
{
	Class<I> imageClass;
	TrackerObjectQuad tracker;

	TrackerObjectQuadPanel videoPanel;
	TrackerQuadInfoPanel infoBar;

	int whichAlg;

	I gray;

	Quadrilateral_F64 target = new Quadrilateral_F64();
	Quadrilateral_F64 targetDefault = new Quadrilateral_F64();

	boolean targetSelected = false;
	boolean selectionChanged = false;

	boolean success;
	boolean firstFrame = true;

	public VideoTrackerObjectQuadApp(List<PathLabel> examples,
									 Class<I> imageType ) {
		super(examples, ImageType.pl(3, imageType));
		this.imageClass = imageType;

		gray = GeneralizedImageOps.createSingleBand(imageType,1,1);

//		addAlgorithm(0, "Circulant", 0);
//		addAlgorithm(0, "TLD", 1);
//		addAlgorithm(0, "Mean-Shift Region Fixed", 2);
//		addAlgorithm(0, "Mean-Shift Region Scale", 3);
//		addAlgorithm(0, "Mean-Shift Pixel", 4);
//		addAlgorithm(0, "Sparse Flow Tracker", 5);

		videoPanel = new TrackerObjectQuadPanel(this);
		infoBar = new TrackerQuadInfoPanel(this);

		add(infoBar, BorderLayout.WEST);
		add(videoPanel, BorderLayout.CENTER);
	}

	@Override
	protected void handleInputChange(InputMethod method, int width, int height) {
		if( !(method == InputMethod.VIDEO || method == InputMethod.WEBCAM) )
			throw new IllegalArgumentException("Must be a video or webcam!");

		firstFrame = true;
		streamPaused = true; // paused the video or webcam so that user can select
		createNewTracker();
		infoBar.setPlay(false);

		videoPanel.setPreferredSize(new Dimension(width, height));
		videoPanel.setMaximumSize(new Dimension(width, height));
	}

	@Override
	public void processImage(BufferedImage buffered, Planar<I> frame) {
		boolean grayScale = false;

		if( tracker.getImageType().getFamily() == ImageType.Family.GRAY) {
			gray.reshape(frame.width,frame.height);
			GConvertImage.average(frame, gray);
			grayScale = true;
		}

		if( targetSelected ) {
			if( selectionChanged ) {
				selectionChanged = false;
				if( grayScale)
					success = tracker.initialize(gray, target);
				else
					success = tracker.initialize(frame, target);
			} else
			if( grayScale)
				success = tracker.process(gray, target);
			else
				success = tracker.process(frame, target);
		}

		updateGUI(frame,buffered,1000.0);
	}

	private void createNewTracker() {
		if( whichAlg == 0 )
			tracker = FactoryTrackerObjectQuad.circulant(new ConfigCirculantTracker(), imageClass);
		else if( whichAlg == 1 )
			tracker = FactoryTrackerObjectQuad.tld(new ConfigTld(false),imageClass);
		else if( whichAlg == 2 ) {
			ConfigComaniciu2003 config = new ConfigComaniciu2003();
			config.scaleChange = 0;
			tracker = FactoryTrackerObjectQuad.meanShiftComaniciu2003(config,imageType);
		} else if( whichAlg == 3 ) {
			ConfigComaniciu2003 config = new ConfigComaniciu2003();
			config.scaleChange = 0.05f;
			tracker = FactoryTrackerObjectQuad.meanShiftComaniciu2003(config,imageType);
		} else if( whichAlg == 4 ) {
			tracker = FactoryTrackerObjectQuad.meanShiftLikelihood(30, 5, 256, MeanShiftLikelihoodType.HISTOGRAM, imageType);
		} else if( whichAlg == 5 ) {
			tracker = FactoryTrackerObjectQuad.sparseFlow(null,imageClass,null);
		} else
			throw new RuntimeException("Unknown algorithm");

		// use default rectangle
		videoPanel.setDefaultTarget(targetDefault);
		firstFrame = true;
		targetSelected = true;
		selectionChanged = true;
		target.set(targetDefault);
	}



	protected void updateGUI(Planar<I> frame, BufferedImage imageGUI, double fps) {
		if( firstFrame ) {
			videoPanel.setBackGround(imageGUI);
			infoBar.setFPS(0);
			infoBar.setTracking("");
			firstFrame = false;
		} else {
			videoPanel.setBackGround(imageGUI);
			videoPanel.setTarget(target, success);
			infoBar.setFPS(fps);
			if( success ) {
				infoBar.setTracking("FOUND");
			} else {
				infoBar.setTracking("?");
			}
		}
		videoPanel.repaint();
	}

	@Override
	public void selectedTarget(Quadrilateral_F64 target) {
		System.out.println(target.a.x+" "+target.a.y+" "+target.b.x+" "+target.b.y+" "+target.c.x+" "+target.c.y+" "+target.d.x+" "+target.d.y);
		this.target.set(target);
		targetSelected = true;
		selectionChanged = true;
		streamPaused = false;
	}

	@Override
	public void togglePause() {
		streamPaused = !streamPaused;
	}

	@Override
	public void selectTarget() {
		streamPaused = true;
		infoBar.setTracking("");
		targetSelected = false;
		videoPanel.enterSelectMode();
	}

	@Override
	public void resetVideo() {
		System.out.println("Reset video called");
	}

	@Override
	public void openFile(File file) {
		String videoName = file.getPath();
		String path = videoName.substring(0,videoName.lastIndexOf('.'));
				try {
			parseQuad(path+"_rect.txt");
		} catch (FileNotFoundException e) {
			System.out.println("Can't find predefined region for "+file.getName());
		}

		super.openFile(file);
	}


	private void parseQuad( String fileName ) throws FileNotFoundException {
		Reader r = new FileReader(fileName);
		BufferedReader in = new BufferedReader(r);

		try {
			String w[] = in.readLine().split(" ");

			if( w.length != 8 )
				throw new RuntimeException("Unexpected number of variables in rectangle: "+w.length);

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
			throw new RuntimeException(e);
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

	public static void main(String[] args) {
//		Class type = GrayF32.class;
		Class type = GrayU8.class;

//		app.setBaseDirectory(UtilIO.pathExample("");
//		app.loadInputData(UtilIO.pathExample("tracking/file_list.txt");

		List<PathLabel> examples = new ArrayList<PathLabel>();
		examples.add(new PathLabel("WildCat", UtilIO.pathExample("tracking/wildcat_robot.mjpeg")));
		examples.add(new PathLabel("Tree", UtilIO.pathExample("tracking/tree.mjpeg")));
		examples.add(new PathLabel("Book", UtilIO.pathExample("tracking/track_book.mjpeg")));
		examples.add(new PathLabel("Face", UtilIO.pathExample("tracking/track_peter.mjpeg")));
		examples.add(new PathLabel("Chipmunk", UtilIO.pathExample("tracking/chipmunk.mjpeg")));
		examples.add(new PathLabel("Balls", UtilIO.pathExample("tracking/balls_blue_red.mjpeg")));
		examples.add(new PathLabel("Driving Snow", UtilIO.pathExample("tracking/snow_follow_car.mjpeg")));
		examples.add(new PathLabel("Driving Night", UtilIO.pathExample("tracking/night_follow_car.mjpeg")));

		VideoTrackerObjectQuadApp app = new VideoTrackerObjectQuadApp(examples,type);

		app.openFile(new File(examples.get(0).getPath()));

		app.waitUntilDoneProcessing();

		ShowImages.showWindow(app, "Tracking Rectangle",true);
	}
}
