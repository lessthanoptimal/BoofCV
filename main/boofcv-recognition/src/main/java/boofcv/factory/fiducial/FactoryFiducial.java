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

import boofcv.abst.fiducial.*;
import boofcv.abst.fiducial.calib.*;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.feature.describe.llah.LlahHasher;
import boofcv.alg.feature.describe.llah.LlahOperations;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckDetector;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckUtils;
import boofcv.alg.fiducial.dots.UchiyaMarkerImageTracker;
import boofcv.alg.fiducial.dots.UchiyaMarkerTracker;
import boofcv.alg.fiducial.qrcode.QrCodePositionPatternDetector;
import boofcv.alg.fiducial.square.DetectFiducialSquareBinary;
import boofcv.alg.fiducial.square.DetectFiducialSquareHamming;
import boofcv.alg.fiducial.square.DetectFiducialSquareImage;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetectorPixel;
import boofcv.alg.shapes.ellipse.EdgeIntensityEllipse;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.factory.geo.ConfigHomography;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.homography.Homography2D_F64;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating fiducial detectors which implement {@link FiducialDetector}.
 *
 * @author Peter Abeles
 */
public class FactoryFiducial {

	/**
	 * Detector for square binary based fiducials.
	 *
	 * @param configFiducial Description of the fiducial. Can't be null.
	 * @param configThreshold Threshold for binary image. null for default.
	 * @param imageType Type of image it's processing
	 * @return FiducialDetector
	 * @see DetectFiducialSquareBinary DetectFiducialSquareBinary for a description of this fiducial type.
	 */
	public static <T extends ImageGray<T>>
	SquareBinary_to_FiducialDetector<T> squareBinary( ConfigFiducialBinary configFiducial,
													  @Nullable ConfigThreshold configThreshold,
													  Class<T> imageType ) {

		if (configThreshold == null) {
			configThreshold = ConfigThreshold.local(ThresholdType.LOCAL_MEAN, 21);
		}

		configFiducial.checkValidity();

		final InputToBinary<T> binary = FactoryThresholdBinary.threshold(configThreshold, imageType);
		final DetectPolygonBinaryGrayRefine<T> squareDetector = FactoryShapeDetector.
				polygon(configFiducial.squareDetector, imageType);

		final DetectFiducialSquareBinary<T> alg =
				new DetectFiducialSquareBinary<>(configFiducial.gridWidth,
						configFiducial.borderWidthFraction, configFiducial.minimumBlackBorderFraction,
						binary, squareDetector, imageType);
		alg.setAmbiguityThreshold(configFiducial.ambiguousThreshold);
		return new SquareBinary_to_FiducialDetector<>(alg, configFiducial.targetWidth);
	}

	/**
	 * Detector for square binary based fiducials.
	 *
	 * @param configMarkers Describes the markers it will detect.
	 * @param configDetector (Optional) Describes how it will detect the markers.
	 * @param imageType Type of image it's processing
	 * @return FiducialDetector
	 * @see DetectFiducialSquareBinary DetectFiducialSquareBinary for a description of this fiducial type.
	 */
	public static <T extends ImageGray<T>>
	SquareHamming_to_FiducialDetector<T> squareHamming( ConfigHammingMarker configMarkers,
														@Nullable ConfigFiducialHammingDetector configDetector,
														Class<T> imageType ) {
		if (configDetector == null) {
			configDetector = new ConfigFiducialHammingDetector();
		}
		configMarkers.checkValidity();
		configDetector.checkValidity();

		final InputToBinary<T> binary = FactoryThresholdBinary.threshold(configDetector.configThreshold, imageType);
		final DetectPolygonBinaryGrayRefine<T> squareDetector = FactoryShapeDetector.
				polygon(configDetector.squareDetector, imageType);

		final var alg = new DetectFiducialSquareHamming<>(configMarkers, configDetector.minimumBlackBorderFraction,
				binary, squareDetector, imageType);
		alg.setAmbiguousPenaltyFrac(configDetector.ambiguousPenaltyFrac);
		return new SquareHamming_to_FiducialDetector<>(alg);
	}

