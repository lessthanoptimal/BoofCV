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

package boofcv.abst.fiducial.calib;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.fiducial.calib.circle.DetectCircleAsymmetricGrid;
import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.Grid;
import boofcv.alg.fiducial.calib.circle.EllipsesIntoClusters;
import boofcv.alg.fiducial.calib.circle.KeyPointsCircleHexagonalGrid;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.image.GrayF32;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Calibration implementation of circle hexagonal grid fiducial.
 *
 * @see DetectCircleAsymmetricGrid
 * @see KeyPointsCircleHexagonalGrid
 *
 * @author Peter Abeles
 */
public class CalibrationDetectorCircleAsymmGrid implements DetectorFiducialCalibration {

	// Detectors the grids
	private DetectCircleAsymmetricGrid<GrayF32> detector;
	// extracts key points from detected grid
	private KeyPointsCircleHexagonalGrid keypoint = new KeyPointsCircleHexagonalGrid();

	// Storage for 2D location of points on fiducial
	private List<Point2D_F64> layout;

	private CalibrationObservation results;

	double spaceToDiameter;

	/**
	 * Configures the detector based on the pass in configuration class
	 * @param config Configuration for detector and target description
	 */
	public CalibrationDetectorCircleAsymmGrid(ConfigCircleHexagonalGrid config ) {

		InputToBinary<GrayF32> inputToBinary =
				FactoryThresholdBinary.threshold(config.thresholding,GrayF32.class);

		BinaryEllipseDetector<GrayF32> ellipseDetector =
				FactoryShapeDetector.ellipse(config.ellipse,GrayF32.class);

		spaceToDiameter = (config.centerDistance/config.circleDiameter);
		double spaceToRadius = 2.0*spaceToDiameter;

		EllipsesIntoClusters e2c = new EllipsesIntoClusters(spaceToRadius*1.25,config.ellipseSizeSimilarity);

		detector = new DetectCircleAsymmetricGrid<>(config.numRows,config.numCols,inputToBinary,
				ellipseDetector,e2c);


		layout = createLayout(detector.getRows(),detector.getColumns(), config.centerDistance);
	}

	@Override
	public boolean process(GrayF32 input) {
		results = new CalibrationObservation();
		detector.process(input);

		List<Grid> grids = detector.getGrids();

		if( grids.size() != 1 )
			return false;

		if( !keypoint.process(grids.get(0)) )
			return false;

		FastQueue<Point2D_F64> foundPixels = keypoint.getKeyPoints();

		for (int i = 0; i < foundPixels.size; i++) {
			results.add(foundPixels.get(i),i);
		}
		return true;
	}

	@Override
	public CalibrationObservation getDetectedPoints() {
		return results;
	}

	@Override
	public List<Point2D_F64> getLayout() {
		return layout;
	}

	/**
	 * Specifies the physical location of each point on the 2D calibration plane.  The fiducial is centered on the
	 * coordinate system
	 * @param numRows Number of rows
	 * @param numCols Number of columns
	 * @param centerDistance Space between each circle's center along x and y axis
	 * @return 2D locations
	 */
	public static List<Point2D_F64> createLayout( int numRows , int numCols , double centerDistance ) {

		List<Point2D_F64> ret = new ArrayList<>();

		double spaceX = centerDistance/2.0;
		double spaceY = centerDistance*Math.sin(UtilAngle.radian(60));

		double width = (numCols-1)*spaceX;
		double height = (numRows-1)*spaceY;

		for (int row = 0; row < numRows; row++) {
			double y = (numRows-row-1)*spaceY - height/2;
			for (int col = 0; col < numCols; col++) {
				double x = col*spaceX - width/2;

				if( row%2==0 && col%2==1)
					continue;
				if( row%2==1 && col%2==0)
					continue;
				ret.add( new Point2D_F64(x,y));
			}
		}

		return ret;
	}

	public DetectCircleAsymmetricGrid<GrayF32> getDetector() {
		return detector;
	}

	public KeyPointsCircleHexagonalGrid getKeypointFinder() {
		return keypoint;
	}

	public int getRows() {
		return detector.getRows();
	}

	public int getColumns() {
		return detector.getColumns();
	}

	/**
	 * Distance between centers to circle radius ratio
	 */
	public double getSpaceToDiameter() {
		return spaceToDiameter;
	}
}
