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

package boofcv.factory.feature.detdesc;

import boofcv.abst.feature.describe.ConfigSiftDescribe;
import boofcv.abst.feature.describe.ConfigSiftScaleSpace;
import boofcv.abst.feature.describe.ConfigSurfDescribe;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detdesc.*;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.*;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.DescribePointSurfMod;
import boofcv.alg.feature.describe.DescribePointSurfMultiSpectral;
import boofcv.alg.feature.detdesc.DetectDescribeSift;
import boofcv.alg.feature.detdesc.DetectDescribeSurfMultiSpectral;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.feature.detect.interest.SiftDetector;
import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageMultiBand;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;

/**
 * Creates instances of {@link DetectDescribePoint} for different feature detectors/describers.
 *
 * @author Peter Abeles
 */
public class FactoryDetectDescribe {

	/**
	 * Creates a new SIFT feature detector and describer.
	 *
	 * @param configSS Configuration for scale-space.  Pass in null for default options.
	 * @param configDetector Configuration for detector.  Pass in null for default options.
	 * @param configOri Configuration for region orientation.  Pass in null for default options.
	 * @param configDesc Configuration for descriptor. Pass in null for default options.
	 * @return SIFT
	 */
	public static DetectDescribePoint<ImageFloat32,SurfFeature>
	sift( ConfigSiftScaleSpace configSS,
		  ConfigSiftDetector configDetector ,
		  ConfigSiftOrientation configOri ,
		  ConfigSiftDescribe configDesc) {

		if( configSS == null )
			configSS = new ConfigSiftScaleSpace();
		configSS.checkValidity();

		SiftImageScaleSpace ss = new SiftImageScaleSpace(configSS.blurSigma, configSS.numScales, configSS.numOctaves,
				configSS.doubleInputImage);

		SiftDetector detector = FactoryInterestPointAlgs.siftDetector(configDetector);

		OrientationHistogramSift orientation = FactoryOrientationAlgs.sift(configOri);
		DescribePointSift describe = FactoryDescribePointAlgs.sift(configDesc);

		DetectDescribeSift combined = new DetectDescribeSift(ss,detector,orientation,describe);

		return new WrapDetectDescribeSift(combined);
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
	 * @see DescribePointSurfMultiSpectral
	 *
	 * @param configDetector		Configuration for SURF detector
	 * @param configDesc			Configuration for SURF descriptor
	 * @param configOrientation		Configuration for orientation
	 * @return SURF detector and descriptor
	 */
	public static <T extends ImageSingleBand, II extends ImageSingleBand>
	DetectDescribePoint<T,SurfFeature> surfFast( ConfigFastHessian configDetector ,
												 ConfigSurfDescribe.Speed configDesc,
												 ConfigAverageIntegral configOrientation,
												 Class<T> imageType) {

		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		FastHessianFeatureDetector<II> detector = FactoryInterestPointAlgs.fastHessian(configDetector);
		DescribePointSurf<II> describe = FactoryDescribePointAlgs.surfSpeed(configDesc, integralType);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.average_ii(configOrientation, integralType);

		return new WrapDetectDescribeSurf<T,II>( detector, orientation, describe );
	}

	/**
	 * <p>
	 * Color version of SURF stable.  Features are detected in a gray scale image, but the descriptors are
	 * computed using a color image.  Each band in the page adds to the descriptor length.  See
	 * {@link DetectDescribeSurfMultiSpectral} for details.
	 * </p>
	 *
	 * @see FastHessianFeatureDetector
	 * @see DescribePointSurf
	 * @see DescribePointSurfMultiSpectral
	 *
	 * @param configDetector		Configuration for SURF detector
	 * @param configDesc			Configuration for SURF descriptor
	 * @param configOrientation		Configuration for orientation
	 * @return SURF detector and descriptor
	 */
	public static <T extends ImageSingleBand, II extends ImageSingleBand>
	DetectDescribePoint<T,SurfFeature> surfColorFast( ConfigFastHessian configDetector ,
													  ConfigSurfDescribe.Speed configDesc,
													  ConfigAverageIntegral configOrientation,
													  ImageType<T> imageType) {

		Class bandType = imageType.getImageClass();
		Class<II> integralType = GIntegralImageOps.getIntegralType(bandType);

		FastHessianFeatureDetector<II> detector = FactoryInterestPointAlgs.fastHessian(configDetector);
		DescribePointSurf<II> describe = FactoryDescribePointAlgs.surfSpeed(configDesc, integralType);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.average_ii(configOrientation, integralType);

		if( imageType.getFamily() == ImageType.Family.MULTI_SPECTRAL ) {
			DescribePointSurfMultiSpectral<II> describeMulti =
					new DescribePointSurfMultiSpectral<II>(describe,imageType.getNumBands());

			DetectDescribeSurfMultiSpectral<II> deteDesc =
					new DetectDescribeSurfMultiSpectral<II>(detector,orientation,describeMulti);

			return new SurfMultiSpectral_to_DetectDescribePoint( deteDesc,bandType,integralType );
		} else {
			throw new IllegalArgumentException("Image type not supported");
		}
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
	 * @see DescribePointSurfMultiSpectral
	 * @see FastHessianFeatureDetector
	 * @see boofcv.alg.feature.describe.DescribePointSurfMod
	 *
	 * @param configDetector Configuration for SURF detector.  Null for default.
	 * @param configDescribe Configuration for SURF descriptor.  Null for default.
	 * @param configOrientation Configuration for region orientation.  Null for default.
	 * @param imageType Specify type of input image.
	 * @return SURF detector and descriptor
	 */
	public static <T extends ImageSingleBand, II extends ImageSingleBand>
	DetectDescribePoint<T,SurfFeature> surfStable( ConfigFastHessian configDetector,
												   ConfigSurfDescribe.Stablility configDescribe,
												   ConfigSlidingIntegral configOrientation,
												   Class<T> imageType ) {

		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		FastHessianFeatureDetector<II> detector = FactoryInterestPointAlgs.fastHessian(configDetector);
		DescribePointSurfMod<II> describe = FactoryDescribePointAlgs.surfStability(configDescribe, integralType);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.sliding_ii(configOrientation, integralType);

		return new WrapDetectDescribeSurf( detector, orientation, describe );
	}

	/**
	 * <p>
	 * Color version of SURF stable feature.  Features are detected in a gray scale image, but the descriptors are
	 * computed using a color image.  Each band in the page adds to the descriptor length.  See
	 * {@link DetectDescribeSurfMultiSpectral} for details.
	 * </p>
	 *
	 * @see DescribePointSurfMultiSpectral
	 * @see FastHessianFeatureDetector
	 * @see boofcv.alg.feature.describe.DescribePointSurfMod
	 *
	 * @param configDetector Configuration for SURF detector.  Null for default.
	 * @param configDescribe Configuration for SURF descriptor.  Null for default.
	 * @param configOrientation Configuration for region orientation.  Null for default.
	 * @param imageType Specify type of color input image.
	 * @return SURF detector and descriptor
	 */
	public static <T extends ImageMultiBand, II extends ImageSingleBand>
	DetectDescribePoint<T,SurfFeature> surfColorStable( ConfigFastHessian configDetector,
														ConfigSurfDescribe.Stablility configDescribe,
														ConfigSlidingIntegral configOrientation,
														ImageType<T> imageType ) {

		Class bandType = imageType.getImageClass();
		Class<II> integralType = GIntegralImageOps.getIntegralType(bandType);

		FastHessianFeatureDetector<II> detector = FactoryInterestPointAlgs.fastHessian(configDetector);
		DescribePointSurfMod<II> describe = FactoryDescribePointAlgs.surfStability(configDescribe, integralType);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.sliding_ii(configOrientation, integralType);

		if( imageType.getFamily() == ImageType.Family.MULTI_SPECTRAL ) {
			DescribePointSurfMultiSpectral<II> describeMulti =
					new DescribePointSurfMultiSpectral<II>(describe,imageType.getNumBands());

			DetectDescribeSurfMultiSpectral<II> deteDesc =
					new DetectDescribeSurfMultiSpectral<II>(detector,orientation,describeMulti);

			return new SurfMultiSpectral_to_DetectDescribePoint( deteDesc,bandType,integralType );
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
	public static <T extends ImageSingleBand, D extends TupleDesc>
	DetectDescribePoint<T,D> fuseTogether( InterestPointDetector<T> detector,
										   OrientationImage<T> orientation,
										   DescribeRegionPoint<T, D> describe) {
		return new DetectDescribeFusion<T, D>(detector,orientation,describe);
	}

}
