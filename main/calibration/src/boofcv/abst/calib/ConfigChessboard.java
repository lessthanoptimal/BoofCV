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

import boofcv.alg.feature.detect.chess.DetectChessboardFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.struct.Configuration;

/**
 * Calibration parameters for chessboard style calibration grid.
 *
 * @see DetectChessboardFiducial
 *
 * @author Peter Abeles
 */
public class ConfigChessboard implements Configuration {
	/**
	 * Number of squares wide the grid is. Target dependent.
	 */
	public int numCols = -1;
	/**
	 * Number of squares tall the grid is. Target dependent.
	 */
	public int numRows = -1;

	/**
	 * Configuration for thresholding the image
	 */
	public ConfigThreshold thresholding = ConfigThreshold.local(ThresholdType.LOCAL_SQUARE,20);

	/**
	 * Configuration for square detector
	 */
	public ConfigPolygonDetector square = new ConfigPolygonDetector(true, 4);

	/**
	 * Physical width of each square on the calibration target
	 */
	public double squareWidth;

	{
		thresholding.bias = -10;

		// it erodes the original shape meaning it has to move a greater distance
		square.configRefineCorners.maxCornerChangePixel = 5;

//		square.contour2Poly_splitFraction = 0.25;
//		square.contour2Poly_minimumSplitFraction = 0.01;

		square.minimumEdgeIntensity = 0.1;

		square.minContourImageWidthFraction = 0.05;

		square.refineWithCorners = true;
		square.refineWithLines = false;

		// good value for squares.  Set it here to make it not coupled to default values
		square.configRefineCorners.cornerOffset = 1;
		square.configRefineCorners.lineSamples = 10;
		square.configRefineCorners.convergeTolPixels = 0.05;
		square.configRefineCorners.maxIterations = 10;

		// defaults for if the user toggles it to lines
		square.configRefineLines.cornerOffset = 1;
		square.configRefineLines.lineSamples = 10;
		square.configRefineLines.convergeTolPixels = 0.05;
		square.configRefineLines.maxIterations = 10;
	}

	public ConfigChessboard(int numCols, int numRows, double squareWidth ) {
		this.numCols = numCols;
		this.numRows = numRows;
		this.squareWidth = squareWidth;
	}


	@Override
	public void checkValidity() {
		if( numCols <= 0 || numRows <= 0 )
			throw new IllegalArgumentException("Must specify then number of rows and columns in the target");
	}
}
