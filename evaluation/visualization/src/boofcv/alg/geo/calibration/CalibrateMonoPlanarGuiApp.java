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

import boofcv.abst.calib.CalibrateMonoPlanar;
import boofcv.abst.calib.ConfigChessboard;
import boofcv.abst.calib.PlanarCalibrationDetector;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.border.BorderType;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.gui.VisualizeApp;
import boofcv.io.MediaManager;
import boofcv.io.ProgressMonitorThread;
import boofcv.io.UtilIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;

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
public class CalibrateMonoPlanarGuiApp extends JPanel 
		implements VisualizeApp {

	// computes calibration parameters
	CalibrateMonoPlanar calibrator;
	// displays results
	MonoPlanarPanel gui = new MonoPlanarPanel();
	// needed by ProcessThread for displaying its dialog
	JPanel owner;

	List<String> images;
	MediaManager media = DefaultMediaManager.INSTANCE;
	
	public CalibrateMonoPlanarGuiApp() {
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(800,525));
		this.owner = this;
		
		add(gui,BorderLayout.CENTER);
	}

	public void configure( PlanarCalibrationDetector detector ,
						   PlanarCalibrationTarget target,
						   List<String> images  ,
						   boolean flipY ) {

		calibrator = new CalibrateMonoPlanar(detector,flipY);
		calibrator.configure(target,true,2);
		this.images = images;
	}

	@Override
	public void loadConfigurationFile(String fileName) {
		ParseMonoCalibrationConfig parser = new ParseMonoCalibrationConfig(media);

		if( parser.parse(fileName) ) {
			configure(parser.detector,parser.target,parser.images,parser.flipY);
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
				ImageFloat32 input = ConvertBufferedImage.convertFrom(orig,(ImageFloat32)null);
				if( calibrator.addImage(input) ) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							gui.addImage(file.getName(), orig);
							gui.repaint();
							monitor.setMessage(0, file.getName());
						}
					});
				} else {
					System.out.println("Failed to detect image.  "+file.getName());
				}
			}
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setObservations(calibrator.getObservations());
			}});
		gui.repaint();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				monitor.setMessage(1,"Estimating Parameters");
			}});

		final IntrinsicParameters param = calibrator.process();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setResults(calibrator.getErrors());
				gui.setCalibration(calibrator.getZhangParam());
			}});
		monitor.stopThread();

		if( outputFileName != null )
			UtilIO.saveXML(param, outputFileName);

		// tell it how to undistort the image
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				ImageDistort<ImageFloat32,ImageFloat32> dist = LensDistortionOps.removeDistortion(
						true, BorderType.VALUE, param, null, ImageType.single(ImageFloat32.class));
				gui.setCorrection(dist);

				gui.repaint();
			}});

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
//		PlanarCalibrationDetector detector = FactoryPlanarCalibrationTarget.detectorSquareGrid(new ConfigSquareGrid(15,15, 14.0/18.0));
//		PlanarCalibrationDetector detector = FactoryPlanarCalibrationTarget.detectorSquareGrid(new ConfigSquareGrid(5,7));
		PlanarCalibrationDetector detector = FactoryPlanarCalibrationTarget.detectorChessboard(new ConfigChessboard(5,7));

//		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridSquare(15, 15, 0.5, 7.0 / 18.0);
//		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridSquare(5, 7,30,30);
		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridChess(5, 7, 30);

//		String directory = "../data/evaluation/calibration/mono/Sony_DSC-HX5V_Square";
//		String directory = "../data/evaluation/calibration/mono/Sony_DSC-HX5V_Chess";
//		String directory = "../data/evaluation/calibration/mono/PULNiX_CCD_6mm_Zhang";
		String directory = "../data/evaluation/calibration/stereo/Bumblebee2_Chess";
//		String directory = "../data/evaluation/calibration/stereo/Bumblebee2_Square";

		CalibrateMonoPlanarGuiApp app = new CalibrateMonoPlanarGuiApp();
//		app.configure(detector,target,BoofMiscOps.directoryList(directory, "frame" ),false);
		app.configure(detector,target,BoofMiscOps.directoryList(directory, "left" ),false);
//		app.configure(detector,target,BoofMiscOps.directoryList(directory, "CalibIm" ),false);

		JFrame frame = new JFrame("Planar Calibration");
		frame.add(app, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);

		app.process("intrinsic.xml");
	}
}
