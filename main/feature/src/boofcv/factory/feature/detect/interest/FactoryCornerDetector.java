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
import boofcv.abst.feature.detect.intensity.*;
import boofcv.abst.filter.blur.MedianImageFilter;
import boofcv.alg.feature.detect.intensity.FastCornerIntensity;
import boofcv.alg.feature.detect.intensity.GradientCornerIntensity;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.image.ImageSingleBand;

/**
 * <p>
 * Creates interest point detectors that are designed to detect the corners in an image.
 * </p>
 *
 * <p>
 * NOTE: Sometimes the image border is ignored and some times it is not.  If feature intensities are not
 * computed along the image border then it will be full of zeros.  In that case the ignore border region
 * needs to be increased for non-max suppression or else it might generate a false positive.
 * </p>
 *
 * @author Peter Abeles
 */
// TODO rename
// Hmm for the name these are all point features which take in the image derivatve
// These do not estimate the feature's scale or orientation
public class FactoryCornerDetector {

	/**
	 * Detects Harris corners.
	 *
	 * @see boofcv.alg.feature.detect.intensity.HarrisCornerIntensity
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<T,D> createHarris(int featureRadius, boolean weighted,
											 float cornerThreshold, int maxFeatures, Class<D> derivType)
	{
		GradientCornerIntensity<D> cornerIntensity = FactoryIntensityPointAlg.harris(featureRadius, 0.04f, weighted, derivType);
		return createGeneral(cornerIntensity,featureRadius,cornerThreshold,maxFeatures);
	}

	/**
	 * Detects Shi-Tomasi corners.
	 *
	 * @see boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<T,D> createShiTomasi(int featureRadius, boolean weighted,
												float cornerThreshold, int maxFeatures, Class<D> derivType)
	{
		GradientCornerIntensity<D> cornerIntensity = FactoryIntensityPointAlg.shiTomasi(featureRadius, weighted, derivType);
		return createGeneral(cornerIntensity,featureRadius,cornerThreshold,maxFeatures);
	}

	/**
	 * Detects Kitchen and Rosenfeld corners.
	 *
	 * @see boofcv.alg.feature.detect.intensity.KitRosCornerIntensity
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<T,D> createKitRos( int minSeparation ,
											  float cornerThreshold , int maxFeatures , Class<D> derivType )
	{
		GeneralFeatureIntensity<T,D> intensity = new WrapperKitRosCornerIntensity<T,D>(derivType);
		return createGeneral(intensity,minSeparation,cornerThreshold,maxFeatures);
	}

	/**
	 * Creates a Fast corner detector.
	 *
	 * @see FastCornerIntensity
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<T,D> createFast( int minSeparation ,
											int pixelTol , int maxFeatures , Class<T> imageType)
	{
		FastCornerIntensity<T> alg = FactoryIntensityPointAlg.fast12(pixelTol, 11, imageType);
		GeneralFeatureIntensity<T,D> intensity = new WrapperFastCornerIntensity<T,D>(alg);
		return createGeneral(intensity,minSeparation,pixelTol,maxFeatures);
	}

	/**
	 * Creates a median filter corner detector.
	 *
	 * @see boofcv.alg.feature.detect.intensity.MedianCornerIntensity
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<T,D> createMedian( int featureRadius , float pixelTol , int maxFeatures , Class<T> imageType)
	{
		MedianImageFilter<T> medianFilter = FactoryBlurFilter.median(imageType,featureRadius);
		GeneralFeatureIntensity<T,D> intensity = new WrapperMedianCornerIntensity<T,D>(medianFilter,imageType);
		return createGeneral(intensity,featureRadius,pixelTol,maxFeatures);
	}

	/**
	 * Creates a Hessian based blob detector.
	 *
	 * @see HessianBlobIntensity
	 */
	public static <T extends ImageSingleBand,D extends ImageSingleBand>
	GeneralFeatureDetector<T,D> createHessian( HessianBlobIntensity.Type type ,
											   int minSeparation , float cornerThreshold , int maxFeatures , Class<D> derivType)
	{
		GeneralFeatureIntensity<T,D> intensity = FactoryIntensityPoint.hessian(type, derivType);
		return createGeneral(intensity,minSeparation,cornerThreshold,maxFeatures);
	}

	protected static <T extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<T,D> createGeneral( GradientCornerIntensity<D> cornerIntensity ,
											  int minSeparation , float cornerThreshold , int maxFeatures ) {
		GeneralFeatureIntensity<T, D> intensity = new WrapperGradientCornerIntensity<T,D>(cornerIntensity);
		return createGeneral(intensity,minSeparation,cornerThreshold,maxFeatures);
	}

	protected static <T extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<T,D> createGeneral( GeneralFeatureIntensity<T,D> intensity ,
											  int minSeparation , float cornerThreshold , int maxFeatures ) {
		int intensityBorder = intensity.getIgnoreBorder();
		FeatureExtractor extractor = FactoryFeatureExtractor.
				nonmax(minSeparation, cornerThreshold, intensityBorder, false, true);
		GeneralFeatureDetector<T,D> det = new GeneralFeatureDetector<T,D>(intensity,extractor);
		det.setMaxFeatures(maxFeatures);
		
		return det;
	}
}
