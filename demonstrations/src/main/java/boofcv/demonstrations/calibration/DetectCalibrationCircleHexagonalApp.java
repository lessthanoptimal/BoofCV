/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.fiducial.calib.CalibrationDetectorCircleHexagonalGrid;
import boofcv.abst.fiducial.calib.ConfigCircleHexagonalGrid;
import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.Grid;
import boofcv.alg.fiducial.calib.circle.EllipsesIntoClusters;
import boofcv.alg.fiducial.calib.circle.KeyPointsCircleHexagonalGrid.Tangents;
import boofcv.alg.fiducial.calib.squares.SquareGrid;
import boofcv.alg.fiducial.calib.squares.SquareNode;
import boofcv.alg.filter.binary.Contour;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.io.UtilIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import org.ddogleg.struct.FastQueue;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays detected hexagonal circular grid.  Visualizes several of its processing steps making it easier to debug.
 *
 * @author Peter Abeles
 */
public class DetectCalibrationCircleHexagonalApp extends CommonDetectCalibrationApp
{
	CalibrationDetectorCircleHexagonalGrid detector;
	ConfigCircleHexagonalGrid config;

	Color colorId[];

	public DetectCalibrationCircleHexagonalApp(int numRows , int numColumns ,
											   double circleDiameter, double centerDistance,
											   List<String> exampleInputs) {
		super(new DetectCalibrationCirclePanel(numRows,numColumns,circleDiameter,centerDistance,true),exampleInputs);

		config = new ConfigCircleHexagonalGrid(numRows, numColumns, circleDiameter, centerDistance);

		declareDetector();

		colorId = new Color[]{Color.RED,Color.BLUE,Color.CYAN,Color.ORANGE};
	}

	@Override
	public void declareDetector() {
		if( controlPanel.isManual()) {
			config.thresholding.type = ThresholdType.FIXED;
			config.thresholding.fixedThreshold = controlPanel.getThresholdLevel();
		} else {
			config.thresholding.type = ThresholdType.LOCAL_MEAN;
		}

		config.numRows = controlPanel.getGridRows();
		config.numCols = controlPanel.getGridColumns();
		config.circleDiameter = ((DetectCalibrationCirclePanel)controlPanel).getCircleDiameter();
		config.centerDistance = ((DetectCalibrationCirclePanel)controlPanel).getCircleSpacing();

		detector = FactoryFiducialCalibration.circleHexagonalGrid(config);
	}

	@Override
	protected void renderClusters(Graphics2D g2, double scale) {
		List<EllipseRotated_F64> found = detector.getDetector().getEllipseDetector().getFoundEllipses(null);
		List<List<EllipsesIntoClusters.Node>> clusters = detector.getDetector().getClusters();

		g2.setStroke(new BasicStroke(2));
		int id = 0;
		for( List<EllipsesIntoClusters.Node> c : clusters ) {

			g2.setColor(colorId[Math.min(id++,colorId.length-1)]);
			for( EllipsesIntoClusters.Node n : c ) {
				EllipseRotated_F64 a = found.get(n.which);
				for (int i = 0; i < n.connections.size; i++) {
					EllipseRotated_F64 b = found.get(n.connections.get(i));
					g2.drawLine((int)(a.center.x*scale),(int)(a.center.y*scale),(int)(b.center.x*scale),(int)(b.center.y*scale));
				}
			}
		}
	}

	@Override
	protected void renderGraph(Graphics2D g2 , double scale ) {
		List<Grid> grids = detector.getDetector().getGrids();

		g2.setStroke(new BasicStroke(2));
		for( Grid g : grids ) {
			g2.setColor(Color.CYAN);
			drawGraph(g2, g, 0, 0, scale);
			g2.setColor(Color.ORANGE);
			drawGraph(g2, g, 1, 1, scale);
		}
	}

	private void drawGraph(Graphics2D g2, Grid g, int row0 , int col0 , double scale) {
		for (int row = row0; row < g.rows; row += 2) {
			for (int col = col0; col < g.columns; col += 2) {
				EllipseRotated_F64 a = g.get(row,col);
				if( col+2 < g.columns) {
					EllipseRotated_F64 b = g.get(row, col + 2);
					g2.drawLine((int)(scale*a.center.x),(int)(scale*a.center.y),
							(int)(scale*b.center.x),(int)(scale*b.center.y));
				}
				if( row+2 < g.rows) {
					EllipseRotated_F64 b = g.get(row+2, col);
					g2.drawLine((int)(scale*a.center.x),(int)(scale*a.center.y),
							(int)(scale*b.center.x),(int)(scale*b.center.y));
				}
			}
		}
	}

