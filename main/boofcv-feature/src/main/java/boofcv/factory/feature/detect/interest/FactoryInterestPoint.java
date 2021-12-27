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

package boofcv.factory.feature.detect.interest;

import boofcv.abst.feature.describe.ConfigSiftScaleSpace;
import boofcv.abst.feature.detect.extract.NonMaxLimiter;
import boofcv.abst.feature.detect.interest.*;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.filter.derivative.ImageHessian;
import boofcv.alg.feature.detect.intensity.FastCornerDetector;
import boofcv.alg.feature.detect.interest.*;
import boofcv.alg.feature.detect.selector.FeatureSelectLimit;
import boofcv.alg.feature.detect.selector.FeatureSelectLimitIntensity;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.factory.feature.detect.selector.FactorySelectLimit;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.PyramidFloat;
import georegression.struct.point.Point2D_I16;
import org.jetbrains.annotations.Nullable;

/**
 * <p>Factory for creating interest point detectors which conform to the {@link InterestPointDetector}
 * interface </p>
 * <p>
 * NOTE: Higher level interface than {@link GeneralFeatureDetector}. This will automatically
 * compute image derivatives across scale space as needed, unlike GeneralFeatureDetector which
 * just detects features at a particular scale and requires image derivatives be passed in.
 * </p>
 *
 * @author Peter Abeles
 * @see FactoryFeatureExtractor
 */
public class FactoryInterestPoint {

	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	InterestPointDetector<T> generic( ConfigDetectInterestPoint config,
									  Class<T> inputType, @Nullable Class<D> derivType ) {
		switch (config.type) {
			case FAST_HESSIAN:
				return FactoryInterestPoint.fastHessian(config.fastHessian, inputType);
			case SIFT:
				return FactoryInterestPoint.sift(config.scaleSpaceSift, config.sift, inputType);
			case POINT: {
				if (derivType == null)
					derivType = GImageDerivativeOps.getDerivativeType(inputType);
				GeneralFeatureDetector<T, D> alg = FactoryDetectPoint.create(config.point, inputType, derivType);
				return FactoryInterestPoint.wrapPoint(alg, config.point.scaleRadius, inputType, derivType);
			}
			default:
				throw new IllegalArgumentException("Unknown detector");
		}
	}

	/**
	 * Creates a Fast corner detector with no non-maximum suppression
	 *
	 * @param configFast Configuration for FAST feature detector
	 * @param imageType ype of input image.
	 * @see FastCornerDetector
	 */
	public static <T extends ImageGray<T>>
	InterestPointDetector<T> createFast( @Nullable ConfigFastCorner configFast,
										 int featureLimitPerSet,
										 @Nullable ConfigSelectLimit configSelect,
										 Class<T> imageType ) {
		if (configFast == null)
			configFast = new ConfigFastCorner();
		configFast.checkValidity();

		FastCornerDetector<T> alg = FactoryIntensityPointAlg.fast(
				configFast.pixelTol, configFast.minContinuous, imageType);

		FeatureSelectLimit<Point2D_I16> selector = FactorySelectLimit.spatial(configSelect, Point2D_I16.class);

		var ret = new FastToInterestPoint<>(alg, selector);
		ret.setFeatureLimitPerSet(featureLimitPerSet <= 0 ? Integer.MAX_VALUE : featureLimitPerSet);
		return ret;
	}

	/**
	 * Wraps {@link GeneralFeatureDetector} inside an {@link InterestPointDetector}.
	 *
	 * @param feature Feature detector.
	 * @param scaleRadius Radius of descriptor used by scale invariant features
	 * @param inputType Image type of input image.
	 * @param derivType Image type for gradient.
	 * @return The interest point detector.
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	InterestPointDetector<T> wrapPoint( GeneralFeatureDetector<T, D> feature,
										double scaleRadius, Class<T> inputType, @Nullable Class<D> derivType ) {
		if (derivType == null)
			derivType = GImageDerivativeOps.getDerivativeType(inputType);

		ImageGradient<T, D> gradient = null;
		ImageHessian<D> hessian = null;

		if (feature.getRequiresGradient() || feature.getRequiresHessian())
			gradient = FactoryDerivative.sobel(inputType, derivType);
		if (feature.getRequiresHessian())
			hessian = FactoryDerivative.hessianSobel(derivType);

		return new GeneralToInterestPoint<>(feature, gradient, hessian, scaleRadius, derivType);
	}

	/**
	 * Wraps {@link FeatureLaplacePyramid} inside an {@link InterestPointDetector}.
	 *
	 * @param feature Feature detector.
	 * @param scales Scales at which features are detected at.
	 * @param pyramid Should it be constructed as a pyramid or scale-space
	 * @param inputType Image type of input image.
	 * @return The interest point detector.
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	InterestPointDetector<T> wrapDetector( FeatureLaplacePyramid<T, D> feature,
										   double[] scales, boolean pyramid,
										   Class<T> inputType ) {

		PyramidFloat<T> ss;

		if (pyramid)
			ss = FactoryPyramid.scaleSpacePyramid(scales, inputType);
		else
			ss = FactoryPyramid.scaleSpace(scales, inputType);

		return new WrapFLPtoInterestPoint<>(feature, ss);
	}

	/**
	 * Wraps {@link FeaturePyramid} inside an {@link InterestPointDetector}.
	 *
	 * @param feature Feature detector.
	 * @param scales Scales at which features are detected at.
	 * @param pyramid Should it be constructed as a pyramid or scale-space
	 * @param inputType Image type of input image.
	 * @return The interest point detector.
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	InterestPointDetector<T> wrapDetector( FeaturePyramid<T, D> feature,
										   double[] scales, boolean pyramid,
										   Class<T> inputType ) {

		PyramidFloat<T> ss;

		if (pyramid)
			ss = FactoryPyramid.scaleSpacePyramid(scales, inputType);
		else
			ss = FactoryPyramid.scaleSpace(scales, inputType);

		return new WrapFPtoInterestPoint<>(feature, ss);
	}

	/**
	 * Creates a {@link FastHessianFeatureDetector} detector which is wrapped inside
	 * an {@link InterestPointDetector}
	 *
	 * @param config Configuration for detector. Pass in null for default options.
	 * @return The interest point detector.
	 * @see FastHessianFeatureDetector
	 */
	public static <T extends ImageGray<T>>
	InterestPointDetector<T> fastHessian( ConfigFastHessian config, Class<T> imageType ) {
		return new WrapFHtoInterestPoint(FactoryInterestPointAlgs.fastHessian(config), imageType);
	}

	public static <T extends ImageGray<T>>
	InterestPointDetector<T> sift( @Nullable ConfigSiftScaleSpace configSS,
								   @Nullable ConfigSiftDetector configDet, Class<T> imageType ) {

		if (configSS == null)
			configSS = new ConfigSiftScaleSpace();
		if (configDet == null)
			configDet = new ConfigSiftDetector();

		var ss = new SiftScaleSpace(configSS.firstOctave, configSS.lastOctave, configSS.numScales, configSS.sigma0);
		NonMaxLimiter nonmax = FactoryFeatureExtractor.nonmaxLimiter(
				configDet.extract, configDet.selector, configDet.maxFeaturesPerScale);
		FeatureSelectLimitIntensity<ScalePoint> selectorAll = FactorySelectLimit.intensity(configDet.selector);
		var detector = new SiftDetector(selectorAll, configDet.edgeR, nonmax);
		detector.maxFeaturesAll = configDet.maxFeaturesAll;

		return new WrapSiftDetector<>(ss, detector, imageType);
	}
}
