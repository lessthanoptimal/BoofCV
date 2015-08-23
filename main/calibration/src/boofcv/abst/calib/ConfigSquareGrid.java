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

import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.struct.Configuration;

/**
 * Calibration parameters for square-grid style calibration grid.
 *
 * @see boofcv.alg.feature.detect.grid.DetectSquareGridFiducial
 *
 * @author Peter Abeles
 */
public class ConfigSquareGrid implements Configuration {
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
	 * Configuration for square detector
	 */
	public ConfigPolygonDetector square = new ConfigPolygonDetector(4,true);

	/**
	 * Physical width of the square.
	 */
	public double squareWidth;

	/**
	 * Physical width of hte space between each square
	 */
	public double spaceWidth;

	{
		square.contour2Poly_splitDistanceFraction = 0.1;

		square.refineWithCorners = true;
		square.refineWithLines = false;
	}

	public ConfigSquareGrid(int numCols, int numRows, double squareWidth, double spaceWidth) {
		this.numCols = numCols;
		this.numRows = numRows;
		this.squareWidth = squareWidth;
		this.spaceWidth = spaceWidth;
	}

	public double getSpacetoSquareRatio() {
		return spaceWidth/squareWidth;
	}

	@Override
	public void checkValidity() {
		if( numCols <= 0 || numRows <= 0 )
			throw new IllegalArgumentException("Must specify then number of rows and columns in the target");
	}
}
