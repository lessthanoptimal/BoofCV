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

package boofcv.demonstrations.calibration;

import boofcv.abst.calib.ConfigChessboard;
import boofcv.alg.feature.detect.chess.DetectChessboardFiducial;
import boofcv.alg.feature.detect.squares.SquareEdge;
import boofcv.alg.feature.detect.squares.SquareGrid;
import boofcv.alg.feature.detect.squares.SquareNode;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.gui.SelectInputPanel;
import boofcv.gui.VisualizeApp;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.SimpleStringNumberReader;
import boofcv.io.UtilIO;
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

	ConfigChessboard config;

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

	public void configure( int numCols , int numRows , boolean forCalibration ) {
		config = new ConfigChessboard(numCols,numRows,30);

		if( !forCalibration ) {
			config.square.refineWithCorners = false;
			config.square.refineWithLines = true;
		}

		alg = FactoryPlanarCalibrationTarget.detectorChessboard(config).getAlg();
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

		configure(numCols, numRows, true);
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

			default:
				throw new RuntimeException("Unknown mode");
		}
		Graphics2D g2 = workImage.createGraphics();
		if( foundTarget ) {

			if( calibGUI.isShowNumbers() ) {
				drawNumbers(g2, alg.getCalibrationPoints(),null,1);
			}
			if( calibGUI.isShowPoints() ) {
				List<Point2D_F64> candidates =  alg.getCalibrationPoints();
				for( Point2D_F64 c : candidates ) {
					VisualizeFeatures.drawPoint(g2, (int)(c.x+0.5), (int)(c.y+0.5), 1, Color.RED);
				}
			}
			calibGUI.setSuccessMessage("FOUND",true);
		} else {

			calibGUI.setSuccessMessage("FAILED",false);
		}

		if( calibGUI.isShowGraph() ) {
			renderGraph(g2);
		}

		if( calibGUI.isShowOrder() ) {
			renderOrder(g2);
		}

		renderClusters();

		gui.setBufferedImage(workImage);
		gui.setScale(calibGUI.getScale());
		gui.repaint();

		processed = true;
	}

	private void renderOrder(Graphics2D g2) {
		List<SquareGrid> grids = alg.getFindSeeds().getGrids().getGrids();
		g2.setStroke(new BasicStroke(3));

		for (int i = 0; i < grids.size(); i++) {
			SquareGrid g = grids.get(i);
			int a = grids.size() == 1 ? 0 : 255 * i / (grids.size() - 1);

			for (int j = 0; j < g.nodes.size() - 1; j++) {
				double fraction = j / ((double) g.nodes.size() - 1);
				fraction = fraction * 0.6 + 0.4;

				int lineRGB = (int) (fraction * a) << 16 | (int) (fraction * (255 - a)) << 8;

				g2.setColor(new Color(lineRGB));
				SquareNode p0 = g.nodes.get(j);
				SquareNode p1 = g.nodes.get(j + 1);
				g2.drawLine((int) p0.center.x, (int) p0.center.y, (int) p1.center.x, (int) p1.center.y);
			}
		}
	}

	private void renderClusters() {

		Graphics2D g2 = workImage.createGraphics();


		if( calibGUI.doShowSquares ) {
			FastQueue<Polygon2D_F64> squares =  alg.getFindSeeds().getDetectorSquare().getFoundPolygons();

			for (int i = 0; i < squares.size(); i++) {
				Polygon2D_F64 p = squares.get(i);
				if (isInGrids(p))
					continue;
				g2.setColor(Color.cyan);
				g2.setStroke(new BasicStroke(4));
				VisualizeShapes.drawPolygon(p, true, g2, true);
				g2.setColor(Color.blue);
				g2.setStroke(new BasicStroke(2));
				VisualizeShapes.drawPolygon(p, true, g2, true);

				drawCornersInside(g2,p);
			}
		}

		if( calibGUI.doShowGrids ) {
			List<SquareGrid> grids = alg.getFindSeeds().getGrids().getGrids();

			for (int i = 0; i < grids.size(); i++) {
				SquareGrid g = grids.get(i);
				int a = grids.size() == 1 ? 0 : 255 * i / (grids.size() - 1);

				int rgb = a << 16 | (255 - a) << 8;

				g2.setStroke(new BasicStroke(3));

				Color color = new Color(rgb);
				for (int j = 0; j < g.nodes.size(); j++) {
					SquareNode n = g.nodes.get(j);
					g2.setColor(color);
					VisualizeShapes.drawPolygon(n.corners, true, g2, true);

					drawCornersInside(g2,n.corners);
				}
			}
		}
	}

	private void renderGraph( Graphics2D g2 ) {
		List<List<SquareNode>> graphs = alg.getFindSeeds().getGraphs();

		g2.setStroke(new BasicStroke(3));
		for( int i = 0; i < graphs.size(); i++ ) {

			List<SquareNode> graph = graphs.get(i);

			int key = graphs.size() == 1 ? 0 : 255 * i / (graphs.size() - 1);

			int rgb = key << 8 | (255 - key);
			g2.setColor(new Color(rgb));

			List<SquareEdge> edges = new ArrayList<SquareEdge>();

			for( SquareNode n : graph ) {
				for (int j = 0; j < 4; j++) {
					if( n.edges[j] != null && !edges.contains(n.edges[j])) {
						edges.add( n.edges[j]);
					}
				}
			}

			for( SquareEdge e : edges ) {
				Point2D_F64 a = e.a.center;
				Point2D_F64 b = e.b.center;

				g2.drawLine((int)a.x,(int)a.y,(int)b.x,(int)b.y);
			}
		}
	}

	private void drawCornersInside( Graphics2D g2 , Polygon2D_F64 poly ) {
		double x0 = poly.get(0).x;
		double y0 = poly.get(0).y;

		double x1 = poly.get(1).x;
		double y1 = poly.get(1).y;

		double x2 = poly.get(2).x;
		double y2 = poly.get(2).y;

		double x3 = poly.get(3).x;
		double y3 = poly.get(3).y;

		double dx02 = x2-x0;
		double dy02 = y2-y0;

		double dx13 = x3-x1;
		double dy13 = y3-y1;

		double fraction = 0.2;

		x0 += dx02*fraction;
		y0 += dy02*fraction;

		x2 -= dx02*fraction;
		y2 -= dy02*fraction;

		x1 += dx13*fraction;
		y1 += dy13*fraction;

		x3 -= dx13*fraction;
		y3 -= dy13*fraction;

		VisualizeFeatures.drawPoint(g2, x0, y0, 3, Color.RED, false);
		VisualizeFeatures.drawPoint(g2, x1, y1, 3, new Color(190, 0, 0), false);
		VisualizeFeatures.drawPoint(g2, x2, y2, 3, Color.GREEN, false);
		VisualizeFeatures.drawPoint(g2, x3, y3, 3, new Color(0, 190, 0), false);

	}

	private boolean isInGrids( Polygon2D_F64 p ) {
		List<SquareGrid> grids = alg.getFindSeeds().getGrids().getGrids();

		for (int i = 0; i < grids.size(); i++) {
			SquareGrid g = grids.get(i);
			for (int j = 0; j < g.nodes.size(); j++) {
				if( g.nodes.get(j).corners == p )
					return true;
			}
		}
		return false;
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

		if( calibGUI.isManual()) {
			config.thresholding.type = ThresholdType.FIXED;
			config.thresholding.fixedThreshold = calibGUI.getThresholdLevel();
		} else {
			config.thresholding.type = ThresholdType.LOCAL_SQUARE;
		}
		alg = FactoryPlanarCalibrationTarget.detectorChessboard(config).getAlg();
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

//		String prefix = UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess/");
		String prefix = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess/");

		app.loadConfigurationFile(prefix + "info.txt");
//		app.configure(5,7, true);

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
