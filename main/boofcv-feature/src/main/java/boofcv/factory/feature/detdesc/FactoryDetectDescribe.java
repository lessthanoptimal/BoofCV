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

package boofcv.factory.feature.detdesc;

import boofcv.abst.feature.describe.ConfigSurfDescribe;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detdesc.*;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.ConfigAverageIntegral;
import boofcv.abst.feature.orientation.ConfigSlidingIntegral;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.DescribePointSurfMod;
import boofcv.alg.feature.describe.DescribePointSurfPlanar;
import boofcv.alg.feature.detdesc.CompleteSift;
import boofcv.alg.feature.detdesc.DetectDescribeSurfPlanar;
import boofcv.alg.feature.detdesc.DetectDescribeSurfPlanar_MT;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.feature.describe.ConfigDescribeRegionPoint;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.factory.feature.orientation.FactoryOrientation;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageMultiBand;
import boofcv.struct.image.ImageType;

import javax.annotation.Nullable;

/**
 * Creates instances of {@link DetectDescribePoint} for different feature detectors/describers.
 *
 * @author Peter Abeles
 */
public class FactoryDetectDescribe {

	/**
	 * Generic factory for all detector / descriptors
	 */
	public static <T extends ImageGray<T>>
	DetectDescribePoint<T,TupleDesc_F64> generic( ConfigDetectDescribe config , Class<T> imageType )
	{
		DetectDescribePoint detDesc = null;

		if( config.typeDetector == ConfigDetectInterestPoint.DetectorType.FAST_HESSIAN ) {
			switch (config.typeDescribe) {
				case SURF_FAST -> detDesc = FactoryDetectDescribe.surfFast(
						config.detectFastHessian, config.describeSurfFast, config.orientation.averageIntegral, imageType);
				case SURF_STABLE -> detDesc = FactoryDetectDescribe.surfStable(
						config.detectFastHessian, config.describeSurfStability, config.orientation.slidingIntegral, imageType);
			}
		} else if( config.typeDescribe == ConfigDescribeRegionPoint.DescriptorType.SIFT ) {
			var configSift = new ConfigCompleteSift();
			configSift.scaleSpace = config.scaleSpaceSift;
			configSift.detector = config.detectSift;
			configSift.describe = config.describeSift;
			detDesc = FactoryDetectDescribe.sift(configSift, imageType);
		}

		if( detDesc != null )
			return detDesc;

		InterestPointDetector detector;
		switch (config.typeDetector) {
			case FAST_HESSIAN -> detector = FactoryInterestPoint.fastHessian(config.detectFastHessian, imageType);
			case SIFT -> detector =
					FactoryInterestPoint.sift(config.scaleSpaceSift, config.detectSift, imageType);
			case POINT -> {
				GeneralFeatureDetector alg = FactoryDetectPoint.create(config.detectPoint, imageType, null);
				detector = FactoryInterestPoint.wrapPoint(alg, config.detectPoint.scaleRadius, imageType, alg.getDerivType());
			}
			default -> throw new IllegalArgumentException("Unknown detector");
		}
		DescribeRegionPoint descriptor = switch (config.typeDescribe) {
			case SURF_FAST -> FactoryDescribeRegionPoint.surfFast(config.describeSurfFast, imageType);
			case SURF_STABLE -> FactoryDescribeRegionPoint.surfStable(config.describeSurfStability, imageType);
			case SIFT -> FactoryDescribeRegionPoint.sift(config.scaleSpaceSift, config.describeSift, imageType);
			case BRIEF -> FactoryDescribeRegionPoint.brief(config.describeBrief, imageType);
			case TEMPLATE -> FactoryDescribeRegionPoint.template(config.describeTemplate, imageType);
			default -> throw new IllegalArgumentException("Unknown descriptor");
		};

		OrientationImage orientation = null;

		// only compute orientation if the descriptor will use it
		if( descriptor.isOriented() ) {
//			if( descriptor.isScalable() ) {
				Class integralType = GIntegralImageOps.getIntegralType(imageType);
				OrientationIntegral orientationII =
				switch( config.orientation.type ) {
					case AVERAGE-> FactoryOrientationAlgs.average_ii(config.orientation.averageIntegral, integralType);
					case SLIDING -> FactoryOrientationAlgs.sliding_ii(config.orientation.slidingIntegral, integralType);
					default -> throw new RuntimeException("Unsupported orientation type for scalable.");
				};
				orientation = FactoryOrientation.convertImage(orientationII, imageType);
//			}
			// TODO add fixed scale orientations
			// TODO move into FactoryOrientation
		}

		return FactoryDetectDescribe.fuseTogether(detector,orientation,descriptor);
	}

