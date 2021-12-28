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

package boofcv.factory.tracker;

import boofcv.abst.feature.associate.*;
import boofcv.abst.feature.describe.ConfigSurfDescribe;
import boofcv.abst.feature.describe.DescribeBrief_RadiusAngle;
import boofcv.abst.feature.describe.DescribeNCC_RadiusAngle;
import boofcv.abst.feature.describe.DescribePointRadiusAngle;
import boofcv.abst.feature.detdesc.DetectDescribeFusion;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.*;
import boofcv.abst.feature.orientation.ConfigAverageIntegral;
import boofcv.abst.feature.orientation.ConfigSlidingIntegral;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.tracker.*;
import boofcv.alg.feature.describe.DescribePointBrief;
import boofcv.alg.feature.describe.DescribePointPixelRegionNCC;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.alg.feature.detect.intensity.FastCornerDetector;
import boofcv.alg.feature.detect.intensity.GradientCornerIntensity;
import boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.dda.DetectDescribeAssociateTracker;
import boofcv.alg.tracker.hybrid.HybridTrackerScalePoint;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.associate.ConfigAssociateGreedy;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeAlgs;
import boofcv.factory.feature.describe.FactoryDescribePointRadiusAngle;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientation;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import boofcv.struct.pyramid.PyramidDiscrete;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Factory for creating trackers which implement {@link boofcv.abst.tracker.PointTracker}. These trackers
 * are intended for use in SFM applications. Some features which individual trackers can provide are lost when
 * using the high level interface {@link PointTracker}. To create low level tracking algorithms see
 * {@link FactoryTrackerAlg}
 *
 * @author Peter Abeles
 * @see FactoryTrackerAlg
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class FactoryPointTracker {

	/**
	 * Can create and configure any built in tracker.
	 *
	 * @param config Specifies the tracker
	 * @param imageType Type of input image
	 * @param derivType Type of derivative image. If null then the default is used
	 * @return Instance of the tracker
	 */
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	PointTracker<I> tracker( ConfigPointTracker config, Class<I> imageType, @Nullable Class<D> derivType ) {
		if (config.typeTracker == ConfigPointTracker.TrackerType.KLT) {
			return klt(config.klt, config.detDesc.detectPoint, imageType, derivType);
		}

		DetectDescribePoint detDesc = FactoryDetectDescribe.generic(config.detDesc, imageType);
		AssociateDescription2D associate = FactoryAssociation.generic2(config.associate, detDesc);

		return switch (config.typeTracker) {
			case DDA -> FactoryPointTracker.dda(detDesc, associate, config.dda);
			case HYBRID -> FactoryPointTracker.hybrid(
					detDesc, associate, config.detDesc.findNonMaxRadius(), config.klt, config.hybrid, imageType);
			default -> throw new RuntimeException("BUG! KLT all trackers should have been handled already");
		};
	}

	/**
	 * Pyramid KLT feature tracker.
	 *
	 * @param numLevels Number of levels in the image pyramid
	 * @param configDetect Configuration for detecting point features
	 * @param featureRadius Size of the tracked feature. Try 3 or 5
	 * @param imageType Input image type.
	 * @param derivType Image derivative  type.
	 * @return KLT based tracker.
	 * @see boofcv.alg.tracker.klt.PyramidKltTracker
	 */
	@Deprecated
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	PointTracker<I> klt( int numLevels, @Nullable ConfigPointDetector configDetect, int featureRadius,
						 Class<I> imageType, Class<D> derivType ) {
		ConfigPKlt config = new ConfigPKlt();
		config.pyramidLevels = ConfigDiscreteLevels.levels(numLevels);
		config.templateRadius = featureRadius;

		return klt(config, configDetect, imageType, derivType);
	}

	/**
	 * Pyramid KLT feature tracker.
	 *
	 * @param config Config for the tracker. Try PkltConfig.createDefault().
	 * @param configDetect Configuration for detecting point features
	 * @return KLT based tracker.
	 * @see boofcv.alg.tracker.klt.PyramidKltTracker
	 */
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	PointTrackerKltPyramid<I, D> klt( @Nullable ConfigPKlt config, @Nullable ConfigPointDetector configDetect,
									  Class<I> imageType, @Nullable Class<D> derivType ) {

		if (derivType == null)
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		if (config == null) {
			config = new ConfigPKlt();
		}
		config.checkValidity();

		if (configDetect == null) {
			configDetect = new ConfigPointDetector();
			configDetect.type = PointDetectorTypes.SHI_TOMASI;
		}
		configDetect.checkValidity();

		GeneralFeatureDetector<I, D> detector = FactoryDetectPoint.create(configDetect, imageType, derivType);

		InterpolateRectangle<I> interpInput = FactoryInterpolation.bilinearRectangle(imageType);
		InterpolateRectangle<D> interpDeriv = FactoryInterpolation.bilinearRectangle(derivType);

		ImageGradient<I, D> gradient = FactoryDerivative.sobel(imageType, derivType);

		PyramidDiscrete<I> pyramid = FactoryPyramid.discreteGaussian(config.pyramidLevels, -1, 2, true, ImageType.single(imageType));

		var ret = new PointTrackerKltPyramid<>(config.config, config.toleranceFB,
				config.templateRadius, config.pruneClose, pyramid, detector,
				gradient, interpInput, interpDeriv, derivType);
		ret.configMaxTracks = config.maximumTracks;
		return ret;
	}

	/**
	 * Creates a tracker which detects Fast-Hessian features and describes them with SURF using the faster variant
	 * of SURF.
	 *
	 * @param configDetector Configuration for SURF detector
	 * @param configDescribe Configuration for SURF descriptor
	 * @param configOrientation Configuration for orientation
	 * @param imageType Type of image the input is.
	 * @return SURF based tracker.
	 * @see DescribePointSurf
	 */
	// TODO remove maxTracks?  Use number of detected instead
	@Deprecated
	public static <I extends ImageGray<I>>
	PointTracker<I> dda_FH_SURF_Fast( ConfigFastHessian configDetector,
									  ConfigSurfDescribe.Fast configDescribe,
									  ConfigAverageIntegral configOrientation,
									  Class<I> imageType ) {
		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
		AssociateDescription<TupleDesc_F64> assoc =
				FactoryAssociation.greedy(new ConfigAssociateGreedy(true, 5), score);
		AssociateDescription2D<TupleDesc_F64> associate2D = new AssociateDescTo2D<>(assoc);

		DetectDescribePoint<I, TupleDesc_F64> fused =
				FactoryDetectDescribe.surfFast(configDetector, configDescribe, configOrientation, imageType);

		return new PointTrackerDda<>(new DetectDescribeAssociateTracker<>(fused, associate2D, new ConfigTrackerDda()));
	}

	/**
	 * Creates a tracker which detects Fast-Hessian features and describes them with SURF using the faster variant
	 * of SURF.
	 *
	 * @param configDetector Configuration for SURF detector
	 * @param configDescribe Configuration for SURF descriptor
	 * @param configOrientation Configuration for orientation
	 * @param imageType Type of image the input is.
	 * @return SURF based tracker.
	 * @see DescribePointSurf
	 */
	// TODO remove maxTracks?  Use number of detected instead
	@Deprecated
	public static <I extends ImageGray<I>>
	PointTracker<I> dda_FH_SURF_Stable( ConfigFastHessian configDetector,
										ConfigSurfDescribe.Stability configDescribe,
										ConfigSlidingIntegral configOrientation,
										Class<I> imageType ) {
		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
		AssociateDescription<TupleDesc_F64> assoc =
				FactoryAssociation.greedy(new ConfigAssociateGreedy(true, 5), score);
		AssociateDescription2D<TupleDesc_F64> associate2D = new AssociateDescTo2D<>(assoc);

		DetectDescribePoint<I, TupleDesc_F64> fused =
				FactoryDetectDescribe.surfStable(configDetector, configDescribe, configOrientation, imageType);

		return new PointTrackerDda<>(new DetectDescribeAssociateTracker<>(fused, associate2D, new ConfigTrackerDda()));
	}

	/**
	 * Creates a tracker which detects Shi-Tomasi corner features and describes them with BRIEF.
	 *
	 * @param maxAssociationError Maximum allowed association error. Try 200.
	 * @param configExtract Configuration for extracting features
	 * @param imageType Type of image being processed.
	 * @param derivType Type of image used to store the image derivative. null == use default
	 * @see ShiTomasiCornerIntensity
	 * @see DescribePointBrief
	 */
	@Deprecated
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	PointTracker<I> dda_ST_BRIEF( int maxAssociationError,
								  ConfigGeneralDetector configExtract,
								  Class<I> imageType, Class<D> derivType ) {
		if (derivType == null)
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		DescribePointBrief<I> brief = FactoryDescribeAlgs.
				brief(FactoryBriefDefinition.gaussian2(new Random(123), 16, 512),
						FactoryBlurFilter.gaussian(ImageType.single(imageType), 0, 4));
		DescribePointRadiusAngle describeBrief = new DescribeBrief_RadiusAngle<>(brief, imageType);

		GeneralFeatureDetector<I, D> detectPoint = createShiTomasi(configExtract, derivType);

		ScoreAssociateHamming_B score = new ScoreAssociateHamming_B();

		AssociateDescription2D<TupleDesc_B> association =
				new AssociateDescTo2D<>(FactoryAssociation.greedy(
						new ConfigAssociateGreedy(true, maxAssociationError), score));

		InterestPointDetector<I> detector = FactoryInterestPoint.wrapPoint(detectPoint, 1, imageType, derivType);

		DetectDescribePoint fused = FactoryDetectDescribe.fuseTogether(detector, null, describeBrief);


		return new PointTrackerDda<>(new DetectDescribeAssociateTracker<>(fused, association, new ConfigTrackerDda()));
	}

	/**
	 * Creates a tracker which detects FAST corner features and describes them with BRIEF.
	 *
	 * @param configFast Configuration for FAST detector
	 * @param configExtract Configuration for extracting features
	 * @param maxAssociationError Maximum allowed association error. Try 200.
	 * @param imageType Type of image being processed.
	 * @see FastCornerDetector
	 * @see DescribePointBrief
	 */
	@Deprecated
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	PointTracker<I> dda_FAST_BRIEF( ConfigFastCorner configFast,
									ConfigGeneralDetector configExtract,
									int maxAssociationError,
									Class<I> imageType ) {
		DescribePointBrief<I> brief = FactoryDescribeAlgs.
				brief(FactoryBriefDefinition.gaussian2(new Random(123), 16, 512),
						FactoryBlurFilter.gaussian(ImageType.single(imageType), 0, 4));
		DescribePointRadiusAngle describeBrief = new DescribeBrief_RadiusAngle<>(brief, imageType);

		GeneralFeatureDetector<I, D> corner = FactoryDetectPoint.createFast(configExtract, configFast, imageType);

		ScoreAssociateHamming_B score = new ScoreAssociateHamming_B();

		AssociateDescription2D<TupleDesc_B> association =
				new AssociateDescTo2D<>(FactoryAssociation.greedy(
						new ConfigAssociateGreedy(true, maxAssociationError), score));

		InterestPointDetector<I> detector = FactoryInterestPoint.wrapPoint(corner, 1, imageType, null);
		DetectDescribePoint fused = FactoryDetectDescribe.fuseTogether(detector, null, describeBrief);

		return new PointTrackerDda<>(new DetectDescribeAssociateTracker<>(fused, association, new ConfigTrackerDda()));
	}

	/**
	 * Creates a tracker which detects Shi-Tomasi corner features and describes them with NCC.
	 *
	 * @param configExtract Configuration for extracting features
	 * @param describeRadius Radius of the region being described. Try 2.
	 * @param imageType Type of image being processed.
	 * @param derivType Type of image used to store the image derivative. null == use default
	 * @see ShiTomasiCornerIntensity
	 * @see DescribePointPixelRegionNCC
	 */
	@Deprecated
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	PointTracker<I> dda_ST_NCC( ConfigGeneralDetector configExtract, int describeRadius,
								Class<I> imageType, @Nullable Class<D> derivType ) {

		if (derivType == null)
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		int w = 2*describeRadius + 1;

		DescribePointPixelRegionNCC<I> ncc = FactoryDescribeAlgs.pixelRegionNCC(w, w, imageType);
		DescribePointRadiusAngle describeNCC = new DescribeNCC_RadiusAngle(ncc, imageType);

		GeneralFeatureDetector<I, D> corner = createShiTomasi(configExtract, derivType);

		ScoreAssociateNccFeature score = new ScoreAssociateNccFeature();

		AssociateDescription2D association =
				new AssociateDescTo2D<>(FactoryAssociation.greedy(
						new ConfigAssociateGreedy(true, Double.MAX_VALUE), score));

		InterestPointDetector<I> detector = FactoryInterestPoint.wrapPoint(corner, 1, imageType, null);
		DetectDescribePoint fused = FactoryDetectDescribe.fuseTogether(detector, null, describeNCC);

		return new PointTrackerDda<>(new DetectDescribeAssociateTracker<>(fused, association, new ConfigTrackerDda()));
	}

	/**
	 * Creates a tracker which uses the detect, describe, associate architecture.
	 *
	 * @param detector Interest point detector.
	 * @param orientation Optional orientation estimation algorithm. Can be null.
	 * @param describe Region description.
	 * @param associate Description association.
	 * @param config Configuration
	 * @param <I> Type of input image.
	 * @param <Desc> Type of region description
	 * @return tracker
	 */
	public static <I extends ImageGray<I>, Desc extends TupleDesc<Desc>>
	DetectDescribeAssociateTracker<I, Desc> dda( InterestPointDetector<I> detector,
												 OrientationImage<I> orientation,
												 DescribePointRadiusAngle<I, Desc> describe,
												 AssociateDescription2D<Desc> associate,
												 ConfigTrackerDda config ) {

		DetectDescribeFusion<I, Desc> fused = new DetectDescribeFusion<>(detector, orientation, describe);
		return new DetectDescribeAssociateTracker<>(fused, associate, config);
	}

	public static <I extends ImageGray<I>, Desc extends TupleDesc<Desc>>
	PointTrackerDda<I, Desc> dda( DetectDescribePoint<I, Desc> detDesc,
								  AssociateDescription2D<Desc> associate,
								  ConfigTrackerDda config ) {
		return new PointTrackerDda<>(new DetectDescribeAssociateTracker<>(detDesc, associate, config));
	}

	/**
	 * Creates a tracker which detects Fast-Hessian features, describes them with SURF, nominally tracks them using KLT.
	 *
	 * @param kltConfig Configuration for KLT tracker
	 * @param configDetector Configuration for SURF detector
	 * @param configDescribe Configuration for SURF descriptor
	 * @param configOrientation Configuration for region orientation
	 * @param imageType Type of image the input is.
	 * @param <I> Input image type.
	 * @return SURF based tracker.
	 * @see DescribePointSurf
	 */
	@Deprecated
	public static <I extends ImageGray<I>>
	PointTracker<I> combined_FH_SURF_KLT( ConfigPKlt kltConfig,
										  ConfigTrackerHybrid configHybrid,
										  ConfigFastHessian configDetector,
										  ConfigSurfDescribe.Stability configDescribe,
										  ConfigSlidingIntegral configOrientation,
										  Class<I> imageType ) {

		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.defaultScore(TupleDesc_F64.class);
		AssociateDescription<TupleDesc_F64> assoc = FactoryAssociation.
				greedy(new ConfigAssociateGreedy(true, Double.MAX_VALUE), score);
		AssociateDescription2D<TupleDesc_F64> associate2D = new AssociateDescTo2D<>(assoc);

		DetectDescribePoint<I, TupleDesc_F64> fused =
				FactoryDetectDescribe.surfStable(configDetector, configDescribe, configOrientation, imageType);

		return hybrid(fused, associate2D, configDetector.extract.radius, kltConfig, configHybrid, imageType);
	}

	/**
	 * Creates a tracker which detects Shi-Tomasi corner features, describes them with SURF, and
	 * nominally tracks them using KLT.
	 *
	 * @param configExtract Configuration for extracting features
	 * @param kltConfig Configuration for KLT
	 * @param configDescribe Configuration for SURF descriptor
	 * @param configOrientation Configuration for region orientation. If null then orientation isn't estimated
	 * @param imageType Type of image the input is.
	 * @param derivType Image derivative type.      @return SURF based tracker.
	 * @see ShiTomasiCornerIntensity
	 * @see DescribePointSurf
	 */
	@Deprecated
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	PointTracker<I> combined_ST_SURF_KLT( ConfigGeneralDetector configExtract,
										  ConfigPKlt kltConfig,
										  ConfigTrackerHybrid configHybrid,
										  ConfigSurfDescribe.Stability configDescribe,
										  ConfigSlidingIntegral configOrientation,
										  Class<I> imageType,
										  @Nullable Class<D> derivType ) {

		if (derivType == null)
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		GeneralFeatureDetector<I, D> corner = createShiTomasi(configExtract, derivType);
		InterestPointDetector<I> detector = FactoryInterestPoint.wrapPoint(corner, 1, imageType, derivType);

		DescribePointRadiusAngle<I, TupleDesc_F64> regionDesc
				= FactoryDescribePointRadiusAngle.surfStable(configDescribe, imageType);

		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
		AssociateDescription<TupleDesc_F64> assoc = FactoryAssociation.
				greedy(new ConfigAssociateGreedy(true, Double.MAX_VALUE), score);
		AssociateDescription2D<TupleDesc_F64> associate2D = new AssociateDescTo2D<>(assoc);

		OrientationImage<I> orientation = null;

		if (configOrientation != null) {
			Class integralType = GIntegralImageOps.getIntegralType(imageType);
			OrientationIntegral orientationII = FactoryOrientationAlgs.sliding_ii(configOrientation, integralType);
			orientation = FactoryOrientation.convertImage(orientationII, imageType);
		}

		return hybrid(detector, orientation, regionDesc, associate2D, configExtract.radius,
				kltConfig, configHybrid, imageType);
	}

	/**
	 * Creates a tracker that is a hybrid between KLT and Detect-Describe-Associate (DDA) trackers.
	 *
	 * @param detector Feature detector.
	 * @param orientation Optional feature orientation. Can be null.
	 * @param describe Feature description
	 * @param associate Association algorithm.
	 * @param kltConfig Configuration for KLT tracker
	 * @param imageType Input image type.   @return Feature tracker
	 * @see HybridTrackerScalePoint
	 */
	public static <I extends ImageGray<I>, Desc extends TupleDesc<Desc>>
	PointTracker<I> hybrid( InterestPointDetector<I> detector,
							@Nullable OrientationImage<I> orientation,
							DescribePointRadiusAngle<I, Desc> describe,
							AssociateDescription2D<Desc> associate,
							int tooCloseRadius,
							ConfigPKlt kltConfig,
							ConfigTrackerHybrid configHybrid,
							Class<I> imageType ) {
		DetectDescribeFusion<I, Desc> fused = new DetectDescribeFusion<>(detector, orientation, describe);

		return hybrid(fused, associate, tooCloseRadius, kltConfig, configHybrid, imageType);
	}

	/**
	 * Creates a tracker that is a hybrid between KLT and Detect-Describe-Associate (DDA) trackers.
	 *
	 * @param detector Feature detector and describer.
	 * @param associate Association algorithm.
	 * @param kltConfig Configuration for KLT tracker
	 * @param imageType Input image type.   @return Feature tracker
	 * @see HybridTrackerScalePoint
	 */
	public static <I extends ImageGray<I>, D extends ImageGray<D>, Desc extends TupleDesc<Desc>>
	PointTracker<I> hybrid( DetectDescribePoint<I, Desc> detector,
							AssociateDescription2D<Desc> associate,
							int tooCloseRadius,
							ConfigPKlt kltConfig,
							ConfigTrackerHybrid configHybrid,
							Class<I> imageType ) {
		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);

		if (kltConfig == null) {
			kltConfig = new ConfigPKlt();
		}

		// if the radius is negative then prune too close is disabled
		if (!configHybrid.pruneCloseTracks) {
			tooCloseRadius = -1;
		}

		HybridTrackerScalePoint<I, D, Desc> tracker = FactoryTrackerAlg.
				hybrid(detector, associate, tooCloseRadius, kltConfig, configHybrid, imageType, derivType);
		tracker.rand = new Random(configHybrid.seed);

		var pointHybrid = new PointTrackerHybrid<>(tracker, kltConfig.pyramidLevels, imageType, derivType);
		pointHybrid.thresholdRespawn.setTo(configHybrid.thresholdRespawn);
		return pointHybrid;
	}

	public static <I extends ImageGray<I>, D extends ImageGray<D>, Desc extends TupleDesc<Desc>>
	PointTracker<I> dda( GeneralFeatureDetector<I, D> detector,
						 DescribePointRadiusAngle<I, Desc> describe,
						 AssociateDescription2D<Desc> associate,
						 double scale,
						 Class<I> imageType ) {
		InterestPointDetector<I> detectInterest = FactoryInterestPoint.wrapPoint(detector, scale, imageType, null);
		DetectDescribePoint<I, Desc> fused = FactoryDetectDescribe.fuseTogether(detectInterest, null, describe);

		return new PointTrackerDda<>(new DetectDescribeAssociateTracker<>(fused, associate, new ConfigTrackerDda()));
	}

	/**
	 * Creates a Shi-Tomasi corner detector specifically designed for SFM. Smaller feature radius work better.
	 * Variable detectRadius to control the number of features. When larger features are used weighting should
	 * be set to true, but because this is so small, it is set to false
	 */
	@Deprecated
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	GeneralFeatureDetector<I, D> createShiTomasi( ConfigGeneralDetector config,
												  Class<D> derivType ) {
		GradientCornerIntensity<D> cornerIntensity = FactoryIntensityPointAlg.shiTomasi(1, false, derivType);

		return FactoryDetectPoint.createGeneral(cornerIntensity, config);
	}
}
