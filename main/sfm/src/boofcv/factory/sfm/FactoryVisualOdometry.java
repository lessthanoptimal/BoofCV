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

package boofcv.factory.sfm;

import boofcv.abst.feature.associate.AssociateDescTo2D;
import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.associate.EnforceUniqueByScore;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detdesc.DetectDescribeMulti;
import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.feature.tracker.PointTrackerTwoPass;
import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.abst.geo.EstimateNofPnP;
import boofcv.abst.geo.RefinePnP;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.abst.sfm.DepthSparse3D_to_PixelTo3D;
import boofcv.abst.sfm.ImagePixelTo3D;
import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.abst.sfm.d3.*;
import boofcv.alg.feature.associate.AssociateMaxDistanceNaive;
import boofcv.alg.feature.associate.AssociateStereo2D;
import boofcv.alg.geo.DistanceModelMonoPixels;
import boofcv.alg.geo.pose.*;
import boofcv.alg.sfm.DepthSparse3D;
import boofcv.alg.sfm.StereoSparse3D;
import boofcv.alg.sfm.d3.*;
import boofcv.alg.sfm.robust.DistancePlane2DToPixelSq;
import boofcv.alg.sfm.robust.GenerateSe2_PlanePtPixel;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.EstimatorToGenerator;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.sfm.PlanePtPixel;
import boofcv.struct.sfm.Stereo2D3D;
import georegression.fitting.se.ModelManagerSe2_F64;
import georegression.fitting.se.ModelManagerSe3_F64;
import georegression.struct.se.Se2_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;

/**
 * Factory for creating visual odometry algorithms.
 *
 * @author Peter Abeles
 */
public class FactoryVisualOdometry {