	/**
	 * Creates a new SIFT feature detector and describer.
	 *
	 * @see CompleteSift
	 *
	 * @param config Configuration for the SIFT detector and descriptor.
	 * @return SIFT
	 */
	public static <T extends ImageGray<T>>
	DetectDescribePoint<T,TupleDesc_F64> sift( @Nullable ConfigCompleteSift config, Class<T> imageType)
	{
		CompleteSift dds = FactoryDetectDescribeAlgs.sift(config);
		return new DetectDescribe_CompleteSift<>(dds,imageType);
	}

	/**
	 * <p>
	 * Creates a SURF descriptor.  SURF descriptors are invariant to illumination, orientation, and scale.
	 * BoofCV provides two variants. Creates a variant which is designed for speed at the cost of some stability.
	 * Different descriptors are created for color and gray-scale images.
	 * </p>
	 *
	 * <p>
	 * [1] Add tech report when its finished.  See SURF performance web page for now.
	 * </p>
	 *
	 * @see FastHessianFeatureDetector
	 * @see DescribePointSurf
	 * @see DescribePointSurfPlanar
	 *
	 * @param configDetector		Configuration for SURF detector
	 * @param configDesc			Configuration for SURF descriptor
	 * @param configOrientation		Configuration for orientation
	 * @return SURF detector and descriptor
	 */
	public static <T extends ImageGray<T>, II extends ImageGray<II>>
	DetectDescribePoint<T,TupleDesc_F64> surfFast( @Nullable ConfigFastHessian configDetector ,
												   @Nullable ConfigSurfDescribe.Fast configDesc,
												   @Nullable ConfigAverageIntegral configOrientation,
												   Class<T> imageType) {

		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		FastHessianFeatureDetector<II> detector = FactoryInterestPointAlgs.fastHessian(configDetector);
		DescribePointSurf<II> describe = FactoryDescribePointAlgs.surfSpeed(configDesc, integralType);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.average_ii(configOrientation, integralType);

		if(BoofConcurrency.USE_CONCURRENT) {
			return new WrapDetectDescribeSurf_MT<>(detector, orientation, describe, imageType);
		} else {
			return new WrapDetectDescribeSurf<>(detector, orientation, describe, imageType);
		}
	}

	/**
	 * <p>
	 * Color version of SURF stable.  Features are detected in a gray scale image, but the descriptors are
	 * computed using a color image.  Each band in the page adds to the descriptor length.  See
	 * {@link DetectDescribeSurfPlanar} for details.
	 * </p>
	 *
	 * @see FastHessianFeatureDetector
	 * @see DescribePointSurf
	 * @see DescribePointSurfPlanar
	 *
	 * @param configDetector		Configuration for SURF detector
	 * @param configDesc			Configuration for SURF descriptor
	 * @param configOrientation		Configuration for orientation
	 * @return SURF detector and descriptor
	 */
	public static <T extends ImageGray<T>, II extends ImageGray<II>>
	DetectDescribePoint<T,TupleDesc_F64> surfColorFast( @Nullable ConfigFastHessian configDetector ,
														@Nullable ConfigSurfDescribe.Fast configDesc,
														@Nullable ConfigAverageIntegral configOrientation,
														ImageType<T> imageType) {

		Class bandType = imageType.getImageClass();
		Class<II> integralType = GIntegralImageOps.getIntegralType(bandType);

		FastHessianFeatureDetector<II> detector = FactoryInterestPointAlgs.fastHessian(configDetector);
		DescribePointSurf<II> describe = FactoryDescribePointAlgs.surfSpeed(configDesc, integralType);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.average_ii(configOrientation, integralType);

		if( imageType.getFamily() == ImageType.Family.PLANAR) {
			DescribePointSurfPlanar<II> describeMulti =
					new DescribePointSurfPlanar<>(describe, imageType.getNumBands());

			DetectDescribeSurfPlanar<II> detectDesc = createDescribeSurfPlanar(detector, orientation, describeMulti);

			return new SurfPlanar_to_DetectDescribePoint( detectDesc,bandType,integralType );
		} else {
			throw new IllegalArgumentException("Image type not supported");
		}
	}

	protected static <II extends ImageGray<II>> DetectDescribeSurfPlanar<II> createDescribeSurfPlanar(FastHessianFeatureDetector<II> detector, OrientationIntegral<II> orientation, DescribePointSurfPlanar<II> describeMulti) {
		DetectDescribeSurfPlanar<II> detectDesc;
		if( BoofConcurrency.USE_CONCURRENT ) {
			detectDesc = new DetectDescribeSurfPlanar_MT<>(detector, orientation, describeMulti);
		} else {
			detectDesc = new DetectDescribeSurfPlanar<>(detector, orientation, describeMulti);
		}
		return detectDesc;
	}

