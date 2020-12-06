/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.feature.describe.DescribeRegionPoint;
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
import boofcv.alg.geo.DistanceFromModelMultiView;
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
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.EstimatorToGenerator;
import boofcv.factory.geo.FactoryMultiView;
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
import org.ddogleg.fitting.modelset.ModelMatcher;
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
	 * Monocular plane based visual odometry algorithm which uses both points on the plane and off plane for motion
	 * estimation.
	 *
	 * @param thresholdAdd New points are spawned when the number of on plane inliers drops below this value.
	 * @param thresholdRetire Tracks are dropped when they are not contained in the inlier set for this many frames
	 * in a row.  Try 2
	 * @param inlierPixelTol Threshold used to determine inliers in pixels.  Try 1.5
	 * @param ransacIterations Number of RANSAC iterations.  Try 200
	 * @param tracker Image feature tracker
	 * @param imageType Type of input image it processes
	 * @return New instance of
	 * @see VisOdomMonoPlaneInfinity
	 */
	public static <T extends ImageGray<T>>
	MonocularPlaneVisualOdometry<T> monoPlaneInfinity( int thresholdAdd,
													   int thresholdRetire,
													   double inlierPixelTol,
													   int ransacIterations,
													   PointTracker<T> tracker,
													   ImageType<T> imageType ) {

		//squared pixel error
		double ransacTOL = inlierPixelTol*inlierPixelTol;

		ModelManagerSe2_F64 manager = new ModelManagerSe2_F64();
		DistancePlane2DToPixelSq distance = new DistancePlane2DToPixelSq();
		GenerateSe2_PlanePtPixel generator = new GenerateSe2_PlanePtPixel();

		ModelMatcher<Se2_F64, PlanePtPixel> motion =
				new Ransac<>(2323, manager, generator, distance, ransacIterations, ransacTOL);

		VisOdomMonoPlaneInfinity<T> alg =
				new VisOdomMonoPlaneInfinity<>(thresholdAdd, thresholdRetire, inlierPixelTol, motion, tracker);

		return new MonoPlaneInfinity_to_MonocularPlaneVisualOdometry<>(alg, distance, generator, imageType);
	}

	/**
	 * Monocular plane based visual odometry algorithm which creates a synthetic overhead view and tracks image
	 * features inside this synthetic view.
	 *
	 * @param cellSize (Overhead) size of ground cells in overhead image in world units
	 * @param maxCellsPerPixel (Overhead) Specifies the minimum resolution.  Higher values allow lower resolutions.
	 * Try 20
	 * @param mapHeightFraction (Overhead)  Truncates the overhead view.  Must be from 0 to 1.0.  1.0 includes
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
	 * @param configVO Configuration for visual odometry
	 * @param sparseDisparity Estimates the 3D location of features
	 * @param tracker Image point feature tracker.
	 * @param imageType Type of image being processed.
	 * @return StereoVisualOdometry
	 * @see VisOdomMonoDepthPnP
	 */
	public static <T extends ImageGray<T>>
	StereoVisualOdometry<T> stereoMonoPnP( ConfigVisOdomTrackPnP configVO,
										   StereoDisparitySparse<T> sparseDisparity,
										   PointTracker<T> tracker,
										   Class<T> imageType ) {
		if (configVO == null)
			configVO = new ConfigVisOdomTrackPnP();

		// Range from sparse disparity
		var pixelTo3D = new StereoSparse3D<>(sparseDisparity, imageType);

		Estimate1ofPnP estimator = FactoryMultiView.pnp_1(configVO.pnp, -1, 1);
		final DistanceFromModelMultiView<Se3_F64, Point2D3D> distance = new PnPDistanceReprojectionSq();

		var manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64, Point2D3D> generator = new EstimatorToGenerator<>(estimator);

		// Need to square the error RANSAC inliers
		double ransacTOL = configVO.ransac.inlierThreshold*configVO.ransac.inlierThreshold;

		var motion = new Ransac<>(configVO.ransac.randSeed, manager, generator, distance,
				configVO.ransac.iterations, ransacTOL);

		RefinePnP refine = null;

		if (configVO.refineIterations > 0) {
			refine = FactoryMultiView.pnpRefine(1e-12, configVO.refineIterations);
		}

		VisOdomKeyFrameManager keyframe = switch (configVO.keyframes.type) {
			case MAX_GEO -> new MaxGeoKeyFrameManager(configVO.keyframes.geoMinCoverage);
			case TICK_TOCK -> new TickTockKeyFrameManager(configVO.keyframes.tickPeriod);
		};

		VisOdomMonoDepthPnP<T> alg = new VisOdomMonoDepthPnP<>(motion, pixelTo3D, refine, tracker);
		alg.getBundleViso().bundle.setSba(FactoryMultiView.bundleSparseMetric(configVO.bundle));
		alg.getBundleViso().bundle.configConverge.setTo(configVO.bundleConverge);
		alg.setFrameManager(keyframe);
		alg.setThresholdRetireTracks(configVO.dropOutlierTracks);
		alg.getBundleViso().getSelectTracks().maxFeaturesPerFrame = configVO.bundleMaxFeaturesPerFrame;
		alg.getBundleViso().getSelectTracks().minTrackObservations = configVO.bundleMinObservations;
		return new WrapVisOdomMonoStereoDepthPnP<>(alg, pixelTo3D, distance, imageType);
	}

	/**
	 * Depth sensor based visual odometry algorithm which runs a sparse feature tracker in the visual camera and
	 * estimates the range of tracks once when first detected using the depth sensor.
	 *
	 * @param thresholdAdd Add new tracks when less than this number are in the inlier set.  Tracker dependent. Set to
	 * a value &le; 0 to add features every frame.
	 * @param thresholdRetire Discard a track if it is not in the inlier set after this many updates.  Try 2
	 * @param sparseDepth Extracts depth of pixels from a depth sensor.
	 * @param visualType Type of visual image being processed.
	 * @param depthType Type of depth image being processed.
	 * @return StereoVisualOdometry
	 * @see VisOdomMonoDepthPnP
	 */
	@Deprecated
	public static <Vis extends ImageGray<Vis>, Depth extends ImageGray<Depth>>
	DepthVisualOdometry<Vis, Depth> depthDepthPnP( double inlierPixelTol,
												   int thresholdAdd,
												   int thresholdRetire,
												   int ransacIterations,
												   int refineIterations,
												   boolean doublePass,
												   DepthSparse3D<Depth> sparseDepth,
												   PointTracker<Vis> tracker,
												   Class<Vis> visualType, Class<Depth> depthType ) {

		// Range from sparse disparity
		ImagePixelTo3D pixelTo3D = new DepthSparse3D_to_PixelTo3D<>(sparseDepth);

		Estimate1ofPnP estimator = FactoryMultiView.pnp_1(EnumPNP.P3P_FINSTERWALDER, -1, 2);
		final DistanceFromModelMultiView<Se3_F64, Point2D3D> distance = new PnPDistanceReprojectionSq();

		ModelManagerSe3_F64 manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64, Point2D3D> generator = new EstimatorToGenerator<>(estimator);

		// 1/2 a pixel tolerance for RANSAC inliers
		double ransacTOL = inlierPixelTol*inlierPixelTol;

		ModelMatcher<Se3_F64, Point2D3D> motion =
				new Ransac<>(2323, manager, generator, distance, ransacIterations, ransacTOL);

		RefinePnP refine = null;

		if (refineIterations > 0) {
			refine = FactoryMultiView.pnpRefine(1e-12, refineIterations);
		}


		VisOdomMonoDepthPnP<Vis> alg = new VisOdomMonoDepthPnP<>(motion, pixelTo3D, refine, tracker);
		alg.setThresholdRetireTracks(thresholdRetire);

		return new VisOdomPixelDepthPnP_to_DepthVisualOdometry<>
				(sparseDepth, alg, distance, ImageType.single(visualType), depthType);
	}

	public static <Vis extends ImageGray<Vis>, Depth extends ImageGray<Depth>>
	DepthVisualOdometry<Vis, Depth> depthDepthPnP( ConfigVisOdomTrackPnP configVO,
												   DepthSparse3D<Depth> sparseDepth,
												   PointTracker<Vis> tracker,
												   Class<Vis> visualType, Class<Depth> depthType ) {

		// Range from sparse disparity
		ImagePixelTo3D pixelTo3D = new DepthSparse3D_to_PixelTo3D<>(sparseDepth);

		Estimate1ofPnP estimator = FactoryMultiView.pnp_1(configVO.pnp, -1, 1);
		final DistanceFromModelMultiView<Se3_F64, Point2D3D> distance = new PnPDistanceReprojectionSq();

		ModelManagerSe3_F64 manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64, Point2D3D> generator = new EstimatorToGenerator<>(estimator);

		// 1/2 a pixel tolerance for RANSAC inliers
		double ransacTOL = configVO.ransac.inlierThreshold*configVO.ransac.inlierThreshold;

		var motion = new Ransac<>(configVO.ransac.randSeed, manager, generator, distance,
				configVO.ransac.iterations, ransacTOL);

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

	public static <T extends ImageGray<T>, Desc extends TupleDesc>
	StereoVisualOdometry<T> stereoDualTrackerPnP( ConfigVisOdomTrackPnP configVO,
												  PointTracker<T> trackerLeft,
												  PointTracker<T> trackerRight,
												  ConfigStereoDualTrackPnP hack,
												  Class<T> imageType ) {
		if (configVO == null)
			configVO = new ConfigVisOdomTrackPnP();
		configVO.checkValidity();

		EstimateNofPnP pnp = FactoryMultiView.pnp_N(configVO.pnp, -1);
		DistanceFromModelMultiView<Se3_F64, Point2D3D> distanceMono = new PnPDistanceReprojectionSq();
		PnPStereoDistanceReprojectionSq distanceStereo = new PnPStereoDistanceReprojectionSq();
		PnPStereoEstimator pnpStereo = new PnPStereoEstimator(pnp, distanceMono, 0);

		ModelManagerSe3_F64 manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64, Stereo2D3D> generator = new EstimatorToGenerator<>(pnpStereo);

		// Pixel tolerance for RANSAC inliers - euclidean error squared from left + right images
		double ransacTOL = 2*configVO.ransac.inlierThreshold*configVO.ransac.inlierThreshold;

		ModelMatcher<Se3_F64, Stereo2D3D> motion = new Ransac<>(configVO.ransac.randSeed, manager, generator,
				distanceStereo, configVO.ransac.iterations, ransacTOL);
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

		DescribeRegionPoint<T, Desc> descriptor = FactoryDescribeRegionPoint.
				generic(hack.stereoDescribe, ImageType.single(imageType));
		Class<Desc> descType = descriptor.getDescriptionType();
		ScoreAssociation<Desc> scorer = FactoryAssociation.defaultScore(descType);
		AssociateStereo2D<Desc> associateL2R = new AssociateStereo2D<>(scorer, hack.epipolarTol, descType);

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

		return new WrapVisOdomDualTrackPnP<>(
				alg, pnpStereo, distanceMono, distanceStereo, associateL2R, refinePnP, imageType);
	}

	/**
	 * Creates a stereo visual odometry algorithm that uses the two most recent frames (4 images total) to estimate
	 * motion.
	 *
	 * @see VisOdomStereoQuadPnP
	 */
	public static <T extends ImageGray<T>, Desc extends TupleDesc>
	StereoVisualOdometry<T> stereoQuadPnP( ConfigStereoQuadPnP config, Class<T> imageType ) {
		EstimateNofPnP pnp = FactoryMultiView.pnp_N(config.pnp, -1);
		DistanceFromModelMultiView<Se3_F64, Point2D3D> distanceMono = new PnPDistanceReprojectionSq();
		PnPStereoDistanceReprojectionSq distanceStereo = new PnPStereoDistanceReprojectionSq();
		PnPStereoEstimator pnpStereo = new PnPStereoEstimator(pnp, distanceMono, 0);

		ModelManagerSe3_F64 manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64, Stereo2D3D> generator = new EstimatorToGenerator<>(pnpStereo);

		// Pixel tolerance for RANSAC inliers - euclidean error squared from left + right images
		double ransacTOL = 2*config.ransac.inlierThreshold*config.ransac.inlierThreshold;

		ModelMatcher<Se3_F64, Stereo2D3D> motion = new Ransac<>(config.ransac.randSeed, manager, generator,
				distanceStereo, config.ransac.iterations, ransacTOL);
		RefinePnPStereo refinePnP = null;

		if (config.refineIterations > 0) {
			refinePnP = new PnPStereoRefineRodrigues(1e-12, config.refineIterations);
		}

		DetectDescribePoint<T, Desc> detector = (DetectDescribePoint)
				FactoryDetectDescribe.generic(config.detectDescribe, imageType);

		Class<Desc> descType = detector.getDescriptionType();


		// need to make sure associations are unique
		ScoreAssociation<Desc> scorer = FactoryAssociation.defaultScore(descType);
		AssociateStereo2D<Desc> associateL2R = new AssociateStereo2D<>(scorer, config.epipolarTol, descType);
		associateL2R.setMaxScoreThreshold(config.associateL2R.maxErrorThreshold);

		AssociateDescription2D<Desc> associateF2F = FactoryAssociation.generic2(config.associateF2F, detector);

		Triangulate2ViewsMetric triangulate = FactoryMultiView.triangulate2ViewMetric(
				new ConfigTriangulation(ConfigTriangulation.Type.GEOMETRIC));

		VisOdomStereoQuadPnP<T, Desc> alg = new VisOdomStereoQuadPnP<>(
				detector, associateF2F, associateL2R, triangulate, motion, refinePnP);

		alg.getBundle().sba = FactoryMultiView.bundleSparseMetric(config.bundle);
		alg.getBundle().configConverge.setTo(config.bundleConverge);

		return new WrapVisOdomQuadPnP<>(alg, refinePnP, associateL2R, distanceStereo, distanceMono, imageType);
	}

	/**
	 * Wraps around a {@link StereoVisualOdometry} instance and will rescale the input images and adjust the cameras
	 * intrinsic parameters automatically.  Rescaling input images is often an easy way to improve runtime performance
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
	 * intrinsic parameters automatically.  Rescaling input images is often an easy way to improve runtime performance
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
