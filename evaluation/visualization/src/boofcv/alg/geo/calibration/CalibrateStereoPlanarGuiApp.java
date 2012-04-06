/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.app.CalibrateMonoPlanarApp;
import boofcv.app.CalibrateStereoPlanar;
import boofcv.app.PlanarCalibrationDetector;
import boofcv.app.WrapPlanarChessTarget;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.VisualizeApp;
import boofcv.io.MediaManager;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageFloat32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
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
		setPreferredSize(new Dimension(1400,525));
		this.owner = this;

		add(gui,BorderLayout.CENTER);
	}

	public void process( String outputFileName ) {
		calibrator.reset();

		int N = leftImages.size();

		for( int i = 0; i < N; i++ ) {
			final BufferedImage leftOrig = media.openImage(leftImages.get(i));
			final BufferedImage rightOrig = media.openImage(rightImages.get(i));
			if( leftOrig != null && rightOrig != null ) {
				ImageFloat32 leftInput = ConvertBufferedImage.convertFrom(leftOrig, (ImageFloat32) null);
				ImageFloat32 rightInput = ConvertBufferedImage.convertFrom(rightOrig, (ImageFloat32) null);
				if( calibrator.addPair(leftInput,rightInput ) ) {
					gui.addPair("Image " + i, leftOrig, rightOrig);
					gui.repaint();
				}
			}
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setObservations(calibrator.getCalibLeft().getObservations(),calibrator.getCalibLeft().getErrors(),
						calibrator.getCalibRight().getObservations(),calibrator.getCalibRight().getErrors());
			}});
		gui.repaint();

		StereoParameters param = calibrator.process();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setObservations(calibrator.getCalibLeft().getObservations(),calibrator.getCalibLeft().getErrors(),
						calibrator.getCalibRight().getObservations(),calibrator.getCalibRight().getErrors());
			}});
		gui.repaint();

		param.print();

//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
//				monitor.setMessage(1,"Estimating Parameters");
//			}});
//
//		IntrinsicParameters param = calibrator.process();
//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
//				gui.setResults(calibrator.getErrors());
//				gui.setCalibration(calibrator.getFound());
//			}});
//		monitor.stopThread();
//
//		if( outputFileName != null )
//			BoofMiscOps.saveXML(param, outputFileName);
//
//		// tell it how to undistort the image
//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
//				ParametersZhang99 found = calibrator.getFound();
//
//				tran.set(found.a,found.b,found.c,found.x0,found.y0,found.distortion);
//				gui.setCorrection(dist);
//
//				gui.repaint();
//			}});
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
	 * @param leftImages Images taken by left camera.
	 * @param rightImages Images taken by right camera.
	 */
	public void configure( PlanarCalibrationDetector detector ,
						   PlanarCalibrationTarget target,
						   List<String> leftImages , List<String> rightImages  ) {

		if( leftImages.size() != rightImages.size() )
			throw new IllegalArgumentException("Number of left and right images must be the same");

		calibrator = new CalibrateStereoPlanar(detector,true);
		calibrator.configure(target,true,2);
		this.leftImages = leftImages;
		this.rightImages = rightImages;
	}

	@Override
	public void loadConfigurationFile(String fileName) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void loadInputData(String fileName) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean getHasProcessedImage() {
		return true;
	}

	public static void main( String args[] ) {
//		PlanarCalibrationDetector detector = new WrapPlanarGridTarget(3,4);
		PlanarCalibrationDetector detector = new WrapPlanarChessTarget(3,4,6);

//		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridSquare(3,4,30,30);
		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridChess(3, 4, 30);

		String directory = "../data/evaluation/calibration/stereo/Bumblebee2_Chess";
//		String directory = "../data/evaluation/calibration/stereo/Bumblebee2_Square";

		List<String> leftImages = CalibrateMonoPlanarApp.directoryList(directory, "left");
		List<String> rightImages = CalibrateMonoPlanarApp.directoryList(directory, "right");

		CalibrateStereoPlanarGuiApp app = new CalibrateStereoPlanarGuiApp();
		app.configure(detector,target, leftImages,rightImages);

		JFrame frame = new JFrame("Planar Stereo Calibration");
		frame.add(app, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);

		app.process("intrinsic.xml");

	}
}
