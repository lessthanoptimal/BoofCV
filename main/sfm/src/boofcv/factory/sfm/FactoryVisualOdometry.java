/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.feature.detdesc.DetectDescribeMulti;
import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.ExtractTrackDescription;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.abst.geo.EstimateNofPnP;
import boofcv.abst.geo.RefinePnP;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.abst.sfm.StereoVisualOdometry;
import boofcv.abst.sfm.WrapVisOdomPixelDepthPnP;
import boofcv.abst.sfm.WrapVisOdomQuadPnP;
import boofcv.abst.sfm.WrapVisOdomStereoPnP;
import boofcv.alg.feature.associate.AssociateMaxDistanceNaive;
import boofcv.alg.feature.associate.AssociateStereo2D;
import boofcv.alg.geo.DistanceModelMonoPixels;
import boofcv.alg.geo.pose.*;
import boofcv.alg.sfm.StereoSparse3D;
import boofcv.alg.sfm.d3.VisOdomDualTrackPnP;
import boofcv.alg.sfm.d3.VisOdomPixelDepthPnP;
import boofcv.alg.sfm.d3.VisOdomQuadPnP;
import boofcv.alg.sfm.robust.EstimatorToGenerator;
import boofcv.alg.sfm.robust.GeoModelRefineToModelFitter;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.sfm.Stereo2D3D;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;

/**
 * Factory for creating visual odometry algorithms.
 *
 * @author Peter Abeles
 */
// TODO reduce duplicate code in tracker functions
public class FactoryVisualOdometry {

	/**
	 * Stereo visual odometry algorithm which only uses the right camera to estimate a points 3D location.  The camera's
	 * pose is updated relative to the left camera using PnP algorithms.  See {@link VisOdomPixelDepthPnP} for more
	 * details.
	 *
	 * @param thresholdAdd Add new tracks when less than this number are in the inlier set.  Tracker dependent. Set to
	 *                     a value <= 0 to add features every frame.
	 * @param thresholdRetire Discard a track if it is not in the inlier set after this many updates.  Try 2
	 * @param sparseDisparity Estimates the 3D location of features
	 * @param imageType Type of image being processed.
	 * @return StereoVisualOdometry
	 */
	public static <T extends ImageSingleBand>
	StereoVisualOdometry<T> stereoDepth(double inlierPixelTol,
										int thresholdAdd,
										int thresholdRetire ,
										int ransacIterations ,
										int refineIterations ,
										StereoDisparitySparse<T> sparseDisparity,
										PointTracker<T> tracker ,
										Class<T> imageType) {

		// Range from sparse disparity
		StereoSparse3D<T> pixelTo3D = new StereoSparse3D<T>(sparseDisparity,imageType);

		Estimate1ofPnP estimator = FactoryMultiView.computePnP_1(EnumPNP.P3P_FINSTERWALDER,-1,2);
		final DistanceModelMonoPixels<Se3_F64,Point2D3D> distance = new PnPDistanceReprojectionSq();

		EstimatorToGenerator<Se3_F64,Point2D3D> generator =
				new EstimatorToGenerator<Se3_F64,Point2D3D>(estimator) {
					@Override
					public Se3_F64 createModelInstance() {
						return new Se3_F64();
					}
				};

		// 1/2 a pixel tolerance for RANSAC inliers
		double ransacTOL = inlierPixelTol * inlierPixelTol;

		ModelMatcher<Se3_F64, Point2D3D> motion =
				new Ransac<Se3_F64, Point2D3D>(2323, generator, distance, ransacIterations, ransacTOL);

		RefinePnP refine = null;

		if( refineIterations > 0 ) {
			refine = FactoryMultiView.refinePnP(1e-12,refineIterations);
		}

		VisOdomPixelDepthPnP<T> alg =
				new VisOdomPixelDepthPnP<T>(thresholdAdd,thresholdRetire ,motion,pixelTo3D,refine,null,null,null);

		return new WrapVisOdomPixelDepthPnP<T,Se3_F64,Point2D3D>(alg,pixelTo3D,distance,imageType);
	}

