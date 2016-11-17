/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.fiducial.calib.chess.DetectChessboardFiducial;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.alg.shapes.polygon.RefineBinaryPolygon;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link DetectChessboardFiducial} for {@link DetectorFiducialCalibration}
 * 
 * @author Peter Abeles
 */
public class CalibrationDetectorChessboard implements DetectorFiducialCalibration {

	DetectChessboardFiducial<GrayF32> alg;

	List<Point2D_F64> layoutPoints;
	CalibrationObservation detected;

	public CalibrationDetectorChessboard(ConfigChessboard config) {

		RefineBinaryPolygon<GrayF32> refineLine =
				FactoryShapeDetector.refinePolygon(config.configRefineLines,GrayF32.class);
		RefineBinaryPolygon<GrayF32> refineCorner = config.refineWithCorners ?
				FactoryShapeDetector.refinePolygon(config.configRefineLines,GrayF32.class) : null;

		config.square.refine = null;

		BinaryPolygonDetector<GrayF32> detectorSquare =
				FactoryShapeDetector.polygon(config.square,GrayF32.class);

		InputToBinary<GrayF32> inputToBinary =
				FactoryThresholdBinary.threshold(config.thresholding,GrayF32.class);

		alg = new DetectChessboardFiducial<>(
				config.numRows, config.numCols, config.maximumCornerDistance,detectorSquare,
				refineLine,refineCorner,inputToBinary);

		layoutPoints = gridChess(config.numRows, config.numCols, config.squareWidth);
	}

	@Override
	public boolean process(GrayF32 input) {
		detected = new CalibrationObservation();
		if( alg.process(input) )  {
			List<Point2D_F64> found = alg.getCalibrationPoints();
			for (int i = 0; i < found.size(); i++) {
				detected.add( found.get(i) , i );
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public CalibrationObservation getDetectedPoints() {
		return detected;
	}

	@Override
	public List<Point2D_F64> getLayout() {
		return layoutPoints;
	}

	/**
	 * Returns number of rows in the chessboard grid
	 * @return number of rows
	 */
	public int getGridRows() {
		return alg.getRows();
	}

	/**
	 * Returns number of columns in the chessboard grid
	 * @return number of columns
	 */
	public int getGridColumns() {
		return alg.getColumns();
	}


	public DetectChessboardFiducial<GrayF32> getAlgorithm() {
		return alg;
	}

	/**
	 * This target is composed of a checkered chess board like squares.  Each corner of an interior square
	 * touches an adjacent square, but the sides are separated.  Only interior square corners provide
	 * calibration points.
	 *
	 * @param numRows Number of grid rows in the calibration target
	 * @param numCols Number of grid columns in the calibration target
	 * @param squareWidth How wide each square is.  Units are target dependent.
	 * @return Target description
	 */
	public static List<Point2D_F64> gridChess(int numRows, int numCols, double squareWidth)
	{
		List<Point2D_F64> all = new ArrayList<>();

		// convert it into the number of calibration points
		numCols = numCols - 1;
		numRows = numRows - 1;

		// center the grid around the origin. length of a size divided by two
		double startX = -((numCols-1)*squareWidth)/2.0;
		double startY = -((numRows-1)*squareWidth)/2.0;

		for( int i = numRows-1; i >= 0; i-- ) {
			double y = startY+i*squareWidth;
			for( int j = 0; j < numCols; j++ ) {
				double x = startX+j*squareWidth;
				all.add( new Point2D_F64(x,y));
			}
		}

		return all;
	}
}
