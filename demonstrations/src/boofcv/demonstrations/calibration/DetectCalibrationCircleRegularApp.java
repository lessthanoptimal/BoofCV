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

import boofcv.abst.fiducial.calib.CalibrationDetectorCircleRegularGrid;
import boofcv.abst.fiducial.calib.ConfigCircleRegularGrid;
import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.Grid;
import boofcv.alg.fiducial.calib.circle.EllipsesIntoClusters;
import boofcv.alg.fiducial.calib.squares.SquareGrid;
import boofcv.alg.fiducial.calib.squares.SquareNode;
import boofcv.alg.filter.binary.Contour;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays detected regular circular grid.  Visualizes several of its processing steps making it easier to debug.
 *
 * @author Peter Abeles
 */
public class DetectCalibrationCircleRegularApp extends CommonDetectCalibrationApp
{
	CalibrationDetectorCircleRegularGrid detector;
	ConfigCircleRegularGrid config;

	Color colorId[];

	public DetectCalibrationCircleRegularApp(int numRows , int numColumns ,
											 double circleRadius, double centerDistance,
											 List<String> exampleInputs) {
		super(new DetectCalibrationCirclePanel(numRows,numColumns,circleRadius,centerDistance,false),exampleInputs);

		config = new ConfigCircleRegularGrid(numRows, numColumns, circleRadius, centerDistance);

		declareDetector();

		colorId = new Color[]{Color.RED,Color.BLUE,Color.CYAN,Color.ORANGE};
	}

	@Override
	public void declareDetector() {
		if( controlPanel.isManual()) {
			config.thresholding.type = ThresholdType.FIXED;
			config.thresholding.fixedThreshold = controlPanel.getThresholdLevel();
		} else {
			config.thresholding.type = ThresholdType.LOCAL_SQUARE;
		}

		config.numRows = controlPanel.getGridRows();
		config.numCols = controlPanel.getGridColumns();
		config.circleRadius = ((DetectCalibrationCirclePanel)controlPanel).getCircleRadius();
		config.centerDistance = ((DetectCalibrationCirclePanel)controlPanel).getCircleSpacing();

		detector = FactoryFiducialCalibration.circleRegularGrid(config);
	}

	@Override
	protected void renderClusters(Graphics2D g2, double scale) {
		List<EllipseRotated_F64> found = detector.getDetector().getEllipseDetector().getFoundEllipses().toList();
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
		throw new RuntimeException("Shouldn't be an option");
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
		return detector.getDetector().getEllipseDetector().getFoundEllipses().toList();
	}

	@Override
	protected List<SquareGrid> getGrids() {
		return new ArrayList<>();
	}

	public static void main(String[] args) {

		List<String>  examples = new ArrayList<>();

		for (int i = 0; i <= 5; i++) {
			examples.add(UtilIO.pathExample(String.format("calibration/mono/Sony_DSC-HX5V_CircleRegular/image%05d.jpg", i)));
		}

		DetectCalibrationCircleRegularApp app = new DetectCalibrationCircleRegularApp(10, 8, 0.75,2.5,examples);

		app.openFile(new File(examples.get(0)));
		app.waitUntilDoneProcessing();

		ShowImages.showWindow(app,"Circle Regular Grid Detector",true);
	}
}
