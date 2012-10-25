package boofcv.factory.sfm;

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.abst.sfm.StereoVisualOdometry;
import boofcv.abst.sfm.WrapPixelDepthVoEpipolar;
import boofcv.alg.geo.DistanceModelMonoPixels;
import boofcv.alg.geo.pose.PnPDistanceReprojectionSq;
import boofcv.alg.sfm.PixelDepthVoEpipolar;
import boofcv.alg.sfm.PointPoseTrack;
import boofcv.alg.sfm.StereoSparse3D;
import boofcv.alg.sfm.robust.EstimatorToGenerator;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.Ransac;
import boofcv.struct.geo.PointPosePair;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.se.Se3_F64;

/**
 * Factory for creating visual odometry algorithms.
 *
 * @author Peter Abeles
 */
public class FactoryVisualOdometry {

//	public static <T extends ImageBase>
//	MonocularVisualOdometry<T> monoSimple( int minTracks , double minPixelChange ,
//										   double pixelNoise ,
//										   ImagePointTracker<T> tracker ,
//										   PointTransform_F64 pixelToNormalized )
//	{
//		// translate from pixel to normalized coordinate error
//		Point2D_F64 tempA = new Point2D_F64();
//		Point2D_F64 tempB = new Point2D_F64();
//		pixelToNormalized.compute(pixelNoise,pixelNoise,tempA);
//		pixelToNormalized.compute(0,0,tempB);
//		double noise = (Math.abs(tempA.x-tempB.x) + Math.abs(tempA.y-tempB.y))/2;
//
//		EpipolarMatrixEstimator essentialAlg = FactoryMultiView.computeSingleFundamental(7,false,1);
//		TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();
//
//		ModelGenerator<Se3_F64,AssociatedPair> generateEpipolarMotion =
//				new Se3FromEssentialGenerator(essentialAlg,triangulate);
//
//		DistanceFromModel<Se3_F64,AssociatedPair> distanceSe3 =
//				new DistanceSe3SymmetricSq(triangulate,1,1,0,1,1,0); // TODO use intrinsic
//
//		ModelMatcher<Se3_F64,AssociatedPair> epipolarMotion =
//				new Ransac<Se3_F64,AssociatedPair>(2323,generateEpipolarMotion,distanceSe3,
//						100,2*noise*noise);
//
//		RefineEpipolarMatrix refineE = FactoryMultiView.refineFundamental(1e-8,400, EpipolarError.SIMPLE);
//
//		ModelGenerator<Se3_F64,PointPosePair> generateMotion =
//				new GenerateMotionPnP( FactoryMultiView.computePnPwithEPnP(5,0.1));
//		DistanceFromModel<Se3_F64,PointPosePair> distanceMotion =
//				new DistanceFromModelResidualN<Se3_F64,PointPosePair>(new PnPResidualReprojection());
//
//		ModelMatcher<Se3_F64,PointPosePair> computeMotion =
//				new Ransac<Se3_F64,PointPosePair>(2323,generateMotion,distanceMotion,
//						100,noise*noise);
//
//		RefinePerspectiveNPoint refineMotion = FactoryMultiView.refinePnpEfficient(5,0.1);
//
//		MonocularSimpleVo<T> mono = new MonocularSimpleVo<T>(minTracks,minTracks*2,minPixelChange,tracker,pixelToNormalized,
//				epipolarMotion,refineE,computeMotion,refineMotion);
//
//		return new WrapMonocularSimpleVo<T>(mono);
//	}

//	public static <T extends ImageBase>
//	MonocularVisualOdometry<T> monoSeparated( int minTracks , double minPixelChange ,
//											  double pixelNoise , double triangulateAngle ,
//											  ImagePointTracker<T> tracker ,
//											  PointTransform_F64 pixelToNormalized )
//	{
//		// translate from pixel to normalized coordinate error
//		Point2D_F64 tempA = new Point2D_F64();
//		Point2D_F64 tempB = new Point2D_F64();
//		pixelToNormalized.compute(pixelNoise,pixelNoise,tempA);
//		pixelToNormalized.compute(0,0,tempB);
//		double noise = (Math.abs(tempA.x-tempB.x) + Math.abs(tempA.y-tempB.y))/2;
//
//		EpipolarMatrixEstimator essentialAlg = FactoryMultiView.computeSingleFundamental(7,false,1);
//		TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();
//
//		ModelGenerator<Se3_F64,AssociatedPair> generateEpipolarMotion =
//				new Se3FromEssentialGenerator(essentialAlg,triangulate);
//
//		DistanceFromModel<Se3_F64,AssociatedPair> distanceSe3 =
//				new DistanceSe3SymmetricSq(triangulate,1,1,0,1,1,0); // TODO use intrinsic
//
//		int N = generateEpipolarMotion.getMinimumPoints();
//
//		ModelMatcher<Se3_F64,AssociatedPair> epipolarMotion =
//				new Ransac<Se3_F64,AssociatedPair>(2323,generateEpipolarMotion,distanceSe3,
//						2000,2*noise*noise);
//
//		ModelMatcherTranGivenRot estimateTran = new ModelMatcherTranGivenRot(234,2000,noise*noise);
//
//
//		MonocularSeparatedMotion<T> mono =
//				new MonocularSeparatedMotion<T>(tracker,pixelToNormalized,epipolarMotion,estimateTran,
//						4*noise,minPixelChange,triangulateAngle);
//
//		return new WrapMonocularSeparatedMotion<T>(mono);
//	}

	public static <T extends ImageSingleBand>
	StereoVisualOdometry<T> stereoDepth(int minTracks, double inlierPixelTol,
										ImagePointTracker<T> tracker,
										StereoDisparitySparse<T> sparseDisparity,
										Class<T> imageType) {

		// motion estimation using essential matrix
		Estimate1ofPnP estimator = FactoryMultiView.computePnP_1(EnumPNP.P3P_FINSTERWALDER,-1,2);
		DistanceModelMonoPixels<Se3_F64,PointPosePair> distance = new PnPDistanceReprojectionSq();

		EstimatorToGenerator<Se3_F64,PointPosePair> generator =
				new EstimatorToGenerator<Se3_F64,PointPosePair>(estimator) {
					@Override
					public Se3_F64 createModelInstance() {
						return new Se3_F64();
					}
				};


		// 1/2 a pixel tolerance for RANSAC inliers
		double ransacTOL = inlierPixelTol * inlierPixelTol;

		ModelMatcher<Se3_F64, PointPosePair> motion =
				new Ransac<Se3_F64, PointPosePair>(2323, generator, distance,
						200, ransacTOL);

		// Range from sparse disparity
		StereoSparse3D<T> pixelTo3D = new StereoSparse3D<T>(sparseDisparity,imageType);

		// triangulate the two views
		TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();

		// setup the tracker
		KeyFramePointTracker<T,PointPoseTrack> keyTracker =
				new KeyFramePointTracker<T,PointPoseTrack>(tracker,null,PointPoseTrack.class);

		PixelDepthVoEpipolar<T> alg = new PixelDepthVoEpipolar<T>(minTracks,motion,pixelTo3D,keyTracker,triangulate);

		return new WrapPixelDepthVoEpipolar<T>(alg,pixelTo3D,keyTracker,distance,imageType);
	}
}
