/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.intensity.*;
import boofcv.abst.feature.detect.interest.*;
import boofcv.abst.filter.blur.BlurStorageFilter;
import boofcv.alg.feature.detect.intensity.FastCornerDetector;
import boofcv.alg.feature.detect.intensity.GradientCornerIntensity;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.feature.detect.selector.FeatureSelectLimit;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.factory.feature.detect.selector.FactoryMaxSelector;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

import javax.annotation.Nullable;

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
	 * @param configDetector Configuration for feature detector.
	 * @param configCorner   Configuration for corner intensity computation. If null radius will match detector radius
	 * @param derivType       Type of derivative image.
	 * @see boofcv.alg.feature.detect.intensity.HarrisCornerIntensity
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createHarris( @Nullable ConfigGeneralDetector configDetector,
											   @Nullable ConfigHarrisCorner configCorner, Class<D> derivType) {
		if( configDetector == null)
			configDetector = new ConfigGeneralDetector();
		if( configCorner == null) {
			configCorner = new ConfigHarrisCorner();
			configCorner.radius = configDetector.radius;
		}

		GradientCornerIntensity<D> cornerIntensity =
				FactoryIntensityPointAlg.harris(
						configCorner.radius,(float)configCorner.kappa,configCorner.weighted, derivType);
		return createGeneral(cornerIntensity, configDetector);
	}

	/**
	 * Detects Shi-Tomasi corners.
	 *
	 * @param configDetector Configuration for feature extractor.
	 * @param configCorner   Configuration for corner intensity computation. If null radius will match detector radius
	 * @param derivType       Type of derivative image.
	 * @see boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createShiTomasi( @Nullable ConfigGeneralDetector configDetector,
												  @Nullable ConfigShiTomasi configCorner,
												  Class<D> derivType) {
		if( configDetector == null)
			configDetector = new ConfigGeneralDetector();

		if( configCorner == null ) {
			configCorner = new ConfigShiTomasi();
			configCorner.radius = configDetector.radius;
		}

		GradientCornerIntensity<D> cornerIntensity =
				FactoryIntensityPointAlg.shiTomasi(configCorner.radius, configCorner.weighted, derivType);
		return createGeneral(cornerIntensity, configDetector);
	}

	/**
	 * Detects Kitchen and Rosenfeld corners.
	 *
	 * @param configDetector Configuration for feature detector.
	 * @param derivType       Type of derivative image.
	 * @see boofcv.alg.feature.detect.intensity.KitRosCornerIntensity
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createKitRos(@Nullable ConfigGeneralDetector configDetector, Class<D> derivType) {
		if( configDetector == null)
			configDetector = new ConfigGeneralDetector();

		GeneralFeatureIntensity<T, D> intensity = new WrapperKitRosCornerIntensity<>(derivType);
		return createGeneral(intensity, configDetector);
	}

	/**
	 * Creates a Fast corner detector with feature intensity for additional pruning. Fast features
	 * have minimums and maximums.
	 *
	 * @param configDetector Configuration for feature extractor.
	 * @param configFast Configuration for FAST feature detector
	 * @param imageType ype of input image.
	 * @see FastCornerDetector
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createFast(ConfigGeneralDetector configDetector, @Nullable ConfigFastCorner configFast,
											Class<T> imageType) {

		if( configFast == null )
			configFast = new ConfigFastCorner();
		configFast.checkValidity();

		FastCornerDetector<T> alg = FactoryIntensityPointAlg.fast(configFast.pixelTol, configFast.minContinuous, imageType);
		alg.setMaxFeaturesFraction(configFast.maxFeatures);
		GeneralFeatureIntensity<T, D> intensity = new WrapperFastCornerIntensity<>(alg);
		return createGeneral(intensity, configDetector);
	}

	/**
	 * Creates a Fast corner detector
	 *
	 * @param configFast Configuration for FAST feature detector
	 * @param imageType ype of input image.
	 * @see FastCornerDetector
	 */
	public static <T extends ImageGray<T>>
	PointDetector<T> createFast( @Nullable ConfigFastCorner configFast , Class<T> imageType) {
		if( configFast == null )
			configFast = new ConfigFastCorner();
		configFast.checkValidity();

		FastCornerDetector<T> alg = FactoryIntensityPointAlg.fast(configFast.pixelTol, configFast.minContinuous, imageType);
		alg.setMaxFeaturesFraction(configFast.maxFeatures);

		return new WrapFastToPointDetector<>(alg);
	}

	/**
	 * Creates a median filter corner detector.
	 *
	 * @param configDetector Configuration for feature detector.
	 * @param imageType       Type of input image.
	 * @see boofcv.alg.feature.detect.intensity.MedianCornerIntensity
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createMedian(@Nullable ConfigGeneralDetector configDetector, Class<T> imageType) {

		if( configDetector == null)
			configDetector = new ConfigGeneralDetector();

		BlurStorageFilter<T> medianFilter = FactoryBlurFilter.median(ImageType.single(imageType), configDetector.radius);
		GeneralFeatureIntensity<T, D> intensity = new WrapperMedianCornerIntensity<>(medianFilter);
		return createGeneral(intensity, configDetector);
	}

	/**
	 * Creates a Hessian based blob detector. Minimums and Maximums. Uses gradient images.
	 *
	 * @param type            The type of Hessian based blob detector to use. DETERMINANT often works well.
	 * @param configDetector Configuration for feature detector.
	 * @param derivType       Type of derivative image.
	 * @see HessianBlobIntensity
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createHessianDeriv(HessianBlobIntensity.Type type,
													@Nullable ConfigGeneralDetector configDetector, Class<D> derivType) {
		if( configDetector == null)
			configDetector = new ConfigGeneralDetector();

		GeneralFeatureIntensity<T, D> intensity = FactoryIntensityPoint.hessian(type, derivType);
		return createGeneral(intensity, configDetector);
	}

	/**
	 * Creates a Hessian based blob detector. Minimums and Maximums. Direct from input image.
	 *
	 * @param type            The type of Hessian based blob detector to use. DETERMINANT often works well.
	 * @param configDetector Configuration for feature detector.
	 * @see HessianBlobIntensity
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createHessianDirect(HessianBlobIntensity.Type type,
													 @Nullable ConfigGeneralDetector configDetector,
													 Class<T> imageType ) {
		if( configDetector == null)
			configDetector = new ConfigGeneralDetector();

		GeneralFeatureIntensity<T, D> intensity;
		switch (type) {
			case DETERMINANT:
				intensity = FactoryIntensityPoint.hessianDet(imageType);
				break;

			case TRACE:
				intensity = (GeneralFeatureIntensity)FactoryIntensityPoint.laplacian(imageType);
				break;

			default:
				throw new IllegalArgumentException("Unknown type");
		}
		return createGeneral(intensity, configDetector);
	}

	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createGeneral(GradientCornerIntensity<D> cornerIntensity,
											   ConfigGeneralDetector config) {
		GeneralFeatureIntensity<T, D> intensity = new WrapperGradientCornerIntensity<>(cornerIntensity);
		return createGeneral(intensity, config);
	}

	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createGeneral(GeneralFeatureIntensity<T, D> intensity,
											   ConfigGeneralDetector config ) {
		// create a copy since it's going to modify the detector config
		ConfigGeneralDetector foo = new ConfigGeneralDetector();
		foo.setTo(config);
		config = foo;
		config.ignoreBorder += config.radius;
		if( !intensity.localMaximums() )
			config.detectMaximums = false;
		if( !intensity.localMinimums() )
			config.detectMinimums = false;
		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(config);
		FeatureSelectLimit selector = FactoryMaxSelector.create(config.maxSelector);
		GeneralFeatureDetector<T, D> det = new GeneralFeatureDetector<>(intensity, extractor, selector);
		det.setMaxFeatures(config.maxFeatures);

		return det;
	}
}
