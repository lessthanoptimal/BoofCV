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

import boofcv.alg.feature.detect.chess.DetectChessboardCorners;
import boofcv.alg.fiducial.calib.chess.DetectChessboardFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.struct.Configuration;

/**
 * Calibration parameters for chessboard style calibration grid.
 *
 * @see DetectChessboardFiducial
 *
 * @author Peter Abeles
 */
public class ConfigChessboard2 implements Configuration {

	/**
	 * Number of squares tall the grid is. Target dependent.
	 */
	public int numRows = -1;

	/**
	 * Number of squares wide the grid is. Target dependent.
	 */
	public int numCols = -1;


	public int cornerRadius = 1;

	/**
	 * Threshold on corner intensity
	 */
	public double cornerThreshold = 1.0;

	public int pyramidTopSize = 100;

	public ConfigThreshold threshold = ConfigThreshold.global(ThresholdType.GLOBAL_OTSU);

	/**
	 * Physical width of each square on the calibration target
	 */
	public double squareWidth;

	{
		threshold.maxPixelValue = DetectChessboardCorners.GRAY_LEVELS;
		threshold.scale = 0.9;
		threshold.down = false;
	}

	public ConfigChessboard2(int numRows, int numCols, double squareWidth) {
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
