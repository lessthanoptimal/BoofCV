/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import boofcv.abst.calib.CalibrateStereoPlanar;
import boofcv.abst.calib.ConfigChessboard;
import boofcv.abst.calib.PlanarCalibrationDetector;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.gui.VisualizeApp;
import boofcv.io.MediaManager;
import boofcv.io.ProgressMonitorThread;
import boofcv.io.UtilIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class CalibrateStereoPlanarGuiApp extends JPanel
		implements VisualizeApp {

	// computes calibration parameters
	CalibrateStereoPlanar calibrator;
	// displays results
	StereoPlanarPanel gui = new StereoPlanarPanel();
	// needed by ProcessThread for displaying its dialog
	JPanel owner;

	// file reference to calibration images
	List<String> leftImages;
	List<String> rightImages;

	MediaManager media = DefaultMediaManager.INSTANCE;

	public CalibrateStereoPlanarGuiApp() {
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(1500,525));
		this.owner = this;

		add(gui,BorderLayout.CENTER);
	}

	public void process( String outputFileName ) {
		// displays progress so the impatient don't give up
		final ProcessThread monitor = new ProcessThread();
		monitor.start();

		// load images
		calibrator.reset();

		int N = leftImages.size();

		for( int i = 0; i < N; i++ ) {
			final BufferedImage leftOrig = media.openImage(leftImages.get(i));
			final BufferedImage rightOrig = media.openImage(rightImages.get(i));
			if( leftOrig != null && rightOrig != null ) {
				ImageFloat32 leftInput = ConvertBufferedImage.convertFrom(leftOrig, (ImageFloat32) null);
				ImageFloat32 rightInput = ConvertBufferedImage.convertFrom(rightOrig, (ImageFloat32) null);
				if( calibrator.addPair(leftInput,rightInput ) ) {
					final int number = i;
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							gui.addPair("Image " + number, leftOrig, rightOrig);
							gui.repaint();
							monitor.setMessage(0, "Image "+number);
						}});
				} else {
					System.out.println("Feature detection failed in:");
					System.out.println(leftImages.get(i)+" and/or "+rightImages.get(i));
				}
			}
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setObservations(calibrator.getCalibLeft().getObservations(),calibrator.getCalibLeft().getErrors(),
						calibrator.getCalibRight().getObservations(),calibrator.getCalibRight().getErrors());
			}});
		gui.repaint();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				monitor.setMessage(1,"Estimating Parameters");
			}});

		StereoParameters param = calibrator.process();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setObservations(calibrator.getCalibLeft().getObservations(),calibrator.getCalibLeft().getErrors(),
						calibrator.getCalibRight().getObservations(),calibrator.getCalibRight().getErrors());
			}});
		gui.repaint();

		// compute stereo rectification
		setRectification(param);

		monitor.stopThread();

		calibrator.printStatistics();
		param.print();
		if( outputFileName != null )
			UtilIO.saveXML(param, outputFileName);
	}

	/**
	 * Computes stereo rectification and then passes the distortion along to the gui.
	 */
	private void setRectification(StereoParameters param) {

		// calibration matrix for left and right camera
		DenseMatrix64F K1 = PerspectiveOps.calibrationMatrix(param.getLeft(), null);
		DenseMatrix64F K2 = PerspectiveOps.calibrationMatrix(param.getRight(), null);

		RectifyCalibrated rectify = RectifyImageOps.createCalibrated();
		rectify.process(K1,new Se3_F64(),K2,param.getRightToLeft().invert(null));

		DenseMatrix64F rect1 = rectify.getRect1();
		DenseMatrix64F rect2 = rectify.getRect2();

//		RectifyImageOps.fullViewLeft(param.getLeft(),toRight,rect1,rect2,rectify.getCalibrationMatrix());

		// Rectification distortion for each image
		final ImageDistort<ImageFloat32,ImageFloat32> distort1 = RectifyImageOps.rectifyImage(param.getLeft(),
				rect1,ImageFloat32.class);
		final ImageDistort<ImageFloat32,ImageFloat32> distort2 = RectifyImageOps.rectifyImage(param.getRight(),
				rect2,ImageFloat32.class);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setRectification(distort1, distort2);
			}
		});
		gui.repaint();
	}

	@Override
	public void setMediaManager(MediaManager manager) {
		this.media = manager;
	}

	/**
	 * Configures the calibration tool. For the calibration images, the image index in both lists must
	 * correspond to images taken at the same time.
	 *
	 * @param detector Calibration target detector.
	 * @param target Description of the target being detected.
	 * @param assumeZeroSkew If true the skew parameter is assumed to be zero
	 * @param flipY If true the y-axis will be inverted.
	 * @param leftImages Images taken by left camera.
	 * @param rightImages Images taken by right camera.
	 */
	public void configure( PlanarCalibrationDetector detector ,
						   PlanarCalibrationTarget target,
						   boolean assumeZeroSkew ,
						   boolean flipY ,
						   List<String> leftImages , List<String> rightImages  ) {

		if( leftImages.size() != rightImages.size() )
			throw new IllegalArgumentException("Number of left and right images must be the same");

		calibrator = new CalibrateStereoPlanar(detector,flipY);
		calibrator.configure(target,assumeZeroSkew,2);
		this.leftImages = leftImages;
		this.rightImages = rightImages;
	}

	@Override
	public void loadConfigurationFile(String fileName) {
		ParseStereoCalibrationConfig parser = new ParseStereoCalibrationConfig(media);

		if( parser.parse(fileName) ) {
			configure(parser.detector,parser.target,parser.assumeZeroSkew,parser.flipY,
					parser.getLeftImages(),parser.getRightImages());
		} else {
			System.err.println("Configuration failed");
		}
	}

	@Override
	public void loadInputData(String fileName) {
		new Thread() {
			public void run() {
				process(null);
			}
		}.start();
	}

	/**
	 * Displays a progress monitor and updates its state periodically
	 */
	public class ProcessThread extends ProgressMonitorThread
	{
		public ProcessThread() {
			super(new ProgressMonitor(owner, "Computing Calibration", "", 0, 2));
		}

		public void setMessage( final int state , final String message ) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					monitor.setProgress(state);
					monitor.setNote(message);
				}});
		}

		@Override
		public void doRun() {
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return true;
	}

	public static void main( String args[] ) {
//		PlanarCalibrationDetector detector = FactoryPlanarCalibrationTarget.detectorSquareGrid(new ConfigSquareGrid(5,7));
		PlanarCalibrationDetector detector = FactoryPlanarCalibrationTarget.detectorChessboard(new ConfigChessboard(5,7));

//		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridSquare(5,7,30,30);
		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridChess(5, 7, 30);

		String directory = "../data/evaluation/calibration/stereo/Bumblebee2_Chess";
//		String directory = "../data/evaluation/calibration/stereo/Bumblebee2_Square";

		List<String> leftImages = BoofMiscOps.directoryList(directory, "left");
		List<String> rightImages = BoofMiscOps.directoryList(directory, "right");

		Collections.sort(leftImages);
		Collections.sort(rightImages);

		CalibrateStereoPlanarGuiApp app = new CalibrateStereoPlanarGuiApp();
		app.configure(detector,target,true,false, leftImages,rightImages);

		JFrame frame = new JFrame("Planar Stereo Calibration");
		frame.add(app, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);

		app.process("stereo.xml");
	}
}
