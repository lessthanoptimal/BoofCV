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

package boofcv.abst.calib;

import boofcv.alg.feature.detect.chess.DetectChessCalibrationPoints;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link DetectChessCalibrationPoints} for {@link PlanarCalibrationDetector}
 * 
 * @author Peter Abeles
 */
public class WrapPlanarChessTarget implements PlanarCalibrationDetector {

	DetectChessCalibrationPoints<ImageFloat32,ImageFloat32> alg;

	List<Point2D_F64> layoutPoints;

	public WrapPlanarChessTarget(ConfigChessboard config ) {
		alg = new DetectChessCalibrationPoints<ImageFloat32, ImageFloat32>(
				config.numCols,config.numRows,config.nonmaxRadius,
				config.relativeSizeThreshold,ImageFloat32.class);
		alg.setUserBinaryThreshold(config.binaryGlobalThreshold);
		alg.setUserAdaptiveRadius(config.binaryAdaptiveRadius);
		alg.setUserAdaptiveBias(config.binaryAdaptiveBias);

		layoutPoints = gridChess(config.numCols,config.numRows,config.squareWidth);
	}

	@Override
	public boolean process(ImageFloat32 input) {
		return alg.process(input);
	}

	@Override
	public List<Point2D_F64> getDetectedPoints() {
		// points should be at sub-pixel accuracy and in the correct orientation
		return alg.getPoints();
	}

	@Override
	public List<Point2D_F64> getLayout() {
		return layoutPoints;
	}

	public DetectChessCalibrationPoints<ImageFloat32, ImageFloat32> getAlg() {
		return alg;
	}

	/**
	 * This target is composed of a checkered chess board like squares.  Each corner of an interior square
	 * touches an adjacent square, but the sides are separated.  Only interior square corners provide
	 * calibration points.
	 *
	 * @param numCols Number of grid columns in the calibration target
	 * @param numRows Number of grid rows in the calibration target
	 * @param squareWidth How wide each square is.  Units are target dependent.
	 * @return Target description
	 */
	public static List<Point2D_F64> gridChess( int numCols , int numRows , double squareWidth )
	{
		List<Point2D_F64> all = new ArrayList<Point2D_F64>();

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
