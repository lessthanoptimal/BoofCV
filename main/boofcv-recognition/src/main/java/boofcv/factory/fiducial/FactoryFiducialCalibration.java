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

package boofcv.factory.fiducial;

import boofcv.abst.fiducial.calib.*;
import boofcv.alg.fiducial.calib.chess.DetectChessboardFiducial;

import javax.annotation.Nullable;

/**
 * Creates detectors of calibration targets.  These detectors return found key points in the image and
 * their known 2D location on the fiducial.
 *
 * @author Peter Abeles
 */
public class FactoryFiducialCalibration {

	/**
	 * Detector for a grid of square targets.   All squares must be entirely visible inside the image.
	 *
	 * @see boofcv.alg.fiducial.calib.grid.DetectSquareGridFiducial
	 *
	 * @param config Configuration for chessboard detector
	 * @return Square grid target detector.
	 */
	public static CalibrationDetectorSquareGrid squareGrid(@Nullable ConfigSquareGrid config, ConfigGridDimen configDimen) {
		if( config == null )
			config = new ConfigSquareGrid();
		config.checkValidity();

		return new CalibrationDetectorSquareGrid(config,configDimen);
	}

	/**
	 * Detector for chessboard targets.  Squares can be partially outside, but inside corners must be entirely
	 * inside the image.
	 *
	 * @see DetectChessboardFiducial
	 *
	 * @param configDet Configuration for chessboard detector
	 * @return Square grid target detector.
	 */
	@Deprecated
	public static CalibrationDetectorChessboard chessboard(@Nullable ConfigChessboard configDet ,
														   ConfigGridDimen configGrid ) {
		if( configDet == null )
			configDet = new ConfigChessboard();
		configDet.checkValidity();

		return new CalibrationDetectorChessboard(configDet,configGrid);
	}

	/**
	 * Detector for chessboard targets.  Squares can be partially outside, but inside corners must be entirely
	 * inside the image.
	 *
	 * @see DetectChessboardFiducial
	 *
	 * @param config Configuration for chessboard detector
	 * @return Square grid target detector.
	 */
	public static CalibrationDetectorChessboard2 chessboard2(@Nullable ConfigChessboard2 config , ConfigGridDimen dimen ) {
		if( config == null )
			config = new ConfigChessboard2();
		config.checkValidity();

		return new CalibrationDetectorChessboard2(config,dimen);
	}

	/**
	 * Detector for a grid of binary targets.  Allows for squares to be obscured or partially outside of the
	 * image.
	 *
	 * @see CalibrationDetectorSquareFiducialGrid
	 *
	 * @param config Configuration of binary target
	 * @return Detector for binary grid target
	 */
	public static CalibrationDetectorSquareFiducialGrid binaryGrid(ConfigSquareGridBinary config ) {
		config.checkValidity();

		return new CalibrationDetectorSquareFiducialGrid(config);
	}

	/**
	 * Detector for hexagonal grid of circles.  All circles must be entirely inside of the image.
	 *
	 * @param config Configuration for target
	 * @return The detector
	 */
	public static CalibrationDetectorCircleHexagonalGrid circleHexagonalGrid( @Nullable ConfigCircleHexagonalGrid config ,
																			 ConfigGridDimen configGrid ) {
		if( config == null )
			config = new ConfigCircleHexagonalGrid();
		config.checkValidity();

		return new CalibrationDetectorCircleHexagonalGrid(config,configGrid);
	}

	/**
	 * Detector for regular grid of circles.  All circles must be entirely inside of the image.
	 *
	 * @param config Configuration for target
	 * @return The detector
	 */
	public static CalibrationDetectorCircleRegularGrid circleRegularGrid( @Nullable ConfigCircleRegularGrid config ,
																		  ConfigGridDimen configGrid ) {
		if( config == null )
			config = new ConfigCircleRegularGrid();
		config.checkValidity();

		return new CalibrationDetectorCircleRegularGrid(config,configGrid);
	}
}
