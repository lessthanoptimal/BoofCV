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

package boofcv.factory.feature.detect.line;

import boofcv.abst.feature.detect.line.*;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.line.HoughTransformBinary;
import boofcv.alg.feature.detect.line.HoughTransformGradient;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating high level implementations of {@link DetectLine} and {@link DetectLineSegment}. For more access
 * to internal data structures see {@link FactoryDetectLineAlgs}.
 *
 * @author Peter Abeles
 */
public class FactoryDetectLine {

	/**
	 * Detects line segments inside an image using the {@link DetectLineSegmentsGridRansac} algorithm.
	 *
	 * @param config Configuration for line detector
	 * @param imageType Type of single band input image.
	 * @return Line segment detector
	 * @see DetectLineSegmentsGridRansac
	 */
	public static <I extends ImageGray<I>>
	DetectLineSegment<I> lineRansac( @Nullable ConfigLineRansac config,
									 Class<I> imageType ) {

		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		return FactoryDetectLineAlgs.lineRansac(config, imageType, derivType);
	}

	/**
	 * Detects lines using a foot of norm parametrization and sub images to reduce degenerate
	 * configurations, see {@link DetectLineHoughFootSubimage} for details.
	 *
	 * @param config Configuration for line detector. If null then default will be used.
	 * @param imageType input image type
	 * @return Line detector.
	 * @see DetectLineHoughFootSubimage
	 */
	public static <T extends ImageGray<T>>
	DetectLine<T> houghLineFootSub( @Nullable ConfigHoughFootSubimage config, Class<T> imageType ) {

		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		DetectEdgeLines alg = FactoryDetectLineAlgs.houghFootSub(config, derivType);

		return new DetectEdgeLinesToLines(alg, imageType, derivType);
	}

	/**
	 * Used to find edges along the side of objects. Image gradient is found and thresholded. The pixel coordinate
	 * of each active pixel and its gradient is then used to estimate the parameter of a line.
	 * Uses {@link HoughTransformGradient} with {@link boofcv.alg.feature.detect.line.HoughParametersFootOfNorm}
	 *
	 * @param configHough Configuration for hough binary transform.
	 * @param configParam Configuration for polar parameters
	 * @param imageType type of input image
	 * @return Line detector.
	 */
	public static <T extends ImageGray<T>>
	DetectLine<T> houghLineFoot( @Nullable ConfigHoughGradient configHough,
								 @Nullable ConfigParamFoot configParam,
								 Class<T> imageType ) {
		if (configHough == null)
			configHough = new ConfigHoughGradient();

		if (configParam == null)
			configParam = new ConfigParamFoot();

		ImageGradient<T, ?> gradient = FactoryDerivative.gradient(
				configHough.edgeThreshold.gradient, ImageType.single(imageType), null);
		HoughTransformGradient hough = FactoryDetectLineAlgs.houghLineFoot(configHough, configParam,
				gradient.getDerivativeType().getImageClass());
		var detector = new HoughGradient_to_DetectLine(hough, gradient, imageType);
		detector.nonMaxSuppression = configHough.edgeThreshold.nonMax;
		detector.thresholdEdge = configHough.edgeThreshold.threshold;
		return detector;
	}

	/**
	 * Used to find edges along the side of objects. Image gradient is found and thresholded. The pixel coordinate
	 * of each active pixel and its gradient is then used to estimate the parameter of a line.
	 * Uses {@link HoughTransformGradient} with {@link boofcv.alg.feature.detect.line.HoughParametersPolar}
	 *
	 * @param configHough Configuration for hough binary transform.
	 * @param configParam Configuration for polar parameters
	 * @param imageType type of input image
	 * @return Line detector.
	 */
	public static <T extends ImageGray<T>>
	DetectLine<T> houghLinePolar( @Nullable ConfigHoughGradient configHough,
								  @Nullable ConfigParamPolar configParam,
								  Class<T> imageType ) {
		if (configHough == null)
			configHough = new ConfigHoughGradient();

		if (configParam == null)
			configParam = new ConfigParamPolar();

		ImageGradient<T, ?> gradient = FactoryDerivative.gradient(
				configHough.edgeThreshold.gradient, ImageType.single(imageType), null);

		HoughTransformGradient hough = FactoryDetectLineAlgs.houghLinePolar(configHough, configParam,
				gradient.getDerivativeType().getImageClass());
		var detector = new HoughGradient_to_DetectLine(hough, gradient, imageType);
		detector.nonMaxSuppression = configHough.edgeThreshold.nonMax;
		detector.thresholdEdge = configHough.edgeThreshold.threshold;
		return detector;
	}

	/**
	 * Line detector which converts image into a binary one and assumes every pixel with a value of 1 belongs
	 * to a line. Uses {@link HoughTransformBinary} with {@link boofcv.alg.feature.detect.line.HoughParametersPolar}
	 *
	 * @param configHough Configuration for hough binary transform.
	 * @param configParam Configuration for polar parameters
	 * @param imageType type of input image
	 * @return Line detector.
	 */
	public static <T extends ImageGray<T>>
	DetectLine<T> houghLinePolar( @Nullable ConfigHoughBinary configHough,
								  @Nullable ConfigParamPolar configParam,
								  Class<T> imageType ) {
		if (configHough == null)
			configHough = new ConfigHoughBinary();

		if (configParam == null)
			configParam = new ConfigParamPolar();

		HoughTransformBinary hough = FactoryDetectLineAlgs.houghLinePolar(configHough, configParam);

		if (configHough.binarization == ConfigHoughBinary.Binarization.EDGE) {
			ImageGradient<T, ?> gradient = FactoryDerivative.gradient(configHough.thresholdEdge.gradient, ImageType.single(imageType), null);
			var detector = new HoughBinary_to_DetectLine(hough, gradient);
			detector.nonMaxSuppression = configHough.thresholdEdge.nonMax;
			detector.thresholdEdge = configHough.thresholdEdge.threshold;
			return detector;
		} else {
			InputToBinary<T> thresholder = FactoryThresholdBinary.threshold(configHough.thresholdImage, imageType);
			return new HoughBinary_to_DetectLine<>(hough, thresholder);
		}
	}
}
