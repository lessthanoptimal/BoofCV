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

import boofcv.abst.calib.ConfigChessboard;
import boofcv.alg.feature.detect.grid.DetectSquareCalibrationPoints;
import boofcv.alg.feature.detect.quadblob.QuadBlob;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.SelectInputPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.SimpleStringNumberReader;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects and displays detected calibration grids.  Shows intermediate steps.
 *
 * @author Peter Abeles
 */
public class DetectCalibrationSquaresApp
		extends SelectInputPanel implements GridCalibPanel.Listener
{
	int targetColumns;
	int targetRows;

	// detects the calibration target
	DetectSquareCalibrationPoints alg;

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

	// if a target was found or not
	boolean foundTarget;

	public DetectCalibrationSquaresApp() {
		JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout());
		
		calibGUI = new GridCalibPanel(true);
		calibGUI.setListener( this );
		calibGUI.setMinimumSize(calibGUI.getPreferredSize());

		panel.add(calibGUI,BorderLayout.WEST);
		panel.add(gui,BorderLayout.CENTER);
		
		setMainGUI(panel);
	}

	public void configure( int numCols , int numRows ) {
		this.targetColumns = numCols;
		this.targetRows = numRows;

		alg = new DetectSquareCalibrationPoints(1.0,1.0,targetColumns,targetRows);
	}


	@Override
	public void loadConfigurationFile( String configName ) {
		Reader r = media.openFile(configName);
		
		SimpleStringNumberReader reader = new SimpleStringNumberReader('#');
		if( !reader.read(r) )
			throw new RuntimeException("Parsing configuration failed");

		if( reader.remainingTokens() != 7)
			throw new RuntimeException("Unexpected number of tokens in config file: "+reader.remainingTokens());

		if( !(reader.nextString().compareToIgnoreCase("square") == 0)) {
			throw new RuntimeException("Not a square grid config file");
		}

		boolean zeroSkew = reader.nextString().compareTo("true") == 0;
		boolean flipY = reader.nextString().compareTo("true") == 0;

		int numCols = (int)reader.nextDouble();
		int numRows = (int)reader.nextDouble();

		configure(numCols,numRows);
	}

	public synchronized void process( final BufferedImage input ) {
		this.input = input;
		workImage = new BufferedImage(input.getWidth(),input.getHeight(),BufferedImage.TYPE_INT_RGB);
		gray.reshape(input.getWidth(),input.getHeight());
		binary.reshape(gray.width,gray.height);
		ConvertBufferedImage.convertFrom(input,gray);

		doRefreshAll();
	}

	@Override
	public void refreshAll( Object[] cookies ) {
		super.refreshAll(cookies);

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
				VisualizeBinaryData.renderLabeledBG(alg.getBlobs(), numLabels, workImage);
				break;

			default:
				throw new RuntimeException("Unknown mode");
		}
		Graphics2D g2 = workImage.createGraphics();
		List<Point2D_F64> targetBounds = alg.getTargetQuadrilateral();

		List<Point2D_F64> targetPoints = alg.getInterestPoints();
		if( calibGUI.isShowPoints())
			drawPoints(g2, targetPoints);

		if( foundTarget ) {
			if( calibGUI.isShowNumbers())
				drawNumbers(g2, targetPoints);
			if( calibGUI.isShowGraph())
				drawGraph(g2, alg.getInterestSquares());
			if( calibGUI.isShowBound())
				drawBounds(g2,targetBounds);
			calibGUI.setSuccessMessage("FOUND",true);
		} else {
			drawSquareCorners(g2,alg.getSquaresBad(),Color.RED);
			drawSquareCorners(g2,alg.getInterestSquares(),Color.BLUE);

			if( calibGUI.isShowGraph())
				drawGraph(g2, alg.getInterestSquares());

			calibGUI.setSuccessMessage("FAILED", false);
		}
		
		gui.setBufferedImage(workImage);
		gui.setScale(calibGUI.getScale());
		gui.repaint();
		
		processedImage = true;
	}

	public static void drawBounds( Graphics2D g2 , java.util.List<Point2D_F64> corners ) {
		Point2D_F64 c0 = corners.get(0);
		Point2D_F64 c1 = corners.get(1);
		Point2D_F64 c2 = corners.get(2);
		Point2D_F64 c3 = corners.get(3);

		g2.setColor(Color.BLUE);
		g2.setStroke(new BasicStroke(2.0f));
		g2.drawLine((int) c0.x, (int) c0.y, (int) c1.x, (int) c1.y);
		g2.drawLine((int) c1.x, (int) c1.y, (int) c2.x, (int) c2.y);
		g2.drawLine((int) c2.x, (int) c2.y, (int) c3.x, (int) c3.y);
		g2.drawLine((int) c3.x, (int) c3.y, (int) c0.x, (int) c0.y);
	}

	private void drawPoints( Graphics2D g2 , java.util.List<Point2D_F64> foundTarget) {
		g2.setStroke(new BasicStroke(1.0f));
		for( int i = 0; i < foundTarget.size(); i++ ) {
			Point2D_F64 p = foundTarget.get(i);
			VisualizeFeatures.drawPoint(g2, (int)p.x, (int)p.y, 2 , Color.RED);
		}
	}

	public static void drawGraph(Graphics2D g2, List<QuadBlob> squares) {

		if( squares.size() == 0)
			return;

		g2.setStroke(new BasicStroke(2.0f));
		for( int i = 0; i < squares.size(); i++ ) {
			QuadBlob p = squares.get(i);
			Point2D_I32 c = p.center;

			int red = 255;
			int green = 255*i/squares.size();
			int blue = 255*(i%(squares.size()/2))/(squares.size()/2);

			g2.setColor(new Color(red,green,blue));
			for( QuadBlob w : p.conn ) {
				g2.drawLine(c.x,c.y,w.center.x,w.center.y);
			}
		}
		g2.setColor(Color.RED);
		for( int i = 0; i < squares.size(); i++ ) {
			QuadBlob p = squares.get(i);
			Point2D_I32 c = p.center;
			g2.drawString(String.format("%d", p.conn.size()), c.x, c.y);
		}

//		for( int i = 0; i < squares.size(); i++ ) {
//			QuadBlob p = squares.get(i);
//			for( int j = 0; j < p.corners.size(); j++ ) {
//				Point2D_I32 c = p.corners.get(j);
//				VisualizeFeatures.drawPoint(g2, c.x, c.y, 1, Color.BLUE );
//			}
//		}
	}

	public static void drawBlobNumbers(Graphics2D g2, List<QuadBlob> squares) {

		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(2.0f));
		for( int i = 0; i < squares.size(); i++ ) {
			QuadBlob p = squares.get(i);
			Point2D_I32 c = p.center;
			g2.drawString(String.format("%d", p.index), c.x, c.y);

		}
	}

	/**
	 * Draw the number assigned to each corner point with a bold outline
	 */
	public static void drawNumbers( Graphics2D g2 , java.util.List<Point2D_F64> foundTarget ) {

		Font regular = new Font("Serif", Font.PLAIN, 16);
		g2.setFont(regular);

		AffineTransform origTran = g2.getTransform();
		for( int i = 0; i < foundTarget.size(); i++ ) {
			Point2D_F64 p = foundTarget.get(i);
			String text = String.format("%2d",i);

			int x = (int)p.x;
			int y = (int)p.y;

			g2.setColor(Color.BLACK);
			g2.drawString(text,x-1,y);
			g2.drawString(text,x+1,y);
			g2.drawString(text,x,y-1);
			g2.drawString(text,x,y+1);
			g2.setTransform(origTran);
			g2.setColor(Color.GREEN);
			g2.drawString(text,x,y);
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
	public void changeInput(String name, int index) {

		BufferedImage image = media.openImage(inputRefs.get(index).getPath());
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
		doRefreshAll();
	}

	/**
	 * Process the gray scale image.  Use a manually selected threshold or
	 */
	private void detectTarget() {

		ConfigChessboard config = new ConfigChessboard(1,1);

		if( calibGUI.isManual() ) {
			GThresholdImageOps.threshold(gray,binary,calibGUI.getThresholdLevel(),true);
		} else {
			GThresholdImageOps.adaptiveSquare(gray, binary, config.binaryAdaptiveRadius, config.binaryAdaptiveBias, true, null, null);
		}
		foundTarget = alg.process(binary);

	}

	public static void main(String args[]) throws FileNotFoundException {

		DetectCalibrationSquaresApp app = new DetectCalibrationSquaresApp();

//		String prefix = "../data/applet/calibration/mono/Sony_DSC-HX5V_Square/";
//		String prefix = "../data/evaluation/calibration/stereo/Bumblebee2_Square/";
		String prefix = "../data/evaluation/calibration/mono/PULNiX_CCD_6mm_Zhang/";

		app.loadConfigurationFile(prefix + "info.txt");

		List<PathLabel> inputs = new ArrayList<PathLabel>();

		for( int i = 1; i < 6; i++ ) {
			String name = String.format("View %02d",i);
//			String fileName = String.format("frame%02d.jpg",i);
//			String fileName = String.format("left%02d.jpg",i);
//			String fileName = String.format("right%02d.jpg",i);
			String fileName = String.format("CalibIm%d.gif",i);
			inputs.add(new PathLabel(name, prefix + fileName));
		}
//		inputs.add(new PathLabel("View 01",prefix+"right02.jpg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Calibration Target Detection");
	}
}
