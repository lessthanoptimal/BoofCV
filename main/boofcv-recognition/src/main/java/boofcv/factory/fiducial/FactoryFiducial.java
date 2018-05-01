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

package boofcv.factory.fiducial;

import boofcv.abst.fiducial.*;
import boofcv.abst.fiducial.calib.*;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.fiducial.qrcode.QrCodePositionPatternDetector;
import boofcv.alg.fiducial.square.DetectFiducialSquareBinary;
import boofcv.alg.fiducial.square.DetectFiducialSquareImage;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.image.ImageGray;

/**
 * Factory for creating fiducial detectors which implement {@link FiducialDetector}.
 *
 * @author Peter Abeles
 */
public class FactoryFiducial {

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
	public static <T extends ImageGray<T>>
	SquareBinary_to_FiducialDetector<T> squareBinary( ConfigFiducialBinary configFiducial,
													  ConfigThreshold configThreshold,
													  Class<T> imageType ) {

		if( configThreshold == null ) {
			configThreshold = ConfigThreshold.local(ThresholdType.LOCAL_MEAN,21);
		}

		configFiducial.checkValidity();

		final InputToBinary<T> binary = FactoryThresholdBinary.threshold(configThreshold, imageType);
		final DetectPolygonBinaryGrayRefine<T> squareDetector = FactoryShapeDetector.
				polygon(configFiducial.squareDetector,imageType);

		final DetectFiducialSquareBinary<T> alg =
				new DetectFiducialSquareBinary<>(configFiducial.gridWidth,
						configFiducial.borderWidthFraction, configFiducial.minimumBlackBorderFraction,
						binary, squareDetector, imageType);
		alg.setAmbiguityThreshold(configFiducial.ambiguousThreshold);
		return new SquareBinary_to_FiducialDetector<>(alg, configFiducial.targetWidth);
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
	public static  <T extends ImageGray<T>>
	SquareImage_to_FiducialDetector<T> squareImage( ConfigFiducialImage configFiducial,
													ConfigThreshold configThreshold,
													Class<T> imageType ) {

		if( configThreshold == null ) {
			configThreshold = ConfigThreshold.local(ThresholdType.LOCAL_MEAN,21);
		}

		configFiducial.squareDetector.detector.clockwise = false;

		InputToBinary<T> binary = FactoryThresholdBinary.threshold(configThreshold, imageType);
		DetectPolygonBinaryGrayRefine<T> squareDetector =
				FactoryShapeDetector.polygon(configFiducial.squareDetector, imageType);
		DetectFiducialSquareImage<T> alg = new DetectFiducialSquareImage<>(binary,
				squareDetector, configFiducial.borderWidthFraction, configFiducial.minimumBlackBorderFraction,
				configFiducial.maxErrorFraction, imageType);

		return new SquareImage_to_FiducialDetector<>(alg);
	}

	/**
	 * Wrapper around chessboard calibration detector.   Refine with lines is set to true automatically.  This
	 * isn't being used for calibration and its better to use the whole line.
	 *
	 * @param config Description of the chessboard.
	 * @param imageType Type of image it's processing
	 * @return FiducialDetector
	 */
	public static <T extends ImageGray<T>>
	CalibrationFiducialDetector<T> calibChessboard( ConfigChessboard config, Class<T> imageType) {
		return new CalibrationFiducialDetector<>(config, imageType);
	}

	/**
	 * Wrapper around square-grid calibration detector.  Refine with lines is set to true automatically.  This
	 * isn't being used for calibration and its better to use the whole line.
	 *
	 * @param config Description of the chessboard.
	 * @param imageType Type of image it's processing
	 * @return FiducialDetector
	 */
	public static <T extends ImageGray<T>>
	CalibrationFiducialDetector<T> calibSquareGrid( ConfigSquareGrid config, Class<T> imageType) {
		return new CalibrationFiducialDetector<>(config, imageType);
	}

	public static <T extends ImageGray<T>>
	CalibrationFiducialDetector<T> calibSquareGridBinary( ConfigSquareGridBinary config, Class<T> imageType) {
		return new CalibrationFiducialDetector<>(config, imageType);
	}

	public static <T extends ImageGray<T>>
	CalibrationFiducialDetector<T> calibCircleHexagonalGrid(ConfigCircleHexagonalGrid config, Class<T> imageType) {
		return new CalibrationFiducialDetector<>(config, imageType);
	}

	public static <T extends ImageGray<T>>
	CalibrationFiducialDetector<T> calibCircleRegularGrid(ConfigCircleRegularGrid config, Class<T> imageType) {
		return new CalibrationFiducialDetector<>(config, imageType);
	}

	public static <T extends ImageGray<T>>
	QrCodePreciseDetector<T> qrcode(ConfigQrCode config, Class<T> imageType) {
		if( config == null )
			config = new ConfigQrCode();

		config.checkValidity();

		InputToBinary<T> inputToBinary = FactoryThresholdBinary.threshold(config.threshold,imageType);

		DetectPolygonBinaryGrayRefine<T> squareDetector = FactoryShapeDetector.polygon(config.polygon, imageType);
		QrCodePositionPatternDetector<T> detectPositionPatterns =
				new QrCodePositionPatternDetector<>(squareDetector,config.versionMaximum);

		return new QrCodePreciseDetector<>(inputToBinary,detectPositionPatterns, false, imageType);
	}

}