	@Override
	protected void renderGrid(Graphics2D g2 , double scale ) {
		List<Grid> grids = detector.getDetector().getGrider().getGrids().toList();

		BasicStroke thin = new BasicStroke(3);
		BasicStroke thick = new BasicStroke(5);

		Rectangle2D_I32 r = new Rectangle2D_I32();
		for( Grid g : grids ) {
			double x0 = Double.MAX_VALUE;
			double x1 = -Double.MAX_VALUE;
			double y0 = Double.MAX_VALUE;
			double y1 = -Double.MAX_VALUE;

			for (int i = 0; i < g.ellipses.size(); i++) {
				EllipseRotated_F64 e = g.ellipses.get(i);
				if( e == null ) continue;
				x0 = Math.min(e.center.x,x0);
				x1 = Math.max(e.center.x,x1);
				y0 = Math.min(e.center.y,y0);
				y1 = Math.max(e.center.y,y1);
			}

			r.x0 = (int)(scale*x0+0.5);
			r.x1 = (int)(scale*x1+0.5);
			r.y0 = (int)(scale*y0+0.5);
			r.y1 = (int)(scale*y1+0.5);
			g2.setColor(Color.WHITE);
			g2.setStroke(thick);
			VisualizeShapes.drawRectangle(r,g2);
			g2.setColor(Color.ORANGE);
			g2.setStroke(thin);
			VisualizeShapes.drawRectangle(r,g2);
		}

		renderTangents(g2,scale);
	}

	protected void renderTangents(Graphics2D g2 , double scale ) {
		FastQueue<Tangents> tangents = detector.getKeypointFinder().getTangents();

		BasicStroke thick = new BasicStroke(2.0f);
		BasicStroke thin = new BasicStroke(1.0f);

		int radius = Math.max(1,(int)(3*scale+0.5));
		int width = radius*2+1;

		for( Tangents l : tangents.toList() ) {
			for (int i = 0; i < l.size; i++) {
				Point2D_F64 p = l.get(i);

				int x = (int)(scale*p.x + 0.5);
				int y = (int)(scale*p.y + 0.5);

				g2.setColor(Color.WHITE);
				g2.setStroke(thick);
				g2.drawOval(x-radius,y-radius,width,width);
				g2.setColor(Color.BLACK);
				g2.setStroke(thin);
				g2.drawOval(x-radius,y-radius,width,width);
			}
		}
	}

	@Override
	protected boolean process(GrayF32 image) {
		return detector.process(image);
	}

	@Override
	protected GrayU8 getBinaryImage() {
		return detector.getDetector().getBinary();
	}

	@Override
	protected List<List<SquareNode>> getClusters() {
		return new ArrayList<>();
	}

	@Override
	protected List<Point2D_F64> getCalibrationPoints() {
		return detector.getKeypointFinder().getKeyPoints().toList();
	}

	@Override
	protected List<Contour> getContours() {
		return detector.getDetector().getEllipseDetector().getEllipseDetector().getContourFinder().getContours().toList();
	}

	@Override
	protected List<Polygon2D_F64> getFoundPolygons() {
		return new ArrayList<>();
	}

	@Override
	protected List<EllipseRotated_F64> getFoundEllipses() {
		return detector.getDetector().getEllipseDetector().getFoundEllipses(null);
	}

	@Override
	protected List<SquareGrid> getGrids() {
		return new ArrayList<>();
	}

	public static void main(String[] args) {

		List<String>  examples = new ArrayList<>();

		for (int i = 1; i <= 7; i++) {
			examples.add(UtilIO.pathExample(String.format("calibration/mono/Sony_DSC-HX5V_CircleHexagonal/image%02d.jpg", i)));
		}

		DetectCalibrationCircleHexagonalApp app = new DetectCalibrationCircleHexagonalApp(24, 28, 1,1.2,examples);

		app.openFile(new File(examples.get(0)));
		app.display("Circle Hexagonal Grid Detector");
	}
}
