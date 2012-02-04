/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.alg.feature.detect.calibgrid.*;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * @author Peter Abeles
 */
// TODO show original corners
// TODO zoom in and out
// TODO Show sub-pixel corners
public class DebugSubpixelTargetApp <T extends ImageSingleBand>
		extends SelectImagePanel implements ProcessInput , SubpixelCalibControlPanel.Listener
{
	Class<T> imageType;
	// detects the calibration target
	DetectCalibrationTarget<T> detectAlg;

	RefineCalibrationGridCorner<T> refineAlg;

	// gray scale image that targets are detected inside of
	T gray;

	JScrollPane scroll;
	SubpixelGridTargetDisplay<T> display;
	SubpixelCalibControlPanel control;

	java.util.List<Point2D_I32> crudePoints;
	java.util.List<Point2D_F32> refinedPoints = new ArrayList<Point2D_F32>();

	// has an image been processed
	boolean processedImage = false;

	public DebugSubpixelTargetApp(Class<T> imageType) {
		this.imageType = imageType;
		detectAlg = new DetectCalibrationTarget<T>(imageType,500,4,3);
		gray = GeneralizedImageOps.createSingleBand(imageType,1,1);

		refineAlg = new WrapCornerIntensity<T,ImageSingleBand>(10,imageType);

		// construct the GUI
		JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout());

		display = new SubpixelGridTargetDisplay<T>(imageType);
		control = new SubpixelCalibControlPanel(this);

		scroll = new JScrollPane(display);
//		scroll.setHorizontalScrollBarPolicy();

		panel.add(scroll,BorderLayout.CENTER);
		panel.add(control,BorderLayout.WEST);

		setMainGUI(panel);
	}

	public void process( BufferedImage image ) {
		gray.reshape(image.getWidth(),image.getHeight());
		ConvertBufferedImage.convertFrom(image, gray);

		if( !detectAlg.process(gray,control.getThresholdLevel()) ) {
			System.out.println("Detect Target Failed!");
		} else {
			// TODO Clean up
			crudePoints = detectAlg.getCalibrationPoints();
			refinedPoints.clear();
			for( int i = 0; i < crudePoints.size(); i++ ) {
				refinedPoints.add( new Point2D_F32());
			}
			refineAlg.refine(crudePoints,4,3,gray,refinedPoints);

			display.setRefinedPoints(refinedPoints);
			display.setCrudePoints(detectAlg.getCalibrationPoints());
		}


		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				display.setImage(gray);
				display.setPreferredSize(new Dimension(gray.width,gray.height));

				processedImage = true;
			}
		});
	}
	
	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	@Override
	public void changeImage(String name, int index) {
		ImageListManager manager = getInputManager();

		BufferedImage image = manager.loadImage(index);
		if (image != null) {
			process(image);
		}
	}

	/**
	 * centers the image view around the specified point.  ONLY CALL FROM A SWING THREAD!
	 * 
	 * @param cx Pixel coordinate at original scale
	 * @param cy Pixel coordinate at original scale
	 */
	private void centerView( double cx , double cy ) {
		double scale = display.getScale();

		Rectangle r = display.getVisibleRect();
		int x = (int)(cx*scale-r.width/2);
		int y = (int)(cy*scale-r.height/2);

		scroll.getHorizontalScrollBar().setValue(x);
		scroll.getVerticalScrollBar().setValue(y);
	}

	public static void main(String args[]) {

		DebugSubpixelTargetApp<ImageFloat32> app
				= new DebugSubpixelTargetApp<ImageFloat32>(ImageFloat32.class);
//		DebugSubpixelTargetApp<ImageUInt8> app
//				= new DebugSubpixelTargetApp<ImageUInt8>(ImageUInt8.class);

		ImageListManager manager = new ImageListManager();
		manager.add("View 01","../data/evaluation/calibration/hp_dm1/img01.jpg");
		manager.add("View 02","../data/evaluation/calibration/hp_dm1/img02.jpg");
		manager.add("View 03","../data/evaluation/calibration/hp_dm1/img03.jpg");
		manager.add("View 04","../data/evaluation/calibration/hp_dm1/img04.jpg");
		manager.add("View 05","../data/evaluation/calibration/hp_dm1/img05.jpg");
		manager.add("View 06","../data/evaluation/calibration/hp_dm1/img06.jpg");
		manager.add("View 07","../data/evaluation/calibration/hp_dm1/img07.jpg");
		manager.add("View 08","../data/evaluation/calibration/hp_dm1/img08.jpg");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Calibration Target Subpixel Refinement");
	}

	@Override
	public void changeScale(final double scale) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Point2D_F64 center = display.getCenter();
				display.setScale(scale);
				scroll.getViewport().setView(display);

				// center the view
				centerView(center.x,center.y);

				display.repaint();
			}});
	}
}
