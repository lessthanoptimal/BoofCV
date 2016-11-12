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

package boofcv.demonstrations.calibration;

import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.abst.geo.calibration.CalibrateStereoPlanar;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.gui.VisualizeApp;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.ProgressMonitorThread;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayF32;
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
				GrayF32 leftInput = ConvertBufferedImage.convertFrom(leftOrig, (GrayF32) null);
				GrayF32 rightInput = ConvertBufferedImage.convertFrom(rightOrig, (GrayF32) null);
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
			} else {
				System.out.println("Failed to load left  = "+leftImages.get(i));
				System.out.println("Failed to load right = "+rightImages.get(i));

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
	private void setRectification(final StereoParameters param) {

		// calibration matrix for left and right camera
		DenseMatrix64F K1 = PerspectiveOps.calibrationMatrix(param.getLeft(), null);
		DenseMatrix64F K2 = PerspectiveOps.calibrationMatrix(param.getRight(), null);

		RectifyCalibrated rectify = RectifyImageOps.createCalibrated();
		rectify.process(K1,new Se3_F64(),K2,param.getRightToLeft().invert(null));

		final DenseMatrix64F rect1 = rectify.getRect1();
		final DenseMatrix64F rect2 = rectify.getRect2();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setRectification(param.getLeft(),rect1,param.getRight(),rect2);
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
	 * @param assumeZeroSkew If true the skew parameter is assumed to be zero
	 * @param leftImages Images taken by left camera.
	 * @param rightImages Images taken by right camera.
	 */
	public void configure( DetectorFiducialCalibration detector ,
						   int numRadial,
						   boolean includeTangential,
						   boolean assumeZeroSkew ,
						   List<String> leftImages , List<String> rightImages  ) {

		if( leftImages.size() != rightImages.size() )
			throw new IllegalArgumentException("Number of left and right images must be the same");

		calibrator = new CalibrateStereoPlanar(detector);
		calibrator.configure(assumeZeroSkew,numRadial,includeTangential);
		this.leftImages = leftImages;
		this.rightImages = rightImages;
	}

	@Override
	public void loadConfigurationFile(String fileName) {
		ParseStereoCalibrationConfig parser = new ParseStereoCalibrationConfig(media);

		if( parser.parse(fileName) ) {
			configure(parser.detector,parser.numRadial,
					parser.includeTangential,parser.assumeZeroSkew,
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
		DetectorFiducialCalibration detector =
				FactoryFiducialCalibration.chessboard(new ConfigChessboard(7, 5, 30));
//				FactoryCalibrationTarget.squareGrid(new ConfigSquareGrid(4, 3, 30, 30));


		String directory = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess");
//		String directory = UtilIO.pathExample("calibration/stereo/Bumblebee2_Square");

		List<String> leftImages = BoofMiscOps.directoryList(directory, "left");
		List<String> rightImages = BoofMiscOps.directoryList(directory, "right");

		Collections.sort(leftImages);
		Collections.sort(rightImages);

		CalibrateStereoPlanarGuiApp app = new CalibrateStereoPlanarGuiApp();
		app.configure(detector,2,false,true, leftImages,rightImages);

		ShowImages.showWindow(app,"Planar Stereo Calibration",true);

		app.process("stereo.yaml");
	}
}
