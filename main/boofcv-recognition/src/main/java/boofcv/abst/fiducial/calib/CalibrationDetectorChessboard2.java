/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.feature.detect.chess.DetectChessboardCornersPyramid;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterFinder;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterToGrid;
import boofcv.alg.fiducial.calib.chess.DetectChessboardPatterns;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Detector for chessboard calibration targets. Returns the first chessboard which is detected and matches
 * the expected size
 * 
 * @author Peter Abeles
 */
public class CalibrationDetectorChessboard2
		extends DetectChessboardPatterns
		implements DetectorFiducialCalibration {

	int cornerRows,cornerCols;

	List<Point2D_F64> layoutPoints;
	CalibrationObservation detected;

	// transform from input pixels to undistorted pixels
	Point2Transform2_F64 pixel2undist;

	public CalibrationDetectorChessboard2(ConfigChessboard2 config, ConfigGridDimen shape ) {
		super(config);

		cornerRows = shape.numRows-1;
		cornerCols = shape.numCols-1;

		layoutPoints = gridChess(shape.numRows, shape.numCols, shape.shapeSize);

		clusterToGrid.setCheckShape((r,c)->r==cornerRows&&c==cornerCols);
	}

	@Override
	public boolean process(GrayF32 input) {
		super.findPatterns(input);

		if( found.size >= 1 ) {
			detected = new CalibrationObservation(input.width, input.height);
			ChessboardCornerClusterToGrid.GridInfo info = found.get(0);

			for (int i = 0; i < info.nodes.size(); i++) {
				detected.add(info.nodes.get(i), i);
			}

			// remove lens distortion
			if( pixel2undist != null ) {
				for (int i = 0; i < info.nodes.size(); i++) {
					PointIndex2D_F64 p = detected.points.get(i);
					pixel2undist.compute(p.x,p.y,p);
				}
			}

			return true;
		} else {
			detected = new CalibrationObservation(input.width, input.height);
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

	@Override
	public void setLensDistortion(LensDistortionNarrowFOV distortion, int width, int height) {
		if( distortion == null )
			pixel2undist = null;
		else {
			pixel2undist = distortion.undistort_F64(true, true);
		}
	}

	public DetectChessboardCornersPyramid getDetector() {
		return detector;
	}

	public ChessboardCornerClusterFinder getClusterFinder() {
		return clusterFinder;
	}

	public ChessboardCornerClusterToGrid getClusterToGrid() {
		return clusterToGrid;
	}

	public int getCornerRows() {
		return cornerRows;
	}

	public int getCornerCols() {
		return cornerCols;
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
