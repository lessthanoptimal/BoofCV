/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.feature.detect.extract.GeneralFeatureDetector;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.abst.feature.detect.intensity.WrapperGradientCornerIntensity;
import boofcv.abst.feature.detect.intensity.WrapperHessianBlobIntensity;
import boofcv.abst.filter.ImageFunctionSparse;
import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.feature.detect.intensity.GradientCornerIntensity;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.interest.FeatureLaplacePyramid;
import boofcv.alg.feature.detect.interest.FeatureLaplaceScaleSpace;
import boofcv.alg.feature.detect.interest.FeaturePyramid;
import boofcv.alg.feature.detect.interest.FeatureScaleSpace;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.factory.filter.derivative.FactoryDerivativeSparse;
import boofcv.struct.image.ImageSingleBand;

/**
 * @author Peter Abeles
 */
public class FactoryInterestPointAlgs {

	/**
	 * Creates a {@link boofcv.alg.feature.detect.interest.FeatureLaplaceScaleSpace} which is uses the Harris corner detector.
	 *
	 * @param featureRadius Size of the feature used to detect the corners.
	 * @param cornerThreshold Minimum corner intensity required
	 * @param maxFeatures Max number of features that can be found.
	 * @param imageType Type of input image.
	 * @param derivType Image derivative type.
	 * @return CornerLaplaceScaleSpace
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	FeatureLaplaceScaleSpace<T,D> harrisLaplace( int featureRadius ,
												 float cornerThreshold ,
												 int maxFeatures ,
												 Class<T> imageType ,
												 Class<D> derivType)
	{
		GradientCornerIntensity<D> harris = FactoryIntensityPointAlg.harris(featureRadius, 0.04f, false, derivType);
		GeneralFeatureIntensity<T, D> intensity = new WrapperGradientCornerIntensity<T,D>(harris);
		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(featureRadius, cornerThreshold, featureRadius, true);
		GeneralFeatureDetector<T,D> detector = new GeneralFeatureDetector<T,D>(intensity,extractor);
		detector.setMaxFeatures(maxFeatures);

		ImageFunctionSparse<T> sparseLaplace = FactoryDerivativeSparse.createLaplacian(imageType,null);

		return new FeatureLaplaceScaleSpace<T,D>(detector,sparseLaplace,2);
	}

	/**
	 * Creates a {@link FeatureLaplaceScaleSpace} which is uses a hessian blob detector.
	 *
	 * @param featureRadius Size of the feature used to detect the corners.
	 * @param cornerThreshold Minimum corner intensity required
	 * @param maxFeatures Max number of features that can be found.
	 * @param imageType Type of input image.
	 * @param derivType Image derivative type.
	 * @return CornerLaplaceScaleSpace
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	FeatureLaplaceScaleSpace<T,D> hessianLaplace( int featureRadius ,
												  float cornerThreshold ,
												  int maxFeatures ,
												  Class<T> imageType ,
												  Class<D> derivType)
	{
		GeneralFeatureIntensity<T, D> intensity = new WrapperHessianBlobIntensity<T,D>(HessianBlobIntensity.Type.DETERMINANT,derivType);
		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(featureRadius, cornerThreshold, featureRadius, true);
		GeneralFeatureDetector<T,D> detector = new GeneralFeatureDetector<T,D>(intensity,extractor);
		detector.setMaxFeatures(maxFeatures);

		ImageFunctionSparse<T> sparseLaplace = FactoryDerivativeSparse.createLaplacian(imageType,null);

		return new FeatureLaplaceScaleSpace<T,D>(detector,sparseLaplace,2);
	}

	/**
	 * Creates a {@link boofcv.alg.feature.detect.interest.FeaturePyramid} which is uses a hessian blob detector.
	 *
	 * @param featureRadius Size of the feature used to detect the corners.
	 * @param cornerThreshold Minimum corner intensity required
	 * @param maxFeatures Max number of features that can be found.
	 * @param imageType Type of input image.
	 * @param derivType Image derivative type.
	 * @return CornerLaplaceScaleSpace
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	FeaturePyramid<T,D> hessianPyramid( int featureRadius ,
										float cornerThreshold ,
										int maxFeatures ,
										Class<T> imageType ,
										Class<D> derivType)
	{
		GeneralFeatureIntensity<T, D> intensity = new WrapperHessianBlobIntensity<T,D>(HessianBlobIntensity.Type.DETERMINANT,derivType);
		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(featureRadius, cornerThreshold, featureRadius, true);
		GeneralFeatureDetector<T,D> detector = new GeneralFeatureDetector<T,D>(intensity,extractor);
		detector.setMaxFeatures(maxFeatures);

		AnyImageDerivative<T,D> deriv = GImageDerivativeOps.createDerivatives(imageType, FactoryImageGenerator.create(derivType));

		return new FeaturePyramid<T,D>(detector,deriv,0);
	}

	/**
	 * Creates a {@link FeatureLaplaceScaleSpace} which is uses the Harris corner detector.
	 *
	 * @param featureRadius Size of the feature used to detect the corners.
	 * @param cornerThreshold Minimum corner intensity required
	 * @param maxFeatures Max number of features that can be found.
	 * @param imageType Type of input image.
	 * @param derivType Image derivative type.
	 * @return CornerLaplaceScaleSpace
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	FeaturePyramid<T,D> harrisPyramid( int featureRadius ,
									   float cornerThreshold ,
									   int maxFeatures ,
									   Class<T> imageType ,
									   Class<D> derivType)
	{
		GradientCornerIntensity<D> harris = FactoryIntensityPointAlg.harris(featureRadius, 0.04f, false, derivType);
		GeneralFeatureIntensity<T, D> intensity = new WrapperGradientCornerIntensity<T,D>(harris);
		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(featureRadius, cornerThreshold, featureRadius, true);
		GeneralFeatureDetector<T,D> detector = new GeneralFeatureDetector<T,D>(intensity,extractor);
		detector.setMaxFeatures(maxFeatures);

		AnyImageDerivative<T,D> deriv = GImageDerivativeOps.createDerivatives(imageType, FactoryImageGenerator.create(derivType));

		return new FeaturePyramid<T,D>(detector,deriv,0);
	}

	/**
	 * Creates a {@link boofcv.alg.feature.detect.interest.FeatureLaplacePyramid} which is uses a hessian blob detector.
	 *
	 * @param featureRadius Size of the feature used to detect the corners.
	 * @param cornerThreshold Minimum corner intensity required
	 * @param maxFeatures Max number of features that can be found.
	 * @param imageType Type of input image.
	 * @param derivType Image derivative type.
	 * @return CornerLaplaceScaleSpace
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	FeatureLaplacePyramid<T,D> hessianLaplacePyramid( int featureRadius ,
													  float cornerThreshold ,
													  int maxFeatures ,
													  Class<T> imageType ,
													  Class<D> derivType)
	{
		GeneralFeatureIntensity<T, D> intensity = new WrapperHessianBlobIntensity<T,D>(HessianBlobIntensity.Type.DETERMINANT,derivType);
		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(featureRadius, cornerThreshold, featureRadius, true);
		GeneralFeatureDetector<T,D> detector = new GeneralFeatureDetector<T,D>(intensity,extractor);
		detector.setMaxFeatures(maxFeatures);

		AnyImageDerivative<T,D> deriv = GImageDerivativeOps.createDerivatives(imageType, FactoryImageGenerator.create(derivType));

		ImageFunctionSparse<T> sparseLaplace = FactoryDerivativeSparse.createLaplacian(imageType,null);

		return new FeatureLaplacePyramid<T,D>(detector,sparseLaplace,deriv,1);
	}

	/**
	 * Creates a {@link FeatureLaplaceScaleSpace} which is uses the Harris corner detector.
	 *
	 * @param featureRadius Size of the feature used to detect the corners.
	 * @param cornerThreshold Minimum corner intensity required
	 * @param maxFeatures Max number of features that can be found.
	 * @param imageType Type of input image.
	 * @param derivType Image derivative type.
	 * @return CornerLaplaceScaleSpace
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	FeatureLaplacePyramid<T,D> harrisLaplacePyramid( int featureRadius ,
													 float cornerThreshold ,
													 int maxFeatures ,
													 Class<T> imageType ,
													 Class<D> derivType)
	{
		GradientCornerIntensity<D> harris = FactoryIntensityPointAlg.harris(featureRadius, 0.04f, false, derivType);
		GeneralFeatureIntensity<T, D> intensity = new WrapperGradientCornerIntensity<T,D>(harris);
		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(featureRadius, cornerThreshold, featureRadius, true);
		GeneralFeatureDetector<T,D> detector = new GeneralFeatureDetector<T,D>(intensity,extractor);
		detector.setMaxFeatures(maxFeatures);

		AnyImageDerivative<T,D> deriv = GImageDerivativeOps.createDerivatives(imageType, FactoryImageGenerator.create(derivType));
		ImageFunctionSparse<T> sparseLaplace = FactoryDerivativeSparse.createLaplacian(imageType,null);

		return new FeatureLaplacePyramid<T,D>(detector,sparseLaplace,deriv,1);
	}

	/**
	 * Creates a {@link FeatureLaplaceScaleSpace} which is uses the Harris corner detector.
	 *
	 * @param featureRadius Size of the feature used to detect the corners.
	 * @param cornerThreshold Minimum corner intensity required
	 * @param maxFeatures Max number of features that can be found.
	 * @param imageType Type of input image.
	 * @param derivType Image derivative type.
	 * @return CornerLaplaceScaleSpace
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	FeatureScaleSpace<T,D> harrisScaleSpace( int featureRadius ,
											 float cornerThreshold ,
											 int maxFeatures ,
											 Class<T> imageType ,
											 Class<D> derivType)
	{
		GradientCornerIntensity<D> harris = FactoryIntensityPointAlg.harris(featureRadius, 0.04f, false, derivType);
		GeneralFeatureIntensity<T, D> intensity = new WrapperGradientCornerIntensity<T,D>(harris);
		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(featureRadius, cornerThreshold, featureRadius, true);
		GeneralFeatureDetector<T,D> detector = new GeneralFeatureDetector<T,D>(intensity,extractor);
		detector.setMaxFeatures(maxFeatures);

		return new FeatureScaleSpace<T,D>(detector,2);
	}

	/**
	 * Creates a {@link FeatureLaplaceScaleSpace} which is uses a hessian blob detector.
	 *
	 * @param featureRadius Size of the feature used to detect the corners.
	 * @param cornerThreshold Minimum corner intensity required
	 * @param maxFeatures Max number of features that can be found.
	 * @param imageType Type of input image.
	 * @param derivType Image derivative type.
	 * @return CornerLaplaceScaleSpace
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	FeatureScaleSpace<T,D> hessianScaleSpace( int featureRadius ,
												float cornerThreshold ,
												int maxFeatures ,
												Class<T> imageType ,
												Class<D> derivType)
	{
		GeneralFeatureIntensity<T, D> intensity = new WrapperHessianBlobIntensity<T,D>(HessianBlobIntensity.Type.DETERMINANT,derivType);
		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(featureRadius, cornerThreshold, featureRadius, true);
		GeneralFeatureDetector<T,D> detector = new GeneralFeatureDetector<T,D>(intensity,extractor);
		detector.setMaxFeatures(maxFeatures);

		return new FeatureScaleSpace<T,D>(detector,2);
	}

}
