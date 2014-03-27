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

import boofcv.alg.feature.detect.grid.DetectSquareCalibrationPoints;
import boofcv.alg.feature.detect.grid.RefineCalibrationGridCorner;
import boofcv.alg.feature.detect.grid.refine.WrapRefineCornerCanny;
import boofcv.alg.feature.detect.quadblob.QuadBlob;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.SelectInputPanel;
import boofcv.gui.VisualizeApp;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Application for debugging sub-pixel algorithm of square corner calibration grids.  Displays
 * the initial "crude" corner location estimate along with the refined.  Zooming in and out
 * of the image allows a better view of the difference between the two corner estimates.
 *
 * @author Peter Abeles
 */
public class DebugSquaresSubpixelApp
		extends SelectInputPanel implements VisualizeApp, SubpixelCalibControlPanel.Listener
{
	// target size
	int targetColumns;
	int targetRows;

	// detects the calibration target
	DetectSquareCalibrationPoints detectAlg;

	// refines the initial corner estimate
	RefineCalibrationGridCorner refineAlg;

	// gray scale image that targets are detected inside of
	ImageFloat32 gray = new ImageFloat32(1,1);
	ImageUInt8 binary = new ImageUInt8(1,1);

	// GUI related classes
	JScrollPane scroll;
	SubpixelGridTargetDisplay<ImageFloat32> display;
	SubpixelCalibControlPanel control;

	// has an image been processed
	boolean processedImage = false;

	public DebugSquaresSubpixelApp( int numSquaresCol , int numSquaresRow ) {
		this.targetColumns = numSquaresCol;
		this.targetRows = numSquaresRow;

		detectAlg = new DetectSquareCalibrationPoints(1.0,1.0,targetColumns,targetRows);

//		refineAlg = new WrapCornerIntensity<T,ImageSingleBand>(1,imageType);
//		refineAlg = new WrapRefineCornerSegmentFit();
		refineAlg = new WrapRefineCornerCanny();

		// construct the GUI
		JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout());

		display = new SubpixelGridTargetDisplay<ImageFloat32>(ImageFloat32.class);
		control = new SubpixelCalibControlPanel(this);

		scroll = new JScrollPane(display);

		panel.add(scroll,BorderLayout.CENTER);
		panel.add(control,BorderLayout.WEST);

		setMainGUI(panel);
	}

	public void process( BufferedImage image ) {
		System.out.println("Processing subpixel app");
		gray.reshape(image.getWidth(),image.getHeight());
		binary.reshape(image.getWidth(), image.getHeight());
		ConvertBufferedImage.convertFrom(image,gray);

		List<Point2D_I32> crude = null;
		List<Point2D_F64> refined = null;

		GThresholdImageOps.adaptiveSquare(gray, binary, 50, -10, true,null,null);
		if( !detectAlg.process(binary) ) {
			System.out.println("Detect Target Failed!");
		} else {
			List<QuadBlob> squares = detectAlg.getInterestSquares();
			crude = new ArrayList<Point2D_I32>();
			refined = new ArrayList<Point2D_F64>();

			refineAlg.refine(detectAlg.getInterestSquares(),gray);

			for( QuadBlob b : squares ) {
				for( Point2D_I32 c : b.corners )
					crude.add(c);
				for( Point2D_F64 c : b.subpixel )
					refined.add(c);
			}
		}
		
		final List<Point2D_I32> _crude = crude;
		final List<Point2D_F64> _refined = refined;

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				display.setImage(gray);
				display.setRefinedPoints(_refined);
				display.setCrudePoints(_crude);
				display.setPreferredSize(new Dimension(gray.width,gray.height));
				display.repaint();
				processedImage = true;
			}
		});
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
		return processedImage;
	}

	@Override
	public void changeInput(String name, int index) {

		BufferedImage image = media.openImage(inputRefs.get(index).getPath());
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

	@Override
	public void updateGUI() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Point2D_F64 center = display.getCenter();
				display.setScale(control.getScale());
				scroll.getViewport().setView(display);

				display.setShow( control.isShowPixel(),control.isShowSubpixel());

				// center the view
				centerView(center.x, center.y);

				display.repaint();
			}});
	}


	public static void main(String args[]) {

		DebugSquaresSubpixelApp app = new DebugSquaresSubpixelApp(5,7);

		String prefix = "../data/evaluation/calibration/mono/Sony_DSC-HX5V_Square/";
//		String prefix = "../data/evaluation/calibration/mono/PULNiX_CCD_6mm_Zhang/";
		
		List<PathLabel> images = new ArrayList<PathLabel>();

//		images.add( new PathLabel("View 01",prefix+"CalibIm1.gif"));
//		images.add( new PathLabel("View 02",prefix+"CalibIm2.gif"));
//		images.add( new PathLabel("View 03",prefix+"CalibIm3.gif"));
//		images.add( new PathLabel("View 04",prefix+"CalibIm4.gif"));
//		images.add( new PathLabel("View 05",prefix+"CalibIm5.gif"));

		images.add( new PathLabel("View 01",prefix+"frame01.jpg"));
		images.add( new PathLabel("View 02",prefix+"frame02.jpg"));
		images.add( new PathLabel("View 03",prefix+"frame03.jpg"));
		images.add( new PathLabel("View 04",prefix+"frame04.jpg"));
		images.add( new PathLabel("View 05",prefix+"frame05.jpg"));
		images.add( new PathLabel("View 06",prefix+"frame06.jpg"));
		images.add( new PathLabel("View 07",prefix+"frame07.jpg"));
		images.add( new PathLabel("View 12",prefix+"frame12.jpg"));

		app.setInputList(images);


		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Calibration Target Subpixel Refinement");
	}
}
