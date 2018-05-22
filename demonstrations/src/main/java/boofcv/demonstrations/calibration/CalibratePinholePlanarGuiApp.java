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
import boofcv.abst.geo.calibration.CalibrateMonoPlanar;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.gui.VisualizeApp;
import boofcv.gui.calibration.MonoPlanarPanel;
import boofcv.io.MediaManager;
import boofcv.io.ProgressMonitorThread;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayF32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Computes intrinsic camera calibration parameters from a set of calibration images.  Results
 * are displayed in a window allowing their accuracy to be easily seen.
 *
 * @author Peter Abeles
 */
public class CalibratePinholePlanarGuiApp extends JPanel
		implements VisualizeApp {

	// computes calibration parameters
	CalibrateMonoPlanar calibrator;
	DetectorFiducialCalibration detector;
	// displays results
	MonoPlanarPanel gui = new MonoPlanarPanel();
	// needed by ProcessThread for displaying its dialog
	JPanel owner;

	List<String> images;
	MediaManager media = DefaultMediaManager.INSTANCE;

	public CalibratePinholePlanarGuiApp() {
		setLayout(new BorderLayout());
		gui.mainView.setPreferredSize(new Dimension(500,480));
		this.owner = this;

		add(gui,BorderLayout.CENTER);
	}

	public void configure( DetectorFiducialCalibration detector ,
						   List<String> images  ,
						   int numRadial, boolean includeTangential )
	{
		this.detector = detector;
		if( images.size() == 0 )
			throw new IllegalArgumentException("No images!");
		BoofMiscOps.sortFileNames(images);
		calibrator = new CalibrateMonoPlanar(detector.getLayout());
		calibrator.configurePinhole(true,numRadial,includeTangential);
		this.images = images;
	}

	@Override
	public void loadConfigurationFile(String fileName) {
		ParseMonoCalibrationConfig parser = new ParseMonoCalibrationConfig(media);

		if( parser.parse(fileName) ) {
			configure(parser.detector,parser.images,
					parser.numRadial,parser.includeTangential);
		} else {
			System.err.println("Configuration failed");
		}
	}

	public void process( String outputFileName ) {
		calibrator.reset();
		final ProcessThread monitor = new ProcessThread();
		monitor.start();

		for( int i = 0; i < images.size(); i++ ) {
			final File file = new File(images.get(i));
			final BufferedImage orig = media.openImage(images.get(i));
			if( orig != null ) {
				GrayF32 input = ConvertBufferedImage.convertFrom(orig,(GrayF32)null);
				if( detector.process(input)) {
					calibrator.addImage(detector.getDetectedPoints());
					SwingUtilities.invokeLater(() -> {
						gui.addImage(file);
						gui.repaint();
						monitor.setMessage(0, file.getName());
					});
				} else {
					System.out.println("Failed to detect image.  "+file.getName());
				}
			} else {
				System.out.println("Failed to load "+images.get(i));
			}
		}

		SwingUtilities.invokeLater(() -> gui.setObservations(calibrator.getObservations()));
		gui.repaint();

		SwingUtilities.invokeLater(() -> monitor.setMessage(1,"Estimating Parameters"));

		final CameraPinholeRadial param = calibrator.process();
		SwingUtilities.invokeLater(() -> {
			gui.setResults(calibrator.getErrors());
			gui.setCalibration(calibrator.getZhangParam());
		});
		monitor.stopThread();

		if( outputFileName != null )
			CalibrationIO.save(param, outputFileName);

		// tell it how to undistort the image
		SwingUtilities.invokeLater(() -> {
			gui.setCorrection(param);

			gui.repaint();
		});

		// print the output
		calibrator.printStatistics();
		System.out.println();
		System.out.println("--- Intrinsic Parameters ---");
		System.out.println();
		param.print();
	}

	@Override
	public void setMediaManager(MediaManager manager) {
		media = manager;
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
			SwingUtilities.invokeLater(() -> {
				monitor.setProgress(state);
				monitor.setNote(message);
			});
		}

		@Override
		public void doRun() {
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

	@Override
	public boolean getHasProcessedImage() {
		return true;
	}

	public static void main( String args[] ) {
		DetectorFiducialCalibration detector =
//				FactoryFiducialCalibration.squareGrid(new ConfigSquareGrid(8, 8, 0.5, 7.0 / 18.0));
//				FactoryFiducialCalibration.squareGrid(new ConfigSquareGrid(4,3,30,30));
				FactoryFiducialCalibration.chessboard(new ConfigChessboard(7, 5, 30));
//				FactoryFiducialCalibration.circleHexagonalGrid(new ConfigCircleHexagonalGrid(5, 8, 1, 6));

		List<String> images;
//		images = UtilIO.directoryList(UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Square"),"frame");
		images = UtilIO.listByPrefix(UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess"),"frame");
//		images = UtilIO.directoryList(UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_CircleHexagonal"),"image");
//		images = UtilIO.directoryList(UtilIO.pathExample("calibration/mono/PULNiX_CCD_6mm_Zhang"),"CalibIm");
//		images = UtilIO.directoryList(UtilIO.pathExample("calibration//stereo/Bumblebee2_Square"),"left");

		CalibratePinholePlanarGuiApp app = new CalibratePinholePlanarGuiApp();
		app.configure(detector,images,2,false);

		JFrame frame = new JFrame("Pinhole Calribation with Planar Targets");
		frame.add(app, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		app.process("intrinsic.yaml");
	}
}
