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

package boofcv.factory.feature.detdesc;

import boofcv.abst.feature.describe.ConfigSurfDescribe;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detdesc.DetectDescribeFusion;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detdesc.WrapDetectDescribeSift;
import boofcv.abst.feature.detdesc.WrapDetectDescribeSurf;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.ConfigAverageIntegral;
import boofcv.abst.feature.orientation.ConfigSlidingIntegral;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.detdesc.DetectDescribeSift;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.feature.detect.interest.SiftDetector;
import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.BoofDefaults;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

/**
 * Creates instances of {@link DetectDescribePoint} for different feature detectors/describers.
 *
 * @author Peter Abeles
 */
public class FactoryDetectDescribe {

	/**
	 * Creates a new SIFT feature and describer.  Only provides access to a few critical parameters and uses
	 * default settings for the rest.
	 *
	 * @param numOfOctaves Number of octaves to detect.  Try 4
	 * @param detectThreshold Minimum corner intensity required.  Try 1
	 * @param doubleInputImage Should the input image be doubled? Try false.
	 * @param maxFeaturesPerScale Max detected features per scale.  Disable with < 0.  Try 500
	 * @return SIFT
	 */
	public static DetectDescribePoint<ImageFloat32,SurfFeature>
	sift( int numOfOctaves ,
		  float detectThreshold ,
		  boolean doubleInputImage ,
		  int maxFeaturesPerScale ) {
		return sift(1.6,5,numOfOctaves,doubleInputImage,3,detectThreshold,maxFeaturesPerScale,10,36);
	}

	/**
	 * Creates a new SIFT feature detector and describer.  Provides access to most parameters.
	 *
	 * @param scaleSigma Amount of blur applied to each scale inside an octaves.  Try 1.6
	 * @param numOfScales Number of scales per octaves.  Try 5.  Must be >= 3
	 * @param numOfOctaves Number of octaves to detect.  Try 4
	 * @param doubleInputImage Should the input image be doubled? Try false.
	 * @param extractRadius   Size of the feature used to detect the corners. Try 2
	 * @param detectThreshold Minimum corner intensity required.  Try 1
	 * @param maxFeaturesPerScale Max detected features per scale.  Disable with < 0.  Try 500
	 * @param edgeThreshold Threshold for edge filtering.  Disable with a value <= 0.  Try 5
	 * @param oriHistogramSize Orientation histogram size.  Standard is 36
	 * @return SIFT
	 */
	public static DetectDescribePoint<ImageFloat32,SurfFeature>
	sift( double scaleSigma ,
		  int numOfScales ,
		  int numOfOctaves ,
		  boolean doubleInputImage ,
		  int extractRadius,
		  float detectThreshold,
		  int maxFeaturesPerScale,
		  double edgeThreshold ,
		  int oriHistogramSize ) {

		double sigmaToRadius = BoofDefaults.SCALE_SPACE_CANONICAL_RADIUS;

		SiftImageScaleSpace ss = new SiftImageScaleSpace((float)scaleSigma, numOfScales, numOfOctaves,
				doubleInputImage);

		SiftDetector detector = FactoryInterestPointAlgs.siftDetector(extractRadius, detectThreshold,
				maxFeaturesPerScale, edgeThreshold);

		OrientationHistogramSift orientation = new OrientationHistogramSift(oriHistogramSize,sigmaToRadius,1.5);
		DescribePointSift describe = new DescribePointSift(4,8,8,0.5, sigmaToRadius);

		DetectDescribeSift combined = new DetectDescribeSift(ss,detector,orientation,describe);

		return new WrapDetectDescribeSift(combined);
	}

	/**
	 * <p>
	 * Creates a SURF descriptor.  SURF descriptors are invariant to illumination, orientation, and scale.
	 * BoofCV provides two variants. Creates a variant which is designed for speed at the cost of some stability.
	 * </p>
	 *
	 * <p>
	 * [1] Add tech report when its finished.  See SURF performance web page for now.
	 * </p>
	 *
	 * @see FastHessianFeatureDetector
	 * @see DescribePointSurf
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
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.average_ii(configOrientation, integralType);
		DescribePointSurf<II> describe = FactoryDescribePointAlgs.surfSpeed(configDesc, integralType);
		FastHessianFeatureDetector<II> detector = FactoryInterestPointAlgs.fastHessian(configDetector);

		return new WrapDetectDescribeSurf<T,II>( detector, orientation, describe );
	}

	/**
	 * <p>
	 * Creates a SURF descriptor.  SURF descriptors are invariant to illumination, orientation, and scale.
	 * BoofCV provides two variants. Creates a variant which is designed for stability..
	 * </p>
	 *
	 * <p>
	 * [1] Add tech report when its finished.  See SURF performance web page for now.
	 * </p>
	 *
	 * @see FastHessianFeatureDetector
	 * @see DescribePointSurf
	 *
	 * @param configDetector Configuration for SURF detector.  Null for default.
	 * @param configDescribe Configuration for SURF descriptor.  Null for default.
	 * @param configOrientation Configuration for region orientation.  Null for default.
	 * @return SURF detector and descriptor
	 */
	public static <T extends ImageSingleBand, II extends ImageSingleBand>
	DetectDescribePoint<T,SurfFeature> surfStable( ConfigFastHessian configDetector,
												   ConfigSurfDescribe.Stablility configDescribe,
												   ConfigSlidingIntegral configOrientation,
												   Class<T> imageType) {

		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);
		FastHessianFeatureDetector<II> detector = FactoryInterestPointAlgs.fastHessian(configDetector);
		DescribePointSurf<II> describe = FactoryDescribePointAlgs.surfStability(configDescribe, integralType);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.sliding_ii(configOrientation, integralType);

		return new WrapDetectDescribeSurf<T,II>( detector, orientation, describe );
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
