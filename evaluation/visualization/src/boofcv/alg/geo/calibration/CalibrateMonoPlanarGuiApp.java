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

import boofcv.alg.distort.AddRadialDistortionPixel;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.app.CalibrateMonoPlanarApp;
import boofcv.app.ParseCalibrationConfig;
import boofcv.app.PlanarCalibrationDetector;
import boofcv.app.WrapPlanarGridTarget;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.ProcessInput;
import boofcv.io.*;
import boofcv.io.image.UtilImageIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageFloat32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

/**
 * Computes intrinsic camera calibration parameters from a set of calibration images.  Results
 * are displayed in a window allowing their accuracy to be easily seen.
 *
 * @author Peter Abeles
 */
public class CalibrateMonoPlanarGuiApp extends JPanel 
		implements ConfigureFileInterface, MediaManagerInput, ProcessInput {

	// computes calibration parameters
	CalibrateMonoPlanarApp calibrator;
	// displays results
	MonoPlanarPanel gui = new MonoPlanarPanel();
	// needed by ProcessThread for displaying its dialog
	JPanel owner;

	// transform used to undistort image
	AddRadialDistortionPixel tran = new AddRadialDistortionPixel();
	ImageDistort<ImageFloat32> dist;

	List<String> images;
	MediaManager media = DefaultMediaManager.INSTANCE;
	
	public CalibrateMonoPlanarGuiApp() {
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(800,525));
		this.calibrator = calibrator;
		this.owner = this;
		
		add(gui,BorderLayout.CENTER);

		// Distortion algorithm for removing radial distortion
		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(ImageFloat32.class);
		PixelTransform_F32 tran = new PointToPixelTransform_F32(this.tran);
		ImageBorder<ImageFloat32> border = FactoryImageBorder.value(ImageFloat32.class, 0);
		dist = FactoryDistort.distort(interp,border,ImageFloat32.class);
		dist.setModel(tran);
	}

	public void configure( PlanarCalibrationDetector detector ,
						   PlanarCalibrationTarget target,
						   List<String> images  ) {

		calibrator = new CalibrateMonoPlanarApp(detector,true);
		calibrator.configure(target,true,2);
		this.images = images;
	}

	public void configure( PlanarCalibrationDetector detector ,
						   PlanarCalibrationTarget target,
						   String directory ) {

		images = CalibrateMonoPlanarApp.directoryImageList(directory);

		calibrator = new CalibrateMonoPlanarApp(detector,true);
		calibrator.configure(target,true,2);
	}

	@Override
	public void configure(String fileName) {
		ParseCalibrationConfig parser = new ParseCalibrationConfig(media);

		if( parser.parse(fileName) ) {
			configure(parser.detector,parser.target,parser.images);
		} else {
			System.err.println("Configuration failed");
		}
	}
	
	public void process() {
		calibrator.reset();
		ProcessThread monitor = new ProcessThread();
		monitor.start();

		for( int i = 0; i < images.size(); i++ ) {
			final File file = new File(images.get(i));
			final BufferedImage orig = media.openImage(images.get(i));
			if( orig != null ) {
				calibrator.addImage(file.getName(),orig);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						gui.addImage(file.getName(),orig);
						gui.repaint();
					}});
			}
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setObservations(calibrator.getObservations());
			}});
		gui.repaint();

		calibrator.process();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setResults(calibrator.getErrors());
				gui.setCalibration(calibrator.getFound());
			}});
		monitor.stopThread();

		// tell it how to undistort the image
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ParametersZhang98 found = calibrator.getFound();

				tran.set(found.a,found.b,found.c,found.x0,found.y0,found.distortion);
				gui.setCorrection(dist);

				gui.repaint();
			}});
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
			super(new ProgressMonitor(owner, "Computing Calibration", "", 0, 3));
		}

		@Override
		public void doRun() {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					monitor.setProgress(calibrator.state);
					monitor.setNote(calibrator.message);
				}});
		}
	}

	@Override
	public void setInputManager(InputListManager manager) {
		new Thread() {
			public void run() {
				process();
			}
		}.start();
	}

	@Override
	public boolean getHasProcessedImage() {
		return true;
	}

	public static void main( String args[] ) {
//		PlanarCalibrationDetector detector = new WrapPlanarGridTarget(8,8);
		PlanarCalibrationDetector detector = new WrapPlanarGridTarget(3,4);
//		PlanarCalibrationDetector detector = new WrapPlanarChessTarget(3,4);

//		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridSquare(8,8,0.5,7.0/18.0);
		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridSquare(3,4,30,30);
//		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridChess(3, 4, 30);

//		String directory = "../data/evaluation/calibration/mono/Sony_DSC-HX5V_Chess";
		String directory = "../data/evaluation/calibration/mono/Sony_DSC-HX5V_Square";
//		String directory = "../data/evaluation/calibration/mono/Sony_DSC-PULNiX_CCD_6mm_Zhang";

		CalibrateMonoPlanarGuiApp app = new CalibrateMonoPlanarGuiApp();
		app.configure(detector,target,directory);

		JFrame frame = new JFrame("Planar Calibration");
		frame.add(app, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);

		app.process();
	}
}
