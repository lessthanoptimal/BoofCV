/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.shapes.polyline.ConfigPolylineSplitMerge;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;

/**
 * Calibration parameters for square-grid style calibration grid.
 *
 * @see boofcv.alg.fiducial.calib.grid.DetectSquareGridFiducial
 *
 * @author Peter Abeles
 */
public class ConfigSquareGrid implements Configuration {

	/**
	 * Number of black squares tall the grid is. Target dependent.
	 */
	public int numRows = -1;

	/**
	 * Number of black squares wide the grid is. Target dependent.
	 */
	public int numCols = -1;

	/**
	 * Configuration for thresholding the image
	 */
	public ConfigThreshold thresholding = ConfigThreshold.local(ThresholdType.BLOCK_MEAN,ConfigLength.relative(0.02,5));

	/**
	 * Configuration for square detector
	 *
	 * NOTE: Number of sides, clockwise, and convex are all set by the detector in its consturctor. Values
	 * specified here are ignored.
	 */
	public ConfigPolygonDetector square = new ConfigPolygonDetector();

	/**
	 * Physical width of the square.
	 */
	public double squareWidth;

	/**
	 * Physical width of the space between each square
	 */
	public double spaceWidth;

	{
		// this is being used as a way to smooth out the binary image.  Speeds things up quite a bit
		thresholding.scale = 0.85;

		((ConfigPolylineSplitMerge)square.detector.contourToPoly).cornerScorePenalty = 0.5;
		square.detector.minimumContour = ConfigLength.fixed(10);

		square.refineGray.cornerOffset = 1;
		square.refineGray.lineSamples = 15;
		square.refineGray.convergeTolPixels = 0.2;
		square.refineGray.maxIterations = 10;
	}

	public ConfigSquareGrid(int numRows, int numCols, double squareWidth, double spaceWidth) {
		this.numRows = numRows;
		this.numCols = numCols;
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
