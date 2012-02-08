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

package boofcv.factory.feature.tracker;

import boofcv.abst.feature.associate.GeneralAssociation;
import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.feature.detect.extract.GeneralFeatureDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.tracker.*;
import boofcv.alg.feature.associate.AssociateSurfBasic;
import boofcv.alg.feature.associate.ScoreAssociateEuclideanSq;
import boofcv.alg.feature.associate.ScoreAssociateNccFeature;
import boofcv.alg.feature.associate.ScoreAssociation;
import boofcv.alg.feature.describe.DescribePointBrief;
import boofcv.alg.feature.describe.DescribePointPixelRegionNCC;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.brief.BriefFeature;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.alg.feature.describe.brief.ScoreAssociationBrief;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.feature.orientation.OrientationIntegral;
import boofcv.alg.tracker.pklt.PkltManager;
import boofcv.alg.tracker.pklt.PkltManagerConfig;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.interest.FactoryCornerDetector;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.feature.NccFeature;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageSingleBand;

import java.util.Random;


/**
 * Factory for creating trackers which implement {@link ImagePointTracker}.
 *
 * @author Peter Abeles
 */
public class FactoryPointSequentialTracker {

	/**
	 * Creates a tracker using KLT features/tracker.
	 *
	 * @param maxFeatures Maximum number of features it can detect/track. Try 200 initially.
	 * @param scaling Scales in the image pyramid. Recommend [1,2,4] or [2,4]
	 * @param imageType Input image type.
	 * @param derivType Image derivative  type.
	 * @return KLT based tracker.
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	ImagePointTracker<I> klt( int maxFeatures , int scaling[] , Class<I> imageType , Class<D> derivType )
	{
		PkltManagerConfig<I, D> config =
				PkltManagerConfig.createDefault(imageType,derivType);
		config.pyramidScaling = scaling;
		config.maxFeatures = maxFeatures;
		PkltManager<I, D> trackManager = new PkltManager<I, D>(config);

		return new PstWrapperKltPyramid<I,D>(trackManager);
	}

	/**
	 * Creates a tracker using KLT features/tracker.
	 *
	 * @param config Config for the tracker. Try PkltManagerConfig.createDefault().
	 * @return KLT based tracker.
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	ImagePointTracker<I> klt( PkltManagerConfig<I, D> config )
	{
		PkltManager<I, D> trackManager = new PkltManager<I, D>(config);

		return new PstWrapperKltPyramid<I,D>(trackManager);
	}

	/**
	 * Creates a tracker using SURF features.
	 *
	 * @param maxMatches When features are associated with each other what is the maximum number of associations.
	 * @param detectPerScale Controls how many features can be detected.  Try a value of 200 initially.
	 * @param minSeparation How close together detected features can be.  Recommended value = 2.
	 * @param imageType Type of image the input is.
	 * @param <I> Input image type.
	 * @param <II> Integral image type.
	 * @return SURF based tracker.
	 */
	public static <I extends ImageSingleBand,II extends ImageSingleBand>
	ImagePointTracker<I> surf( int maxMatches , int detectPerScale , int minSeparation ,
									Class<I> imageType )
	{
		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(minSeparation, 1, 10, false, true);

		FastHessianFeatureDetector<II> detector = new FastHessianFeatureDetector<II>(extractor,detectPerScale, 2, 9,4,4);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.average_ii(6,1,6,0,integralType);
		DescribePointSurf<II> describe = new DescribePointSurf<II>(integralType);

		ScoreAssociation<TupleDesc_F64> score = new ScoreAssociateEuclideanSq();
		AssociateSurfBasic assoc = new AssociateSurfBasic(FactoryAssociation.greedy(score, 100000, maxMatches, true));

		return new PstWrapperSurf<I,II>(detector,orientation,describe,assoc,integralType);
	}

	/**
	 * Creates a tracker for BRIEF features.
	 *
	 * @param maxFeatures Maximum number of features it will track.
	 * @param maxAssociationError Maximum allowed association error.  Try 200.
	 * @param pixelDetectTol Tolerance for detecting FAST features.  Try 20.
	 * @param imageType Type of image being processed.
	 */
	public static <I extends ImageSingleBand>
	ImagePointTracker<I> brief( int maxFeatures , int maxAssociationError , int pixelDetectTol , Class<I> imageType ) {
		DescribePointBrief<I> alg = FactoryDescribePointAlgs.brief(FactoryBriefDefinition.gaussian2(new Random(123), 16, 512),
				FactoryBlurFilter.gaussian(imageType, 0, 4));
		GeneralFeatureDetector<I,?> fast = FactoryCornerDetector.createFast(3, pixelDetectTol, maxFeatures, imageType);
		InterestPointDetector<I> detector = FactoryInterestPoint.wrapCorner(fast, imageType, null);
		ScoreAssociationBrief score = new ScoreAssociationBrief();

		GeneralAssociation<BriefFeature> association =
				FactoryAssociation.greedy(score,maxAssociationError,maxFeatures,true);

		return new PstWrapperBrief<I>(alg,detector,association);
	}

	/**
	 * Creates a tracker for rectangular pixel regions that are associated using normalized
	 * cross correlation (NCC)..
	 *
	 * @param maxFeatures Maximum number of features it will track.
	 * @param regionWidth How wide the region is.  Try 5
	 * @param regionHeight How tall the region is.  Try 5
	 * @param pixelDetectTol Tolerance for detecting features.  Try 20.
	 * @param imageType Type of image being processed.
	 */
	public static <I extends ImageSingleBand,D extends ImageSingleBand>
	ImagePointTracker<I> pixelNCC( int maxFeatures , int regionWidth , int regionHeight ,
								   int pixelDetectTol , Class<I> imageType , Class<D> derivType ) {
		DescribePointPixelRegionNCC<I> alg = FactoryDescribePointAlgs.pixelRegionNCC(regionWidth,regionHeight,imageType);
		GeneralFeatureDetector<I,D> corner = FactoryCornerDetector.createFast(2, pixelDetectTol, maxFeatures, imageType);

		InterestPointDetector<I> detector = FactoryInterestPoint.wrapCorner(corner, imageType, derivType);
		ScoreAssociateNccFeature score = new ScoreAssociateNccFeature();

		GeneralAssociation<NccFeature> association =
				FactoryAssociation.greedy(score,0,maxFeatures,true);

		return new PstWrapperPixelNcc<I>(alg,detector,association);
	}
}