	/**
	 * <p>Detector for square image based fiducials. </p>
	 *
	 * <p>For this fiducial to work images need to be added to it. Which is why {@link SquareImage_to_FiducialDetector}
	 * is returned instead of the more generic {@link FiducialDetector}.</p>
	 *
	 * @param configFiducial Description of the fiducial. null for default.
	 * @param configThreshold Threshold for binary image. null for default.
	 * @param imageType Type of image it's processing
	 * @return FiducialDetector
	 * @see DetectFiducialSquareImage DetectFiducialSquareImage for a description of this fiducial type.
	 */
	public static <T extends ImageGray<T>>
	SquareImage_to_FiducialDetector<T> squareImage( @Nullable ConfigFiducialImage configFiducial,
													@Nullable ConfigThreshold configThreshold,
													Class<T> imageType ) {
		if (configFiducial == null) {
			configFiducial = new ConfigFiducialImage();
		}

		if (configThreshold == null) {
			configThreshold = ConfigThreshold.local(ThresholdType.LOCAL_MEAN, 21);
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
	 * Chessboard detector based on binary images. Fast but not as robust as the X-Corner method. Not recommended
	 * for fisheye images.
	 *
	 * @param config Description of the chessboard.
	 * @param imageType Type of image it's processing
	 * @return FiducialDetector
	 */
	public static <T extends ImageGray<T>>
	CalibrationFiducialDetector<T> calibChessboardB( @Nullable ConfigChessboardBinary config,
													 ConfigGridDimen dimen, Class<T> imageType ) {
		return new CalibrationFiducialDetector<>(config, dimen, imageType);
	}

	/**
	 * Chessboard detector which searches for x-corners. Very robust but is about 2x to 3x slower on large images
	 * than the binary method.
	 *
	 * @param config Description of the chessboard.
	 * @param imageType Type of image it's processing
	 * @return FiducialDetector
	 */
	public static <T extends ImageGray<T>>
	CalibrationFiducialDetector<T> calibChessboardX( @Nullable ConfigChessboardX config,
													 ConfigGridDimen dimen,
													 Class<T> imageType ) {
		return new CalibrationFiducialDetector<>(config, dimen, imageType);
	}

	/**
	 * Wrapper around square-grid calibration detector. Refine with lines is set to true automatically. This
	 * isn't being used for calibration, and it's better to use the whole line.
	 *
	 * @param config Description of the chessboard.
	 * @param imageType Type of image it's processing
	 * @return FiducialDetector
	 */
	public static <T extends ImageGray<T>>
	CalibrationFiducialDetector<T> calibSquareGrid( @Nullable ConfigSquareGrid config,
													ConfigGridDimen configDimen, Class<T> imageType ) {
		return new CalibrationFiducialDetector<>(config, configDimen, imageType);
	}

	public static <T extends ImageGray<T>>
	CalibrationFiducialDetector<T> calibCircleHexagonalGrid( @Nullable ConfigCircleHexagonalGrid config,
															 ConfigGridDimen configDimen, Class<T> imageType ) {
		return new CalibrationFiducialDetector<>(config, configDimen, imageType);
	}

	public static <T extends ImageGray<T>>
	CalibrationFiducialDetector<T> calibCircleRegularGrid( @Nullable ConfigCircleRegularGrid config,
														   ConfigGridDimen configDimen, Class<T> imageType ) {
		return new CalibrationFiducialDetector<>(config, configDimen, imageType);
	}

	/**
	 * Returns a QR Code detector
	 *
	 * @param config Configuration
	 * @param imageType type of input image
	 * @return the detector
	 */
	public static <T extends ImageGray<T>>
	QrCodePreciseDetector<T> qrcode( @Nullable ConfigQrCode config, Class<T> imageType ) {
		if (config == null)
			config = new ConfigQrCode();

		config.checkValidity();

		InputToBinary<T> inputToBinary = FactoryThresholdBinary.threshold(config.threshold, imageType);

		DetectPolygonBinaryGrayRefine<T> squareDetector = FactoryShapeDetector.polygon(config.polygon, imageType);
		var detectPositionPatterns = new QrCodePositionPatternDetector<>(squareDetector, config.versionMaximum);

		var detector = new QrCodePreciseDetector<>(inputToBinary, detectPositionPatterns, config.forceEncoding, false, imageType);
		detector.getDecoder().considerTransposed = config.considerTransposed;
		return detector;
	}

	/**
	 * Creates a new {@link ECoCheckDetector}. This will detect chessboard patterns that have been
	 * encoded with a marker ID and coordinates of every corner.
	 *
	 * @param configDetector Configuration for chessboard detector
	 * @param configMarkers Configuration for what markers it should search for
	 * @return New detector
	 */
	public static <T extends ImageGray<T>>
	ECoCheck_to_FiducialDetector<T> ecocheck( @Nullable ConfigECoCheckDetector configDetector,
											  ConfigECoCheckMarkers configMarkers, Class<T> imageType ) {
		if (configDetector == null)
			configDetector = new ConfigECoCheckDetector();
		configDetector.checkValidity();
		configMarkers.checkValidity();

		// Set up the utils, which configures how the targets will be encoded
		var utils = new ECoCheckUtils();
		utils.setParametersFromConfig(configMarkers);
		utils.fixate();

		var alg = new ECoCheckDetector<>(utils, configDetector.chessboard, imageType);
		return new ECoCheck_to_FiducialDetector<>(alg, configMarkers.markerShapes);
	}

	/**
	 * QR Code but with the ability to estimate it's 3D pose using PnP. Implements {@link FiducialDetector}.
	 */
	public static <T extends ImageGray<T>>
	QrCodeDetectorPnP<T> qrcode3D( @Nullable ConfigQrCode config, Class<T> imageType ) {
		return new QrCodeDetectorPnP<>(qrcode(config, imageType));
	}

	/**
	 * Creates detector for random dot markers using Uchiya Marker approach.
	 *
	 * @param config Specifies the configuration. If null then default is used
	 * @param imageType Type of input gray scale image
	 * @return The fiducial detector
	 * @see UchiyaMarkerImageTracker
	 */
	public static <T extends ImageGray<T>>
	Uchiya_to_FiducialDetector<T> randomDots( ConfigUchiyaMarker config, Class<T> imageType ) {
		config.checkValidity();

		var ellipseDetector = new BinaryEllipseDetectorPixel(config.contourRule);
		ellipseDetector.setMaxDistanceFromEllipse(config.maxDistanceFromEllipse);
		ellipseDetector.setMinimumMinorAxis(config.minimumMinorAxis);
		ellipseDetector.setMaxMajorToMinorRatio(config.maxMajorToMinorRatio);
		ellipseDetector.setMinimumContour(config.contourMinimumLength);

		EdgeIntensityEllipse<T> check = new EdgeIntensityEllipse<>(
				config.checkEdge.checkRadialDistance,
				config.checkEdge.numSampleContour,
				config.checkEdge.minimumEdgeIntensity, imageType);

		InputToBinary<T> inputToBinary = FactoryThresholdBinary.threshold(config.threshold, imageType);

		ConfigLlah llah = config.llah;

		LlahHasher hasher = switch (config.llah.hashType) {
			case AFFINE -> new LlahHasher.Affine(llah.quantizationK, llah.hashTableSize);
			case CROSS_RATIO -> new LlahHasher.CrossRatio(llah.quantizationK, llah.hashTableSize);
		};

		LlahOperations ops = new LlahOperations(config.llah.numberOfNeighborsN, config.llah.sizeOfCombinationM, hasher);
		Ransac<Homography2D_F64, AssociatedPair> ransac =
				FactoryMultiViewRobust.homographyRansac(new ConfigHomography(false), config.ransac);
		UchiyaMarkerTracker uchiya = new UchiyaMarkerTracker(ops, ransac);

		UchiyaMarkerImageTracker<T> tracker = new UchiyaMarkerImageTracker<>(inputToBinary, ellipseDetector, check, uchiya);

		return new Uchiya_to_FiducialDetector<T>(tracker, config.markerWidth, config.markerHeight, ImageType.single(imageType));
	}
}
