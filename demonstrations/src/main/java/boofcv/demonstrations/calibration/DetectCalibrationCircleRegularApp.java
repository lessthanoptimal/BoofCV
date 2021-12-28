/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.fiducial.calib.CalibrationDetectorCircleRegularGrid;
import boofcv.abst.fiducial.calib.ConfigCircleRegularGrid;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.abst.filter.binary.BinaryContourInterface;
import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.Grid;
import boofcv.alg.fiducial.calib.circle.EllipsesIntoClusters;
import boofcv.alg.fiducial.calib.squares.SquareGrid;
import boofcv.alg.fiducial.calib.squares.SquareNode;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.io.UtilIO;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.curve.EllipseRotated_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays detected regular circular grid. Visualizes several of its processing steps making it easier to debug.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectCalibrationCircleRegularApp extends CommonDetectCalibrationApp {
	CalibrationDetectorCircleRegularGrid detector;
	ConfigGridDimen configGrid;
	ConfigCircleRegularGrid configDet = new ConfigCircleRegularGrid();

	Color[] colorId;
	Line2D.Double line = new Line2D.Double();

	public DetectCalibrationCircleRegularApp( int numRows, int numColumns,
											  double circleDiameter, double centerDistance,
											  List<String> exampleInputs ) {
		super(exampleInputs);
		setUpGui(new DetectCalibrationCirclePanel(numRows, numColumns, circleDiameter, centerDistance, false));
		configGrid = new ConfigGridDimen(numRows, numColumns, circleDiameter, centerDistance);
		controlPanel.getThreshold().setConfiguration(configDet.thresholding);
		declareDetector();

		colorId = new Color[]{Color.RED, Color.BLUE, Color.CYAN, Color.ORANGE};
	}

	@Override
	public void declareDetector() {
		configDet.thresholding = controlPanel.getThreshold().createConfig();
		configGrid.numRows = controlPanel.getGridRows();
		configGrid.numCols = controlPanel.getGridColumns();
		configGrid.shapeSize = ((DetectCalibrationCirclePanel)controlPanel).getCircleDiameter();
		configGrid.shapeDistance = ((DetectCalibrationCirclePanel)controlPanel).getCircleSpacing();

		detector = FactoryFiducialCalibration.circleRegularGrid(configDet, configGrid);
		reprocessImageOnly();
	}

	@Override
	protected void renderClusters( Graphics2D g2, double scale ) {
		List<BinaryEllipseDetector.EllipseInfo> found = detector.getDetector().getEllipseDetector().getFound().toList();
		List<List<EllipsesIntoClusters.Node>> clusters = detector.getDetector().getClusters();

		g2.setStroke(new BasicStroke(2));
		int id = 0;
		for (List<EllipsesIntoClusters.Node> c : clusters) {

			g2.setColor(colorId[Math.min(id++, colorId.length - 1)]);
			for (EllipsesIntoClusters.Node n : c) {
				EllipseRotated_F64 a = found.get(n.which).ellipse;
				line.x1 = a.center.x*scale;
				line.y1 = a.center.y*scale;
				for (int i = 0; i < n.connections.size; i++) {
					EllipseRotated_F64 b = found.get(n.connections.get(i)).ellipse;
					line.x2 = b.center.x*scale;
					line.y2 = b.center.y*scale;
					g2.draw(line);
				}
			}
		}
	}

	@Override
	protected void renderGraph( Graphics2D g2, double scale ) {
		throw new RuntimeException("Shouldn't be an option");
	}

	@Override
	protected void renderGrid( Graphics2D g2, double scale ) {
		List<Grid> grids = detector.getDetector().getGrider().getGrids().toList();

		BasicStroke thin = new BasicStroke(3);
		BasicStroke thick = new BasicStroke(5);

		for (Grid g : grids) {
			double x0 = Double.MAX_VALUE;
			double x1 = -Double.MAX_VALUE;
			double y0 = Double.MAX_VALUE;
			double y1 = -Double.MAX_VALUE;

			for (int i = 0; i < g.ellipses.size(); i++) {
				EllipseRotated_F64 e = g.ellipses.get(i);
				if (e == null) continue;
				x0 = Math.min(e.center.x, x0);
				x1 = Math.max(e.center.x, x1);
				y0 = Math.min(e.center.y, y0);
				y1 = Math.max(e.center.y, y1);
			}

			x0 *= scale;
			y0 *= scale;
			x1 *= scale;
			y1 *= scale;

			g2.setColor(Color.WHITE);
			g2.setStroke(thick);
			VisualizeShapes.drawRectangle(x0, y0, x1, y1, line, g2);
			g2.setColor(Color.ORANGE);
			g2.setStroke(thin);
			VisualizeShapes.drawRectangle(x0, y0, x1, y1, line, g2);
		}
	}

	@Override
	public void renderOrder( Graphics2D g2, double scale, List<PointIndex2D_F64> points ) {
		renderOrderA(g2, scale, points);
	}

	public static void renderOrderA( Graphics2D g2, double scale, List<PointIndex2D_F64> points ) {
		g2.setStroke(new BasicStroke(5));

		Color[] colorsSquare = new Color[4];
		colorsSquare[0] = new Color(0, 255, 0);
		colorsSquare[1] = new Color(100, 180, 0);
		colorsSquare[2] = new Color(160, 140, 0);
		colorsSquare[3] = new Color(0, 140, 100);

		Line2D.Double line = new Line2D.Double();

		for (int i = 1; i + 6 < points.size(); i += 4) {
			Point2D_F64 p0 = points.get(i).p;
			Point2D_F64 p1 = points.get(i + 6).p;

			double fraction = i/((double)points.size() - 2);

			int red = (int)(0xFF*fraction) + (int)(0x00*(1 - fraction));
			int green = 0x00;
			int blue = (int)(0x00*fraction) + (int)(0xff*(1 - fraction));

			int lineRGB = red << 16 | green << 8 | blue;

			line.setLine(scale*p0.x, scale*p0.y, scale*p1.x, scale*p1.y);

			g2.setColor(new Color(lineRGB));
			g2.draw(line);
		}

		for (int i = 0; i + 3 < points.size(); i += 4) {
			Point2D_F64 p0 = points.get(i).p;
			Point2D_F64 p1 = points.get(i + 1).p;
			Point2D_F64 p2 = points.get(i + 2).p;
			Point2D_F64 p3 = points.get(i + 3).p;

			line.setLine(scale*p0.x, scale*p0.y, scale*p1.x, scale*p1.y);
			g2.setColor(colorsSquare[0]);
			g2.draw(line);

			line.setLine(scale*p1.x, scale*p1.y, scale*p2.x, scale*p2.y);
			g2.setColor(colorsSquare[1]);
			g2.draw(line);

			line.setLine(scale*p2.x, scale*p2.y, scale*p3.x, scale*p3.y);
			g2.setColor(colorsSquare[2]);
			g2.draw(line);

			line.setLine(scale*p3.x, scale*p3.y, scale*p0.x, scale*p0.y);
			g2.setColor(colorsSquare[3]);
			g2.draw(line);
		}
	}

	@Override
	protected boolean process( GrayF32 image ) {
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
	protected List<PointIndex2D_F64> getCalibrationPoints() {
		return detector.getKeypointFinder().getKeyPoints().toList();
	}

	@Override
	protected List<Contour> getContours() {

		BinaryContourInterface contour = detector.getDetector().getEllipseDetector().getEllipseDetector().getContourFinder();

		return BinaryImageOps.convertContours(contour);
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

	public static void main( String[] args ) {

		List<String> examples = new ArrayList<>();

		for (int i = 0; i <= 5; i++) {
			examples.add(UtilIO.pathExample(String.format("calibration/mono/Sony_DSC-HX5V_CircleRegular/image%05d.jpg", i)));
		}
		examples.add(UtilIO.pathExample("fiducial/circle_regular/movie.mp4"));

		SwingUtilities.invokeLater(() -> {
			DetectCalibrationCircleRegularApp app = new DetectCalibrationCircleRegularApp(10, 8, 1.5, 2.5, examples);

			app.openFile(new File(examples.get(0)));
			app.display("Circle Regular Grid Detector");
		});
	}
}
