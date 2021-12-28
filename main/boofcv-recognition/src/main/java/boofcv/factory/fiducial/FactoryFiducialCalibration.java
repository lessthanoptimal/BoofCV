/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.abst.geo.calibration.MultiToSingleFiducialCalibration;
import boofcv.alg.fiducial.calib.ConfigCalibrationTarget;
import boofcv.alg.fiducial.calib.chess.DetectChessboardBinaryPattern;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckDetector;
import boofcv.struct.image.GrayF32;
import org.jetbrains.annotations.Nullable;

/**
 * Creates detectors of calibration targets. These detectors return found key points in the image and
 * their known 2D location on the fiducial.
 *
 * @author Peter Abeles
 */
public class FactoryFiducialCalibration {

	public static DetectSingleFiducialCalibration genericSingle( ConfigCalibrationTarget config ) {
		return switch (config.type) {
			case CHESSBOARD -> FactoryFiducialCalibration.chessboardX(null, config.grid);
			case ECOCHECK -> new MultiToSingleFiducialCalibration(FactoryFiducialCalibration.ecocheck(null, config.ecocheck));
			case SQUARE_GRID -> FactoryFiducialCalibration.squareGrid(null, config.grid);
			case CIRCLE_GRID -> FactoryFiducialCalibration.circleRegularGrid(null, config.grid);
			case CIRCLE_HEXAGONAL -> FactoryFiducialCalibration.circleHexagonalGrid(null, config.grid);
			default -> throw new RuntimeException("Target type not yet supported.");
		};
	}

	/**
	 * Detector for a grid of square targets. All squares must be entirely visible inside the image.
	 *
	 * @param config Configuration for chessboard detector
	 * @return Square grid target detector.
	 * @see boofcv.alg.fiducial.calib.grid.DetectSquareGridFiducial
	 */
	public static CalibrationDetectorSquareGrid squareGrid( @Nullable ConfigSquareGrid config, ConfigGridDimen configDimen ) {
		if (config == null)
			config = new ConfigSquareGrid();
		config.checkValidity();

		return new CalibrationDetectorSquareGrid(config, configDimen);
	}

	/**
	 * Chessboard detector based on binary images. Fast but not as robust as the X-Corner method. Not recommended
	 * for fisheye images.
	 *
	 * @param configDet Configuration for chessboard detector
	 * @return Square grid target detector.
	 * @see DetectChessboardBinaryPattern
	 */
	public static CalibrationDetectorChessboardBinary chessboardB( @Nullable ConfigChessboardBinary configDet,
																   ConfigGridDimen configGrid ) {
		if (configDet == null)
			configDet = new ConfigChessboardBinary();
		configDet.checkValidity();

		return new CalibrationDetectorChessboardBinary(configDet, configGrid);
	}

	/**
	 * Chessboard detector which searches for x-corners. Very robust but is about 2x to 3x slower on large images
	 * than the binary method. Comparable speed on smaller images.
	 *
	 * @param config Configuration for chessboard detector
	 * @return Square grid target detector.
	 * @see CalibrationDetectorChessboardX
	 */
	public static CalibrationDetectorChessboardX chessboardX( @Nullable ConfigChessboardX config,
															  ConfigGridDimen dimen ) {
		if (config == null)
			config = new ConfigChessboardX();
		config.checkValidity();

		return new CalibrationDetectorChessboardX(config, dimen);
	}

	/**
	 * Chessboard detector which searches for x-corners. Very robust but is about 2x to 3x slower on large images
	 * than the binary method. Comparable speed on smaller images.
	 *
	 * @param configDetector Configuration for chessboard detector
	 * @param configMarkers Configuration for what markers it should search for
	 * @return Square grid target detector.
	 * @see CalibrationDetectorChessboardX
	 */
	public static CalibrationDetectorMultiECoCheck ecocheck( @Nullable ConfigECoCheckDetector configDetector,
															 ConfigECoCheckMarkers configMarkers ) {
		ECoCheckDetector<GrayF32> detector = FactoryFiducial.ecocheck(
				configDetector, configMarkers, GrayF32.class).getDetector();

		return new CalibrationDetectorMultiECoCheck(detector, configMarkers.markerShapes.toList());
	}

	/**
	 * Detector for hexagonal grid of circles. All circles must be entirely inside of the image.
	 *
	 * @param config Configuration for target
	 * @return The detector
	 */
	public static CalibrationDetectorCircleHexagonalGrid circleHexagonalGrid( @Nullable ConfigCircleHexagonalGrid config,
																			  ConfigGridDimen configGrid ) {
		if (config == null)
			config = new ConfigCircleHexagonalGrid();
		config.checkValidity();

		return new CalibrationDetectorCircleHexagonalGrid(config, configGrid);
	}

	/**
	 * Detector for regular grid of circles. All circles must be entirely inside of the image.
	 *
	 * @param config Configuration for target
	 * @return The detector
	 */
	public static CalibrationDetectorCircleRegularGrid circleRegularGrid( @Nullable ConfigCircleRegularGrid config,
																		  ConfigGridDimen configGrid ) {
		if (config == null)
			config = new ConfigCircleRegularGrid();
		config.checkValidity();

		return new CalibrationDetectorCircleRegularGrid(config, configGrid);
	}
}
