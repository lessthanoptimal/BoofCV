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
import boofcv.abst.feature.tracker.*;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.associate.AssociateSurfBasic;
import boofcv.alg.feature.describe.DescribePointBrief;
import boofcv.alg.feature.describe.DescribePointPixelRegionNCC;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.feature.orientation.OrientationIntegral;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.combined.CombinedTrackerScalePoint;
import boofcv.alg.tracker.combined.PyramidKltForCombined;
import boofcv.alg.tracker.klt.KltConfig;
import boofcv.alg.tracker.pklt.GenericPkltFeatSelector;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.feature.*;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidUpdaterDiscrete;

import java.util.Random;


/**
 * Factory for creating trackers which implement {@link ImagePointTracker}.
 *
 * @author Peter Abeles
 */
public class FactoryPointSequentialTracker {

	/**
	 * Pyramid KLT feature tracker.
	 *
	 * @see boofcv.struct.pyramid.PyramidUpdaterDiscrete
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
		PkltConfig<I, D> config =
				PkltConfig.createDefault(imageType, derivType);
		config.pyramidScaling = scaling;
		config.maxFeatures = maxFeatures;
		config.featureRadius = featureRadius;

		return klt(config,spawnSubW,spawnSubH);
	}

	/**
	 * Pyramid KLT feature tracker.
	 *
	 * @see boofcv.struct.pyramid.PyramidUpdaterDiscrete
	 *
	 * @param config Config for the tracker. Try PkltConfig.createDefault().
	 * @param spawnSubW     Forces a more even distribution of features.  Width.  Try 2
	 * @param spawnSubH     Forces a more even distribution of features.  Height.  Try 3
	 * @return KLT based tracker.
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	ImagePointTracker<I> klt(PkltConfig<I, D> config, int spawnSubW, int spawnSubH) {
		GeneralFeatureDetector<I, D> detector =
				FactoryDetectPoint.createShiTomasi(config.featureRadius, false, config.config.minDeterminant, config.maxFeatures, config.typeDeriv);
		detector.setRegions(spawnSubW, spawnSubH);

		GenericPkltFeatSelector<I, D> featureSelector = new GenericPkltFeatSelector<I, D>(detector, null);

		InterpolateRectangle<I> interpInput = FactoryInterpolation.<I>bilinearRectangle(config.typeInput);
		InterpolateRectangle<D> interpDeriv = FactoryInterpolation.<D>bilinearRectangle(config.typeDeriv);

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(config.typeInput, config.typeDeriv);

		PyramidUpdaterDiscrete<I> pyramidUpdater = FactoryPyramid.discreteGaussian(config.typeInput, -1, 2);

		return new PointTrackerKltPyramid<I, D>(config,pyramidUpdater,featureSelector,
				gradient,interpInput,interpDeriv);
	}

	/**
	 * Creates a tracker which detects Fast-Hessian features and describes them with SURF.
	 *
	 * @see boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity
	 * @see DescribePointSurf
	 * @see DetectAssociateTracker
	 *
	 * @param maxTracks The maximum number of tracks it will return. A value <= 0 will return all.
	 * @param minSeparation  How close together detected features can be.  Recommended value = 2.
	 * @param detectPerScale Number of features it will detect per scale.
	 * @param sampleRateFH   Sample rate used by Fast-Hessian detector.  Typically 1 or 2
	 * @param imageType      Type of image the input is.    @return SURF based tracker.
	 */
	public static <I extends ImageSingleBand, II extends ImageSingleBand>
	ImagePointTracker<I> dda_FH_SURF(int maxTracks, int minSeparation, int detectPerScale, int sampleRateFH,
									 Class<I> imageType) {
		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(minSeparation, 1, 10, true);

		int numScalesPerOctave = 4;
		int numOctaves = 4;

		FastHessianFeatureDetector<II> detector =
				new FastHessianFeatureDetector<II>(extractor, detectPerScale,
						sampleRateFH, 9, numScalesPerOctave, numOctaves);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.average_ii(6, 1, 6, 0, integralType);
		DescribePointSurf<II> describe = new DescribePointSurf<II>(integralType);

		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
		AssociateSurfBasic assoc = new AssociateSurfBasic(FactoryAssociation.greedy(score, 100000, maxTracks, true));

		InterestPointDetector<I> id = new WrapFHtoInterestPoint<I,II>(detector);
		DescribeRegionPoint<I,SurfFeature> regionDesc = new WrapDescribeSurf<I,II>(describe,orientation);
		GeneralAssociation<SurfFeature> generalAssoc = new WrapAssociateSurfBasic(assoc);

		return new DetectAssociateTracker<I,SurfFeature>(id, regionDesc, generalAssoc,false);
	}

