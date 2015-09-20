/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.feature.detect.chess.DetectChessboardFiducial;
import boofcv.alg.feature.detect.squares.SquareGrid;
import boofcv.alg.feature.detect.squares.SquareNode;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.gui.SelectInputPanel;
import boofcv.gui.VisualizeApp;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.SimpleStringNumberReader;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class DetectCalibrationChessApp
		extends SelectInputPanel implements VisualizeApp, GridCalibPanel.Listener

{
	DetectChessboardFiducial<ImageFloat32> alg;

	GridCalibPanel calibGUI;
	ImageZoomPanel gui = new ImageZoomPanel();

	// work buffer that stuff is drawn inside of and displayed
	BufferedImage workImage;
	// original untainted image
	BufferedImage input;
	// gray scale image that targets are detected inside of
	ImageFloat32 gray;

	// if a target was found or not
	boolean foundTarget;

	boolean processed = false;

	public DetectCalibrationChessApp() {

		gray = new ImageFloat32(1,1);

		JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout());

		calibGUI = new GridCalibPanel(true);
		calibGUI.setListener(this);
		calibGUI.setMinimumSize(calibGUI.getPreferredSize());

		panel.add(gui, BorderLayout.CENTER);
		panel.add(calibGUI, BorderLayout.WEST);

		gui.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				System.out.println("clicked at " + e.getX() + " " + e.getY());
			}
		});

		setMainGUI(panel);
	}

	public void configure( int numCols , int numRows ) {
		ConfigChessboard config = new ConfigChessboard(numCols,numRows,30);

		alg = FactoryPlanarCalibrationTarget.detectorChessboard(config).getAlg();

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

		if( reader.remainingTokens() != 7) {
			while( reader.remainingTokens() != 0 ) {
				System.out.println("token: "+reader.nextString());
			}
			throw new RuntimeException("Unexpected number of tokens in config file: "+reader.remainingTokens());
		}

		if( !(reader.nextString().compareToIgnoreCase("chess") == 0)) {
			throw new RuntimeException("Not a chessboard config file");
		}

		int numRadial = (int)reader.nextDouble();
		boolean includeTangential = reader.nextString().compareTo("true") == 0;
		boolean zeroSkew = reader.nextString().compareTo("true") == 0;

		int numCols = (int)reader.nextDouble();
		int numRows = (int)reader.nextDouble();

		configure(numCols, numRows);
	}

	public synchronized void process( final BufferedImage input ) {
		this.input = input;
		workImage = new BufferedImage(input.getWidth(),input.getHeight(),BufferedImage.TYPE_INT_RGB);

		gray.reshape(input.getWidth(), input.getHeight());
		ConvertBufferedImage.convertFrom(input, gray, true);

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
				workImage.createGraphics().drawImage(input, null, null);
				break;

			case 1:
				VisualizeBinaryData.renderBinary(alg.getBinary(), false, workImage);
				break;

			case 2:
				renderClusters();
				break;

			default:
				throw new RuntimeException("Unknown mode");
		}
		Graphics2D g2 = workImage.createGraphics();
		if( foundTarget ) {
			if( calibGUI.isShowBound() ) {
//				Polygon2D_I32 bounds =  alg.getFindBound().getBoundPolygon();
//				drawBounds(g2, bounds);
			}

			if( calibGUI.isShowNumbers() ) {
				drawNumbers(g2, alg.getCalibrationPoints(),null,1);
			}
			calibGUI.setSuccessMessage("FOUND",true);
		} else {
			if( calibGUI.isShowBound() ) {
//				Polygon2D_I32 bounds =  alg.getFindBound().getBoundPolygon();
//				drawBounds(g2, bounds);
			}

			calibGUI.setSuccessMessage("FAILED",false);
		}

		if( calibGUI.isShowPoints() ) {
			List<Point2D_F64> candidates =  alg.getCalibrationPoints();
			for( Point2D_F64 c : candidates ) {
				VisualizeFeatures.drawPoint(g2, (int)(c.x+0.5), (int)(c.y+0.5), 1, Color.RED);
			}
		}

		if( calibGUI.doShowGraph ) {
			System.out.println("Maybe I should add this back in with the new detector some how");
		}

		gui.setBufferedImage(workImage);
		gui.setScale(calibGUI.getScale());
		gui.repaint();

		processed = true;
	}

	private void renderClusters() {

		Graphics2D g2 = workImage.createGraphics();

		FastQueue<Polygon2D_F64> squares =  alg.getFindSeeds().getDetectorSquare().getFound();

		for (int i = 0; i < squares.size(); i++) {
			Polygon2D_F64 p = squares.get(i);
			g2.setColor(Color.black);
			g2.setStroke(new BasicStroke(4));
			VisualizeShapes.drawPolygon(p, true, g2, true);
			g2.setColor(Color.white);
			g2.setStroke(new BasicStroke(2));
			VisualizeShapes.drawPolygon(p, true, g2, true);
		}

		List<SquareGrid> grids = alg.getFindSeeds().getGrids().getGrids();

		for( int i = 0; i < grids.size(); i++ ) {
			SquareGrid g = grids.get(i);
			int a = grids.size()==1 ? 0 : 255*i/(grids.size()-1);

			int rgb = a << 16 | (255-a) << 8;

			g2.setStroke(new BasicStroke(3));

			for (int j = 0; j < g.nodes.size()-1; j++) {
				double fraction = j/((double)g.nodes.size()-1);
				fraction = fraction*0.6 + 0.4;

				int lineRGB = (int)(fraction*a) << 16 | (int)(fraction*(255-a)) << 8;

				g2.setColor(new Color(lineRGB));
				SquareNode p0 = g.nodes.get(j);
				SquareNode p1 = g.nodes.get(j+1);
				g2.drawLine((int) p0.center.x, (int) p0.center.y, (int) p1.center.x, (int) p1.center.y);
			}

			g2.setColor(new Color(rgb));
			for (int j = 0; j < g.nodes.size(); j++) {
				SquareNode n = g.nodes.get(j);
				VisualizeShapes.drawPolygon(n.corners, true, g2, true);
			}
		}
	}

	public static void drawNumbers( Graphics2D g2 , java.util.List<Point2D_F64> foundTarget ,
									PointTransform_F32 transform ,
									double scale ) {

		Font regular = new Font("Serif", Font.PLAIN, 16);
		g2.setFont(regular);

		Point2D_F32 adj = new Point2D_F32();

		AffineTransform origTran = g2.getTransform();
		for( int i = 0; i < foundTarget.size(); i++ ) {
			Point2D_F64 p = foundTarget.get(i);

			if( transform != null ) {
				transform.compute((float)p.x,(float)p.y,adj);
			} else {
				adj.set((float)p.x,(float)p.y);
			}

			String text = String.format("%2d",i);

			int x = (int)(adj.x*scale);
			int y = (int)(adj.y*scale);

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

		DetectCalibrationChessApp app = new DetectCalibrationChessApp();

//		String prefix = "../data/applet/calibration/mono/Sony_DSC-HX5V_Chess/";
		String prefix = "../data/applet/calibration/stereo/Bumblebee2_Chess/";

		app.loadConfigurationFile(prefix + "info.txt");

		app.setBaseDirectory(prefix);
//		app.loadInputData(prefix+"images.txt");

		List<PathLabel> inputs = new ArrayList<PathLabel>();

		for (int i = 1; i <= 12; i++) {
//			inputs.add(new PathLabel(String.format("View %02d",i),String.format("%sframe%02d.jpg",prefix,i)));
			inputs.add(new PathLabel(String.format("View %02d",i),String.format("%sleft%02d.jpg",prefix,i)));
		}

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Calibration Target Detection",true);
	}
}
