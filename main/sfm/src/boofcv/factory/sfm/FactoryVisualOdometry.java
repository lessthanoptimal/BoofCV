package boofcv.factory.sfm;

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.abst.geo.RefinePnP;
import boofcv.abst.sfm.StereoVisualOdometry;
import boofcv.abst.sfm.WrapVisOdomPixelDepthPnP;
import boofcv.alg.geo.DistanceModelMonoPixels;
import boofcv.alg.geo.pose.PnPDistanceReprojectionSq;
import boofcv.alg.sfm.StereoSparse3D;
import boofcv.alg.sfm.VisOdomPixelDepthPnP;
import boofcv.alg.sfm.robust.EstimatorToGenerator;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.Ransac;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.se.Se3_F64;

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
				new Ransac<Se3_F64, Point2D3D>(2323, generator, distance,
						200, ransacTOL);

		// Range from sparse disparity
		StereoSparse3D<T> pixelTo3D = new StereoSparse3D<T>(sparseDisparity,imageType);


		RefinePnP refine = null;

		if( refineIterations > 0 ) {
			refine = FactoryMultiView.refinePnP(1e-12,refineIterations);
		}

		VisOdomPixelDepthPnP<T> alg = new VisOdomPixelDepthPnP<T>(thresholdAdd,thresholdRetire ,motion,pixelTo3D,refine,tracker,null);

		return new WrapVisOdomPixelDepthPnP<T>(alg,pixelTo3D,tracker,distance,imageType);
	}
}
