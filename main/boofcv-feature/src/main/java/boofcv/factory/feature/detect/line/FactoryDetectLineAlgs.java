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

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.line.DetectLineHoughFootSubimage;
import boofcv.abst.feature.detect.line.DetectLineSegmentsGridRansac;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.line.*;
import boofcv.alg.feature.detect.line.gridline.*;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.ImageGray;
import georegression.fitting.line.ModelManagerLinePolar2D_F32;
import georegression.struct.line.LinePolar2D_F32;
import org.ddogleg.fitting.modelset.ModelMatcherPost;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating line and line segment detectors.
 *
 * @author Peter Abeles
 */
public class FactoryDetectLineAlgs {

	/**
	 * Detects line segments inside an image using the {@link DetectLineSegmentsGridRansac} algorithm.
	 *
	 * @param config Configuration for line detector
	 * @param imageType Type of single band input image.
	 * @param derivType Image derivative type.
	 * @return Line segment detector
	 * @see DetectLineSegmentsGridRansac
	 */
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	DetectLineSegmentsGridRansac<I, D> lineRansac( @Nullable ConfigLineRansac config,
												   Class<I> imageType,
												   Class<D> derivType ) {

		if (config == null)
			config = new ConfigLineRansac();

		ConfigLineRansac _config = config;

		ImageGradient<I, D> gradient = FactoryDerivative.sobel(imageType, derivType);

		ModelManagerLinePolar2D_F32 manager = new ModelManagerLinePolar2D_F32();

		ModelMatcherPost<LinePolar2D_F32, Edgel> matcher = new Ransac<>(123123, 25, 1, manager, Edgel.class);
		matcher.setModel(
				() -> new GridLineModelFitter((float)_config.thresholdAngle),
				() -> new GridLineModelDistance((float)_config.thresholdAngle));

		GridRansacLineDetector<D> alg;
		if (derivType == GrayF32.class) {
			alg = (GridRansacLineDetector)new ImplGridRansacLineDetector_F32(config.regionSize, 10, matcher);
		} else if (derivType == GrayS16.class) {
			alg = (GridRansacLineDetector)new ImplGridRansacLineDetector_S16(config.regionSize, 10, matcher);
		} else {
			throw new IllegalArgumentException("Unsupported derivative type");
		}

		ConnectLinesGrid connect = null;
		if (config.connectLines)
			connect = new ConnectLinesGrid(Math.PI*0.01, 1, 8);

		return new DetectLineSegmentsGridRansac<>(alg, connect, gradient, config.thresholdEdge, imageType, derivType);
	}

	/**
	 * Detects lines using a foot of norm parametrization and sub images to reduce degenerate
	 * configurations, see {@link DetectLineHoughFootSubimage} for details.
	 *
	 * @param config Configuration for line detector. If null then default will be used.
	 * @param derivType Image derivative type.
	 * @param <D> Image derivative type.
	 * @return Line detector.
	 * @see DetectLineHoughFootSubimage
	 */
	public static <D extends ImageGray<D>>
	DetectLineHoughFootSubimage<D> houghFootSub( @Nullable ConfigHoughFootSubimage config,
												 Class<D> derivType ) {

		if (config == null)
			config = new ConfigHoughFootSubimage();


		return new DetectLineHoughFootSubimage<>(config.localMaxRadius,
				config.minCounts, config.minDistanceFromOrigin, config.thresholdEdge,
				config.totalHorizontalDivisions, config.totalVerticalDivisions, config.maxLines, derivType);
	}

	public static <D extends ImageGray<D>>
	HoughTransformGradient<D> houghLineFoot( ConfigHoughGradient configHough, ConfigParamFoot configParam,
											 Class<D> derivType ) {
		HoughParametersFootOfNorm param = new HoughParametersFootOfNorm(configParam.minDistanceFromOrigin);
		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(
				new ConfigExtract(configHough.localMaxRadius, configHough.minCounts, 0, true));

		HoughTransformGradient<D> hough;
		if (BoofConcurrency.USE_CONCURRENT) {
			hough = new HoughTransformGradient_MT<>(extractor, param, derivType);
		} else {
			hough = new HoughTransformGradient<>(extractor, param, derivType);
		}

		hough.setMaxLines(configHough.maxLines);
		hough.setMergeAngle(configHough.mergeAngle);
		hough.setMergeDistance(configHough.mergeDistance);
		hough.setRefineRadius(configHough.refineRadius);

		return hough;
	}

	public static <D extends ImageGray<D>>
	HoughTransformGradient<D> houghLinePolar( ConfigHoughGradient configHough, ConfigParamPolar configParam,
											  Class<D> derivType ) {
		HoughParametersPolar param = new HoughParametersPolar(configParam.resolutionRange, configParam.numBinsAngle);
		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(
				new ConfigExtract(configHough.localMaxRadius, configHough.minCounts, 0, true));

		HoughTransformGradient<D> hough;
		if (BoofConcurrency.USE_CONCURRENT) {
			hough = new HoughTransformGradient_MT<>(extractor, param, derivType);
		} else {
			hough = new HoughTransformGradient<>(extractor, param, derivType);
		}

		hough.setMaxLines(configHough.maxLines);
		hough.setMergeAngle(configHough.mergeAngle);
		hough.setMergeDistance(configHough.mergeDistance);
		hough.setRefineRadius(configHough.refineRadius);

		return hough;
	}

	public static HoughTransformBinary houghLinePolar( ConfigHoughBinary configHough, ConfigParamPolar configParam ) {
		HoughParametersPolar param = new HoughParametersPolar(configParam.resolutionRange, configParam.numBinsAngle);
		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(
				new ConfigExtract(configHough.localMaxRadius, 0, 0, false));

		HoughTransformBinary hough;
		if (BoofConcurrency.USE_CONCURRENT) {
			hough = new HoughTransformBinary_MT(extractor, param);
		} else {
			hough = new HoughTransformBinary(extractor, param);
		}

		hough.setMaxLines(configHough.maxLines);
		hough.setMergeAngle(configHough.mergeAngle);
		hough.setMergeDistance(configHough.mergeDistance);
		hough.setNumberOfCounts(configHough.minCounts.copy());

		return hough;
	}
}
