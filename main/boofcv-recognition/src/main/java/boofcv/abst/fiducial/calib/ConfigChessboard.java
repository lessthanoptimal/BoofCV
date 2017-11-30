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

import boofcv.abst.shapes.polyline.ConfigPolylineSplitMerge;
import boofcv.alg.fiducial.calib.chess.DetectChessboardFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ConfigThresholdBlockMinMax;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.ConfigRefinePolygonLineToImage;
import boofcv.struct.ConfigLength;
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
	 * Number of squares tall the grid is. Target dependent.
	 */
	public int numRows = -1;

	/**
	 * Number of squares wide the grid is. Target dependent.
	 */
	public int numCols = -1;

	/**
	 * The maximum distance in pixels that two corners can be from each other.  In well focused image
	 * this number can be only a few pixels.  The default value has been selected to handle blurred images
	 */
	public double maximumCornerDistance = 8;

	/**
	 * Configuration for thresholding the image
	 */
	public ConfigThreshold thresholding = new ConfigThresholdBlockMinMax(10,35,true);

	/**
	 * Configuration for square detector
	 */
	public ConfigPolygonDetector square = new ConfigPolygonDetector(true, 3,8);

	/**
	 * Configuration for refining with lines.  Ignored if not used.
	 */
	public ConfigRefinePolygonLineToImage configRefineLines = new ConfigRefinePolygonLineToImage();

	/**
	 * Physical width of each square on the calibration target
	 */
	public double squareWidth;

	{
		// this is being used as a way to smooth out the binary image.  Speeds things up quite a bit
		thresholding.scale = 0.9;

		((ConfigPolylineSplitMerge)square.detector.contourToPoly).minSides = 3;
		((ConfigPolylineSplitMerge)square.detector.contourToPoly).cornerScorePenalty = 0.5;
		((ConfigPolylineSplitMerge)square.detector.contourToPoly).minimumSideLength = 2;
//		((ConfigPolylineSplitMerge)square.detector.contourToPoly).thresholdSideSplitScore = 0;
//		((ConfigPolylineSplitMerge)square.detector.contourToPoly).maxSideError = 1000;
//		((ConfigPolylineSplitMerge)square.detector.contourToPoly).convexTest = 1000;
		square.detector.tangentEdgeIntensity = 2.5; // the initial contour is the result of being eroded
		square.detector.minimumContour = ConfigLength.fixed(10);
		square.detector.canTouchBorder = true;

		// defaults for if the user toggles it to lines
		configRefineLines.cornerOffset = 1;
		configRefineLines.lineSamples = 15;
		configRefineLines.convergeTolPixels = 0.2;
		configRefineLines.maxIterations = 5;
	}

	public ConfigChessboard(int numRows, int numCols, double squareWidth) {
		this.numRows = numRows;
		this.numCols = numCols;
		this.squareWidth = squareWidth;
	}


	@Override
	public void checkValidity() {
		if( numCols <= 0 || numRows <= 0 )
			throw new IllegalArgumentException("Must specify then number of rows and columns in the target");
	}
}
