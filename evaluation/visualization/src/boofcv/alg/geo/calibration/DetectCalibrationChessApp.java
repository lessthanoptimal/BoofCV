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
import boofcv.alg.feature.detect.chess.DetectChessCalibrationPoints;
import boofcv.alg.feature.detect.quadblob.DetectQuadBlobsBinary;
import boofcv.alg.feature.detect.quadblob.QuadBlob;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.SelectInputPanel;
import boofcv.gui.VisualizeApp;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.SimpleStringNumberReader;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class DetectCalibrationChessApp<T extends ImageSingleBand, D extends ImageSingleBand>
		extends SelectInputPanel implements VisualizeApp, GridCalibPanel.Listener

{
	Class<T> imageType;
	DetectChessCalibrationPoints<T,D> alg;

	GridCalibPanel calibGUI;
	ImageZoomPanel gui = new ImageZoomPanel();

	// work buffer that stuff is drawn inside of and displayed
	BufferedImage workImage;
	// original untainted image
	BufferedImage input;
	// gray scale image that targets are detected inside of
	T gray;
	// feature intensity image
	ImageFloat32 intensity = new ImageFloat32(1,1);

	// if a target was found or not
	boolean foundTarget;

	boolean processed = false;

	public DetectCalibrationChessApp(Class<T> imageType) {
		this.imageType = imageType;

		gray = GeneralizedImageOps.createSingleBand(imageType,1,1);

		JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout());

		calibGUI = new GridCalibPanel(true);
		calibGUI.addView("Feature");
		calibGUI.setListener( this );
		calibGUI.setMinimumSize(calibGUI.getPreferredSize());

		panel.add(gui,BorderLayout.CENTER);
		panel.add(calibGUI,BorderLayout.WEST);

		setMainGUI(panel);
	}

	public void configure( int numCols , int numRows ) {
		ConfigChessboard config = new ConfigChessboard(numCols,numRows);

		alg = new DetectChessCalibrationPoints<T,D>(numCols,numRows,5,2,imageType);

		alg.setUserBinaryThreshold(config.binaryGlobalThreshold);
		alg.setUserAdaptiveBias(config.binaryAdaptiveBias);
		alg.setUserAdaptiveRadius(config.binaryAdaptiveRadius);
	}

	@Override
	public void loadConfigurationFile(String fileName) {
		Reader r = media.openFile(fileName);

		SimpleStringNumberReader reader = new SimpleStringNumberReader('#');
		if( !reader.read(r) )
			throw new RuntimeException("Parsing configuration failed");

		if( reader.remainingTokens() != 6) {
			while( reader.remainingTokens() != 0 ) {
				System.out.println("token: "+reader.nextString());
			}
			throw new RuntimeException("Unexpected number of tokens in config file: "+reader.remainingTokens());
		}

		if( !(reader.nextString().compareToIgnoreCase("chess") == 0)) {
			throw new RuntimeException("Not a chessboard config file");
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
		ConvertBufferedImage.convertFrom(input, gray, true);

		intensity.reshape(gray.width,gray.height);

		detectTarget();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setPreferredSize(new Dimension(input.getWidth(), input.getHeight()));
				renderOutput();
			}
		});
	}

	private synchronized void detectTarget() {
		if( calibGUI.isManual())
			alg.setUserBinaryThreshold(calibGUI.getThresholdLevel());
		else
			alg.setUserBinaryThreshold(-1);

		foundTarget = alg.process(gray);
	}

	private synchronized void renderOutput() {
		switch( calibGUI.getSelectedView() ) {
			case 0:
				workImage.createGraphics().drawImage(input,null,null);
				break;

			case 1:
				VisualizeBinaryData.renderBinary(alg.getBinary(), workImage);
				break;

			case 2:
				renderClusters();
				break;

			case 3:
				alg.renderIntensity(intensity);
				float max = ImageStatistics.maxAbs(intensity);
				VisualizeImageData.colorizeSign(intensity,workImage,max);
				break;

			default:
				throw new RuntimeException("Unknown mode");
		}
		Graphics2D g2 = workImage.createGraphics();
		if( foundTarget ) {
			if( calibGUI.isShowBound() ) {
				Polygon2D_I32 bounds =  alg.getFindBound().getBoundPolygon();
				drawBounds(g2, bounds);
			}

			if( calibGUI.isShowNumbers() ) {
				drawNumbers(g2, alg.getPoints(),1);
			}
			calibGUI.setSuccessMessage("FOUND",true);
		} else {
			if( calibGUI.isShowBound() ) {
				Polygon2D_I32 bounds =  alg.getFindBound().getBoundPolygon();
				drawBounds(g2, bounds);
			}

			calibGUI.setSuccessMessage("FAILED",false);
		}

		if( calibGUI.isShowPoints() ) {
			List<Point2D_F64> candidates =  alg.getPoints();
			for( Point2D_F64 c : candidates ) {
				VisualizeFeatures.drawPoint(g2, (int)(c.x+0.5), (int)(c.y+0.5), 1, Color.RED);
			}
		}

		if( calibGUI.doShowGraph ) {

			List<QuadBlob> graph = alg.getFindBound().getGraphBlobs();
			if( graph != null )
				DetectCalibrationSquaresApp.drawGraph(g2,graph);
		}

		gui.setBufferedImage(workImage);
		gui.setScale(calibGUI.getScale());
		gui.repaint();

		processed = true;
	}

	public static void drawBounds( Graphics2D g2 , Polygon2D_I32 bounds) {
		if( bounds.size() <= 0 )
			return;

		g2.setColor(Color.BLUE);
		g2.setStroke(new BasicStroke(2.0f));
		for( int i = 1; i < bounds.vertexes.size(); i++ ) {
			Point2D_I32 p0 = bounds.vertexes.get(i-1);
			Point2D_I32 p1 = bounds.vertexes.get(i);

			g2.drawLine(p0.x,p0.y,p1.x,p1.y);
		}
		Point2D_I32 p0 = bounds.vertexes.get(bounds.size()-1);
		Point2D_I32 p1 = bounds.vertexes.get(0);
		g2.drawLine(p0.x,p0.y,p1.x,p1.y);

	}

	private void renderClusters() {
		DetectQuadBlobsBinary detectBlobs = alg.getFindBound().getDetectBlobs();

		int numLabels = detectBlobs.getNumLabels();
		VisualizeBinaryData.renderLabeledBG(detectBlobs.getLabeledImage(), numLabels, workImage);

		// put a mark in the center of blobs that were declared as being valid
		Graphics2D g2 = workImage.createGraphics();
		if( detectBlobs.getDetected() != null ) {
			for( QuadBlob b : detectBlobs.getDetected() ) {
				for( int i = 0; i < b.corners.size(); i++ ) {
					Point2D_I32 c = b.corners.get(i);
					VisualizeFeatures.drawPoint(g2, c.x, c.y, 1, Color.GREEN);
				}
			}
		}
	}

	public static void drawNumbers( Graphics2D g2 , java.util.List<Point2D_F64> foundTarget , double scale ) {

		Font regular = new Font("Serif", Font.PLAIN, 16);
		g2.setFont(regular);

		AffineTransform origTran = g2.getTransform();
		for( int i = 0; i < foundTarget.size(); i++ ) {
			Point2D_F64 p = foundTarget.get(i);
			String text = String.format("%2d",i);

			int x = (int)(p.x*scale);
			int y = (int)(p.y*scale);

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

	@Override
	public boolean getHasProcessedImage() {
		return processed;
	}

	@Override
	public void changeInput(String name, int index) {

		BufferedImage image = media.openImage(inputRefs.get(index).getPath());
		if (image != null) {
			process(image);
		}
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
	public void calibEventProcess() {
		detectTarget();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setPreferredSize(new Dimension(input.getWidth(), input.getHeight()));
				renderOutput();
			}
		});
	}

	public static void main(String args[]) throws FileNotFoundException {

		DetectCalibrationChessApp app = new DetectCalibrationChessApp(ImageFloat32.class);

		String prefix = "../data/applet/calibration/mono/Sony_DSC-HX5V_Chess/";

		app.loadConfigurationFile(prefix + "info.txt");

//		app.setBaseDirectory(prefix);
//		app.loadInputData(prefix+"images.txt");

		List<PathLabel> inputs = new ArrayList<PathLabel>();

		inputs.add(new PathLabel("View 01",prefix+"frame01.jpg"));
		inputs.add(new PathLabel("View 02",prefix+"frame02.jpg"));
		inputs.add(new PathLabel("View 03",prefix+"frame03.jpg"));
		inputs.add(new PathLabel("View 04",prefix+"frame04.jpg"));
		inputs.add(new PathLabel("View 05",prefix+"frame05.jpg"));
		inputs.add(new PathLabel("View 06",prefix+"frame06.jpg"));
		inputs.add(new PathLabel("View 07",prefix+"frame07.jpg"));
		inputs.add(new PathLabel("View 08",prefix+"frame08.jpg"));
		inputs.add(new PathLabel("View 11",prefix+"frame11.jpg"));
		inputs.add(new PathLabel("View 12",prefix+"frame12.jpg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Calibration Target Detection");
	}
}
