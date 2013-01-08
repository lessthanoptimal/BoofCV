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
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.ModelAssistedTracker;
import boofcv.abst.feature.tracker.PkltConfig;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.feature.tracker.PointTrackerAux;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.abst.geo.EstimateNofPnP;
import boofcv.abst.geo.RefinePnP;
import boofcv.abst.sfm.*;
import boofcv.alg.feature.associate.AssociateMaxDistanceNaive;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.feature.tracker.AssistedPyramidKltTracker;
import boofcv.alg.feature.tracker.AssistedTrackerTwoPass;
import boofcv.alg.feature.tracker.PointToAssistedTracker;
import boofcv.alg.geo.DistanceModelMonoPixels;
import boofcv.alg.geo.pose.*;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.sfm.StereoSparse3D;
import boofcv.alg.sfm.d3.VisOdomPixelDepthPnP;
import boofcv.alg.sfm.d3.VisOdomStereoPnP;
import boofcv.alg.sfm.robust.EstimatorToGenerator;
import boofcv.alg.sfm.robust.GeoModelRefineToModelFitter;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidUpdaterDiscrete;
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
	StereoVisualOdometry<T> stereoDepth(int thresholdAdd,
										int thresholdRetire ,
										StereoDisparitySparse<T> sparseDisparity,
										ModelAssistedTrackerCalibrated<T, Se3_F64,Point2D3D> assistedTracker ,
										Class<T> imageType) {

		// Range from sparse disparity
		StereoSparse3D<T> pixelTo3D = new StereoSparse3D<T>(sparseDisparity,imageType);

		VisOdomPixelDepthPnP<T> alg =
				new VisOdomPixelDepthPnP<T>(thresholdAdd,thresholdRetire ,assistedTracker,pixelTo3D,null,null);

		return new WrapVisOdomPixelDepthPnP<T,Se3_F64,Point2D3D>(alg,pixelTo3D,assistedTracker,imageType);
	}

	/**
	 *
	 * @param tracker Feature tracker
	 * @param inlierPixelTol Tolerance for what defines a fit to the motin model.  Try a value between 1 and 2
	 * @param ransacIterations Number of iterations RANSAC will perform
	 * @param refineIterations Number of iterations used to refine the estimate.  Try 100 or 0 to turn off refinement.
	 * @return
	 */
	public static <T extends ImageSingleBand>
	ModelAssistedTrackerCalibrated<T, Se3_F64,Point2D3D> trackerP3P( PointTracker<T> tracker,
																	 double inlierPixelTol,
																	 int ransacIterations ,
																	 int refineIterations ) {
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

		ModelFitter<Se3_F64,Point2D3D> refine = null;

		if( refineIterations > 0 ) {
			RefinePnP refinePnP = FactoryMultiView.refinePnP(1e-12,refineIterations);
			refine = new GeoModelRefineToModelFitter<Se3_F64,Point2D3D>(refinePnP) {

				@Override
				public Se3_F64 createModelInstance() {
					return new Se3_F64();
				}
			};
		}

		ModelAssistedTracker<T, Se3_F64,Point2D3D> assisted =
				new PointToAssistedTracker<T, Se3_F64,Point2D3D>(tracker,motion,refine);

		return new ModelAssistedToCalibrated<T,Se3_F64,Point2D3D>(assisted) {
			@Override
			public void setCalibration(IntrinsicParameters param) {
				distance.setIntrinsic(param.fx,param.fy,param.skew);
			}
		};
	}

	public static <T extends ImageSingleBand,D extends TupleDesc>
	ModelAssistedTrackerCalibrated<T, Se3_F64,Point2D3D> trackerAssistedDdaP3P( DetectDescribePoint<T, D> detDesc,
																				double inlierPixelTol,
																				int ransacIterations ,
																				int refineIterations ,
																				double maxAssociationError ,
																				double associationSecondTol ) {
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

		ModelFitter<Se3_F64,Point2D3D> refine = null;

		if( refineIterations > 0 ) {
			RefinePnP refinePnP = FactoryMultiView.refinePnP(1e-12,refineIterations);
			refine = new GeoModelRefineToModelFitter<Se3_F64,Point2D3D>(refinePnP) {

				@Override
				public Se3_F64 createModelInstance() {
					return new Se3_F64();
				}
			};
		}

		ScoreAssociation<D> score = FactoryAssociation.defaultScore(detDesc.getDescriptorType());

		AssociateDescription2D<D> association =
				new AssociateDescTo2D<D>(
						FactoryAssociation.greedy(score, maxAssociationError, -1, true));

		AssociateDescription2D<D> association2 =
				new AssociateMaxDistanceNaive<D>(score,true,maxAssociationError,associationSecondTol);

		ModelAssistedTracker<T, Se3_F64,Point2D3D> assisted =
				new AssistedTrackerTwoPass<T, D,Se3_F64,Point2D3D>(detDesc,association,association2,
						false,motion,motion,refine);

		return new ModelAssistedToCalibrated<T,Se3_F64,Point2D3D>(assisted) {
			@Override
			public void setCalibration(IntrinsicParameters param) {
				distance.setIntrinsic(param.fx,param.fy,param.skew);
			}
		};
	}

	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	ModelAssistedTrackerCalibrated<T, Se3_F64,Point2D3D> trackerAssistedKltP3P( GeneralFeatureDetector<T, D> detector,
																				PkltConfig<T, D> trackerConfig,
																				double inlierPixelTol,
																				int ransacIterations ,
																				int refineIterations ) {
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

		ModelFitter<Se3_F64,Point2D3D> refine = null;

		if( refineIterations > 0 ) {
			RefinePnP refinePnP = FactoryMultiView.refinePnP(1e-12,refineIterations);
			refine = new GeoModelRefineToModelFitter<Se3_F64,Point2D3D>(refinePnP) {

				@Override
				public Se3_F64 createModelInstance() {
					return new Se3_F64();
				}
			};
		}

		InterpolateRectangle<T> interpInput = FactoryInterpolation.<T>bilinearRectangle(trackerConfig.typeInput);
		InterpolateRectangle<D> interpDeriv = FactoryInterpolation.<D>bilinearRectangle(trackerConfig.typeDeriv);

		ImageGradient<T,D> gradient = FactoryDerivative.sobel(trackerConfig.typeInput, trackerConfig.typeDeriv);

		PyramidUpdaterDiscrete<T> pyramidUpdater = FactoryPyramid.discreteGaussian(trackerConfig.typeInput, -1, 2);

		ModelAssistedTracker<T, Se3_F64,Point2D3D> assisted =
				new AssistedPyramidKltTracker<T,D,Se3_F64,Point2D3D>(trackerConfig,pyramidUpdater,detector,
						gradient,interpInput,interpDeriv,motion,motion,refine);

		return new ModelAssistedToCalibrated<T,Se3_F64,Point2D3D>(assisted) {
			@Override
			public void setCalibration(IntrinsicParameters param) {
				distance.setIntrinsic(param.fx,param.fy,param.skew);
			}
		};
	}

	public static <T extends ImageSingleBand, D extends ImageSingleBand, Aux>
	StereoVisualOdometry<T> stereoFullPnP( int thresholdAdd, int thresholdRetire, double inlierPixelTol,
										   int ransacIterations ,
										   int refineIterations ,
										   StereoDisparitySparse<T> disparity,
										   PointTrackerAux<T,Aux> trackerLeft, PointTrackerAux<T,Aux> trackerRight,
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

		VisOdomStereoPnP<T,D> alg =  new VisOdomStereoPnP<T,D>(thresholdAdd,thresholdRetire,inlierPixelTol,
				trackerLeft,trackerRight,motion,refine,disparity,imageType);

		return new WrapVisOdomStereoPnP<T>(pnpStereo,distanceMono,distanceStereo,alg,refinePnP,imageType);
	}
}