	/**
	 * Creates a tracker which detects Shi-Tomasi corner features and describes them with BRIEF.
	 *
	 * @see boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity
	 * @see DescribePointBrief
	 * @see DetectAssociateTracker
	 *
	 * @param maxFeatures         Maximum number of features it will track.
	 * @param maxAssociationError Maximum allowed association error.  Try 200.
	 * @param detectionRadius     Size of feature detection region.  Try 2.
	 * @param cornerThreshold     Tolerance for detecting corner features.  Tune. Start at 1.
	 * @param imageType           Type of image being processed.
	 * @param derivType Type of image used to store the image derivative. null == use default
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	ImagePointTracker<I> dda_ShiTomasi_BRIEF(int maxFeatures, int maxAssociationError,
											 int detectionRadius,
											 float cornerThreshold,
											 Class<I> imageType , Class<D> derivType )
	{
		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		DescribePointBrief<I> brief = FactoryDescribePointAlgs.brief(FactoryBriefDefinition.gaussian2(new Random(123), 16, 512),
				FactoryBlurFilter.gaussian(imageType, 0, 4));

		GeneralFeatureDetector<I,D> corner = FactoryDetectPoint.createShiTomasi(detectionRadius,false,cornerThreshold, maxFeatures, derivType);

		InterestPointDetector<I> detector = FactoryInterestPoint.wrapPoint(corner, imageType, derivType);
		ScoreAssociateHamming_B score = new ScoreAssociateHamming_B();

		GeneralAssociation<TupleDesc_B> association =
				FactoryAssociation.greedy(score, maxAssociationError, maxFeatures, true);

		return new DetectAssociateTracker<I,TupleDesc_B>
				(detector, new WrapDescribeBrief<I>(brief), association,false);
	}

	/**
	 * Creates a tracker which detects Shi-Tomasi corner features and describes them with NCC.
	 *
	 * @see boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity
	 * @see DescribePointPixelRegionNCC
	 * @see DetectAssociateTracker
	 *
	 * @param maxFeatures    Maximum number of features it will track.
	 * @param regionWidth    How wide the region is.  Try 5
	 * @param regionHeight   How tall the region is.  Try 5
	 * @param cornerThreshold     Tolerance for detecting corner features.  Tune. Start at 1.
	 * @param imageType      Type of image being processed.
	 * @param derivType      Type of image used to store the image derivative. null == use default
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	ImagePointTracker<I> dda_ShiTomasi_NCC(int maxFeatures, int regionWidth, int regionHeight,
										   float cornerThreshold,
										   Class<I> imageType, Class<D> derivType) {

		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		DescribePointPixelRegionNCC<I> alg = FactoryDescribePointAlgs.pixelRegionNCC(regionWidth, regionHeight, imageType);

		GeneralFeatureDetector corner = FactoryDetectPoint.createShiTomasi(3, false, cornerThreshold, maxFeatures, derivType);

		InterestPointDetector<I> detector = FactoryInterestPoint.wrapPoint(corner, imageType, derivType);
		ScoreAssociateNccFeature score = new ScoreAssociateNccFeature();

		GeneralAssociation<NccFeature> association =
				FactoryAssociation.greedy(score, Double.MAX_VALUE, maxFeatures, true);

		return new DetectAssociateTracker<I,NccFeature>
				(detector, new WrapDescribePixelRegionNCC<I>(alg), association,false);
	}

	/**
	 * Creates a tracker which uses the detect, describe, associate architecture.
	 *
	 * @param detector Interest point detector.
	 * @param describe Region description.
	 * @param associate Description association.
	 * @param updateDescription After a track has been associated should the description be changed?  Try false.
	 * @param imageType     Input image type.
	 * @param <I> Type of input image.
	 * @param <Desc> Type of region description
	 * @return tracker
	 */
	public static <I extends ImageSingleBand, Desc extends TupleDesc>
	DetectAssociateTracker<I,Desc> detectDescribeAssociate(InterestPointDetector<I> detector,
														   DescribeRegionPoint<I, Desc> describe,
														   GeneralAssociation<Desc> associate ,
														   boolean updateDescription ,
														   Class<I> imageType ) {

		DetectAssociateTracker<I,Desc> dat = new DetectAssociateTracker<I,Desc>(detector, describe, associate,false);
		dat.setUpdateDescription(updateDescription);

		return dat;
	}

