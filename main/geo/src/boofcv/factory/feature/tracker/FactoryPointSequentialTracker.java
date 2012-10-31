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

import boofcv.abst.feature.associate.*;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.describe.WrapDescribeBrief;
import boofcv.abst.feature.describe.WrapDescribePixelRegionNCC;
import boofcv.abst.feature.describe.WrapDescribeSurf;
import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.feature.detect.interest.GeneralFeatureDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.detect.interest.WrapFHtoInterestPoint;
import boofcv.abst.feature.tracker.DetectAssociateTracker;
import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.PstWrapperKltPyramid;
import boofcv.alg.feature.associate.AssociateSurfBasic;
import boofcv.alg.feature.describe.DescribePointBrief;
import boofcv.alg.feature.describe.DescribePointPixelRegionNCC;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.feature.orientation.OrientationIntegral;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.tracker.pklt.GenericPkltFeatSelector;
import boofcv.alg.tracker.pklt.PkltManager;
import boofcv.alg.tracker.pklt.PkltManagerConfig;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.feature.*;
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
	 * @param maxFeatures   Maximum number of features it can detect/track. Try 200 initially.
	 * @param scaling       Scales in the image pyramid. Recommend [1,2,4] or [2,4]
	 * @param featureRadius Feature radius.  Try 3 or 5
	 * @param spawnSubW     Forces a more even distribution of features.  Width.  Try 2
	 * @param spawnSubH     Forces a more even distribution of features.  Height.  Try 3
	 * @param imageType     Input image type.
	 * @param derivType     Image derivative  type.
	 * @return KLT based tracker.
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	ImagePointTracker<I> klt(int maxFeatures, int scaling[], int featureRadius, int spawnSubW, int spawnSubH, Class<I> imageType, Class<D> derivType) {
		PkltManagerConfig<I, D> config =
				PkltManagerConfig.createDefault(imageType, derivType);
		config.pyramidScaling = scaling;
		config.maxFeatures = maxFeatures;
		config.featureRadius = featureRadius;

		GeneralFeatureDetector<I, D> detector =
				FactoryDetectPoint.createShiTomasi(config.featureRadius, false, config.config.minDeterminant, config.maxFeatures, config.typeDeriv);
		detector.setRegions(spawnSubW, spawnSubH);

		GenericPkltFeatSelector<I, D> featureSelector = new GenericPkltFeatSelector<I, D>(detector, null);


		PkltManager<I, D> trackManager = new PkltManager<I, D>();
		trackManager.configure(config,
				FactoryInterpolation.<I>bilinearRectangle(config.typeInput),
				FactoryInterpolation.<D>bilinearRectangle(config.typeDeriv),
				featureSelector);

		return new PstWrapperKltPyramid<I, D>(trackManager);
	}

	/**
	 * Creates a tracker using KLT features/tracker.
	 *
	 * @param config Config for the tracker. Try PkltManagerConfig.createDefault().
	 * @return KLT based tracker.
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	ImagePointTracker<I> klt(PkltManagerConfig<I, D> config) {
		PkltManager<I, D> trackManager = new PkltManager<I, D>(config);

		return new PstWrapperKltPyramid<I, D>(trackManager);
	}

	/**
	 * Creates a tracker using SURF features.
	 *
	 * @param maxMatches     The maximum number of matched features that will be considered.
	 *                       Set to a value <= 0 to not bound the number of matches.
	 * @param detectPerScale Controls how many features can be detected.  Try a value of 200 initially.
	 * @param minSeparation  How close together detected features can be.  Recommended value = 2.
	 * @param imageType      Type of image the input is.
	 * @param <I>            Input image type.
	 * @param <II>           Integral image type.
	 * @return SURF based tracker.
	 */
	public static <I extends ImageSingleBand, II extends ImageSingleBand>
	ImagePointTracker<I> dat_SF_SURF(int maxMatches, int detectPerScale, int minSeparation,
									 Class<I> imageType) {
		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(minSeparation, 1, 10, true);

		FastHessianFeatureDetector<II> detector = new FastHessianFeatureDetector<II>(extractor, detectPerScale, 2, 9, 4, 4);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.average_ii(6, 1, 6, 0, integralType);
		DescribePointSurf<II> describe = new DescribePointSurf<II>(integralType);

		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
		AssociateSurfBasic assoc = new AssociateSurfBasic(FactoryAssociation.greedy(score, 100000, maxMatches, true));

		InterestPointDetector<I> id = new WrapFHtoInterestPoint<I,II>(detector);
		DescribeRegionPoint<I,SurfFeature> regionDesc = new WrapDescribeSurf<I,II>(describe,orientation);
		GeneralAssociation<SurfFeature> generalAssoc = new WrapAssociateSurfBasic(assoc);

		return new DetectAssociateTracker<I,SurfFeature>(id, regionDesc, generalAssoc);
	}

	/**
	 * Creates a tracker for BRIEF features.
	 *
	 * @param maxFeatures         Maximum number of features it will track.
	 * @param maxAssociationError Maximum allowed association error.  Try 200.
	 * @param detectionRadius     Size of feature detection region.  Try 2.
	 * @param cornerThreshold     Tolerance for detecting corner features.  Tune. Start at 1.
	 * @param imageType           Type of image being processed.
	 */
	public static <I extends ImageSingleBand>
	ImagePointTracker<I> dat_ShiTomasi_BRIEF(int maxFeatures, int maxAssociationError, int detectionRadius, float cornerThreshold, Class<I> imageType) {
		DescribePointBrief<I> alg = FactoryDescribePointAlgs.brief(FactoryBriefDefinition.gaussian2(new Random(123), 16, 512),
				FactoryBlurFilter.gaussian(imageType, 0, 4));

		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);
		GeneralFeatureDetector corner = FactoryDetectPoint.createShiTomasi(detectionRadius,false,cornerThreshold, maxFeatures, derivType);

		InterestPointDetector<I> detector = FactoryInterestPoint.wrapPoint(corner, imageType, derivType);
		ScoreAssociateHamming_B score = new ScoreAssociateHamming_B();

		GeneralAssociation<TupleDesc_B> association =
				FactoryAssociation.greedy(score, maxAssociationError, maxFeatures, true);

		return new DetectAssociateTracker<I,TupleDesc_B>(detector, new WrapDescribeBrief<I>(alg), association);
	}

	/**
	 * Creates a tracker for rectangular pixel regions that are associated using normalized
	 * cross correlation (NCC)..
	 *
	 * @param maxFeatures    Maximum number of features it will track.
	 * @param regionWidth    How wide the region is.  Try 5
	 * @param regionHeight   How tall the region is.  Try 5
	 * @param cornerThreshold     Tolerance for detecting corner features.  Tune. Start at 1.
	 * @param imageType      Type of image being processed.
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	ImagePointTracker<I> dat_ShiTomasi_NCC(int maxFeatures, int regionWidth, int regionHeight,
										   float cornerThreshold, Class<I> imageType, Class<D> derivType) {
		DescribePointPixelRegionNCC<I> alg = FactoryDescribePointAlgs.pixelRegionNCC(regionWidth, regionHeight, imageType);

		GeneralFeatureDetector corner = FactoryDetectPoint.createShiTomasi(3, false, cornerThreshold, maxFeatures, derivType);

		InterestPointDetector<I> detector = FactoryInterestPoint.wrapPoint(corner, imageType, derivType);
		ScoreAssociateNccFeature score = new ScoreAssociateNccFeature();

		GeneralAssociation<NccFeature> association =
				FactoryAssociation.greedy(score, 0, maxFeatures, true);

		return new DetectAssociateTracker<I,NccFeature>(detector, new WrapDescribePixelRegionNCC<I>(alg), association);
	}

	/**
	 * Creates a tracker which uses the detect, describe, associate architecture.
	 *
	 * @param detector Interest point detector.
	 * @param describe Region description.
	 * @param associate Description association.
	 * @param updateDescription After a track has been associated should the description be changed?  Try false.
	 * @param pruneAfter Prune tracks which have not been associated with any features after this many images.  Try 2.
	 * @param <I> Type of input image.
	 * @param <Desc> Type of region description
	 * @return tracker
	 */
	public static <I extends ImageSingleBand, Desc extends TupleDesc>
	DetectAssociateTracker<I,Desc> detectDescribeAssociate(InterestPointDetector<I> detector,
														   DescribeRegionPoint<I, Desc> describe,
														   GeneralAssociation<Desc> associate ,
														   boolean updateDescription ,
														   int pruneAfter ) {

		DetectAssociateTracker<I,Desc> dat = new DetectAssociateTracker<I,Desc>(detector, describe, associate);
		dat.setPruneThreshold(pruneAfter);
		dat.setUpdateState(updateDescription);

		return dat;
	}
}