	public static <T extends ImageSingleBand, Desc extends TupleDesc>
	StereoVisualOdometry<T> stereoFullPnP( int thresholdAdd, int thresholdRetire,
										   double inlierPixelTol ,
										   double epipolarPixelTol,
										   int ransacIterations ,
										   int refineIterations ,
										   PointTracker<T> trackerLeft, PointTracker<T> trackerRight,
										   Class<T> imageType )
	{
		if( !(trackerLeft instanceof ExtractTrackDescription) || !(trackerRight instanceof ExtractTrackDescription) ) {
			throw new IllegalArgumentException("Both trackers must implement TrackDescription");
		}

		EstimateNofPnP pnp = FactoryMultiView.computePnP_N(EnumPNP.P3P_FINSTERWALDER, -1);
		DistanceModelMonoPixels<Se3_F64,Point2D3D> distanceMono = new PnPDistanceReprojectionSq();
		PnPStereoDistanceReprojectionSq distanceStereo = new PnPStereoDistanceReprojectionSq();
		PnPStereoEstimator pnpStereo = new PnPStereoEstimator(pnp,distanceMono,0);

		EstimatorToGenerator<Se3_F64,Stereo2D3D> generator =
				new EstimatorToGenerator<Se3_F64,Stereo2D3D>(pnpStereo) {
					@Override
					public Se3_F64 createModelInstance() {
						return new Se3_F64();
					}
				};

		// Pixel tolerance for RANSAC inliers - euclidean error squared from left + right images
		double ransacTOL = 2*inlierPixelTol * inlierPixelTol;

		ModelMatcher<Se3_F64, Stereo2D3D> motion =
				new Ransac<Se3_F64, Stereo2D3D>(2323, generator, distanceStereo, ransacIterations, ransacTOL);

		RefinePnPStereo refinePnP = null;
		ModelFitter<Se3_F64,Stereo2D3D> refine = null;

		ExtractTrackDescription<Desc> extractor = (ExtractTrackDescription)trackerLeft;
		Class<Desc> descType = extractor.getDescriptionType();
		ScoreAssociation<Desc> scorer = FactoryAssociation.defaultScore(descType);
		AssociateStereo2D<Desc> associateStereo = new AssociateStereo2D<Desc>(scorer,epipolarPixelTol,descType);

		// need to make sure associations are unique
		AssociateDescription2D<Desc> associateUnique = associateStereo;
		if( !associateStereo.uniqueDestination() || !associateStereo.uniqueSource() ) {
			associateUnique = new EnforceUniqueByScore.Describe2D<Desc>(associateStereo,true,true);
		}

		if( refineIterations > 0 ) {
			refinePnP = new PnPStereoRefineRodrigues(1e-12,refineIterations);
			refine = new GeoModelRefineToModelFitter<Se3_F64,Stereo2D3D>(refinePnP) {

				@Override
				public Se3_F64 createModelInstance() {
					return new Se3_F64();
				}
			};
		}

		TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();

		VisOdomDualTrackPnP<T,Desc> alg =  new VisOdomDualTrackPnP<T,Desc>(thresholdAdd,thresholdRetire,epipolarPixelTol,
				trackerLeft,trackerRight,associateUnique,triangulate,motion,refine);

		return new WrapVisOdomStereoPnP<T>(pnpStereo,distanceMono,distanceStereo,associateStereo,alg,refinePnP,imageType);
	}

	public static <T extends ImageSingleBand,Desc extends TupleDesc>
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

		EstimatorToGenerator<Se3_F64,Stereo2D3D> generator =
				new EstimatorToGenerator<Se3_F64,Stereo2D3D>(pnpStereo) {
					@Override
					public Se3_F64 createModelInstance() {
						return new Se3_F64();
					}
				};

		// Pixel tolerance for RANSAC inliers - euclidean error squared from left + right images
		double ransacTOL = 2*inlierPixelTol * inlierPixelTol;

		ModelMatcher<Se3_F64, Stereo2D3D> motion =
				new Ransac<Se3_F64, Stereo2D3D>(2323, generator, distanceStereo, ransacIterations, ransacTOL);

		RefinePnPStereo refinePnP = null;
		ModelFitter<Se3_F64,Stereo2D3D> refine = null;

		if( refineIterations > 0 ) {
			refinePnP = new PnPStereoRefineRodrigues(1e-12,refineIterations);
			refine = new GeoModelRefineToModelFitter<Se3_F64,Stereo2D3D>(refinePnP) {

				@Override
				public Se3_F64 createModelInstance() {
					return new Se3_F64();
				}
			};
		}
		Class<Desc> descType = detector.getDescriptionType();

		ScoreAssociation<Desc> scorer = FactoryAssociation.defaultScore(descType);

		AssociateDescription2D<Desc> assocSame;
		if( maxDistanceF2F > 0 )
			assocSame = new AssociateMaxDistanceNaive<Desc>(scorer,true,maxAssociationError,maxDistanceF2F);
		else
			assocSame = new AssociateDescTo2D<Desc>(FactoryAssociation.greedy(scorer, maxAssociationError, true));

		AssociateStereo2D<Desc> associateStereo = new AssociateStereo2D<Desc>(scorer,epipolarPixelTol,descType);
		TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();

		associateStereo.setThreshold(maxAssociationError);

		VisOdomQuadPnP<T,Desc> alg = new VisOdomQuadPnP<T,Desc>(
				detector,assocSame,associateStereo,triangulate,motion,refine);

		return new WrapVisOdomQuadPnP<T,Desc>(alg,refinePnP,associateStereo,distanceStereo,distanceMono,imageType);
	}
}