	/**
	 * <p>
	 * Creates a SURF descriptor.  SURF descriptors are invariant to illumination, orientation, and scale.
	 * BoofCV provides two variants. Creates a variant which is designed for stability. Different descriptors are
	 * created for color and gray-scale images.
	 * </p>
	 *
	 * <p>
	 * [1] Add tech report when its finished.  See SURF performance web page for now.
	 * </p>
	 *
	 * @see DescribePointSurfPlanar
	 * @see FastHessianFeatureDetector
	 * @see boofcv.alg.feature.describe.DescribePointSurfMod
	 *
	 * @param configDetector Configuration for SURF detector.  Null for default.
	 * @param configDescribe Configuration for SURF descriptor.  Null for default.
	 * @param configOrientation Configuration for region orientation.  Null for default.
	 * @param imageType Specify type of input image.
	 * @return SURF detector and descriptor
	 */
	public static <T extends ImageGray<T>, II extends ImageGray<II>>
	DetectDescribePoint<T,TupleDesc_F64> surfStable( @Nullable ConfigFastHessian configDetector,
													 @Nullable ConfigSurfDescribe.Stability configDescribe,
													 @Nullable ConfigSlidingIntegral configOrientation,
													 Class<T> imageType ) {

		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		FastHessianFeatureDetector<II> detector = FactoryInterestPointAlgs.fastHessian(configDetector);
		DescribePointSurfMod<II> describe = FactoryDescribePointAlgs.surfStability(configDescribe, integralType);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.sliding_ii(configOrientation, integralType);

		if(BoofConcurrency.USE_CONCURRENT) {
			return new WrapDetectDescribeSurf_MT<>(detector, orientation, describe, imageType);
		} else {
			return new WrapDetectDescribeSurf<>(detector, orientation, describe, imageType);
		}
	}

	/**
	 * <p>
	 * Color version of SURF stable feature.  Features are detected in a gray scale image, but the descriptors are
	 * computed using a color image.  Each band in the page adds to the descriptor length.  See
	 * {@link DetectDescribeSurfPlanar} for details.
	 * </p>
	 *
	 * @see DescribePointSurfPlanar
	 * @see FastHessianFeatureDetector
	 * @see boofcv.alg.feature.describe.DescribePointSurfMod
	 *
	 * @param configDetector Configuration for SURF detector.  Null for default.
	 * @param configDescribe Configuration for SURF descriptor.  Null for default.
	 * @param configOrientation Configuration for region orientation.  Null for default.
	 * @param imageType Specify type of color input image.
	 * @return SURF detector and descriptor
	 */
	public static <T extends ImageMultiBand<T>, II extends ImageGray<II>>
	DetectDescribePoint<T,TupleDesc_F64> surfColorStable( @Nullable ConfigFastHessian configDetector,
														  @Nullable ConfigSurfDescribe.Stability configDescribe,
														  @Nullable ConfigSlidingIntegral configOrientation,
														  ImageType<T> imageType ) {

		Class bandType = imageType.getImageClass();
		Class<II> integralType = GIntegralImageOps.getIntegralType(bandType);

		FastHessianFeatureDetector<II> detector = FactoryInterestPointAlgs.fastHessian(configDetector);
		DescribePointSurfMod<II> describe = FactoryDescribePointAlgs.surfStability(configDescribe, integralType);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.sliding_ii(configOrientation, integralType);

		if( imageType.getFamily() == ImageType.Family.PLANAR) {
			DescribePointSurfPlanar<II> describeMulti =
					new DescribePointSurfPlanar<>(describe, imageType.getNumBands());

			DetectDescribeSurfPlanar<II> detectDesc = createDescribeSurfPlanar(detector, orientation, describeMulti);

			return new SurfPlanar_to_DetectDescribePoint( detectDesc,bandType,integralType );
		} else {
			throw new IllegalArgumentException("Image type not supported");
		}
	}

	/**
	 * Given independent algorithms for feature detection, orientation, and describing, create a new
	 * {@link DetectDescribePoint}.
	 *
	 * @param detector Feature detector
	 * @param orientation Orientation estimation.  Optionally, can be null.
	 * @param describe Feature descriptor
	 * @return {@link DetectDescribePoint}.
	 */
	public static <T extends ImageGray<T>, D extends TupleDesc>
	DetectDescribePoint<T,D> fuseTogether( InterestPointDetector<T> detector,
										   @Nullable OrientationImage<T> orientation,
										   DescribeRegionPoint<T, D> describe) {
		return new DetectDescribeFusion<>(detector, orientation, describe);
	}

}