	/**
	 * Monocular plane based visual odometry algorithm which uses both points on the plane and off plane for motion
	 * estimation.
	 *
	 * @see VisOdomMonoPlaneInfinity
	 *
	 * @param thresholdAdd  New points are spawned when the number of on plane inliers drops below this value.
	 * @param thresholdRetire Tracks are dropped when they are not contained in the inlier set for this many frames
	 *                        in a row.  Try 2
	 * @param inlierPixelTol Threshold used to determine inliers in pixels.  Try 1.5
	 * @param ransacIterations Number of RANSAC iterations.  Try 200
	 * @param tracker Image feature tracker
	 * @param imageType Type of input image it processes
	 * @param <T>
	 * @return New instance of
	 */
	public static <T extends ImageGray>
	MonocularPlaneVisualOdometry<T> monoPlaneInfinity(int thresholdAdd,
													  int thresholdRetire,

													  double inlierPixelTol,
													  int ransacIterations,

													  PointTracker<T> tracker,
													  ImageType<T> imageType) {

		//squared pixel error
		double ransacTOL = inlierPixelTol * inlierPixelTol;

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
	 * @see VisOdomMonoOverheadMotion2D
	 *
	 * @param cellSize (Overhead) size of ground cells in overhead image in world units
	 * @param maxCellsPerPixel (Overhead) Specifies the minimum resolution.  Higher values allow lower resolutions.
	 *                         Try 20
	 * @param mapHeightFraction (Overhead)  Truncates the overhead view.  Must be from 0 to 1.0.  1.0 includes
	 *                          the entire image.

	 * @param inlierGroundTol (RANSAC) RANSAC tolerance in overhead image pixels
	 * @param ransacIterations (RANSAC) Number of iterations used when estimating motion
	 *
	 * @param thresholdRetire (2D Motion) Drop tracks if they are not in inliers set for this many turns.
	 * @param absoluteMinimumTracks (2D Motion) Spawn tracks if the number of inliers drops below the specified number
	 * @param respawnTrackFraction (2D Motion) Spawn tracks if the number of tracks has dropped below this fraction of the
	 *                             original number
	 * @param respawnCoverageFraction (2D Motion) Spawn tracks if the total coverage drops below this relative fraction
	 *
	 * @param tracker Image feature tracker
	 * @param imageType Type of image being processed
	 * @return MonocularPlaneVisualOdometry
	 */
	public static <T extends ImageGray>
	MonocularPlaneVisualOdometry<T> monoPlaneOverhead(double cellSize,
													  double maxCellsPerPixel,
													  double mapHeightFraction ,

													  double inlierGroundTol,
													  int ransacIterations ,

													  int thresholdRetire ,
													  int absoluteMinimumTracks,
													  double respawnTrackFraction,
													  double respawnCoverageFraction,

													  PointTracker<T> tracker ,
													  ImageType<T> imageType ) {

		ImageMotion2D<T,Se2_F64> motion2D = FactoryMotion2D.createMotion2D(
				ransacIterations,inlierGroundTol*inlierGroundTol,thresholdRetire,
				absoluteMinimumTracks,respawnTrackFraction,respawnCoverageFraction,false,tracker,new Se2_F64());


		VisOdomMonoOverheadMotion2D<T> alg =
				new VisOdomMonoOverheadMotion2D<>(cellSize, maxCellsPerPixel, mapHeightFraction, motion2D, imageType);

		return new MonoOverhead_to_MonocularPlaneVisualOdometry<>(alg, imageType);
	}

	/**
	 * Stereo vision based visual odometry algorithm which runs a sparse feature tracker in the left camera and
	 * estimates the range of tracks once when first detected using disparity between left and right cameras.
	 *
	 * @see VisOdomPixelDepthPnP
	 *
	 * @param thresholdAdd Add new tracks when less than this number are in the inlier set.  Tracker dependent. Set to
	 *                     a value &le; 0 to add features every frame.
	 * @param thresholdRetire Discard a track if it is not in the inlier set after this many updates.  Try 2
	 * @param sparseDisparity Estimates the 3D location of features
	 * @param imageType Type of image being processed.
	 * @return StereoVisualOdometry
	 */
	public static <T extends ImageGray>
	StereoVisualOdometry<T> stereoDepth(double inlierPixelTol,
										int thresholdAdd,
										int thresholdRetire ,
										int ransacIterations ,
										int refineIterations ,
										boolean doublePass ,
										StereoDisparitySparse<T> sparseDisparity,
										PointTrackerTwoPass<T> tracker ,
										Class<T> imageType) {

		// Range from sparse disparity
		StereoSparse3D<T> pixelTo3D = new StereoSparse3D<>(sparseDisparity, imageType);

		Estimate1ofPnP estimator = FactoryMultiView.computePnP_1(EnumPNP.P3P_FINSTERWALDER,-1,2);
		final DistanceModelMonoPixels<Se3_F64,Point2D3D> distance = new PnPDistanceReprojectionSq();

		ModelManagerSe3_F64 manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64,Point2D3D> generator =
				new EstimatorToGenerator<>(estimator);

		// 1/2 a pixel tolerance for RANSAC inliers
		double ransacTOL = inlierPixelTol * inlierPixelTol;

		ModelMatcher<Se3_F64, Point2D3D> motion =
				new Ransac<>(2323, manager, generator, distance, ransacIterations, ransacTOL);

		RefinePnP refine = null;

		if( refineIterations > 0 ) {
			refine = FactoryMultiView.refinePnP(1e-12,refineIterations);
		}

		VisOdomPixelDepthPnP<T> alg =
				new VisOdomPixelDepthPnP<>(thresholdAdd, thresholdRetire, doublePass, motion, pixelTo3D, refine, tracker, null, null);

		return new WrapVisOdomPixelDepthPnP<>(alg, pixelTo3D, distance, imageType);
	}

	/**
	 * Depth sensor based visual odometry algorithm which runs a sparse feature tracker in the visual camera and
	 * estimates the range of tracks once when first detected using the depth sensor.
	 *
	 * @see VisOdomPixelDepthPnP
	 *
	 * @param thresholdAdd Add new tracks when less than this number are in the inlier set.  Tracker dependent. Set to
	 *                     a value &le; 0 to add features every frame.
	 * @param thresholdRetire Discard a track if it is not in the inlier set after this many updates.  Try 2
	 * @param sparseDepth Extracts depth of pixels from a depth sensor.
	 * @param visualType Type of visual image being processed.
	 * @param depthType Type of depth image being processed.
	 * @return StereoVisualOdometry
	 */
	public static <Vis extends ImageGray, Depth extends ImageGray>
	DepthVisualOdometry<Vis,Depth> depthDepthPnP(double inlierPixelTol,
												 int thresholdAdd,
												 int thresholdRetire ,
												 int ransacIterations ,
												 int refineIterations ,
												 boolean doublePass ,
												 DepthSparse3D<Depth> sparseDepth,
												 PointTrackerTwoPass<Vis> tracker ,
												 Class<Vis> visualType , Class<Depth> depthType ) {

		// Range from sparse disparity
		ImagePixelTo3D pixelTo3D = new DepthSparse3D_to_PixelTo3D<>(sparseDepth);

		Estimate1ofPnP estimator = FactoryMultiView.computePnP_1(EnumPNP.P3P_FINSTERWALDER,-1,2);
		final DistanceModelMonoPixels<Se3_F64,Point2D3D> distance = new PnPDistanceReprojectionSq();

		ModelManagerSe3_F64 manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64,Point2D3D> generator = new EstimatorToGenerator<>(estimator);

		// 1/2 a pixel tolerance for RANSAC inliers
		double ransacTOL = inlierPixelTol * inlierPixelTol;

		ModelMatcher<Se3_F64, Point2D3D> motion =
				new Ransac<>(2323, manager, generator, distance, ransacIterations, ransacTOL);

		RefinePnP refine = null;

		if( refineIterations > 0 ) {
			refine = FactoryMultiView.refinePnP(1e-12,refineIterations);
		}

		VisOdomPixelDepthPnP<Vis> alg = new VisOdomPixelDepthPnP<>
				(thresholdAdd, thresholdRetire, doublePass, motion, pixelTo3D, refine, tracker, null, null);

		return new VisOdomPixelDepthPnP_to_DepthVisualOdometry<>
				(sparseDepth, alg, distance, ImageType.single(visualType), depthType);
	}

	/**
	 * Creates a stereo visual odometry algorithm that independently tracks features in left and right camera.
	 *
	 * @see VisOdomDualTrackPnP
	 *
	 * @param thresholdAdd When the number of inliers is below this number new features are detected
	 * @param thresholdRetire When a feature has not been in the inlier list for this many ticks it is dropped
	 * @param inlierPixelTol Tolerance in pixels for defining an inlier during robust model matching.  Typically 1.5
	 * @param epipolarPixelTol Tolerance in pixels for enforcing the epipolar constraint
	 * @param ransacIterations Number of iterations performed by RANSAC.  Try 300 or more.
	 * @param refineIterations Number of iterations done during non-linear optimization.  Try 50 or more.
	 * @param trackerLeft Tracker used for left camera
	 * @param trackerRight Tracker used for right camera
	 * @param imageType Type of image being processed
	 * @return Stereo visual odometry algorithm.
	 */
	public static <T extends ImageGray, Desc extends TupleDesc>
	StereoVisualOdometry<T> stereoDualTrackerPnP(int thresholdAdd, int thresholdRetire,
												 double inlierPixelTol,
												 double epipolarPixelTol,
												 int ransacIterations,
												 int refineIterations,
												 PointTracker<T> trackerLeft, PointTracker<T> trackerRight,
												 DescribeRegionPoint<T,Desc> descriptor,
												 Class<T> imageType)
	{
		EstimateNofPnP pnp = FactoryMultiView.computePnP_N(EnumPNP.P3P_FINSTERWALDER, -1);
		DistanceModelMonoPixels<Se3_F64,Point2D3D> distanceMono = new PnPDistanceReprojectionSq();
		PnPStereoDistanceReprojectionSq distanceStereo = new PnPStereoDistanceReprojectionSq();
		PnPStereoEstimator pnpStereo = new PnPStereoEstimator(pnp,distanceMono,0);

		ModelManagerSe3_F64 manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64,Stereo2D3D> generator = new EstimatorToGenerator<>(pnpStereo);

		// Pixel tolerance for RANSAC inliers - euclidean error squared from left + right images
		double ransacTOL = 2*inlierPixelTol * inlierPixelTol;

		ModelMatcher<Se3_F64, Stereo2D3D> motion =
				new Ransac<>(2323, manager, generator, distanceStereo, ransacIterations, ransacTOL);

		RefinePnPStereo refinePnP = null;

		Class<Desc> descType = descriptor.getDescriptionType();
		ScoreAssociation<Desc> scorer = FactoryAssociation.defaultScore(descType);
		AssociateStereo2D<Desc> associateStereo = new AssociateStereo2D<>(scorer, epipolarPixelTol, descType);

		// need to make sure associations are unique
		AssociateDescription2D<Desc> associateUnique = associateStereo;
		if( !associateStereo.uniqueDestination() || !associateStereo.uniqueSource() ) {
			associateUnique = new EnforceUniqueByScore.Describe2D<>(associateStereo, true, true);
		}

		if( refineIterations > 0 ) {
			refinePnP = new PnPStereoRefineRodrigues(1e-12,refineIterations);
		}

		TriangulateTwoViewsCalibrated triangulate = FactoryMultiView.triangulateTwoGeometric();

		VisOdomDualTrackPnP<T,Desc> alg = new VisOdomDualTrackPnP<>(thresholdAdd, thresholdRetire, epipolarPixelTol,
				trackerLeft, trackerRight, descriptor, associateUnique, triangulate, motion, refinePnP);

		return new WrapVisOdomDualTrackPnP<>(pnpStereo, distanceMono, distanceStereo, associateStereo, alg, refinePnP, imageType);
	}

	/**
	 * Stereo visual odometry which uses the two most recent stereo observations (total of four views) to estimate
	 * motion.
	 *
	 * @see VisOdomQuadPnP
	 */
	public static <T extends ImageGray,Desc extends TupleDesc>
	StereoVisualOdometry<T> stereoQuadPnP( double inlierPixelTol ,
										   double epipolarPixelTol ,
										   double maxDistanceF2F,
										   double maxAssociationError,
										   int ransacIterations ,
										   int refineIterations ,
										   DetectDescribeMulti<T,Desc> detector,
										   Class<T> imageType )
	{
		EstimateNofPnP pnp = FactoryMultiView.computePnP_N(EnumPNP.P3P_FINSTERWALDER, -1);
		DistanceModelMonoPixels<Se3_F64,Point2D3D> distanceMono = new PnPDistanceReprojectionSq();
		PnPStereoDistanceReprojectionSq distanceStereo = new PnPStereoDistanceReprojectionSq();
		PnPStereoEstimator pnpStereo = new PnPStereoEstimator(pnp,distanceMono,0);

		ModelManagerSe3_F64 manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64,Stereo2D3D> generator = new EstimatorToGenerator<>(pnpStereo);

		// Pixel tolerance for RANSAC inliers - euclidean error squared from left + right images
		double ransacTOL = 2*inlierPixelTol * inlierPixelTol;

		ModelMatcher<Se3_F64, Stereo2D3D> motion =
				new Ransac<>(2323, manager, generator, distanceStereo, ransacIterations, ransacTOL);

		RefinePnPStereo refinePnP = null;

		if( refineIterations > 0 ) {
			refinePnP = new PnPStereoRefineRodrigues(1e-12,refineIterations);
		}
		Class<Desc> descType = detector.getDescriptionType();

		ScoreAssociation<Desc> scorer = FactoryAssociation.defaultScore(descType);

		AssociateDescription2D<Desc> assocSame;
		if( maxDistanceF2F > 0 )
			assocSame = new AssociateMaxDistanceNaive<>(scorer, true, maxAssociationError, maxDistanceF2F);
		else
			assocSame = new AssociateDescTo2D<>(FactoryAssociation.greedy(scorer, maxAssociationError, true));

		AssociateStereo2D<Desc> associateStereo = new AssociateStereo2D<>(scorer, epipolarPixelTol, descType);
		TriangulateTwoViewsCalibrated triangulate = FactoryMultiView.triangulateTwoGeometric();

		associateStereo.setThreshold(maxAssociationError);

		VisOdomQuadPnP<T,Desc> alg = new VisOdomQuadPnP<>(
				detector, assocSame, associateStereo, triangulate, motion, refinePnP);

		return new WrapVisOdomQuadPnP<>(alg, refinePnP, associateStereo, distanceStereo, distanceMono, imageType);
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
	public static <T extends ImageBase> StereoVisualOdometry<T> scaleInput( StereoVisualOdometry<T> vo , double scaleFactor )
	{
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
	public static <T extends ImageBase> MonocularPlaneVisualOdometry<T> scaleInput( MonocularPlaneVisualOdometry<T> vo , double scaleFactor )
	{
		return new MonocularPlaneVisualOdometryScaleInput<>(vo, scaleFactor);
	}
}
