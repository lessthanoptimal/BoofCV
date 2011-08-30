/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.factory.feature.detect.interest;

import gecv.abst.feature.detect.extract.FeatureExtractor;
import gecv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import gecv.abst.feature.detect.intensity.WrapperGradientCornerIntensity;
import gecv.abst.feature.detect.intensity.WrapperLaplacianBlobIntensity;
import gecv.abst.filter.ImageFunctionSparse;
import gecv.abst.filter.derivative.AnyImageDerivative;
import gecv.alg.feature.detect.intensity.GradientCornerIntensity;
import gecv.alg.feature.detect.intensity.HessianBlobIntensity;
import gecv.alg.feature.detect.interest.*;
import gecv.alg.transform.gss.UtilScaleSpace;
import gecv.core.image.inst.FactoryImageGenerator;
import gecv.factory.feature.detect.extract.FactoryFeatureFromIntensity;
import gecv.factory.feature.detect.intensity.FactoryPointIntensityAlg;
import gecv.factory.filter.derivative.FactoryDerivativeSparse;
import gecv.struct.image.ImageBase;

/**
 * @author Peter Abeles
 */
public class FactoryInterestPointAlgs {

	/**
	 * Creates a {@link gecv.alg.feature.detect.interest.FeatureLaplaceScaleSpace} which is uses the Harris corner detector.
	 *
	 * @param featureRadius Size of the feature used to detect the corners.
	 * @param cornerThreshold Minimum corner intensity required
	 * @param maxFeatures Max number of features that can be found.
	 * @param imageType Type of input image.
	 * @param derivType Image derivative type.
	 * @return CornerLaplaceScaleSpace
	 */
	public static <T extends ImageBase, D extends ImageBase>
	FeatureLaplaceScaleSpace<T,D> harrisLaplace( int featureRadius ,
												 float cornerThreshold ,
												 int maxFeatures ,
												 Class<T> imageType ,
												 Class<D> derivType)
	{
		FeatureExtractor extractor = FactoryFeatureFromIntensity.create(featureRadius,cornerThreshold,featureRadius*2,false,false,false);
		GradientCornerIntensity<D> harris = FactoryPointIntensityAlg.createHarris(derivType,featureRadius,0.04f);
		GeneralFeatureIntensity<T, D> intensity = new WrapperGradientCornerIntensity<T,D>(harris);
		GeneralFeatureDetector<T,D> detector = new GeneralFeatureDetector<T,D>(intensity,extractor,maxFeatures);

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
	public static <T extends ImageBase, D extends ImageBase>
	FeatureLaplaceScaleSpace<T,D> hessianLaplace( int featureRadius ,
												  float cornerThreshold ,
												  int maxFeatures ,
												  Class<T> imageType ,
												  Class<D> derivType)
	{
		FeatureExtractor extractor = FactoryFeatureFromIntensity.create(featureRadius,cornerThreshold,0,false,false,false);
		GeneralFeatureIntensity<T, D> intensity = new WrapperLaplacianBlobIntensity<T,D>(HessianBlobIntensity.Type.DETERMINANT,derivType);
		GeneralFeatureDetector<T,D> detector = new GeneralFeatureDetector<T,D>(intensity,extractor,maxFeatures);

		ImageFunctionSparse<T> sparseLaplace = FactoryDerivativeSparse.createLaplacian(imageType,null);

		return new FeatureLaplaceScaleSpace<T,D>(detector,sparseLaplace,2);
	}

	/**
	 * Creates a {@link gecv.alg.feature.detect.interest.FeaturePyramid} which is uses a hessian blob detector.
	 *
	 * @param featureRadius Size of the feature used to detect the corners.
	 * @param cornerThreshold Minimum corner intensity required
	 * @param maxFeatures Max number of features that can be found.
	 * @param imageType Type of input image.
	 * @param derivType Image derivative type.
	 * @return CornerLaplaceScaleSpace
	 */
	public static <T extends ImageBase, D extends ImageBase>
	FeaturePyramid<T,D> hessianPyramid( int featureRadius ,
										float cornerThreshold ,
										int maxFeatures ,
										Class<T> imageType ,
										Class<D> derivType)
	{
		FeatureExtractor extractor = FactoryFeatureFromIntensity.create(featureRadius,cornerThreshold,0,false,false,false);
		GeneralFeatureIntensity<T, D> intensity = new WrapperLaplacianBlobIntensity<T,D>(HessianBlobIntensity.Type.DETERMINANT,derivType);
		GeneralFeatureDetector<T,D> detector = new GeneralFeatureDetector<T,D>(intensity,extractor,maxFeatures);

		AnyImageDerivative<T,D> deriv = UtilScaleSpace.createDerivatives(imageType, FactoryImageGenerator.create(derivType));

		return new FeaturePyramid<T,D>(detector,deriv,1);
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
	public static <T extends ImageBase, D extends ImageBase>
	FeaturePyramid<T,D> harrisPyramid( int featureRadius ,
									   float cornerThreshold ,
									   int maxFeatures ,
									   Class<T> imageType ,
									   Class<D> derivType)
	{
		FeatureExtractor extractor = FactoryFeatureFromIntensity.create(featureRadius,cornerThreshold,featureRadius*2,false,false,false);
		GradientCornerIntensity<D> harris = FactoryPointIntensityAlg.createHarris(derivType,featureRadius,0.04f);
		GeneralFeatureIntensity<T, D> intensity = new WrapperGradientCornerIntensity<T,D>(harris);
		GeneralFeatureDetector<T,D> detector = new GeneralFeatureDetector<T,D>(intensity,extractor,maxFeatures);

		AnyImageDerivative<T,D> deriv = UtilScaleSpace.createDerivatives(imageType, FactoryImageGenerator.create(derivType));

		return new FeaturePyramid<T,D>(detector,deriv,1);
	}

	/**
	 * Creates a {@link gecv.alg.feature.detect.interest.FeatureLaplacePyramid} which is uses a hessian blob detector.
	 *
	 * @param featureRadius Size of the feature used to detect the corners.
	 * @param cornerThreshold Minimum corner intensity required
	 * @param maxFeatures Max number of features that can be found.
	 * @param imageType Type of input image.
	 * @param derivType Image derivative type.
	 * @return CornerLaplaceScaleSpace
	 */
	public static <T extends ImageBase, D extends ImageBase>
	FeatureLaplacePyramid<T,D> hessianLaplacePyramid( int featureRadius ,
													  float cornerThreshold ,
													  int maxFeatures ,
													  Class<T> imageType ,
													  Class<D> derivType)
	{
		FeatureExtractor extractor = FactoryFeatureFromIntensity.create(featureRadius,cornerThreshold,0,false,false,false);
		GeneralFeatureIntensity<T, D> intensity = new WrapperLaplacianBlobIntensity<T,D>(HessianBlobIntensity.Type.DETERMINANT,derivType);
		GeneralFeatureDetector<T,D> detector = new GeneralFeatureDetector<T,D>(intensity,extractor,maxFeatures);

		AnyImageDerivative<T,D> deriv = UtilScaleSpace.createDerivatives(imageType, FactoryImageGenerator.create(derivType));

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
	public static <T extends ImageBase, D extends ImageBase>
	FeatureLaplacePyramid<T,D> harrisLaplacePyramid( int featureRadius ,
													 float cornerThreshold ,
													 int maxFeatures ,
													 Class<T> imageType ,
													 Class<D> derivType)
	{
		FeatureExtractor extractor = FactoryFeatureFromIntensity.create(featureRadius,cornerThreshold,featureRadius*2,false,false,false);
		GradientCornerIntensity<D> harris = FactoryPointIntensityAlg.createHarris(derivType,featureRadius,0.04f);
		GeneralFeatureIntensity<T, D> intensity = new WrapperGradientCornerIntensity<T,D>(harris);
		GeneralFeatureDetector<T,D> detector = new GeneralFeatureDetector<T,D>(intensity,extractor,maxFeatures);

		AnyImageDerivative<T,D> deriv = UtilScaleSpace.createDerivatives(imageType, FactoryImageGenerator.create(derivType));
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
	public static <T extends ImageBase, D extends ImageBase>
	FeatureScaleSpace<T,D> harrisScaleSpace( int featureRadius ,
											 float cornerThreshold ,
											 int maxFeatures ,
											 Class<T> imageType ,
											 Class<D> derivType)
		{
		FeatureExtractor extractor = FactoryFeatureFromIntensity.create(featureRadius,cornerThreshold,featureRadius*2,false,false,false);
		GradientCornerIntensity<D> harris = FactoryPointIntensityAlg.createHarris(derivType,featureRadius,0.04f);
		GeneralFeatureIntensity<T, D> intensity = new WrapperGradientCornerIntensity<T,D>(harris);
		GeneralFeatureDetector<T,D> detector = new GeneralFeatureDetector<T,D>(intensity,extractor,maxFeatures);

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
	public static <T extends ImageBase, D extends ImageBase>
	FeatureScaleSpace<T,D> hessianScaleSpace( int featureRadius ,
												float cornerThreshold ,
												int maxFeatures ,
												Class<T> imageType ,
												Class<D> derivType)
	{
		FeatureExtractor extractor = FactoryFeatureFromIntensity.create(featureRadius,cornerThreshold,0,false,false,false);
		GeneralFeatureIntensity<T, D> intensity = new WrapperLaplacianBlobIntensity<T,D>(HessianBlobIntensity.Type.DETERMINANT,derivType);
		GeneralFeatureDetector<T,D> detector = new GeneralFeatureDetector<T,D>(intensity,extractor,maxFeatures);

		return new FeatureScaleSpace<T,D>(detector,2);
	}
}
