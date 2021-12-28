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

import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.intensity.*;
import boofcv.abst.feature.detect.interest.*;
import boofcv.abst.filter.blur.BlurStorageFilter;
import boofcv.alg.feature.detect.intensity.FastCornerDetector;
import boofcv.alg.feature.detect.intensity.GradientCornerIntensity;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.feature.detect.selector.FeatureSelectLimitIntensity;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.factory.feature.detect.selector.FactorySelectLimit;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I16;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * <p>
 * Creates instances of {@link GeneralFeatureDetector}, which detects the location of
 * point features inside an image.
 * </p>
 *
 * <p>
 * NOTE: Sometimes the image border is ignored and some times it is not. If feature intensities are not
 * computed along the image border then it will be full of zeros. In that case the ignore border region
 * needs to be increased for non-max suppression or else it might generate a false positive.
 * </p>
 *
 * @author Peter Abeles
 */
public class FactoryDetectPoint {

	/**
	 * Creates a point detector from the generic configuration
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> create( ConfigPointDetector config, @Nullable Class<T> imageType, @Nullable Class<D> derivType ) {
		if (derivType == null)
			derivType = GImageDerivativeOps.getDerivativeType(Objects.requireNonNull(imageType));

		config.general.detectMaximums = true;
		config.general.detectMinimums = false;
		switch (config.type) {
			case FAST, LAPLACIAN -> config.general.detectMinimums = true;
			default -> {
			}
		}

		return switch (config.type) {
			case HARRIS -> FactoryDetectPoint.createHarris(config.general, config.harris, Objects.requireNonNull(derivType));
			case SHI_TOMASI -> FactoryDetectPoint.createShiTomasi(config.general, config.shiTomasi, derivType);
			case FAST -> FactoryDetectPoint.createFast(config.general, config.fast, Objects.requireNonNull(imageType));
			case KIT_ROS -> FactoryDetectPoint.createKitRos(config.general, Objects.requireNonNull(derivType));
			case MEDIAN -> FactoryDetectPoint.createMedian(config.general, Objects.requireNonNull(imageType));
			case DETERMINANT -> FactoryDetectPoint.createHessianDirect(HessianBlobIntensity.Type.DETERMINANT, config.general, Objects.requireNonNull(imageType));
			case LAPLACIAN -> FactoryDetectPoint.createHessianDirect(HessianBlobIntensity.Type.TRACE, config.general, Objects.requireNonNull(imageType));
			case DETERMINANT_H -> FactoryDetectPoint.createHessianDeriv(config.general, HessianBlobIntensity.Type.DETERMINANT, Objects.requireNonNull(derivType));
			case LAPLACIAN_H -> FactoryDetectPoint.createHessianDeriv(config.general, HessianBlobIntensity.Type.TRACE, Objects.requireNonNull(derivType));
			default -> throw new IllegalArgumentException("Unknown type " + config.type);
		};
	}

	/**
	 * Detects Harris corners.
	 *
	 * @param configDetector Configuration for feature detector.
	 * @param configCorner Configuration for corner intensity computation. If null radius will match detector radius
	 * @param derivType Type of derivative image.
	 * @see boofcv.alg.feature.detect.intensity.HarrisCornerIntensity
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createHarris( @Nullable ConfigGeneralDetector configDetector,
											   @Nullable ConfigHarrisCorner configCorner, Class<D> derivType ) {
		if (configDetector == null)
			configDetector = new ConfigGeneralDetector();
		if (configCorner == null) {
			configCorner = new ConfigHarrisCorner();
			configCorner.radius = configDetector.radius;
		}

		GradientCornerIntensity<D> cornerIntensity =
				FactoryIntensityPointAlg.harris(
						configCorner.radius, (float)configCorner.kappa, configCorner.weighted, derivType);
		return createGeneral(cornerIntensity, configDetector);
	}

	/**
	 * Detects Shi-Tomasi corners.
	 *
	 * @param configDetector Configuration for feature extractor.
	 * @param configCorner Configuration for corner intensity computation. If null radius will match detector radius
	 * @param derivType Type of derivative image.
	 * @see boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createShiTomasi( @Nullable ConfigGeneralDetector configDetector,
												  @Nullable ConfigShiTomasi configCorner,
												  Class<D> derivType ) {
		if (configDetector == null)
			configDetector = new ConfigGeneralDetector();

		if (configCorner == null) {
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
	 * @param derivType Type of derivative image.
	 * @see boofcv.alg.feature.detect.intensity.KitRosCornerIntensity
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createKitRos( @Nullable ConfigGeneralDetector configDetector, Class<D> derivType ) {
		if (configDetector == null)
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
	GeneralFeatureDetector<T, D> createFast( ConfigGeneralDetector configDetector, @Nullable ConfigFastCorner configFast,
											 Class<T> imageType ) {

		if (configFast == null)
			configFast = new ConfigFastCorner();
		configFast.checkValidity();

		FastCornerDetector<T> alg = FactoryIntensityPointAlg.fast(configFast.pixelTol, configFast.minContinuous, imageType);
		GeneralFeatureIntensity<T, D> intensity = new WrapperFastCornerIntensity<>(alg);
		return createGeneral(intensity, configDetector);
	}

	/**
	 * Creates a median filter corner detector.
	 *
	 * @param configDetector Configuration for feature detector.
	 * @param imageType Type of input image.
	 * @see boofcv.alg.feature.detect.intensity.MedianCornerIntensity
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createMedian( @Nullable ConfigGeneralDetector configDetector, Class<T> imageType ) {

		if (configDetector == null)
			configDetector = new ConfigGeneralDetector();

		BlurStorageFilter<T> medianFilter = FactoryBlurFilter.median(ImageType.single(imageType), configDetector.radius);
		GeneralFeatureIntensity<T, D> intensity = new WrapperMedianCornerIntensity<>(medianFilter);
		return createGeneral(intensity, configDetector);
	}

	/**
	 * Creates a Hessian based blob detector. Minimums and Maximums. Uses gradient images.
	 *
	 * @param configDetector Configuration for feature detector.
	 * @param type The type of Hessian based blob detector to use. DETERMINANT often works well.
	 * @param derivType Type of derivative image.
	 * @see HessianBlobIntensity
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createHessianDeriv( @Nullable ConfigGeneralDetector configDetector, HessianBlobIntensity.Type type,
													 Class<D> derivType ) {
		if (configDetector == null)
			configDetector = new ConfigGeneralDetector();

		GeneralFeatureIntensity<T, D> intensity = FactoryIntensityPoint.hessian(type, derivType);
		return createGeneral(intensity, configDetector);
	}

	/**
	 * Creates a Hessian based blob detector. Minimums and Maximums. Direct from input image.
	 *
	 * @param type The type of Hessian based blob detector to use. DETERMINANT often works well.
	 * @param configDetector Configuration for feature detector.
	 * @see HessianBlobIntensity
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createHessianDirect( HessianBlobIntensity.Type type,
													  @Nullable ConfigGeneralDetector configDetector,
													  Class<T> imageType ) {
		if (configDetector == null)
			configDetector = new ConfigGeneralDetector();

		GeneralFeatureIntensity<T, D> intensity = switch (type) {
			case DETERMINANT -> FactoryIntensityPoint.hessianDet(imageType);
			case TRACE -> (GeneralFeatureIntensity)FactoryIntensityPoint.laplacian(imageType);
			default -> throw new IllegalArgumentException("Unknown type");
		};
		return createGeneral(intensity, configDetector);
	}

	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createGeneral( GradientCornerIntensity<D> cornerIntensity,
												ConfigGeneralDetector config ) {
		GeneralFeatureIntensity<T, D> intensity = new WrapperGradientCornerIntensity<>(cornerIntensity);
		return createGeneral(intensity, config);
	}

	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> createGeneral( GeneralFeatureIntensity<T, D> intensity,
												ConfigGeneralDetector config ) {
		ConfigGeneralDetector foo = new ConfigGeneralDetector();
		foo.setTo(config);
		config = foo;
		config.ignoreBorder += config.radius;

		NonMaxSuppression extractorMin = null;
		NonMaxSuppression extractorMax = null;
		if (intensity.localMinimums()) {
			config.detectMinimums = true;
			config.detectMaximums = false;
			extractorMin = FactoryFeatureExtractor.nonmax(config);
		}
		if (intensity.localMaximums()) {
			config.detectMinimums = false;
			config.detectMaximums = true;
			extractorMax = FactoryFeatureExtractor.nonmax(config);
		}
		FeatureSelectLimitIntensity<Point2D_I16> selector = FactorySelectLimit.intensity(config.selector, Point2D_I16.class);
		GeneralFeatureDetector<T, D> det = new GeneralFeatureDetector<>(intensity, extractorMin, extractorMax, selector);
		det.setFeatureLimit(config.maxFeatures);

		return det;
	}
}
