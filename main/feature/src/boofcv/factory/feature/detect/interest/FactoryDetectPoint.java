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

import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.feature.detect.intensity.*;
import boofcv.abst.feature.detect.interest.GeneralFeatureDetector;
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
 * Creates instances of {@link GeneralFeatureDetector}, which detects the location of
 * point features inside an image.
 * </p>
 * <p/>
 * <p>
 * NOTE: Sometimes the image border is ignored and some times it is not.  If feature intensities are not
 * computed along the image border then it will be full of zeros.  In that case the ignore border region
 * needs to be increased for non-max suppression or else it might generate a false positive.
 * </p>
 *
 * @author Peter Abeles
 */
public class FactoryDetectPoint {

	/**
	 * Detects Harris corners.
	 *
	 * @param extractRadius   Radius of non-maximum suppression region and corner radius. Try 1 or 2.
	 * @param weighted        Is a Gaussian weight applied to the sample region?  False is much faster.
	 * @param detectThreshold Minimum feature intensity.  Image dependent.  Start tuning at 0 or 1.
	 * @param maxFeatures     The maximum number of detected features it will return.  Try 300
	 * @param derivType       Type of derivative image.
	 * @see boofcv.alg.feature.detect.intensity.HarrisCornerIntensity
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<T, D> createHarris(int extractRadius, boolean weighted,
											  float detectThreshold, int maxFeatures, Class<D> derivType) {
		GradientCornerIntensity<D> cornerIntensity = FactoryIntensityPointAlg.harris(extractRadius, 0.04f, weighted, derivType);
		return createGeneral(cornerIntensity, extractRadius, detectThreshold, maxFeatures);
	}

	/**
	 * Detects Shi-Tomasi corners.
	 *
	 * @param extractRadius   Radius of non-maximum suppression region and corner radius. Try 1 or 2.
	 * @param weighted        Is a Gaussian weight applied to the sample region?  False is much faster.
	 * @param detectThreshold Minimum feature intensity.  Image dependent.  Start tuning at 0 or 1.
	 * @param maxFeatures     The maximum number of detected features it will return.  Try 300
	 * @param derivType       Type of derivative image.
	 * @see boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<T, D> createShiTomasi(int extractRadius, boolean weighted,
												 float detectThreshold, int maxFeatures, Class<D> derivType) {
		GradientCornerIntensity<D> cornerIntensity = FactoryIntensityPointAlg.shiTomasi(extractRadius, weighted, derivType);
		return createGeneral(cornerIntensity, extractRadius, detectThreshold, maxFeatures);
	}

	/**
	 * Detects Kitchen and Rosenfeld corners.
	 *
	 * @param extractRadius   Radius of non-maximum suppression region. Try 1 or 2.
	 * @param detectThreshold Minimum feature intensity.  Image dependent.  Start tuning at 0 or 1.
	 * @param maxFeatures     The maximum number of detected features it will return.  Try 300
	 * @param derivType       Type of derivative image.
	 * @see boofcv.alg.feature.detect.intensity.KitRosCornerIntensity
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<T, D> createKitRos(int extractRadius,
											  float detectThreshold, int maxFeatures, Class<D> derivType) {
		GeneralFeatureIntensity<T, D> intensity = new WrapperKitRosCornerIntensity<T, D>(derivType);
		return createGeneral(intensity, extractRadius, detectThreshold, maxFeatures);
	}

	/**
	 * Creates a Fast corner detector.
	 *
	 * @param extractRadius   Radius of non-maximum suppression region. Try 1 or 2.
	 * @param minContinuous   Minimum number of pixels around the circle that are required to be a corner.  Can be 9 to 12
	 * @param detectThreshold Minimum feature intensity.  Image dependent.  Start tuning at 0 or 1.
	 * @param maxFeatures     The maximum number of detected features it will return.  Try 300
	 * @param imageType       Type of input image.
	 * @see FastCornerIntensity
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<T, D> createFast(int extractRadius,
											int minContinuous,
											int detectThreshold, int maxFeatures, Class<T> imageType) {
		FastCornerIntensity<T> alg = FactoryIntensityPointAlg.fast(detectThreshold, minContinuous, imageType);
		GeneralFeatureIntensity<T, D> intensity = new WrapperFastCornerIntensity<T, D>(alg);
		return createGeneral(intensity, extractRadius, detectThreshold, maxFeatures);
	}

	/**
	 * Creates a median filter corner detector.
	 *
	 * @param extractRadius   Radius of non-maximum suppression region. Try 1 or 2.
	 * @param detectThreshold Minimum feature intensity.  Image dependent.  Start tuning at 0 or 1.
	 * @param maxFeatures     The maximum number of detected features it will return.  Try 300
	 * @param imageType       Type of input image.
	 * @see boofcv.alg.feature.detect.intensity.MedianCornerIntensity
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<T, D> createMedian(int extractRadius, float detectThreshold, int maxFeatures, Class<T> imageType) {
		MedianImageFilter<T> medianFilter = FactoryBlurFilter.median(imageType, extractRadius);
		GeneralFeatureIntensity<T, D> intensity = new WrapperMedianCornerIntensity<T, D>(medianFilter, imageType);
		return createGeneral(intensity, extractRadius, detectThreshold, maxFeatures);
	}

	/**
	 * Creates a Hessian based blob detector.
	 *
	 * @param type            The type of Hessian based blob detector to use. DETERMINANT often works well.
	 * @param extractRadius   Radius of non-maximum suppression region. Try 1 or 2.
	 * @param detectThreshold Minimum feature intensity.  Image dependent.  Start tuning at 0 or 1.
	 * @param maxFeatures     The maximum number of detected features it will return.  Try 300
	 * @param derivType       Type of derivative image.
	 * @see HessianBlobIntensity
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<T, D> createHessian(HessianBlobIntensity.Type type,
											   int extractRadius, float detectThreshold, int maxFeatures, Class<D> derivType) {
		GeneralFeatureIntensity<T, D> intensity = FactoryIntensityPoint.hessian(type, derivType);
		return createGeneral(intensity, extractRadius, detectThreshold, maxFeatures);
	}

	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<T, D> createGeneral(GradientCornerIntensity<D> cornerIntensity,
											   int extractRadius, float detectThreshold, int maxFeatures) {
		GeneralFeatureIntensity<T, D> intensity = new WrapperGradientCornerIntensity<T, D>(cornerIntensity);
		return createGeneral(intensity, extractRadius, detectThreshold, maxFeatures);
	}

	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<T, D> createGeneral(GeneralFeatureIntensity<T, D> intensity,
											   int extractRadius, float detectThreshold, int maxFeatures) {
		int border = intensity.getIgnoreBorder() + extractRadius;
		FeatureExtractor extractor = FactoryFeatureExtractor.
				nonmax(extractRadius, detectThreshold, border, true);
		GeneralFeatureDetector<T, D> det = new GeneralFeatureDetector<T, D>(intensity, extractor);
		det.setMaxFeatures(maxFeatures);

		return det;
	}
}
