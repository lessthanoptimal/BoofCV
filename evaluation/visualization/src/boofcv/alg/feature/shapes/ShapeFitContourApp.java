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

package boofcv.alg.feature.shapes;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.ConnectRule;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.ImageUInt8;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.EllipseRotated_F64;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Fits shapes to contours from binary images
 *
 * @author Peter Abeles
 */
public class ShapeFitContourApp
		extends SelectAlgorithmAndInputPanel implements ChangeListener
{
	// displays intensity image
	ImagePanel gui;

	// converted input image
	ImageUInt8 input = new ImageUInt8(1,1);
	ImageUInt8 binary = new ImageUInt8(1,1);
	ImageUInt8 filtered = new ImageUInt8(1,1);
	// if it has processed an image or not
	boolean processImage = false;

	// rendered output image
	BufferedImage output;

	// Found contours
	List<Contour> contours;

	// parameters for polygon fitting
	int polyAngleTol = 30;
	double polyPixelTol = 2;

	JPanel leftArea;
	StandardAlgConfigPanel controlPolygon = new StandardAlgConfigPanel();

	JSpinner selectAngle;
	JSpinner selectPixel;

	int previousActive=-1;

	public ShapeFitContourApp() {
		super(1);

		addAlgorithm(0, "Polygon", 0);
		addAlgorithm(0, "Ellipse", 1);


		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());

		leftArea = new JPanel();
		leftArea.setLayout(new BorderLayout());
		leftArea.setPreferredSize(new Dimension(150,20));

		selectAngle = new JSpinner(new SpinnerNumberModel(polyAngleTol,5,90,5));
		selectAngle.addChangeListener(this);
		selectAngle.setMaximumSize(selectAngle.getPreferredSize());
		selectPixel = new JSpinner(new SpinnerNumberModel(polyPixelTol,0,10,1));
		selectPixel.addChangeListener(this);
		selectPixel.setMaximumSize(selectPixel.getPreferredSize());

		controlPolygon.addLabeled(selectAngle, "Angle", controlPolygon);
		controlPolygon.addLabeled(selectPixel, "Pixel", controlPolygon);
		controlPolygon.addVerticalGlue(controlPolygon);

		gui = new ImagePanel();

		mainPanel.add(BorderLayout.WEST,leftArea);
		mainPanel.add(BorderLayout.CENTER,gui);
		setMainGUI(mainPanel);
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( contours == null )
			return;

		// display original image
		if( originalCheck.isSelected() ) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					gui.setBufferedImage(inputImage);
					gui.repaint();
					gui.requestFocusInWindow();
				}
			});
			return;
		}

		// display fit results
		Graphics2D g2 = output.createGraphics();
		g2.drawImage(inputImage,0,0,null);
		g2.setStroke(new BasicStroke(3));

		int active = (Integer)cookie;
		if( active == 0 ) {
			double angleTol = UtilAngle.degreeToRadian(polyAngleTol);
			for( Contour c : contours ) {
				List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external,true,polyPixelTol,angleTol,100);

				g2.setColor(Color.RED);
				VisualizeShapes.drawPolygon(vertexes,true,g2);

				g2.setColor(Color.BLUE);
				for( List<Point2D_I32> internal : c.internal ) {
					vertexes = ShapeFittingOps.fitPolygon(internal,true,polyPixelTol,angleTol,100);
					VisualizeShapes.drawPolygon(vertexes,true,g2);
				}
			}
		} else if( active == 1 ) {

			// Filter small contours since they can generate really wacky ellipses
			for( Contour c : contours ) {
				if( c.external.size() > 10) {
					FitData<EllipseRotated_F64> ellipse = ShapeFittingOps.fitEllipse_I32(c.external,0,false,null);

					g2.setColor(Color.RED);
					VisualizeShapes.drawEllipse(ellipse.shape, g2);
				}

				g2.setColor(Color.BLUE);
				for( List<Point2D_I32> internal : c.internal ) {
					if( internal.size() < 10)
						continue;
					FitData<EllipseRotated_F64> ellipse = ShapeFittingOps.fitEllipse_I32(internal, 0, false, null);
					VisualizeShapes.drawEllipse(ellipse.shape, g2);
				}
			}
		}

		if( previousActive != active ) {
			if( active == 0 ) {
				leftArea.add(controlPolygon);
			} else {
				leftArea.remove(controlPolygon);
			}
			leftArea.revalidate();
			leftArea.repaint();
			previousActive = active;
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				gui.setBufferedImage(output);
				gui.repaint();
				gui.requestFocusInWindow();
			}
		});
	}

	public void process( final BufferedImage input ) {
		setInputImage(input);
		this.input.reshape(input.getWidth(),input.getHeight());
		ConvertBufferedImage.convertFromSingle(input, this.input, ImageUInt8.class);
		this.binary.reshape(input.getWidth(), input.getHeight());
		this.filtered.reshape(input.getWidth(),input.getHeight());
		this.output = new BufferedImage( input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);

		// the mean pixel value is often a reasonable threshold when creating a binary image
		double mean = ImageStatistics.mean(this.input);

		// create a binary image by thresholding
		GThresholdImageOps.threshold(this.input, binary, mean, true);

		// reduce noise with some filtering
		BinaryImageOps.erode8(binary, 1, filtered);
		BinaryImageOps.dilate8(filtered, 1, binary);

		// Find the contour around the shapes
		contours = BinaryImageOps.contour(binary, ConnectRule.EIGHT,null);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setPreferredSize(new Dimension(input.getWidth(), input.getHeight()));
				processImage = true;
			}});
		doRefreshAll();
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	@Override
	public void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		if( image != null ) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processImage;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// hijack the show original image event and handle it inside this class
		if( e.getSource() != originalCheck  ) {
			super.actionPerformed(e);
		} else {
			doRefreshAll();
		}
	}

	public static void main( String args[] ) {

		ShapeFitContourApp app = new ShapeFitContourApp();

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();

		inputs.add(new PathLabel("Particles","../data/applet/particles01.jpg"));
		inputs.add(new PathLabel("Shapes","../data/applet/shapes02.png"));
		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Shape Fitting");
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == selectAngle ) {
			polyAngleTol = (Integer)selectAngle.getValue();
		} else if( e.getSource() == selectPixel ) {
			polyPixelTol = (Double)selectPixel.getValue();
		}
		doRefreshAll();
	}
}
