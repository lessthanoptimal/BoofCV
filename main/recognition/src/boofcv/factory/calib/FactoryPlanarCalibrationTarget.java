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

package boofcv.factory.calib;

import boofcv.abst.fiducial.calib.*;
import boofcv.alg.fiducial.calib.chess.DetectChessboardFiducial;

/**
 * Creates descriptions of commonly used calibration targets
 *
 * @author Peter Abeles
 */
public class FactoryPlanarCalibrationTarget {

	/**
	 * Creates a calibration target detector for square grid targets.
	 *
	 * @see boofcv.alg.fiducial.calib.grid.DetectSquareGridFiducial
	 *
	 * @param config Configuration for chessboard detector
	 * @return Square grid target detector.
	 */
	public static PlanarDetectorSquareGrid detectorSquareGrid(ConfigSquareGrid config) {
		config.checkValidity();

		return new PlanarDetectorSquareGrid(config);
	}

	/**
	 * Creates a calibration target detector for chessboard targets.  Adjust the feature radius
	 * for best performance.
	 *
	 * @see DetectChessboardFiducial
	 *
	 * @param config Configuration for chessboard detector
	 * @return Square grid target detector.
	 */
	public static PlanarDetectorChessboard detectorChessboard( ConfigChessboard config ) {
		config.checkValidity();

		return new PlanarDetectorChessboard(config);
	}

	/**
	 * Creates a fiducial that is a grid composed of square binary fiducials.
	 *
	 * @see CalibrationDetectorBinaryGrid
	 *
	 * @param config
	 * @return
	 */
	public static CalibrationDetectorBinaryGrid detectorBinaryGrid( ConfigSquareGridBinary config ) {
		config.checkValidity();

		return new CalibrationDetectorBinaryGrid(config);
	}
}
