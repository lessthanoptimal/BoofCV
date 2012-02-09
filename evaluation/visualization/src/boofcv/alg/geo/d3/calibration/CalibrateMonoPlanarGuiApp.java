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

package boofcv.alg.geo.d3.calibration;

import boofcv.alg.distort.ApplyRadialTransform;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.geo.calibration.CalibrationGridConfig;
import boofcv.alg.geo.calibration.ParametersZhang98;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.app.CalibrateMonoPlanarApp;
import boofcv.app.CalibrationGridInterface;
import boofcv.app.WrapPlanarGridTarget;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageFloat32;

import javax.swing.*;
import java.awt.*;

/**
 * @author Peter Abeles
 */
public class CalibrateMonoPlanarGuiApp extends JPanel {

	CalibrateMonoPlanarApp calibrator;
	MonoPlanarPanel gui = new MonoPlanarPanel();
	JPanel owner;
	
	boolean processing;

	
	public CalibrateMonoPlanarGuiApp(CalibrateMonoPlanarApp calibrator) {
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(750,525));
		this.calibrator = calibrator;
		this.owner = this;
		
		add(gui,BorderLayout.CENTER);
	}
	
	public void process( String directory ) {
		processing = true;
		calibrator.reset();
		new ProcessThread().start();
		
		calibrator.loadImages(directory);
		gui.setImages(calibrator.getImageNames(),
				calibrator.getImages(),calibrator.getObservations());
		gui.repaint();

		calibrator.process();
		gui.setResults(calibrator.getErrors());

		processing = false;
		
		ParametersZhang98 found = calibrator.getFound();
		
		ApplyRadialTransform tran = new ApplyRadialTransform(found.a,found.b,found.c,found.x0,found.y0,found.distortion);

		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(ImageFloat32.class);
		ImageBorder<ImageFloat32> border = FactoryImageBorder.value(ImageFloat32.class,0);
		ImageDistort<ImageFloat32> dist = DistortSupport.createDistort(ImageFloat32.class,tran,interp,border);

		gui.setCorrection(dist);
		
		gui.repaint();
	}


	/**
	 * Displays a progress monitor and updates its state periodically
	 */
	public class ProcessThread extends Thread
	{
		ProgressMonitor progressMonitor;
		public ProcessThread() {
			progressMonitor = new ProgressMonitor(owner, "Computing Calibration", "", 0, 3);
		}

		@Override
		public void run() {
			while( processing ) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						progressMonitor.setProgress(calibrator.state);
						progressMonitor.setNote(calibrator.message);
					}});
				synchronized ( this ) {
					try {
						wait(100);
					} catch (InterruptedException e) {
					}
				}
			}
			progressMonitor.close();
		}
	}

	public static void main( String args[] ) {
		CalibrationGridInterface detector = new WrapPlanarGridTarget();

		CalibrationGridConfig config = new CalibrationGridConfig(8,6,30);

		CalibrateMonoPlanarApp calibrator = new CalibrateMonoPlanarApp(detector,true);
		calibrator.configure(config,false,2);
		
		CalibrateMonoPlanarGuiApp app = new CalibrateMonoPlanarGuiApp(calibrator);
		

		JFrame frame = new JFrame("Planar Calibration");
		frame.add(app, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
		
		app.process("/home/pja/saved/a");
//		app.process("../data/evaluation/calibration/mono/Sony_DSC-HX5V");

	}
}
