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

package boofcv.factory.feature.detect.interest;

import boofcv.abst.feature.describe.ConfigSiftScaleSpace;
import boofcv.abst.feature.detect.extract.NonMaxLimiter;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.interest.*;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.filter.derivative.ImageHessian;
import boofcv.alg.feature.detect.interest.*;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.PyramidFloat;

/**
 * <p>Factory for creating interest point detectors which conform to the {@link InterestPointDetector}
 * interface </p>
 * <p>
 * NOTE: Higher level interface than {@link GeneralFeatureDetector}.  This will automatically
 * compute image derivatives across scale space as needed, unlike GeneralFeatureDetector which
 * just detects features at a particular scale and requires image derivatives be passed in.
 * </p>
 *
 * @author Peter Abeles
 * @see FactoryFeatureExtractor
 */
public class FactoryInterestPoint {

	/**
	 * Wraps {@link GeneralFeatureDetector} inside an {@link InterestPointDetector}.
	 *
	 * @param feature   Feature detector.
	 * @param scale Scale of detected features
	 * @param inputType Image type of input image.
	 * @param derivType Image type for gradient.
	 * @return The interest point detector.
	 */
	public static <T extends ImageGray, D extends ImageGray>
	InterestPointDetector<T> wrapPoint(GeneralFeatureDetector<T, D> feature, double scale , Class<T> inputType, Class<D> derivType) {

		ImageGradient<T, D> gradient = null;
		ImageHessian<D> hessian = null;

		if (feature.getRequiresGradient() || feature.getRequiresHessian())
			gradient = FactoryDerivative.sobel(inputType, derivType);
		if (feature.getRequiresHessian())
			hessian = FactoryDerivative.hessianSobel(derivType);

		return new GeneralToInterestPoint<>(feature, gradient, hessian, scale, derivType);
	}

	/**
	 * Wraps {@link FeatureLaplacePyramid} inside an {@link InterestPointDetector}.
	 *
	 * @param feature   Feature detector.
	 * @param scales    Scales at which features are detected at.
	 * @param pyramid   Should it be constructed as a pyramid or scale-space
	 * @param inputType Image type of input image.
	 * @return The interest point detector.
	 */
	public static <T extends ImageGray, D extends ImageGray>
	InterestPointDetector<T> wrapDetector(FeatureLaplacePyramid<T, D> feature,
										  double[] scales, boolean pyramid,
										  Class<T> inputType) {

		PyramidFloat<T> ss;

		if( pyramid )
			ss = FactoryPyramid.scaleSpacePyramid(scales, inputType);
		else
			ss = FactoryPyramid.scaleSpace(scales, inputType);

		return new WrapFLPtoInterestPoint<>(feature, ss);
	}

	/**
	 * Wraps {@link FeaturePyramid} inside an {@link InterestPointDetector}.
	 *
	 * @param feature   Feature detector.
	 * @param scales    Scales at which features are detected at.
	 * @param pyramid   Should it be constructed as a pyramid or scale-space
	 * @param inputType Image type of input image.
	 * @return The interest point detector.
	 */
	public static <T extends ImageGray, D extends ImageGray>
	InterestPointDetector<T> wrapDetector(FeaturePyramid<T, D> feature,
										  double[] scales, boolean pyramid,
										  Class<T> inputType) {

		PyramidFloat<T> ss;

		if( pyramid )
			ss = FactoryPyramid.scaleSpacePyramid(scales, inputType);
		else
			ss = FactoryPyramid.scaleSpace(scales, inputType);

		return new WrapFPtoInterestPoint<>(feature, ss);
	}

	/**
	 * Creates a {@link FastHessianFeatureDetector} detector which is wrapped inside
	 * an {@link InterestPointDetector}
	 *
	 * @param config Configuration for detector.  Pass in null for default options.
	 * @return The interest point detector.
	 * @see FastHessianFeatureDetector
	 */
	public static <T extends ImageGray>
	InterestPointDetector<T> fastHessian( ConfigFastHessian config ) {
		return new WrapFHtoInterestPoint(FactoryInterestPointAlgs.fastHessian(config));
	}

	public static <T extends ImageGray>
	InterestPointDetector<T> sift(ConfigSiftScaleSpace configSS ,
								  ConfigSiftDetector configDet , Class<T> imageType ) {

		if( configSS == null )
			configSS = new ConfigSiftScaleSpace();
		if( configDet == null )
			configDet = new ConfigSiftDetector();

		SiftScaleSpace scaleSpace =
				new SiftScaleSpace(configSS.firstOctave,configSS.lastOctave,configSS.numScales,configSS.sigma0);
		NonMaxSuppression nonmax = FactoryFeatureExtractor.nonmax(configDet.extract);
		NonMaxLimiter limiter = new NonMaxLimiter(nonmax,configDet.maxFeaturesPerScale);
		SiftDetector detector = new SiftDetector(scaleSpace,configDet.edgeR,limiter);

		return new WrapSiftDetector<>(detector, imageType);
	}
}
