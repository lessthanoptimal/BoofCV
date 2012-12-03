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
import boofcv.abst.feature.detdesc.DetectDescribeFusion;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.GeneralFeatureDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.abst.feature.tracker.*;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.associate.AssociateSurfBasic;
import boofcv.alg.feature.describe.DescribePointBrief;
import boofcv.alg.feature.describe.DescribePointPixelRegionNCC;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.combined.CombinedTrackerScalePoint;
import boofcv.alg.tracker.combined.PyramidKltForCombined;
import boofcv.alg.tracker.klt.KltConfig;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientation;
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

		InterpolateRectangle<I> interpInput = FactoryInterpolation.<I>bilinearRectangle(config.typeInput);
		InterpolateRectangle<D> interpDeriv = FactoryInterpolation.<D>bilinearRectangle(config.typeDeriv);

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(config.typeInput, config.typeDeriv);

		PyramidUpdaterDiscrete<I> pyramidUpdater = FactoryPyramid.discreteGaussian(config.typeInput, -1, 2);

		return new PointTrackerKltPyramid<I, D>(config,pyramidUpdater,detector,
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
	 * @param detectRadius  How close together detected features can be.  Recommended value = 2.
	 * @param detectPerScale Number of features it will detect per scale.
	 * @param sampleRateFH   Sample rate used by Fast-Hessian detector.  Typically 1 or 2
	 * @param modifiedSURF   true for more robust but slower descriptor and false for faster but less robust
	 * @param imageType      Type of image the input is.
	 * @return SURF based tracker.
	 */
	public static <I extends ImageSingleBand>
	ImagePointTracker<I> dda_FH_SURF(int maxTracks, int detectRadius, int detectPerScale, int sampleRateFH,
									 boolean modifiedSURF ,
									 Class<I> imageType)
	{
		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
		AssociateSurfBasic assoc = new AssociateSurfBasic(FactoryAssociation.greedy(score, 100000, maxTracks, true));

		GeneralAssociation<SurfFeature> generalAssoc = new WrapAssociateSurfBasic(assoc);

		DetectDescribePoint<I,SurfFeature> fused =
				FactoryDetectDescribe.surf(1,detectRadius,detectPerScale, sampleRateFH, 9, 4, 4,
						modifiedSURF,imageType);

		return new DetectAssociateTracker<I,SurfFeature>(fused, generalAssoc,false);
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
	 * @param detectThreshold     Tolerance for detecting corner features.  Tune. Start at 1.
	 * @param imageType           Type of image being processed.
	 * @param derivType Type of image used to store the image derivative. null == use default
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	ImagePointTracker<I> dda_ST_BRIEF(int maxFeatures, int maxAssociationError,
									  int detectionRadius,
									  float detectThreshold,
									  Class<I> imageType, Class<D> derivType)
	{
		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		DescribePointBrief<I> brief = FactoryDescribePointAlgs.brief(FactoryBriefDefinition.gaussian2(new Random(123), 16, 512),
				FactoryBlurFilter.gaussian(imageType, 0, 4));

		GeneralFeatureDetector<I,D> corner = FactoryDetectPoint.createShiTomasi(detectionRadius,true,detectThreshold, maxFeatures, derivType);

		InterestPointDetector<I> detector = FactoryInterestPoint.wrapPoint(corner, imageType, derivType);
		ScoreAssociateHamming_B score = new ScoreAssociateHamming_B();

		GeneralAssociation<TupleDesc_B> association =
				FactoryAssociation.greedy(score, maxAssociationError, maxFeatures, true);

		DetectDescribeFusion<I,TupleDesc_B> fused =
				new DetectDescribeFusion<I,TupleDesc_B>(detector,null,new WrapDescribeBrief<I>(brief));

		return new DetectAssociateTracker<I,TupleDesc_B>
				(fused, association,false);
	}

	/**
	 * Creates a tracker which detects FAST corner features and describes them with BRIEF.
	 *
	 * @see boofcv.alg.feature.detect.intensity.FastCornerIntensity
	 * @see DescribePointBrief
	 * @see DetectAssociateTracker
	 *
	 * @param maxFeatures         Maximum number of features it will track.
	 * @param maxAssociationError Maximum allowed association error.  Try 200.
	 * @param detectionRadius     Size of feature detection region.  Try 3.
	 * @param minContinuous       Minimum number of pixels in a row for a circle to be declared a corner.  9 to 12
	 * @param detectThreshold     Tolerance for detecting corner features.  Tune. Try 15.
	 * @param imageType           Type of image being processed.
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	ImagePointTracker<I> dda_FAST_BRIEF(int maxFeatures, int maxAssociationError,
										int detectionRadius,
										int minContinuous,
										int detectThreshold,
										Class<I> imageType )
	{
		DescribePointBrief<I> brief = FactoryDescribePointAlgs.brief(FactoryBriefDefinition.gaussian2(new Random(123), 16, 512),
				FactoryBlurFilter.gaussian(imageType, 0, 4));

		GeneralFeatureDetector<I,D> corner = FactoryDetectPoint.createFast(detectionRadius, minContinuous, detectThreshold, maxFeatures, imageType);

		InterestPointDetector<I> detector = FactoryInterestPoint.wrapPoint(corner, imageType, null);
		ScoreAssociateHamming_B score = new ScoreAssociateHamming_B();

		GeneralAssociation<TupleDesc_B> association =
				FactoryAssociation.greedy(score, maxAssociationError, maxFeatures, true);

		DetectDescribeFusion<I,TupleDesc_B> fused =
				new DetectDescribeFusion<I,TupleDesc_B>(detector,null,new WrapDescribeBrief<I>(brief));

		return new DetectAssociateTracker<I,TupleDesc_B>
				(fused, association,false);
	}

	/**
	 * Creates a tracker which detects Shi-Tomasi corner features and describes them with NCC.
	 *
	 * @see boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity
	 * @see DescribePointPixelRegionNCC
	 * @see DetectAssociateTracker
	 *
	 * @param maxFeatures    Maximum number of features it will track.
	 * @param detectRadius   Size of search region used when extracting image features.  Try 2.
	 * @param describeRadius Radius of the region being described.  Try 2.
	 * @param cornerThreshold     Tolerance for detecting corner features.  Tune. Start at 1.
	 * @param imageType      Type of image being processed.
	 * @param derivType      Type of image used to store the image derivative. null == use default     */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	ImagePointTracker<I> dda_ST_NCC(int maxFeatures, int detectRadius, int describeRadius,
									float cornerThreshold,
									Class<I> imageType, Class<D> derivType) {

		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		int w = 2*describeRadius+1;

		DescribePointPixelRegionNCC<I> alg = FactoryDescribePointAlgs.pixelRegionNCC(w, w, imageType);

		GeneralFeatureDetector corner = FactoryDetectPoint.createShiTomasi(detectRadius, false, cornerThreshold, maxFeatures, derivType);

		InterestPointDetector<I> detector = FactoryInterestPoint.wrapPoint(corner, imageType, derivType);
		ScoreAssociateNccFeature score = new ScoreAssociateNccFeature();

		GeneralAssociation<NccFeature> association =
				FactoryAssociation.greedy(score, Double.MAX_VALUE, maxFeatures, true);

		DetectDescribeFusion<I,NccFeature> fused =
				new DetectDescribeFusion<I,NccFeature>(detector,null,new WrapDescribePixelRegionNCC<I>(alg));

		return new DetectAssociateTracker<I,NccFeature>
				(fused, association,false);
	}

	/**
	 * Creates a tracker which uses the detect, describe, associate architecture.
	 *
	 * @param detector Interest point detector.
	 * @param orientation Optional orientation estimation algorithm. Can be null.
	 * @param describe Region description.
	 * @param associate Description association.
	 * @param updateDescription After a track has been associated should the description be changed?  Try false.
	 * @param <I> Type of input image.
	 * @param <Desc> Type of region description
	 * @return tracker
	 */
	public static <I extends ImageSingleBand, Desc extends TupleDesc>
	DetectAssociateTracker<I,Desc> detectDescribeAssociate(InterestPointDetector<I> detector,
														   OrientationImage<I> orientation ,
														   DescribeRegionPoint<I, Desc> describe,
														   GeneralAssociation<Desc> associate ,
														   boolean updateDescription ) {

		DetectDescribeFusion<I,Desc> fused =
				new DetectDescribeFusion<I,Desc>(detector,orientation,describe);

		DetectAssociateTracker<I,Desc> dat = new DetectAssociateTracker<I,Desc>(fused, associate,updateDescription);

		return dat;
	}

	/**
	 * Creates a tracker which detects Shi-Tomasi corner features and describes them with SURF.
	 *
	 * @see boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity
	 * @see DescribePointSurf
	 * @see DetectAssociateTracker
	 *
	 * @param maxMatches     The maximum number of matched features that will be considered.
	 *                       Set to a value <= 0 to not bound the number of matches.
	 * @param detectPerScale Controls how many features can be detected.  Try a value of 200 initially.
	 * @param sampleRateFH   Sample rate used by Fast-Hessian detector.  Typically 1 or 2
	 * @param detectRadius  Size of tracked KLT feature and how close detected features can be.  Recommended value = 2.
	 * @param trackRadius Size of feature being tracked by KLT
	 * @param pyramidScalingKlt Image pyramid used for KLT
	 * @param reactivateThreshold Tracks are reactivated after this many have been dropped.  Try 10% of maxMatches
	 * @param imageType      Type of image the input is.
	 * @param <I>            Input image type.
	 * @return SURF based tracker.
	 */
	public static <I extends ImageSingleBand>
	ImagePointTracker<I> combined_FH_SURF_KLT(int maxMatches, int detectPerScale,
											  int detectRadius,
											  int sampleRateFH,
											  int trackRadius,
											  int[] pyramidScalingKlt ,
											  int reactivateThreshold ,
											  boolean modifiedSURF ,
											  Class<I> imageType) {

		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
		AssociateSurfBasic assoc = new AssociateSurfBasic(FactoryAssociation.greedy(score, 100000, maxMatches, true));

		InterestPointDetector<I> id = FactoryInterestPoint.fastHessian(1,detectRadius,detectPerScale, sampleRateFH, 9, 4, 4);
		GeneralAssociation<SurfFeature> generalAssoc = new WrapAssociateSurfBasic(assoc);

		DescribeRegionPoint<I,SurfFeature> regionDesc
				= FactoryDescribeRegionPoint.surf(modifiedSURF, imageType);

		DetectDescribePoint<I,SurfFeature> fused =
				FactoryDetectDescribe.surf(1,detectRadius,detectPerScale, sampleRateFH, 9, 4, 4,
						modifiedSURF,imageType);

		return combined(fused,generalAssoc,trackRadius,pyramidScalingKlt,reactivateThreshold,
				imageType);
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
	 * @param detectRadius  Size of tracked KLT feature and how close detected features can be.  Recommended value = 2.
	 * @param detectThreshold Tolerance for detecting corner features.  Tune. Start at 1.
	 * @param trackRadius Size of feature being tracked by KLT
	 * @param pyramidScalingKlt Image pyramid used for KLT
	 * @param reactivateThreshold Tracks are reactivated after this many have been dropped.  Try 10% of maxMatches
	 * @param modifiedSURF   true for more robust but slower descriptor and false for faster but less robust
	 * @param imageType      Type of image the input is.
	 * @param derivType      Image derivative type.
	 * @param <I>            Input image type.
	 * @return SURF based tracker.
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	ImagePointTracker<I> combined_ST_SURF_KLT(int maxMatches,
											  int detectRadius,
											  float detectThreshold,
											  int trackRadius,
											  int[] pyramidScalingKlt ,
											  int reactivateThreshold ,
											  boolean modifiedSURF ,
											  Class<I> imageType,
											  Class<D> derivType ) {

		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		GeneralFeatureDetector corner = FactoryDetectPoint.createShiTomasi(detectRadius, false, detectThreshold, maxMatches, derivType);

		InterestPointDetector<I> detector = FactoryInterestPoint.wrapPoint(corner, imageType, derivType);

		DescribeRegionPoint<I,SurfFeature> regionDesc
				= FactoryDescribeRegionPoint.surf(modifiedSURF, imageType);

		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
		AssociateSurfBasic assoc = new AssociateSurfBasic(FactoryAssociation.greedy(score, 100000, maxMatches, true));

		GeneralAssociation<SurfFeature> generalAssoc = new WrapAssociateSurfBasic(assoc);

		Class integralType = GIntegralImageOps.getIntegralType(imageType);
		OrientationIntegral orientationII = FactoryOrientation.surfDefault(modifiedSURF,integralType);
		OrientationImage<I> orientation = FactoryOrientation.convertImage(orientationII,imageType);

		return combined(detector,orientation,regionDesc,generalAssoc,trackRadius,pyramidScalingKlt,reactivateThreshold,
				imageType);
	}

	/**
	 * Creates a tracker that is a hybrid between KLT and Detect-Describe-Associate (DDA) trackers.
	 *
	 * @see CombinedTrackerScalePoint
	 *
	 * @param detector Feature detector.
	 * @param orientation Optional feature orientation.  Can be null.
	 * @param describe Feature description
	 * @param associate Association algorithm.
	 * @param featureRadiusKlt KLT feature radius
	 * @param pyramidScalingKlt KLT pyramid configuration
	 * @param reactivateThreshold Tracks are reactivated after this many have been dropped.  Try 10% of maxMatches
	 * @param imageType Input image type.
	 * @param <I> Input image type.
	 * @param <Desc> Feature description type.
	 * @return Feature tracker
	 */
	public static <I extends ImageSingleBand, Desc extends TupleDesc>
	ImagePointTracker<I> combined( InterestPointDetector<I> detector,
								   OrientationImage<I> orientation ,
								   DescribeRegionPoint<I, Desc> describe,
								   GeneralAssociation<Desc> associate ,
								   int featureRadiusKlt,
								   int[] pyramidScalingKlt ,
								   int reactivateThreshold,
								   Class<I> imageType )
	{
		DetectDescribeFusion<I,Desc> fused =
				new DetectDescribeFusion<I,Desc>(detector,orientation,describe);

		return combined(fused,associate,featureRadiusKlt,pyramidScalingKlt,reactivateThreshold,imageType);
	}

	/**
	 * Creates a tracker that is a hybrid between KLT and Detect-Describe-Associate (DDA) trackers.
	 *
	 * @see CombinedTrackerScalePoint
	 *
	 * @param detector Feature detector and describer.
	 * @param associate Association algorithm.
	 * @param featureRadiusKlt KLT feature radius
	 * @param pyramidScalingKlt KLT pyramid configuration
	 * @param reactivateThreshold Tracks are reactivated after this many have been dropped.  Try 10% of maxMatches
	 * @param imageType Input image type.
	 * @param <I> Input image type.
	 * @param <D> Derivative image type.
	 * @param <Desc> Feature description type.
	 * @return Feature tracker
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand, Desc extends TupleDesc>
	ImagePointTracker<I> combined( DetectDescribePoint<I,Desc> detector ,
								   GeneralAssociation<Desc> associate ,
								   int featureRadiusKlt,
								   int[] pyramidScalingKlt ,
								   int reactivateThreshold,
								   Class<I> imageType )
	{
		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);

		KltConfig configKlt =  KltConfig.createDefault();

		PyramidKltForCombined<I,D> klt = new PyramidKltForCombined<I, D>(configKlt,
				featureRadiusKlt,pyramidScalingKlt,imageType,derivType);


		CombinedTrackerScalePoint<I, D,Desc> tracker =
				new CombinedTrackerScalePoint<I, D, Desc>(klt,detector,associate);

		return new WrapCombinedTracker<I,D,Desc>(tracker,reactivateThreshold,imageType,derivType);
	}
}
