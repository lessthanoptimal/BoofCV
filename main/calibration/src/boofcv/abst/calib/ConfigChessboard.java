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
	 * Global threshold used on the image.  If <= 0 then a local adaptive threshold is used instead
	 */
	public double binaryGlobalThreshold = -1;

	/**
	 * Size of local region used by adaptive threshold
	 */
	public int binaryAdaptiveRadius = 20;
	/**
	 * Bias used by local adaptive threshold
	 */
	public double binaryAdaptiveBias = -10;

	/**
	 * Configuration for square detector
	 */
	public ConfigPolygonDetector square = new ConfigPolygonDetector(true, 4);

	/**
	 * Physical width of each square on the calibration target
	 */
	public double squareWidth;

	{

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