	/**
	 * Creates a tracker which detects Fast-Hessian features and describes them with SURF.
	 *
	 * @see boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity
	 * @see DescribePointSurf
	 * @see DetectAssociateTracker
	 *
	 * @param maxMatches     The maximum number of matched features that will be considered.
	 *                       Set to a value <= 0 to not bound the number of matches.
	 * @param detectPerScale Controls how many features can be detected.  Try a value of 200 initially.
	 * @param featureRadius  Size of tracked KLT feature and how close detected features can be.  Recommended value = 2.
	 * @param imageType      Type of image the input is.
	 * @param <I>            Input image type.
	 * @param <II>           Integral image type.
	 * @return SURF based tracker.
	 */
	public static <I extends ImageSingleBand, II extends ImageSingleBand>
	ImagePointTracker<I> combined_FH_SURF_KLT(int maxMatches, int detectPerScale, int featureRadius,
											  KltConfig configKlt, int[] pyramidScalingKlt ,
											  int reactiveThreshold ,
											  Class<I> imageType) {
		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(featureRadius, 1, 10, true);

		FastHessianFeatureDetector<II> detector = new FastHessianFeatureDetector<II>(extractor, detectPerScale, 2, 9, 4, 4);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.average_ii(6, 1, 6, 0, integralType);
		DescribePointSurf<II> describe = new DescribePointSurf<II>(integralType);

		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
		AssociateSurfBasic assoc = new AssociateSurfBasic(FactoryAssociation.greedy(score, 100000, maxMatches, true));

		InterestPointDetector<I> id = new WrapFHtoInterestPoint<I,II>(detector);
		DescribeRegionPoint<I,SurfFeature> regionDesc = new WrapDescribeSurf<I,II>(describe,orientation);
		GeneralAssociation<SurfFeature> generalAssoc = new WrapAssociateSurfBasic(assoc);


		return combined(id,regionDesc,generalAssoc,configKlt,featureRadius,pyramidScalingKlt,reactiveThreshold,
				imageType);
	}

	/**
	 * Creates a tracker that is a hybrid between KLT and Detect-Describe-Associate (DDA) trackers.
	 *
	 * @see CombinedTrackerScalePoint
	 *
	 * @param detector Feature detector.
	 * @param describe Feature description
	 * @param associate Association algorithm.
	 * @param configKlt KLT configuration
	 * @param featureRadiusKlt KLT feature radius
	 * @param pyramidScalingKlt KLT pyramid configuration
	 * @param reactiveThreshold Threshold for when dormant tracks are.  Try 10% of expected number of tracks.
	 * @param imageType Input image type.
	 * @param <I> Input image type.
	 * @param <D> Derivative image type.
	 * @param <Desc> Feature description type.
	 * @return Feature tracker
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand, Desc extends TupleDesc>
	ImagePointTracker<I> combined( InterestPointDetector<I> detector,
								   DescribeRegionPoint<I, Desc> describe,
								   GeneralAssociation<Desc> associate ,
								   KltConfig configKlt,
								   int featureRadiusKlt,
								   int[] pyramidScalingKlt ,
								   int reactiveThreshold,
								   Class<I> imageType )
	{
		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);

		PyramidKltForCombined<I,D> klt = new PyramidKltForCombined<I, D>(configKlt,
				featureRadiusKlt,pyramidScalingKlt,imageType,derivType);

		CombinedTrackerScalePoint<I, D,Desc> tracker =
				new CombinedTrackerScalePoint<I, D, Desc>(klt,detector,describe,associate);

		return new WrapCombinedTracker<I,D,Desc>(tracker,reactiveThreshold,imageType,derivType);
	}
}
