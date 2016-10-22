/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.fiducial.*;
import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.abst.fiducial.calib.ConfigSquareGrid;
import boofcv.abst.fiducial.calib.ConfigSquareGridBinary;
import boofcv.alg.fiducial.square.DetectFiducialSquareBinary;
import boofcv.alg.fiducial.square.DetectFiducialSquareImage;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.struct.image.ImageGray;

/**
 * Factory for creating fiducial detectors which implement {@link FiducialDetector}.
 *
 * @author Peter Abeles
 */
public class FactoryFiducial3D {

	/**
	 * Detector for square binary based fiducials.
	 *
	 * @see DetectFiducialSquareBinary DetectFiducialSquareBinary for a description of this fiducial type.
	 *
	 * @param configFiducial Description of the fiducial.  Can't be null.
	 * @param configThreshold Threshold for binary image.  null for default.
	 * @param imageType Type of image it's processing
	 * @return FiducialDetector
	 */
	public static <T extends ImageGray>
	SquareBinary_to_FiducialDetector<T> squareBinary( ConfigFiducialBinary configFiducial,
													  ConfigThreshold configThreshold,
													  Class<T> imageType ) {

		return FactoryFiducial.squareBinary(configFiducial, configThreshold, imageType);
	}

	/**
	 * <p>Detector for square image based fiducials. </p>
	 *
	 * <p>For this fiducial to work images need to be added to it.  Which is why {@link SquareImage_to_FiducialDetector}
	 * is returned instead of the more generic {@link FiducialDetector}.</p>
	 *
	 * @see DetectFiducialSquareImage DetectFiducialSquareImage for a description of this fiducial type.
	 *
	 * @param configFiducial Description of the fiducial.  Can't be null.
	 * @param configThreshold Threshold for binary image. null for default.
	 * @param imageType Type of image it's processing
	 * @return FiducialDetector
	 */
	public static  <T extends ImageGray>
	SquareImage_to_FiducialDetector<T> squareImage( ConfigFiducialImage configFiducial,
													ConfigThreshold configThreshold,
													Class<T> imageType ) {

		return FactoryFiducial.squareImage(configFiducial, configThreshold, imageType);
	}

	/**
	 * Wrapper around chessboard calibration detector.   Refine with lines is set to true automatically.  This
	 * isn't being used for calibration and its better to use the whole line.
	 *
	 * @param config Description of the chessboard.
	 * @param imageType Type of image it's processing
	 * @return FiducialDetector
	 */
	public static <T extends ImageGray>
	CalibrationFiducialDetector3D<T> calibChessboard( ConfigChessboard config, Class<T> imageType) {

		CalibrationFiducialDetector<T> alg = FactoryFiducial.calibChessboard(config, imageType);

		return new CalibrationFiducialDetector3D<>(alg);
	}

	/**
	 * Wrapper around square-grid calibration detector.  Refine with lines is set to true automatically.  This
	 * isn't being used for calibration and its better to use the whole line.
	 *
	 * @param config Description of the chessboard.
	 * @param imageType Type of image it's processing
	 * @return FiducialDetector
	 */
	public static <T extends ImageGray>
	CalibrationFiducialDetector3D<T> calibSquareGrid( ConfigSquareGrid config, Class<T> imageType) {

		CalibrationFiducialDetector<T> alg = FactoryFiducial.calibSquareGrid(config, imageType);

		return new CalibrationFiducialDetector3D<>(alg);
	}

	public static <T extends ImageGray>
	CalibrationFiducialDetector3D<T> calibSquareGridBinary( ConfigSquareGridBinary config, Class<T> imageType) {

		CalibrationFiducialDetector<T> alg = new CalibrationFiducialDetector(config, imageType);

		return new CalibrationFiducialDetector3D<>(alg);
	}
}
