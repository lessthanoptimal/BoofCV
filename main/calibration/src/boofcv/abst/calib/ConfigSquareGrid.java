/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.Configuration;

/**
 * Calibration parameters for square-grid style calibration grid.
 *
 * @see boofcv.alg.feature.detect.grid.DetectSquareCalibrationPoints
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
	 * Increases or decreases the minimum allowed blob size. Try 1.0
	 */
	public double relativeSizeThreshold = 1;
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
	 * Length of the space relative to the length of a square in the grid
	 */
	public double spaceToSquareRatio = 1.0;

	public ConfigSquareGrid(int numCols, int numRows ) {
		this.numCols = numCols;
		this.numRows = numRows;
	}

	public ConfigSquareGrid(int numCols, int numRows , double spaceToSquareRatio ) {
		this.numCols = numCols;
		this.numRows = numRows;
		this.spaceToSquareRatio = spaceToSquareRatio;
	}

	public ConfigSquareGrid(int numCols, int numRows, double spaceToSquareRatio ,
							double relativeSizeThreshold) {
		this.numCols = numCols;
		this.numRows = numRows;
		this.spaceToSquareRatio = spaceToSquareRatio;
		this.relativeSizeThreshold = relativeSizeThreshold;
	}

	@Override
	public void checkValidity() {
		if( numCols <= 0 || numRows <= 0 )
			throw new IllegalArgumentException("Must specify then number of rows and columns in the target");
	}
}
