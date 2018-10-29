/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.ProgressMonitorThread;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayF32;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class CalibrateStereoPlanarGuiApp extends JPanel {

	// computes calibration parameters
	CalibrateStereoPlanar calibrator;
	DetectorFiducialCalibration detector;
	// displays results
	StereoPlanarPanel gui = new StereoPlanarPanel();
	// needed by ProcessThread for displaying its dialog
	JPanel owner;

	// file reference to calibration images
	List<File> leftImages;
	List<File> rightImages;

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
			final BufferedImage leftOrig = media.openImage(leftImages.get(i).getPath());
			final BufferedImage rightOrig = media.openImage(rightImages.get(i).getPath());
			if( leftOrig != null && rightOrig != null ) {
				GrayF32 leftInput = ConvertBufferedImage.convertFrom(leftOrig, (GrayF32) null);
				GrayF32 rightInput = ConvertBufferedImage.convertFrom(rightOrig, (GrayF32) null);
				CalibrationObservation calibLeft,calibRight;
				if( !detector.process(leftInput)) {
					System.out.println("Feature detection failed in "+leftImages.get(i));
					continue;
				}
				calibLeft = detector.getDetectedPoints();
				if( !detector.process(rightInput)) {
					System.out.println("Feature detection failed in "+rightImages.get(i));
					continue;
				}
				calibRight = detector.getDetectedPoints();

				calibrator.addPair(calibLeft,calibRight );
				final int number = i;
				SwingUtilities.invokeLater(() -> {
					gui.addPair("Image " + number, leftImages.get(number), rightImages.get(number));
					gui.repaint();
					monitor.setMessage(0, "Image "+number);
				});
			} else {
				System.out.println("Failed to load left  = "+leftImages.get(i));
				System.out.println("Failed to load right = "+rightImages.get(i));

			}
		}

//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
//				gui.setObservations(calibrator.getCalibLeft().getObservations(),calibrator.getCalibLeft().getErrors(),
//						calibrator.getCalibRight().getObservations(),calibrator.getCalibRight().getErrors());
//			}});
//		gui.repaint();

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
			CalibrationIO.save(param, outputFileName);
	}

	/**
	 * Computes stereo rectification and then passes the distortion along to the gui.
	 */
	private void setRectification(final StereoParameters param) {

		// calibration matrix for left and right camera
		DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(param.getLeft(), (DMatrixRMaj)null);
		DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(param.getRight(), (DMatrixRMaj)null);

		RectifyCalibrated rectify = RectifyImageOps.createCalibrated();
		rectify.process(K1,new Se3_F64(),K2,param.getRightToLeft().invert(null));

		final DMatrixRMaj rect1 = rectify.getRect1();
		final DMatrixRMaj rect2 = rectify.getRect2();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setRectification(param.getLeft(),rect1,param.getRight(),rect2);
			}
		});
		gui.repaint();
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
						   List<File> leftImages , List<File> rightImages  ) {

		if( leftImages.size() != rightImages.size() )
			throw new IllegalArgumentException("Number of left and right images must be the same");

		this.detector = detector;
		calibrator = new CalibrateStereoPlanar(detector.getLayout());
		calibrator.configure(assumeZeroSkew,numRadial,includeTangential);
		this.leftImages = leftImages;
		this.rightImages = rightImages;
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

	public static void main( String args[] ) {
		DetectorFiducialCalibration detector =
				FactoryFiducialCalibration.chessboard(new ConfigChessboard(7, 5, 30));
//				FactoryCalibrationTarget.squareGrid(new ConfigSquareGrid(4, 3, 30, 30));


		String directory = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess");
//		String directory = UtilIO.pathExample("calibration/stereo/Bumblebee2_Square");

		List<String> leftImages = UtilIO.listByPrefix(directory, "left", null);
		List<String> rightImages = UtilIO.listByPrefix(directory, "right", null);

		Collections.sort(leftImages);
		Collections.sort(rightImages);

		List<File> leftFiles = new ArrayList<>();
		List<File> rightFiles = new ArrayList<>();

		for( String s : leftImages ) {
			leftFiles.add( new File(s));
		}
		for( String s : rightImages ) {
			rightFiles.add( new File(s));
		}

		SwingUtilities.invokeLater(()-> {
			CalibrateStereoPlanarGuiApp app = new CalibrateStereoPlanarGuiApp();
			app.configure(detector, 2, false, true, leftFiles, rightFiles);

			ShowImages.showWindow(app, "Planar Stereo Calibration", true);

			new Thread(() -> app.process("stereo.yaml")).start();
		});
	}
}
