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

package boofcv.factory.sfm;

import boofcv.abst.disparity.StereoDisparitySparse;
import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.associate.EnforceUniqueByScore;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.DescribePointRadiusAngle;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.abst.geo.EstimateNofPnP;
import boofcv.abst.geo.RefinePnP;
import boofcv.abst.geo.Triangulate2ViewsMetric;
import boofcv.abst.sfm.DepthSparse3D_to_PixelTo3D;
import boofcv.abst.sfm.ImagePixelTo3D;
import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.abst.sfm.d3.*;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.feature.associate.AssociateStereo2D;
import boofcv.alg.geo.pose.*;
import boofcv.alg.sfm.DepthSparse3D;
import boofcv.alg.sfm.StereoSparse3D;
import boofcv.alg.sfm.d3.*;
import boofcv.alg.sfm.d3.direct.PyramidDirectColorDepth;
import boofcv.alg.sfm.d3.structure.MaxGeoKeyFrameManager;
import boofcv.alg.sfm.d3.structure.TickTockKeyFrameManager;
import boofcv.alg.sfm.d3.structure.VisOdomKeyFrameManager;
import boofcv.alg.sfm.robust.DistancePlane2DToPixelSq;
import boofcv.alg.sfm.robust.GenerateSe2_PlanePtPixel;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribePointRadiusAngle;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.EstimatorToGenerator;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.struct.sfm.PlanePtPixel;
import boofcv.struct.sfm.Stereo2D3D;
import georegression.fitting.se.ModelManagerSe2_F64;
import georegression.fitting.se.ModelManagerSe3_F64;
import georegression.struct.se.Se2_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating visual odometry algorithms.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class FactoryVisualOdometry {
	/**
	 * Creates monocular visual odometry which relies on the ground being a flat plane
	 */
	public static <T extends ImageGray<T>>
	MonocularPlaneVisualOdometry<T> monoPlaneInfinity( @Nullable ConfigPlanarTrackPnP config, Class<T> imageType ) {
		if (config == null)
			config = new ConfigPlanarTrackPnP();
		PointTracker<T> tracker = FactoryPointTracker.tracker(config.tracker, imageType, null);

		//squared pixel error
		double ransacTOL = config.ransac.inlierThreshold*config.ransac.inlierThreshold;

		var manager = new ModelManagerSe2_F64();
		var distance = new DistancePlane2DToPixelSq();
		var generator = new GenerateSe2_PlanePtPixel();

		Ransac<Se2_F64, PlanePtPixel> motion = FactoryMultiViewRobust.
				createRansac(config.ransac, ransacTOL, manager, PlanePtPixel.class);
		motion.setModel(generator::newConcurrent, distance::newConcurrent);

		VisOdomMonoPlaneInfinity<T> alg = new VisOdomMonoPlaneInfinity<>(
				config.thresholdAdd, config.thresholdRetire, config.ransac.inlierThreshold, motion, tracker);

		return new MonoPlaneInfinity_to_MonocularPlaneVisualOdometry<>(alg, distance, generator,
				ImageType.single(imageType));
	}

	/**
	 * Monocular plane based visual odometry algorithm which creates a synthetic overhead view and tracks image
	 * features inside this synthetic view.
	 *
	 * @param cellSize (Overhead) size of ground cells in overhead image in world units
	 * @param maxCellsPerPixel (Overhead) Specifies the minimum resolution. Higher values allow lower resolutions.
	 * Try 20
	 * @param mapHeightFraction (Overhead)  Truncates the overhead view. Must be from 0 to 1.0. 1.0 includes
	 * the entire image.
	 * @param inlierGroundTol (RANSAC) RANSAC tolerance in overhead image pixels
	 * @param ransacIterations (RANSAC) Number of iterations used when estimating motion
	 * @param thresholdRetire (2D Motion) Drop tracks if they are not in inliers set for this many turns.
	 * @param absoluteMinimumTracks (2D Motion) Spawn tracks if the number of inliers drops below the specified number
	 * @param respawnTrackFraction (2D Motion) Spawn tracks if the number of tracks has dropped below this fraction of the
	 * original number
	 * @param respawnCoverageFraction (2D Motion) Spawn tracks if the total coverage drops below this relative fraction
	 * @param tracker Image feature tracker
	 * @param imageType Type of image being processed
	 * @return MonocularPlaneVisualOdometry
	 * @see VisOdomMonoOverheadMotion2D
	 */
	public static <T extends ImageGray<T>>
	MonocularPlaneVisualOdometry<T> monoPlaneOverhead( double cellSize,
													   double maxCellsPerPixel,
													   double mapHeightFraction,
													   double inlierGroundTol,
													   int ransacIterations,
													   int thresholdRetire,
													   int absoluteMinimumTracks,
													   double respawnTrackFraction,
													   double respawnCoverageFraction,

													   PointTracker<T> tracker,
													   ImageType<T> imageType ) {

		ImageMotion2D<T, Se2_F64> motion2D = FactoryMotion2D.createMotion2D(
				ransacIterations, inlierGroundTol*inlierGroundTol, thresholdRetire,
				absoluteMinimumTracks, respawnTrackFraction, respawnCoverageFraction, false, tracker, new Se2_F64());

		VisOdomMonoOverheadMotion2D<T> alg =
				new VisOdomMonoOverheadMotion2D<>(cellSize, maxCellsPerPixel, mapHeightFraction, motion2D, imageType);

		return new MonoOverhead_to_MonocularPlaneVisualOdometry<>(alg, imageType);
	}

	/**
	 * Stereo vision based visual odometry algorithm which runs a sparse feature tracker in the left camera and
	 * estimates the range of tracks once when first detected using disparity between left and right cameras.
	 *
	 * @return StereoVisualOdometry
	 * @see VisOdomMonoDepthPnP
	 */
	public static <T extends ImageGray<T>>
	StereoVisualOdometry<T> stereoMonoPnP( @Nullable ConfigStereoMonoTrackPnP config, Class<T> imageType ) {
		if (config == null)
			config = new ConfigStereoMonoTrackPnP();

		PointTracker<T> tracker = FactoryPointTracker.tracker(config.tracker, imageType, null);
		StereoDisparitySparse<T> disparity = FactoryStereoDisparity.sparseRectifiedBM(config.disparity, imageType);

		return stereoMonoPnP(config.scene, disparity, tracker, imageType);
	}

	/**
	 * Stereo vision based visual odometry algorithm which runs a sparse feature tracker in the left camera and
	 * estimates the range of tracks once when first detected using disparity between left and right cameras.
	 *
	 * @param configVO Configuration for visual odometry
	 * @param sparseDisparity Estimates the 3D location of features
	 * @param tracker Image point feature tracker.
	 * @param imageType Type of image being processed.
	 * @return StereoVisualOdometry
	 * @see VisOdomMonoDepthPnP
	 */
	public static <T extends ImageGray<T>>
	StereoVisualOdometry<T> stereoMonoPnP( @Nullable ConfigVisOdomTrackPnP configVO,
										   StereoDisparitySparse<T> sparseDisparity,
										   PointTracker<T> tracker,
										   Class<T> imageType ) {
		if (configVO == null)
			configVO = new ConfigVisOdomTrackPnP();
		final ConfigVisOdomTrackPnP _configVO = configVO;

		var distance = new PnPDistanceReprojectionSq();
		var manager = new ModelManagerSe3_F64();

		// Need to square the error RANSAC inliers
		double ransacTOL = configVO.ransac.inlierThreshold*configVO.ransac.inlierThreshold;

		Ransac<Se3_F64, Point2D3D> motion = FactoryMultiViewRobust.
				createRansac(configVO.ransac, ransacTOL, manager, Point2D3D.class);
		motion.setModel(() -> {
					Estimate1ofPnP estimator = FactoryMultiView.pnp_1(_configVO.pnp, -1, 1);
					return new EstimatorToGenerator<>(estimator);
				}
				, distance::newConcurrentChild);

		RefinePnP refine = null;

		if (configVO.refineIterations > 0) {
			refine = FactoryMultiView.pnpRefine(1e-12, configVO.refineIterations);
		}

		VisOdomKeyFrameManager keyframe = switch (configVO.keyframes.type) {
			case MAX_GEO -> new MaxGeoKeyFrameManager(configVO.keyframes.geoMinCoverage);
			case TICK_TOCK -> new TickTockKeyFrameManager(configVO.keyframes.tickPeriod);
		};

		// Range from sparse disparity
		var pixelTo3D = new StereoSparse3D<>(sparseDisparity, imageType);

		VisOdomMonoDepthPnP<T> alg = new VisOdomMonoDepthPnP<>(motion, pixelTo3D, refine, tracker);
		alg.getBundleViso().bundle.setSba(FactoryMultiView.bundleSparseMetric(configVO.bundle));
		alg.getBundleViso().bundle.configConverge.setTo(configVO.bundleConverge);
		alg.setFrameManager(keyframe);
		alg.setThresholdRetireTracks(configVO.dropOutlierTracks);
		alg.getBundleViso().getSelectTracks().maxFeaturesPerFrame = configVO.bundleMaxFeaturesPerFrame;
		alg.getBundleViso().getSelectTracks().minTrackObservations = configVO.bundleMinObservations;

		return new WrapVisOdomMonoStereoDepthPnP<>(alg, pixelTo3D, distance, imageType);
	}

	public static <Vis extends ImageGray<Vis>, Depth extends ImageGray<Depth>>
	DepthVisualOdometry<Vis, Depth> rgbDepthPnP( ConfigRgbDepthTrackPnP config,
												 Class<Vis> visualType, Class<Depth> depthType ) {
		PointTracker<Vis> tracker = FactoryPointTracker.tracker(config.tracker, visualType, null);
		DepthSparse3D<Depth> sparseDepth;

		ImageType depthInfo = ImageType.single(depthType);
		if (depthInfo.getDataType().isInteger()) {
			sparseDepth = (DepthSparse3D<Depth>)new DepthSparse3D.I(config.depthScale);
		} else {
			sparseDepth = (DepthSparse3D<Depth>)new DepthSparse3D.F32(config.depthScale);
		}

		return rgbDepthPnP(config.scene, sparseDepth, tracker, visualType, depthType);
	}

	public static <Vis extends ImageGray<Vis>, Depth extends ImageGray<Depth>>
	DepthVisualOdometry<Vis, Depth> rgbDepthPnP( ConfigVisOdomTrackPnP configVO,
												 DepthSparse3D<Depth> sparseDepth,
												 PointTracker<Vis> tracker,
												 Class<Vis> visualType, Class<Depth> depthType ) {
		// Range from sparse disparity
		ImagePixelTo3D pixelTo3D = new DepthSparse3D_to_PixelTo3D<>(sparseDepth);

		var distance = new PnPDistanceReprojectionSq();
		var manager = new ModelManagerSe3_F64();

		// 1/2 a pixel tolerance for RANSAC inliers
		double ransacTOL = configVO.ransac.inlierThreshold*configVO.ransac.inlierThreshold;

		Ransac<Se3_F64, Point2D3D> motion = FactoryMultiViewRobust.
				createRansac(configVO.ransac, ransacTOL, manager, Point2D3D.class);
		motion.setModel(() -> {
			Estimate1ofPnP estimator = FactoryMultiView.pnp_1(configVO.pnp, -1, 1);
			return new EstimatorToGenerator<>(estimator);
		}, distance::newConcurrentChild);

		RefinePnP refine = null;

		if (configVO.refineIterations > 0) {
			refine = FactoryMultiView.pnpRefine(1e-12, configVO.refineIterations);
		}

		VisOdomKeyFrameManager keyframe = switch (configVO.keyframes.type) {
			case MAX_GEO -> new MaxGeoKeyFrameManager(configVO.keyframes.geoMinCoverage);
			case TICK_TOCK -> new TickTockKeyFrameManager(configVO.keyframes.tickPeriod);
		};

		VisOdomMonoDepthPnP<Vis> alg = new VisOdomMonoDepthPnP<>(motion, pixelTo3D, refine, tracker);
		alg.getBundleViso().bundle.setSba(FactoryMultiView.bundleSparseMetric(configVO.bundle));
		alg.getBundleViso().bundle.configConverge.setTo(configVO.bundleConverge);
		alg.setFrameManager(keyframe);
		alg.setThresholdRetireTracks(configVO.dropOutlierTracks);
		alg.getBundleViso().getSelectTracks().maxFeaturesPerFrame = configVO.bundleMaxFeaturesPerFrame;
		alg.getBundleViso().getSelectTracks().minTrackObservations = configVO.bundleMinObservations;

		return new VisOdomPixelDepthPnP_to_DepthVisualOdometry<>
				(sparseDepth, alg, distance, ImageType.single(visualType), depthType);
	}

	/**
	 * Creates an instance of {@link VisOdomDualTrackPnP}.
	 *
	 * @param configVO Configuration
	 * @param imageType Type of input image
	 * @return The new instance
	 */
	public static <T extends ImageGray<T>>
	StereoVisualOdometry<T> stereoDualTrackerPnP( @Nullable ConfigStereoDualTrackPnP configVO, Class<T> imageType ) {
		if (configVO == null)
			configVO = new ConfigStereoDualTrackPnP();
		configVO.checkValidity();

		PointTracker<T> trackerLeft = FactoryPointTracker.tracker(configVO.tracker, imageType, null);
		PointTracker<T> trackerRight = FactoryPointTracker.tracker(configVO.tracker, imageType, null);

		return stereoDualTrackerPnP(configVO.scene, trackerLeft, trackerRight, configVO, imageType);
	}

	public static <T extends ImageGray<T>, Desc extends TupleDesc<Desc>>
	StereoVisualOdometry<T> stereoDualTrackerPnP( ConfigVisOdomTrackPnP configVO,
												  PointTracker<T> trackerLeft,
												  PointTracker<T> trackerRight,
												  ConfigStereoDualTrackPnP hack,
												  Class<T> imageType ) {
		if (configVO == null)
			configVO = new ConfigVisOdomTrackPnP();
		configVO.checkValidity();

		// Pixel tolerance for RANSAC inliers - euclidean error squared from left + right images
		double ransacTOL = 2*configVO.ransac.inlierThreshold*configVO.ransac.inlierThreshold;

		// Each of these data structures is common to all threads OR contains common internal elements
		var sharedLeftToRight = new Se3_F64();
		var distanceLeft = new PnPDistanceReprojectionSq();
		var distanceRight = new PnPDistanceReprojectionSq();
		var distanceStereo = new PnPStereoDistanceReprojectionSq();

		Ransac<Se3_F64, Stereo2D3D> motion = FactoryMultiViewRobust.
				createRansac(configVO.ransac, ransacTOL, new ModelManagerSe3_F64(), Stereo2D3D.class);
		ConfigVisOdomTrackPnP _configVO = configVO;
		motion.setModel(() -> {
			EstimateNofPnP pnp = FactoryMultiView.pnp_N(_configVO.pnp, -1);
			var pnpStereo = new PnPStereoEstimator(pnp,
					distanceLeft.newConcurrentChild(),
					distanceRight.newConcurrentChild(), 0);
			pnpStereo.setLeftToRightReference(sharedLeftToRight);
			return new EstimatorToGenerator<>(pnpStereo);
		}, distanceStereo::newConcurrentChild);

		RefinePnPStereo refinePnP = null;

		if (configVO.refineIterations > 0) {
			refinePnP = new PnPStereoRefineRodrigues(1e-12, configVO.refineIterations);
		}

		Triangulate2ViewsMetric triangulate2 = FactoryMultiView.triangulate2ViewMetric(
				new ConfigTriangulation(ConfigTriangulation.Type.GEOMETRIC));

		VisOdomKeyFrameManager keyframe = switch (configVO.keyframes.type) {
			case MAX_GEO -> new MaxGeoKeyFrameManager(configVO.keyframes.geoMinCoverage);
			case TICK_TOCK -> new TickTockKeyFrameManager(configVO.keyframes.tickPeriod);
		};

		DescribePointRadiusAngle<T, Desc> descriptor = FactoryDescribePointRadiusAngle.
				generic(hack.stereoDescribe, ImageType.single(imageType));
		Class<Desc> descType = descriptor.getDescriptionType();
		ScoreAssociation<Desc> scorer = FactoryAssociation.defaultScore(descType);
		AssociateStereo2D<Desc> associateL2R = new AssociateStereo2D<>(scorer, hack.epipolarTol);

		// need to make sure associations are unique
		AssociateDescription2D<Desc> associateUnique = FactoryAssociation.ensureUnique(associateL2R);
		if (!associateL2R.uniqueDestination() || !associateL2R.uniqueSource()) {
			associateUnique = new EnforceUniqueByScore.Describe2D<>(associateL2R, true, true);
		}

		VisOdomDualTrackPnP<T, Desc> alg = new VisOdomDualTrackPnP<>(
				hack.epipolarTol, trackerLeft, trackerRight, descriptor, associateUnique, triangulate2,
				motion, refinePnP);
		alg.getBundleViso().bundle.setSba(FactoryMultiView.bundleSparseMetric(configVO.bundle));
		alg.getBundleViso().bundle.configConverge.setTo(configVO.bundleConverge);
		alg.setDescribeRadius(hack.stereoRadius);
		alg.setFrameManager(keyframe);
		alg.setThresholdRetireTracks(configVO.dropOutlierTracks);
		alg.getBundleViso().getSelectTracks().maxFeaturesPerFrame = configVO.bundleMaxFeaturesPerFrame;
		alg.getBundleViso().getSelectTracks().minTrackObservations = configVO.bundleMinObservations;

		return new WrapVisOdomDualTrackPnP<>(alg, sharedLeftToRight, distanceLeft, distanceRight, distanceStereo,
				associateL2R, refinePnP, imageType);
	}

	/**
	 * Creates a stereo visual odometry algorithm that uses the two most recent frames (4 images total) to estimate
	 * motion.
	 *
	 * @see VisOdomStereoQuadPnP
	 */
	public static <T extends ImageGray<T>, Desc extends TupleDesc<Desc>>
	StereoVisualOdometry<T> stereoQuadPnP( ConfigStereoQuadPnP config, Class<T> imageType ) {

		// Pixel tolerance for RANSAC inliers - euclidean error squared from left + right images
		double ransacTOL = 2*config.ransac.inlierThreshold*config.ransac.inlierThreshold;

		Ransac<Se3_F64, Stereo2D3D> motion = FactoryMultiViewRobust.
				createRansac(config.ransac, ransacTOL, new ModelManagerSe3_F64(), Stereo2D3D.class);

		// Each of these data structures is common to all threads OR contains common internal elements
		var sharedLeftToRight = new Se3_F64();
		var distanceLeft = new PnPDistanceReprojectionSq();
		var distanceRight = new PnPDistanceReprojectionSq();
		var distanceStereo = new PnPStereoDistanceReprojectionSq();

		// Creates new models, but careful to make sure everything is thread safe and that common priors are
		// referenced by each instance.
		motion.setModel(() -> {
			EstimateNofPnP pnp = FactoryMultiView.pnp_N(config.pnp, -1);
			var pnpStereo = new PnPStereoEstimator(pnp,
					distanceLeft.newConcurrentChild(),
					distanceRight.newConcurrentChild(), 0);
			pnpStereo.setLeftToRightReference(sharedLeftToRight);
			return new EstimatorToGenerator<>(pnpStereo);
		}, distanceStereo::newConcurrentChild);
		RefinePnPStereo refinePnP = null;

		if (config.refineIterations > 0) {
			refinePnP = new PnPStereoRefineRodrigues(1e-12, config.refineIterations);
		}

		DetectDescribePoint<T, Desc> detector = FactoryDetectDescribe.generic(config.detectDescribe, imageType);

		Class<Desc> descType = detector.getDescriptionType();

		// need to make sure associations are unique
		ScoreAssociation<Desc> scorer = FactoryAssociation.defaultScore(descType);
		AssociateStereo2D<Desc> associateL2R = new AssociateStereo2D<>(scorer, config.epipolarTol);
		associateL2R.setMaxScoreThreshold(config.associateL2R.maxErrorThreshold);

		AssociateDescription2D<Desc> associateF2F = FactoryAssociation.generic2(config.associateF2F, detector);

		Triangulate2ViewsMetric triangulate = FactoryMultiView.triangulate2ViewMetric(
				new ConfigTriangulation(ConfigTriangulation.Type.GEOMETRIC));

		VisOdomStereoQuadPnP<T, Desc> alg = new VisOdomStereoQuadPnP<>(
				detector, associateF2F, associateL2R, triangulate, motion, refinePnP);

		alg.getBundle().sba = FactoryMultiView.bundleSparseMetric(config.bundle);
		alg.getBundle().configConverge.setTo(config.bundleConverge);

		return new WrapVisOdomQuadPnP<>(alg, refinePnP, associateL2R,
				distanceStereo, distanceLeft, distanceRight, imageType);
	}

	/**
	 * Wraps around a {@link StereoVisualOdometry} instance and will rescale the input images and adjust the cameras
	 * intrinsic parameters automatically. Rescaling input images is often an easy way to improve runtime performance
	 * with a minimal hit on pose accuracy.
	 *
	 * @param vo Visual odometry algorithm which is being wrapped
	 * @param scaleFactor Scale factor that the image should be reduced by,  Try 0.5 for half size.
	 * @param <T> Image type
	 * @return StereoVisualOdometry
	 */
	public static <T extends ImageBase<T>> StereoVisualOdometry<T> scaleInput( StereoVisualOdometry<T> vo, double scaleFactor ) {
		return new StereoVisualOdometryScaleInput<>(vo, scaleFactor);
	}

	/**
	 * Wraps around a {@link MonocularPlaneVisualOdometry} instance and will rescale the input images and adjust the cameras
	 * intrinsic parameters automatically. Rescaling input images is often an easy way to improve runtime performance
	 * with a minimal hit on pose accuracy.
	 *
	 * @param vo Visual odometry algorithm which is being wrapped
	 * @param scaleFactor Scale factor that the image should be reduced by,  Try 0.5 for half size.
	 * @param <T> Image type
	 * @return StereoVisualOdometry
	 */
	public static <T extends ImageBase<T>> MonocularPlaneVisualOdometry<T> scaleInput( MonocularPlaneVisualOdometry<T> vo, double scaleFactor ) {
		return new MonocularPlaneVisualOdometryScaleInput<>(vo, scaleFactor);
	}

	public static <Vis extends ImageGray<Vis>, Depth extends ImageGray<Depth>>
	DepthVisualOdometry<Planar<Vis>, Depth> depthDirect( DepthSparse3D<Depth> sparse3D,
														 ImageType<Planar<Vis>> visualType, Class<Depth> depthType ) {
		ImagePyramid<Planar<Vis>> pyramid = FactoryPyramid.discreteGaussian(
				ConfigDiscreteLevels.levels(3),
				-1, 2, false, visualType);

		PyramidDirectColorDepth<Vis> alg = new PyramidDirectColorDepth<>(pyramid);

		return new PyramidDirectColorDepth_to_DepthVisualOdometry<>(sparse3D, alg, depthType);
	}
}
