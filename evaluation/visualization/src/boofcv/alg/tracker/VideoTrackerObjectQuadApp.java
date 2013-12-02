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

import boofcv.abst.tracker.ConfigCirculantTracker;
import boofcv.abst.tracker.ConfigComaniciu2003;
import boofcv.abst.tracker.MeanShiftLikelihoodType;
import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.alg.tracker.tld.TldConfig;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.gui.VideoProcessAppBase;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.shapes.Quadrilateral_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

/**
 * Let's the user select a region in the image and tracks it using different algorithms.
 *
 * @author Peter Abeles
 */
public class VideoTrackerObjectQuadApp<I extends ImageSingleBand>
		extends VideoProcessAppBase<MultiSpectral<I>>
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
	boolean processedInputImage = false;
	boolean firstFrame = true;

	public VideoTrackerObjectQuadApp( Class<I> imageType ) {
		super(1, ImageType.ms(3, imageType));
		this.imageClass = imageType;

		gray = GeneralizedImageOps.createSingleBand(imageType,1,1);

		addAlgorithm(0, "TLD", 0);
		addAlgorithm(0, "Sparse Flow", 1);
		addAlgorithm(0, "Ciruclant", 2);
		addAlgorithm(0, "Mean Shift Likelihood HSV", 3);
		addAlgorithm(0, "Mean Shift Likelihood RGB", 4);
		addAlgorithm(0, "Mean Shift Scale", 5);
		addAlgorithm(0, "Mean Shift Fixed", 6);

		videoPanel = new TrackerObjectQuadPanel(this);
		infoBar = new TrackerQuadInfoPanel(this);

		add(infoBar, BorderLayout.WEST);
		add(videoPanel, BorderLayout.CENTER);
		setMainGUI(videoPanel);
	}

	@Override
	public void process( final SimpleImageSequence<MultiSpectral<I>> sequence ) {

		// stop the image processing code
		stopWorker();

		this.sequence = sequence;
		sequence.setLoop(false);
		setPause(false);
		if( !sequence.hasNext() )
			throw new IllegalArgumentException("Empty sequence");

		// start everything up and resume processing
		doRefreshAll();
	}

	@Override
	public void refreshAll(Object[] cookies) {
		if( whichAlg == 0 )
			tracker = FactoryTrackerObjectQuad.tld(new TldConfig(true, imageClass));
		else if( whichAlg == 1 )
			tracker = FactoryTrackerObjectQuad.sparseFlow(new SfotConfig(imageClass));
		else if( whichAlg == 2 )
			tracker = FactoryTrackerObjectQuad.circulant(new ConfigCirculantTracker(), imageClass);
		else if( whichAlg == 3 )
			tracker = FactoryTrackerObjectQuad.meanShiftLikelihood(30, 6, 255,
					MeanShiftLikelihoodType.HISTOGRAM_RGB_to_HSV, imageType);
		else if( whichAlg == 4 )
			tracker = FactoryTrackerObjectQuad.meanShiftLikelihood(30, 4, 255,
					MeanShiftLikelihoodType.HISTOGRAM, imageType);
		else if( whichAlg == 5 ) {
			ConfigComaniciu2003 config = new ConfigComaniciu2003(imageType);
			config.scaleChange = 0.05f;
			tracker = FactoryTrackerObjectQuad.meanShiftComaniciu2003(config);
		} else if( whichAlg == 6 ) {
			ConfigComaniciu2003 config = new ConfigComaniciu2003(imageType);
			config.scaleChange = 0;
			tracker = FactoryTrackerObjectQuad.meanShiftComaniciu2003(config);
		} else
			throw new RuntimeException("Unknown algorithm");

		// use default rectangle
		videoPanel.setDefaultTarget(targetDefault);
		firstFrame = true;
		targetSelected = true;
		selectionChanged = true;
		target.set(targetDefault);
		setPause(false);

		startWorkerThread();
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		stopWorker();

		whichAlg = (Integer)cookie;

		sequence.reset();

		refreshAll(null);
	}

	@Override
	protected void updateAlg(MultiSpectral<I> frame, BufferedImage buffImage) {

		boolean grayScale = false;

		if( tracker.getImageType().getFamily() == ImageType.Family.SINGLE_BAND ) {
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
		processedInputImage = true;
	}

	@Override
	protected void updateAlgGUI(MultiSpectral<I> frame, BufferedImage imageGUI, double fps) {
		if( firstFrame ) {
			videoPanel.setPreferredSize(new Dimension(imageGUI.getWidth(),imageGUI.getHeight()));
			videoPanel.setBackGround((BufferedImage) sequence.getGuiImage());
			infoBar.setFPS(0);
			infoBar.setTracking("");
			firstFrame = false;
			setPause(true);
		} else {
			videoPanel.setBackGround((BufferedImage) sequence.getGuiImage());
			videoPanel.setTarget(target, success);
			infoBar.setFPS(fps);
			if( success ) {
				infoBar.setTracking("FOUND");
			} else {
				infoBar.setTracking("?");
			}
			videoPanel.repaint();
		}
	}

	@Override
	public void selectedTarget(Quadrilateral_F64 target) {
		System.out.println(target.a.x+" "+target.a.y+" "+target.b.x+" "+target.b.y+" "+target.c.x+" "+target.c.y+" "+target.d.x+" "+target.d.y);
		this.target.set(target);
		targetSelected = true;
		selectionChanged = true;
		setPause(false);
	}

	@Override
	public void togglePause() {
		setPause(!isPaused);
	}

	@Override
	public void selectTarget() {
		setPause(true);
		infoBar.setTracking("");
		targetSelected = false;
		videoPanel.enterSelectMode();
	}

	@Override
	public void changeInput(String name, int index) {
		processedInputImage = false;

		String path = inputRefs.get(index).getPath();

		parseQuad(path+"_rect.txt");

		SimpleImageSequence<MultiSpectral<I>> video = media.openVideo(path+".mjpeg", ImageType.ms(3, imageClass));

		process(video);
	}

	private void parseQuad( String fileName ) {
		Reader r = media.openFile(fileName);
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
	@Override
	protected void handleRunningStatus(int status) {
		if( status == 0 ) {
			infoBar.setPlay(false);
		} else if( status == 1 ) {
			infoBar.setPlay(true);
		}
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public boolean getHasProcessedImage() {
		return processedInputImage;
	}


	public static void main(String[] args) {
//		Class type = ImageFloat32.class;
		Class type = ImageUInt8.class;

		VideoTrackerObjectQuadApp app = new VideoTrackerObjectQuadApp(type);

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("snow_follow_car", "../data/applet/tracking/snow_follow_car"));
		inputs.add(new PathLabel("track_book", "../data/applet/tracking/track_book"));
		inputs.add(new PathLabel("track_peter", "../data/applet/tracking/track_peter"));
		inputs.add(new PathLabel("chipmunk", "../data/applet/tracking/chipmunk"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Tracking Rectangle");
	}
}
