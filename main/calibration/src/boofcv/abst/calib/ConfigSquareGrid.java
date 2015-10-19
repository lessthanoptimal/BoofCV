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

import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.ConfigRefinePolygonCornersToImage;
import boofcv.factory.shape.ConfigRefinePolygonLineToImage;
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
	 * Configuration for thresholding the image
	 */
	public ConfigThreshold thresholding = ConfigThreshold.local(ThresholdType.LOCAL_SQUARE,20);

	/**
	 * Configuration for square detector
	 */
	public ConfigPolygonDetector square = new ConfigPolygonDetector(true, 4);

	/**
	 * Physical width of the square.
	 */
	public double squareWidth;

	/**
	 * Physical width of hte space between each square
	 */
	public double spaceWidth;

	/**
	 * Should it refine the corners only?  Useful if the input image is distorted
	 */
	public boolean refineWithCorners = true;

	/**
	 * Configuration for refining with lines.  Ignored if not used.
	 */
	public ConfigRefinePolygonLineToImage configRefineLines = new ConfigRefinePolygonLineToImage();

	/**
	 * Configuration for refining with corners.  Ignored if not used.
	 */
	public ConfigRefinePolygonCornersToImage configRefineCorners = new ConfigRefinePolygonCornersToImage();

	{
		square.contour2Poly_splitFraction = 0.25;
		square.contour2Poly_minimumSplitFraction = 0.0005;
		square.minContourImageWidthFraction = 0.0005;

		// since it runs a separate sub-pixel algorithm these parameters can be tuned to create
		// very crude corners
		configRefineCorners.cornerOffset = 0;
		configRefineCorners.lineSamples = 10;
		configRefineCorners.convergeTolPixels = 0.1;
		configRefineCorners.maxIterations = 10;

		// putting reasonable defaults for if the user decides to optimize by line
		configRefineLines.cornerOffset = 0;
		configRefineLines.lineSamples = 10;
		configRefineLines.convergeTolPixels = 0.1;
		configRefineLines.maxIterations = 10;
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
