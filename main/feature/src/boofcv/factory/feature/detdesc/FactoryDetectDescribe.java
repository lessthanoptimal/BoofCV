/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.feature.detect.extract.NonMaxLimiter;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.*;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.DescribePointSurfMod;
import boofcv.alg.feature.describe.DescribePointSurfPlanar;
import boofcv.alg.feature.detdesc.CompleteSift;
import boofcv.alg.feature.detdesc.DetectDescribeSurfPlanar;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageMultiBand;
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
	 * @see CompleteSift
	 *
	 * @param config Configuration for the SIFT detector and descriptor.
	 * @return SIFT
	 */
	public static <T extends ImageGray>
	DetectDescribePoint<T,BrightFeature> sift(ConfigCompleteSift config )
	{
		if( config == null )
			config = new ConfigCompleteSift();

		ConfigSiftScaleSpace configSS = config.scaleSpace;
		ConfigSiftDetector configDetector = config.detector;
		ConfigSiftOrientation configOri = config.orientation;
		ConfigSiftDescribe configDesc = config.describe;

		SiftScaleSpace scaleSpace = new SiftScaleSpace(
				configSS.firstOctave,configSS.lastOctave,configSS.numScales,configSS.sigma0);
		OrientationHistogramSift<GrayF32> orientation = new OrientationHistogramSift<>(
				configOri.histogramSize,configOri.sigmaEnlarge,GrayF32.class);
		DescribePointSift<GrayF32> describe = new DescribePointSift<>(
				configDesc.widthSubregion,configDesc.widthGrid, configDesc.numHistogramBins,
				configDesc.sigmaToPixels, configDesc.weightingSigmaFraction,
				configDesc.maxDescriptorElementValue,GrayF32.class);

		NonMaxSuppression nns = FactoryFeatureExtractor.nonmax(configDetector.extract);
		NonMaxLimiter nonMax = new NonMaxLimiter(nns,configDetector.maxFeaturesPerScale);
		CompleteSift dds = new CompleteSift(scaleSpace,configDetector.edgeR,nonMax,orientation,describe);
		return new DetectDescribe_CompleteSift<>(dds);
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
	public static <T extends ImageGray, II extends ImageGray>
	DetectDescribePoint<T,BrightFeature> surfFast(ConfigFastHessian configDetector ,
												  ConfigSurfDescribe.Speed configDesc,
												  ConfigAverageIntegral configOrientation,
												  Class<T> imageType) {

		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		FastHessianFeatureDetector<II> detector = FactoryInterestPointAlgs.fastHessian(configDetector);
		DescribePointSurf<II> describe = FactoryDescribePointAlgs.surfSpeed(configDesc, integralType);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.average_ii(configOrientation, integralType);

		return new WrapDetectDescribeSurf<>(detector, orientation, describe);
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
	public static <T extends ImageGray, II extends ImageGray>
	DetectDescribePoint<T,BrightFeature> surfColorFast(ConfigFastHessian configDetector ,
													   ConfigSurfDescribe.Speed configDesc,
													   ConfigAverageIntegral configOrientation,
													   ImageType<T> imageType) {

		Class bandType = imageType.getImageClass();
		Class<II> integralType = GIntegralImageOps.getIntegralType(bandType);

		FastHessianFeatureDetector<II> detector = FactoryInterestPointAlgs.fastHessian(configDetector);
		DescribePointSurf<II> describe = FactoryDescribePointAlgs.surfSpeed(configDesc, integralType);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.average_ii(configOrientation, integralType);

		if( imageType.getFamily() == ImageType.Family.PLANAR) {
			DescribePointSurfPlanar<II> describeMulti =
					new DescribePointSurfPlanar<>(describe, imageType.getNumBands());

			DetectDescribeSurfPlanar<II> deteDesc =
					new DetectDescribeSurfPlanar<>(detector, orientation, describeMulti);

			return new SurfPlanar_to_DetectDescribePoint( deteDesc,bandType,integralType );
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
	public static <T extends ImageGray, II extends ImageGray>
	DetectDescribePoint<T,BrightFeature> surfStable(ConfigFastHessian configDetector,
													ConfigSurfDescribe.Stability configDescribe,
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
	public static <T extends ImageMultiBand, II extends ImageGray>
	DetectDescribePoint<T,BrightFeature> surfColorStable(ConfigFastHessian configDetector,
														 ConfigSurfDescribe.Stability configDescribe,
														 ConfigSlidingIntegral configOrientation,
														 ImageType<T> imageType ) {

		Class bandType = imageType.getImageClass();
		Class<II> integralType = GIntegralImageOps.getIntegralType(bandType);

		FastHessianFeatureDetector<II> detector = FactoryInterestPointAlgs.fastHessian(configDetector);
		DescribePointSurfMod<II> describe = FactoryDescribePointAlgs.surfStability(configDescribe, integralType);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.sliding_ii(configOrientation, integralType);

		if( imageType.getFamily() == ImageType.Family.PLANAR) {
			DescribePointSurfPlanar<II> describeMulti =
					new DescribePointSurfPlanar<>(describe, imageType.getNumBands());

			DetectDescribeSurfPlanar<II> deteDesc =
					new DetectDescribeSurfPlanar<>(detector, orientation, describeMulti);

			return new SurfPlanar_to_DetectDescribePoint( deteDesc,bandType,integralType );
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
	public static <T extends ImageGray, D extends TupleDesc>
	DetectDescribePoint<T,D> fuseTogether( InterestPointDetector<T> detector,
										   OrientationImage<T> orientation,
										   DescribeRegionPoint<T, D> describe) {
		return new DetectDescribeFusion<>(detector, orientation, describe);
	}

}
