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

import boofcv.alg.feature.detect.grid.AutoThresholdCalibrationGrid;
import boofcv.alg.feature.detect.grid.DetectSquareCalibrationPoints;
import boofcv.alg.feature.detect.grid.UtilCalibrationGrid;
import boofcv.alg.feature.detect.quadblob.QuadBlob;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectImagePanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects and displays detected calibration grids.  Shows intermediate steps.
 *
 * @author Peter Abeles
 */
public class DetectCalibrationSquaresApp
		extends SelectImagePanel implements ProcessInput , GridCalibPanel.Listener
{
	int targetColumns;
	int targetRows;

	// detects the calibration target
	DetectSquareCalibrationPoints alg = new DetectSquareCalibrationPoints(500,targetColumns,targetRows);

	// gray scale image that targets are detected inside of
	ImageFloat32 gray = new ImageFloat32(1,1);
	// binary image
	ImageUInt8 binary = new ImageUInt8(1,1);

	// GUI components
	GridCalibPanel calibGUI;
	ImageZoomPanel gui = new ImageZoomPanel();

	// has an image been processed
	boolean processedImage = false;

	// work buffer that stuff is drawn inside of and displayed
	BufferedImage workImage;
	// original untainted image
	BufferedImage input;

	// used to automatically select the threshold
	AutoThresholdCalibrationGrid auto = new AutoThresholdCalibrationGrid(255,20);

	// if a target was found or not
	boolean foundTarget;

	public DetectCalibrationSquaresApp( int numCols , int numRows ) {
		this.targetColumns = numCols;
		this.targetRows = numRows;

		JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout());
		
		calibGUI = new GridCalibPanel(true);
		calibGUI.setListener( this );
		calibGUI.setMinimumSize(calibGUI.getPreferredSize());

		panel.add(calibGUI,BorderLayout.WEST);
		panel.add(gui,BorderLayout.CENTER);
		
		setMainGUI(panel);
	}

	public synchronized void process( final BufferedImage input ) {
		this.input = input;
		workImage = new BufferedImage(input.getWidth(),input.getHeight(),BufferedImage.TYPE_INT_RGB);
		gray.reshape(input.getWidth(),input.getHeight());
		binary.reshape(gray.width,gray.height);
		ConvertBufferedImage.convertFrom(input,gray);

		detectTarget();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setPreferredSize(new Dimension(input.getWidth(), input.getHeight()));
				renderOutput();
			}
		});
	}

	private synchronized void renderOutput() {

		switch( calibGUI.getSelectedView() ) {
			case 0:
				workImage.createGraphics().drawImage(input,null,null);
				break;

			case 1:
				VisualizeBinaryData.renderBinary(binary, workImage);
				break;

			case 2:
				int numLabels = alg.getNumberOfLabels();
				VisualizeBinaryData.renderLabeled(alg.getBlobs(),numLabels,workImage);
				break;

			default:
				throw new RuntimeException("Unknown mode");
		}
		Graphics2D g2 = workImage.createGraphics();
		if( foundTarget ) {
			List<Point2D_I32> targetBounds = alg.getTargetQuadrilateral();
			List<QuadBlob> squares = alg.getSquaresOrdered();
			List<Point2D_I32> targetPoints = new ArrayList<Point2D_I32>();

			UtilCalibrationGrid.extractOrderedPoints(squares,targetPoints,targetColumns);

			if( calibGUI.isShowBound())
				drawBounds(g2,targetBounds);
			if( calibGUI.isShowPoints())
				drawPoints(g2, targetPoints);
			if( calibGUI.isShowNumbers())
				drawNumbers(g2, targetPoints);
			if( calibGUI.isShowGraph())
				drawGraph(g2, alg.getSquaresOrdered());

			calibGUI.setSuccessMessage("FOUND",true);
		} else {
			drawSquareCorners(g2,alg.getSquaresUnordered(),Color.RED);
			drawSquareCorners(g2,alg.getSquaresBad(),Color.BLUE);

			calibGUI.setSuccessMessage("FAILED", false);
		}
		
		gui.setBufferedImage(workImage);
		gui.setScale(calibGUI.getScale());
		gui.repaint();
		
		processedImage = true;
	}

	public static void drawBounds( Graphics2D g2 , java.util.List<Point2D_I32> corners ) {
		Point2D_I32 c0 = corners.get(0);
		Point2D_I32 c1 = corners.get(1);
		Point2D_I32 c2 = corners.get(2);
		Point2D_I32 c3 = corners.get(3);

		g2.setColor(Color.BLUE);
		g2.setStroke(new BasicStroke(2.0f));
		g2.drawLine(c0.x,c0.y,c1.x,c1.y);
		g2.drawLine(c1.x,c1.y,c2.x,c2.y);
		g2.drawLine(c2.x,c2.y,c3.x,c3.y);
		g2.drawLine(c3.x,c3.y,c0.x,c0.y);
	}

	private void drawPoints( Graphics2D g2 , java.util.List<Point2D_I32> foundTarget) {
		g2.setStroke(new BasicStroke(1.0f));
		for( int i = 0; i < foundTarget.size(); i++ ) {
			Point2D_I32 p = foundTarget.get(i);
			VisualizeFeatures.drawPoint(g2, p.x, p.y, 2 , Color.RED);
		}
	}

	public static void drawGraph(Graphics2D g2, List<QuadBlob> squares) {

		g2.setStroke(new BasicStroke(2.0f));
		for( int i = 0; i < squares.size(); i++ ) {
			QuadBlob p = squares.get(i);
			Point2D_I32 c = p.center;

			g2.setColor(Color.ORANGE);
			for( QuadBlob w : p.conn ) {
				g2.drawLine(c.x,c.y,w.center.x,w.center.y);
			}
		}
		for( int i = 0; i < squares.size(); i++ ) {
			QuadBlob p = squares.get(i);
			Point2D_I32 c = p.center;
			VisualizeFeatures.drawPoint(g2, c.x, c.y, Color.GREEN );
		}
	}

	/**
	 * Draw the number assigned to each corner point with a bold outline
	 */
	public static void drawNumbers( Graphics2D g2 , java.util.List<Point2D_I32> foundTarget ) {

		Font regular = new Font("Serif", Font.PLAIN, 16);
		g2.setFont(regular);

		AffineTransform origTran = g2.getTransform();
		for( int i = 0; i < foundTarget.size(); i++ ) {
			Point2D_I32 p = foundTarget.get(i);
			String text = String.format("%2d",i);

			g2.setColor(Color.BLACK);
			g2.drawString(text,p.x-1,p.y);
			g2.drawString(text,p.x+1,p.y);
			g2.drawString(text,p.x,p.y-1);
			g2.drawString(text,p.x,p.y+1);
			g2.setTransform(origTran);
			g2.setColor(Color.GREEN);
			g2.drawString(text,p.x,p.y);
		}
	}

	private void drawSquareCorners( Graphics2D g2 , java.util.List<QuadBlob> squares , Color color ) {

		for( QuadBlob s : squares ) {
			for( Point2D_I32 c : s.corners ) {
				VisualizeFeatures.drawPoint(g2, c.x, c.y, 2 , color);
			}
		}
	}

	@Override
	public void changeImage(String name, int index) {
		ImageListManager manager = getInputManager();

		BufferedImage image = manager.loadImage(index);
		if (image != null) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	@Override
	public void calibEventGUI() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				renderOutput();
			}
		});
	}

	@Override
	public synchronized void calibEventProcess() {
		detectTarget();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setPreferredSize(new Dimension(input.getWidth(), input.getHeight()));
				renderOutput();
			}
		});
	}

	/**
	 * Process the gray scale image.  Use a manually selected threshold or
	 */
	private void detectTarget() {

		if( calibGUI.isManual() ) {
			GThresholdImageOps.threshold(gray,binary,calibGUI.getThresholdLevel(),true);

			foundTarget = alg.process(binary);
		} else {
			foundTarget = auto.process(alg,gray);
			calibGUI.setThreshold((int)auto.getThreshold());
			binary.setTo(auto.getBinary());
		}

	}

	public static void main(String args[]) {

		DetectCalibrationSquaresApp app = new DetectCalibrationSquaresApp(4,3);

		String prefix = "../data/evaluation/calibration/mono/Sony_DSC-HX5V_Square/";

		ImageListManager manager = new ImageListManager();
		manager.add("foo","../CalibIm1.jpg");
		manager.add("View 01",prefix+"frame01.jpg");
		manager.add("View 02",prefix+"frame02.jpg");
		manager.add("View 03",prefix+"frame03.jpg");
		manager.add("View 04",prefix+"frame04.jpg");
		manager.add("View 05",prefix+"frame05.jpg");
		manager.add("View 06",prefix+"frame06.jpg");
		manager.add("View 07",prefix+"frame07.jpg");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Calibration Target Detection");
	}
}
