/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.fiducial;

import java.io.File;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.JCheckBox;
import boofcv.abst.calib.*;
import boofcv.abst.fiducial.*;
import boofcv.core.image.*;
import boofcv.factory.fiducial.*;
import boofcv.factory.filter.binary.*;
import boofcv.gui.VideoProcessAppBase;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.gui.image.*;
import boofcv.io.*;
import boofcv.io.image.*;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.*;
import georegression.struct.se.Se3_F64;

/**
 * Let's the user select a region in the image and tracks it using different algorithms.
 *
 * @author Peter Abeles
 */
public class FiducialTrackerApp<I extends ImageSingleBand>
		extends VideoProcessAppBase<MultiSpectral<I>>
{
	public static final String SQUARE_NUMBER = "Square Number";
	public static final String SQUARE_PICTURE = "Square Picture";
	public static final String CALIB_CHESS = "Chessboard";
	public static final String CALIB_SQUARE_GRID = "Square Grid";


	private static final Font font = new Font("Serif", Font.BOLD, 14);

	Class<I> imageClass;

	ImagePanel panel = new ImagePanel();

	I gray;

	FiducialDetector detector;

	IntrinsicParameters intrinsic;

	boolean processedInputImage = false;
	boolean firstFrame = true;

	JCheckBox computeStability = new JCheckBox("Stability");

	FiducialStability stability = new FiducialStability();

	List<FiducialInfo> fiducialInfo = new ArrayList<FiducialInfo>();
	FiducialStability stabilityMax = new FiducialStability();

	public FiducialTrackerApp(Class<I> imageType) {
		super(0, ImageType.ms(3, imageType));
		this.imageClass = imageType;

		gray = GeneralizedImageOps.createSingleBand(imageType,1,1);

		panel.setPreferredSize(new Dimension(640, 480));
		panel.setFocusable(true);
		panel.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				isPaused = !isPaused;
			}

			@Override public void mousePressed(MouseEvent e) {}
			@Override public void mouseReleased(MouseEvent e) {}
			@Override public void mouseEntered(MouseEvent e) {}
			@Override public void mouseExited(MouseEvent e) {}
		});
		addToToolbar(computeStability);
		computeStability.addActionListener(this);
		computeStability.setSelected(true);

		setMainGUI(panel);

		periodSpinner.setValue(33);
	}

	@Override
	public void process( final SimpleImageSequence<MultiSpectral<I>> sequence ) {

		// stop the image processing code
		stopWorker();

		this.sequence = sequence;
		sequence.setLoop(true);
		setPause(false);
		if( !sequence.hasNext() )
			throw new IllegalArgumentException("Empty sequence");

		// start everything up and resume processing
		doRefreshAll();
	}

	@Override
	public void refreshAll(Object[] cookies) {

		firstFrame = true;
		setPause(false);

		startWorkerThread();
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		stopWorker();

		sequence.reset();

		refreshAll(null);
	}

	@Override
	protected void updateAlg(MultiSpectral<I> frame, BufferedImage buffImage) {

		if( detector.getInputType().getFamily() == ImageType.Family.SINGLE_BAND ) {
			gray.reshape(frame.width, frame.height);
			GConvertImage.average(frame, gray);
		}

		detector.detect(gray);

		processedInputImage = true;
	}

	@Override
	protected void updateAlgGUI(MultiSpectral<I> frame, BufferedImage imageGUI, double fps) {
		if( firstFrame ) {
			panel.setPreferredSize(new Dimension(imageGUI.getWidth(),imageGUI.getHeight()));
			firstFrame = false;
		}

		int height = getHeight();

		Graphics2D g2 = imageGUI.createGraphics();
		Se3_F64 targetToSensor = new Se3_F64();
		for (int i = 0; i < detector.totalFound(); i++) {
			detector.getFiducialToCamera(i, targetToSensor);
			double width = detector.getWidth(i);
			long id = detector.getId(i);

			VisualizeFiducial.drawLabelCenter(targetToSensor, intrinsic, ""+id, g2);
			VisualizeFiducial.drawCube(targetToSensor, intrinsic, width, 3, g2);

			if( computeStability.isSelected() ) {
				handleStability(height, g2, i, id);
			}
		}

		panel.setBufferedImageSafe(imageGUI);
		panel.repaint();
	}

	/**
	 * Computes and visualizes the stability
	 */
	private void handleStability(int height, Graphics2D g2, int index, long fiducialID) {
		FiducialInfo info = findFiducial(fiducialID);
		info.totalObserved++;

		g2.setFont(font);
		if (detector.computeStability(index, 0.25, stability)) {
			stabilityMax.location = Math.max(stability.location, stabilityMax.location);
			stabilityMax.orientation = Math.max(stability.orientation, stabilityMax.orientation);
		}

		if (info.totalObserved > 20) {

			double fractionLocation = stability.location / stabilityMax.location;
			double fractionOrientation = stability.orientation / stabilityMax.orientation;

			int maxHeight = (int) (height * 0.15);

			int x = info.grid * 60;
			int y = 10 + maxHeight;

			g2.setColor(Color.BLUE);
			int h = (int) (fractionLocation * maxHeight);
			g2.fillRect(x, y - h, 20, h);

			g2.setColor(Color.CYAN);
			x += 25;
			h = (int) (fractionOrientation * maxHeight);
			g2.fillRect(x, y - h, 20, h);

			g2.setColor(Color.RED);
			g2.drawString("" + info.id, x - 20, y + 20);
		}
	}

	private FiducialInfo findFiducial( long id ) {
		for (int i = 0; i < fiducialInfo.size(); i++) {
			FiducialInfo info = fiducialInfo.get(i);
			if( info.id == id ) {
				return info;
			}
		}

		FiducialInfo found = new FiducialInfo();
		found.id = id;
		found.grid = fiducialInfo.size();
		fiducialInfo.add(found);

		return found;
	}

	@Override
	public void changeInput(String name, int index) {

		stopWorker();
		processedInputImage = false;

		String videoName = inputRefs.get(index).getPath();
		String path = videoName.substring(0, videoName.lastIndexOf('/'));

		ConfigThreshold configThreshold = ConfigThreshold.local(ThresholdType.LOCAL_SQUARE, 10);

		if( name.compareTo(SQUARE_NUMBER) == 0 ) {
			detector = FactoryFiducial.squareBinary(new ConfigFiducialBinary(0.1), configThreshold, imageClass);
		} else if( name.compareTo(SQUARE_PICTURE) == 0 ) {
			double length = 0.1;
			detector = FactoryFiducial.squareImage(new ConfigFiducialImage(), configThreshold, imageClass);

			SquareImage_to_FiducialDetector<I> d = (SquareImage_to_FiducialDetector<I>)detector;

			String pathImg = new File(path,"../patterns").getPath();
			List<String> names = new ArrayList<String>();
			names.add("chicken.png");
			names.add("yinyang.png");

			for( String foo : names ) {
				BufferedImage img = media.openImage(new File(pathImg,foo).getPath());
				if( img == null )
					throw new RuntimeException("Can't find file "+new File(pathImg,foo).getPath());
				d.addPatternImage(ConvertBufferedImage.convertFromSingle(img, null, imageClass), 125, length);
			}

		} else if( name.compareTo(CALIB_CHESS) == 0 ) {
			detector = FactoryFiducial.calibChessboard(new ConfigChessboard(5,7,0.03), imageClass);
		} else if( name.compareTo(CALIB_SQUARE_GRID) == 0 ) {
			detector = FactoryFiducial.calibSquareGrid(new ConfigSquareGrid(3, 4, 0.03, 0.03), imageClass);
		} else {
			throw new RuntimeException("Unknown selection");
		}

		intrinsic = UtilIO.loadXML(media.openFile(path+"/intrinsic.xml"));

		detector.setIntrinsic(intrinsic);

		fiducialInfo.clear();
		stabilityMax.location = 0;
		stabilityMax.orientation = 0;

		SimpleImageSequence<MultiSpectral<I>> video = media.openVideo(videoName, ImageType.ms(3, imageClass));

		if( video == null ) {
			System.err.println("Can't find video "+videoName);
			System.exit(1);
		}

		process(video);
	}

	@Override
	protected void handleRunningStatus(int status) {

	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public boolean getHasProcessedImage() {
		return processedInputImage;
	}

	private class FiducialInfo {
		int totalObserved;
		long id;
		int grid;
	}

	public static void main(String[] args) {
//		Class type = ImageFloat32.class;
		Class type = ImageUInt8.class;

		FiducialTrackerApp app = new FiducialTrackerApp(type);

//		app.setBaseDirectory(UtilIO.pathExample("fiducial/"));
//		app.loadInputData(UtilIO.pathExample("fiducial/fiducial.txt"));

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel(SQUARE_NUMBER, UtilIO.pathExample("fiducial/binary/movie.mjpeg")));
		inputs.add(new PathLabel(SQUARE_PICTURE, UtilIO.pathExample("fiducial/image/video/movie.mjpeg")));
		inputs.add(new PathLabel(CALIB_CHESS, UtilIO.pathExample("fiducial/chessboard/movie.mjpeg")));
		inputs.add(new PathLabel(CALIB_SQUARE_GRID, UtilIO.pathExample("fiducial/square_grid/movie.mp4")));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Detecting Fiducials", true);
	}
}
