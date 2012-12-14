package boofcv.factory.sfm;

import boofcv.abst.feature.associate.AssociateDescTo2D;
import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.ModelAssistedTracker;
import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.abst.geo.RefinePnP;
import boofcv.abst.sfm.StereoVisualOdometry;
import boofcv.abst.sfm.WrapVisOdomPixelDepthPnP;
import boofcv.alg.feature.associate.AssociateMaxDistanceNaive;
import boofcv.alg.feature.tracker.AssistedTrackerTwoPass;
import boofcv.alg.feature.tracker.PointToAssistedTracker;
import boofcv.alg.geo.DistanceModelMonoPixels;
import boofcv.alg.geo.pose.PnPDistanceReprojectionSq;
import boofcv.alg.sfm.StereoSparse3D;
import boofcv.alg.sfm.d3.VisOdomPixelDepthPnP;
import boofcv.alg.sfm.robust.EstimatorToGenerator;
import boofcv.alg.sfm.robust.GeoModelRefineToModelFitter;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;

/**
 * Factory for creating visual odometry algorithms.
 *
 * @author Peter Abeles
 */
public class FactoryVisualOdometry {

	/**
	 * Stereo visual odometry algorithm which only uses the right camera to estimate a points 3D location.  The camera's
	 * pose is updated relative to the left camera using PnP algorithms.  See {@link VisOdomPixelDepthPnP} for more
	 * details.
	 *
	 * @param thresholdAdd Add new tracks when less than this number are in the inlier set.  Tracker dependent. Set to
	 *                     a value <= 0 to add features every frame.
	 * @param thresholdRetire Discard a track if it is not in the inlier set after this many updates.  Try 2
	 * @param inlierPixelTol Tolerance for what defines a fit to the motin model.  Try a value between 1 and 2
	 * @param tracker Feature tracker
	 * @param sparseDisparity Estimates the 3D location of features
	 * @param ransacIterations Number of iterations RANSAC will perform
	 * @param refineIterations Number of iterations used to refine the estimate.  Try 100 or 0 to turn off refinement.
	 * @param imageType Type of image being processed.
	 * @return StereoVisualOdometry
	 */
	public static <T extends ImageSingleBand>
	StereoVisualOdometry<T> stereoDepth(int thresholdAdd,
										int thresholdRetire ,
										double inlierPixelTol,
										ImagePointTracker<T> tracker,
										StereoDisparitySparse<T> sparseDisparity,
										int ransacIterations ,
										int refineIterations ,
										Class<T> imageType) {

		// motion estimation using essential matrix
		Estimate1ofPnP estimator = FactoryMultiView.computePnP_1(EnumPNP.P3P_FINSTERWALDER,-1,2);
		DistanceModelMonoPixels<Se3_F64,Point2D3D> distance = new PnPDistanceReprojectionSq();

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

		// Range from sparse disparity
		StereoSparse3D<T> pixelTo3D = new StereoSparse3D<T>(sparseDisparity,imageType);

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

		VisOdomPixelDepthPnP<T> alg = new VisOdomPixelDepthPnP<T>(thresholdAdd,thresholdRetire ,assisted,pixelTo3D,null,null);

		return new WrapVisOdomPixelDepthPnP<T>(alg,pixelTo3D,tracker,distance,imageType);
	}

	public static <T extends ImageSingleBand,D extends TupleDesc>
	StereoVisualOdometry<T> stereoDepth(int thresholdAdd,
										int thresholdRetire ,
										double inlierPixelTol,
										DetectDescribePoint<T, D> detDesc,
										StereoDisparitySparse<T> sparseDisparity,
										int ransacIterations ,
										int refineIterations ,
										double maxAssociationError ,
										double associationSecondTol ,
										Class<T> imageType) {


		// motion estimation using essential matrix
		Estimate1ofPnP estimator = FactoryMultiView.computePnP_1(EnumPNP.P3P_FINSTERWALDER,-1,2);
		DistanceModelMonoPixels<Se3_F64,Point2D3D> distance = new PnPDistanceReprojectionSq();

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

		// Range from sparse disparity
		StereoSparse3D<T> pixelTo3D = new StereoSparse3D<T>(sparseDisparity,imageType);

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


		VisOdomPixelDepthPnP<T> alg = new VisOdomPixelDepthPnP<T>(thresholdAdd,thresholdRetire ,assisted,pixelTo3D,null,null);

		return new WrapVisOdomPixelDepthPnP<T>(alg,pixelTo3D,assisted,distance,imageType);
	}
}
