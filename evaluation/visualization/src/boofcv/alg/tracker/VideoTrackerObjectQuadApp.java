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

import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.alg.tracker.tld.TldConfig;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.gui.VideoProcessAppBase;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
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
		extends VideoProcessAppBase<I>
		implements TrackerObjectQuadPanel.Listener  , TrackerQuadInfoPanel.Listener
{
	Class<I> imageType;
	TrackerObjectQuad<I> tracker;

	TrackerObjectQuadPanel videoPanel;
	TrackerQuadInfoPanel infoBar;

	int whichAlg;

	Quadrilateral_F64 target = new Quadrilateral_F64();

	boolean targetSelected = false;
	boolean selectionChanged = false;

	boolean success;
	boolean processedInputImage = false;
	boolean firstFrame = true;

	public VideoTrackerObjectQuadApp( Class<I> imageType ) {
		super(1, imageType);
		this.imageType = imageType;

		addAlgorithm(0, "TLD", 0);
		addAlgorithm(0, "Sparse Flow", 1);

		videoPanel = new TrackerObjectQuadPanel(this);
		infoBar = new TrackerQuadInfoPanel(this);


		add(infoBar, BorderLayout.WEST);
		add(videoPanel, BorderLayout.CENTER);
		setMainGUI(videoPanel);
	}

	@Override
	public void process( final SimpleImageSequence<I> sequence ) {

		// stop the image processing code
		stopWorker();

		this.sequence = sequence;
		sequence.setLoop(false);
		setPause(false);
		if( !sequence.hasNext() )
			throw new IllegalArgumentException("Empty sequence");

		// use default rectangle
		videoPanel.setDefaultTarget(target);
		firstFrame = true;
		targetSelected = true;
		selectionChanged = true;

		// start everything up and resume processing
		doRefreshAll();
	}

	@Override
	protected void updateAlg(I frame, BufferedImage buffImage) {
		if( targetSelected ) {
			if( selectionChanged ) {
				selectionChanged = false;
				success = tracker.initialize(frame, target);
			} else
				success = tracker.process(frame, target);
		}
		processedInputImage = true;
	}

	@Override
	protected void updateAlgGUI(I frame, BufferedImage imageGUI, double fps) {
		if( firstFrame ) {
			videoPanel.setPreferredSize(new Dimension(imageGUI.getWidth(),imageGUI.getHeight()));
			videoPanel.setBackGround((BufferedImage) sequence.getGuiImage());
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
		System.out.println();
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
	public void resetVideo() {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void selectTarget() {
		setPause(true);
		infoBar.setTracking("");
		targetSelected = false;
		videoPanel.enterSelectMode();
	}

	@Override
	protected void setPause( boolean paused ) {
		infoBar.setPlay(!paused);
		super.setPause(paused);
	}

	@Override
	public void refreshAll(Object[] cookies) {
		if( whichAlg == 0 )
			tracker = FactoryTrackerObjectQuad.createTLD(new TldConfig(true,imageType));
		else if( whichAlg == 1 )
			tracker = FactoryTrackerObjectQuad.createSparseFlow(new SfotConfig(imageType));
		else
			throw new RuntimeException("Unknown algorithm");

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
	public void changeInput(String name, int index) {
		processedInputImage = false;

		String path = inputRefs.get(index).getPath();

		parseQuad(path+"_rect.txt");

		SimpleImageSequence<I> video = media.openVideo(path+".mjpeg", ImageDataType.single(imageType));

		process(video);
	}

	private void parseQuad( String fileName ) {
		Reader r = media.openFile(fileName);
		BufferedReader in = new BufferedReader(r);

		try {
			String w[] = in.readLine().split(" ");

			if( w.length != 8 )
				throw new RuntimeException("Unexpected number of variables in rectangle: "+w.length);

			target.a.x = Double.parseDouble(w[0]);
			target.a.y = Double.parseDouble(w[1]);
			target.b.x = Double.parseDouble(w[2]);
			target.b.y = Double.parseDouble(w[3]);
			target.c.x = Double.parseDouble(w[4]);
			target.c.y = Double.parseDouble(w[5]);
			target.d.x = Double.parseDouble(w[6]);
			target.d.y = Double.parseDouble(w[7]);

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
			infoBar.setPlay(true);
		} else if( status == 1 ) {
			infoBar.setPlay(false);
		}
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public boolean getHasProcessedImage() {
		return processedInputImage;
	}


	public static void main(String[] args) {
		Class type = ImageFloat32.class;
//		Class type = ImageUInt8.class;

		VideoTrackerObjectQuadApp app = new VideoTrackerObjectQuadApp(type);

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("snow_follow_car", "../data/applet/tracking/snow_follow_car"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Tracking Rectangle");
	}
}
