/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.abst.feature.detect.intensity.WrapperGradientCornerIntensity;
import boofcv.abst.feature.detect.intensity.WrapperHessianBlobIntensity;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.abst.filter.ImageFunctionSparse;
import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.feature.detect.intensity.GradientCornerIntensity;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.interest.*;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.factory.filter.derivative.FactoryDerivativeSparse;
import boofcv.struct.image.ImageSingleBand;

/**
 * Factory for non-generic specific implementations of interest point detection algorithms.
 *
 * @author Peter Abeles
 */
public class FactoryInterestPointAlgs {

	/**
	 * Creates a {@link FeaturePyramid} which is uses a hessian blob detector.
	 *
	 * @param extractRadius   Size of the feature used to detect the corners.
	 * @param detectThreshold Minimum corner intensity required
	 * @param maxFeatures     Max number of features that can be found.
	 * @param imageType       Type of input image.
	 * @param derivType       Image derivative type.
	 * @return CornerLaplaceScaleSpace
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	FeaturePyramid<T, D> hessianPyramid(int extractRadius,
										float detectThreshold,
										int maxFeatures,
										Class<T> imageType,
										Class<D> derivType) {
		GeneralFeatureIntensity<T, D> intensity = new WrapperHessianBlobIntensity<T, D>(HessianBlobIntensity.Type.DETERMINANT, derivType);
		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(
				new ConfigExtract(extractRadius, detectThreshold, extractRadius, true));
		GeneralFeatureDetector<T, D> detector = new GeneralFeatureDetector<T, D>(intensity, extractor);
		detector.setMaxFeatures(maxFeatures);

		AnyImageDerivative<T, D> deriv = GImageDerivativeOps.createDerivatives(imageType, FactoryImageGenerator.create(derivType));

		return new FeaturePyramid<T, D>(detector, deriv, 2);
	}

	/**
	 * Creates a {@link FeaturePyramid} which is uses the Harris corner detector.
	 *
	 * @param extractRadius   Size of the feature used to detect the corners.
	 * @param detectThreshold Minimum corner intensity required
	 * @param maxFeatures     Max number of features that can be found.
	 * @param imageType       Type of input image.
	 * @param derivType       Image derivative type.
	 * @return CornerLaplaceScaleSpace
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	FeaturePyramid<T, D> harrisPyramid(int extractRadius,
									   float detectThreshold,
									   int maxFeatures,
									   Class<T> imageType,
									   Class<D> derivType) {
		GradientCornerIntensity<D> harris = FactoryIntensityPointAlg.harris(extractRadius, 0.04f, false, derivType);
		GeneralFeatureIntensity<T, D> intensity = new WrapperGradientCornerIntensity<T, D>(harris);
		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(
				new ConfigExtract(extractRadius, detectThreshold, extractRadius, true));
		GeneralFeatureDetector<T, D> detector = new GeneralFeatureDetector<T, D>(intensity, extractor);
		detector.setMaxFeatures(maxFeatures);

		AnyImageDerivative<T, D> deriv = GImageDerivativeOps.createDerivatives(imageType, FactoryImageGenerator.create(derivType));

		return new FeaturePyramid<T, D>(detector, deriv, 2);
	}

	/**
	 * Creates a {@link boofcv.alg.feature.detect.interest.FeatureLaplacePyramid} which is uses a hessian blob detector.
	 *
	 * @param extractRadius   Size of the feature used to detect the corners.
	 * @param detectThreshold Minimum corner intensity required
	 * @param maxFeatures     Max number of features that can be found.
	 * @param imageType       Type of input image.
	 * @param derivType       Image derivative type.
	 * @return CornerLaplaceScaleSpace
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	FeatureLaplacePyramid<T, D> hessianLaplace(int extractRadius,
											   float detectThreshold,
											   int maxFeatures,
											   Class<T> imageType,
											   Class<D> derivType) {
		GeneralFeatureIntensity<T, D> intensity = new WrapperHessianBlobIntensity<T, D>(HessianBlobIntensity.Type.DETERMINANT, derivType);
		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(
				new ConfigExtract(extractRadius, detectThreshold, extractRadius, true));
		GeneralFeatureDetector<T, D> detector = new GeneralFeatureDetector<T, D>(intensity, extractor);
		detector.setMaxFeatures(maxFeatures);

		AnyImageDerivative<T, D> deriv = GImageDerivativeOps.createDerivatives(imageType, FactoryImageGenerator.create(derivType));

		ImageFunctionSparse<T> sparseLaplace = FactoryDerivativeSparse.createLaplacian(imageType, null);

		return new FeatureLaplacePyramid<T, D>(detector, sparseLaplace, deriv, 2);
	}

	/**
	 * Creates a {@link FeatureLaplacePyramid} which is uses the Harris corner detector.
	 *
	 * @param extractRadius   Size of the feature used to detect the corners.
	 * @param detectThreshold Minimum corner intensity required
	 * @param maxFeatures     Max number of features that can be found.
	 * @param imageType       Type of input image.
	 * @param derivType       Image derivative type.
	 * @return CornerLaplaceScaleSpace
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	FeatureLaplacePyramid<T, D> harrisLaplace(int extractRadius,
											  float detectThreshold,
											  int maxFeatures,
											  Class<T> imageType,
											  Class<D> derivType) {
		GradientCornerIntensity<D> harris = FactoryIntensityPointAlg.harris(extractRadius, 0.04f, false, derivType);
		GeneralFeatureIntensity<T, D> intensity = new WrapperGradientCornerIntensity<T, D>(harris);
		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(
				new ConfigExtract(extractRadius, detectThreshold, extractRadius, true));
		GeneralFeatureDetector<T, D> detector = new GeneralFeatureDetector<T, D>(intensity, extractor);
		detector.setMaxFeatures(maxFeatures);

		AnyImageDerivative<T, D> deriv = GImageDerivativeOps.createDerivatives(imageType, FactoryImageGenerator.create(derivType));
		ImageFunctionSparse<T> sparseLaplace = FactoryDerivativeSparse.createLaplacian(imageType, null);

		return new FeatureLaplacePyramid<T, D>(detector, sparseLaplace, deriv, 2);
	}

	/**
	 * Creates a Fast Hessian blob detector used by SURF.
	 *
	 * @param config Configuration for detector. Pass in null for default options.
	 * @param <II> Integral Image
	 * @return The feature detector
	 */
	public static <II extends ImageSingleBand>
	FastHessianFeatureDetector<II> fastHessian( ConfigFastHessian config ) {

		if( config == null )
			config = new ConfigFastHessian();
		config.checkValidity();

		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(
				new ConfigExtract(config.extractRadius, config.detectThreshold, 5, true));
		return new FastHessianFeatureDetector<II>(extractor, config.maxFeaturesPerScale,
				config.initialSampleSize, config.initialSize, config.numberScalesPerOctave, config.numberOfOctaves);
	}

	/**
	 * Creates a SIFT feature detector.
	 *
	 * @see SiftDetector
	 * @see SiftImageScaleSpace
	 *
	 * @param config Configuration for detector. Pass in null for default options.
	 */
	public static SiftDetector siftDetector( ConfigSiftDetector config )
	{
		if( config == null )
			config = new ConfigSiftDetector();
		config.checkValidity();

		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(
				new ConfigExtract(config.extractRadius, config.detectThreshold, 2, true, true, true));
		return new SiftDetector(extractor,config.maxFeaturesPerScale,config.edgeThreshold);
	}

}
