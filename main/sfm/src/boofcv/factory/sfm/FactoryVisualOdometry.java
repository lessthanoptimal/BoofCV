package boofcv.factory.sfm;

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.abst.geo.EpipolarMatrixEstimator;
import boofcv.abst.geo.RefineEpipolarMatrix;
import boofcv.abst.geo.RefinePerspectiveNPoint;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.abst.geo.fitting.DistanceFromModelResidualN;
import boofcv.abst.geo.fitting.GenerateMotionPnP;
import boofcv.abst.sfm.*;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.PointPositionPair;
import boofcv.alg.geo.pose.PnPResidualSimple;
import boofcv.alg.sfm.*;
import boofcv.alg.sfm.robust.DistanceSe3SymmetricSq;
import boofcv.alg.sfm.robust.ModelMatcherTranGivenRot;
import boofcv.alg.sfm.robust.Se3FromEssentialGenerator;
import boofcv.factory.geo.EpipolarError;
import boofcv.factory.geo.FactoryEpipolar;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.numerics.fitting.modelset.DistanceFromModel;
import boofcv.numerics.fitting.modelset.ModelGenerator;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.Ransac;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;

/**
 * Factory for creating visual odometry algorithms.
 *
 * @author Peter Abeles
 */
public class FactoryVisualOdometry {

	public static <T extends ImageBase>
	MonocularVisualOdometry<T> monoSimple( int minTracks , double minPixelChange ,
										   double pixelNoise ,
										   ImagePointTracker<T> tracker ,
										   PointTransform_F64 pixelToNormalized )
	{
		// translate from pixel to normalized coordinate error
		Point2D_F64 tempA = new Point2D_F64();
		Point2D_F64 tempB = new Point2D_F64();
		pixelToNormalized.compute(pixelNoise,pixelNoise,tempA);
		pixelToNormalized.compute(0,0,tempB);
		double noise = (Math.abs(tempA.x-tempB.x) + Math.abs(tempA.y-tempB.y))/2;
		
		EpipolarMatrixEstimator essentialAlg = FactoryEpipolar.computeFundamentalOne(7,false,1);
		TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();
				
		ModelGenerator<Se3_F64,AssociatedPair> generateEpipolarMotion =
				new Se3FromEssentialGenerator(essentialAlg,triangulate);

		DistanceFromModel<Se3_F64,AssociatedPair> distanceSe3 =
				new DistanceSe3SymmetricSq(triangulate,1,1,0,1,1,0); // TODO use intrinsic

		ModelMatcher<Se3_F64,AssociatedPair> epipolarMotion =
				new Ransac<Se3_F64,AssociatedPair>(2323,generateEpipolarMotion,distanceSe3,
						100,2*noise*noise);

		RefineEpipolarMatrix refineE = FactoryEpipolar.refineFundamental(1e-8,400, EpipolarError.SIMPLE);

		ModelGenerator<Se3_F64,PointPositionPair> generateMotion =
				new GenerateMotionPnP( FactoryEpipolar.pnpEfficientPnP(5,0.1));
		DistanceFromModel<Se3_F64,PointPositionPair> distanceMotion =
				new DistanceFromModelResidualN<Se3_F64,PointPositionPair>(new PnPResidualSimple());

		ModelMatcher<Se3_F64,PointPositionPair> computeMotion =
				new Ransac<Se3_F64,PointPositionPair>(2323,generateMotion,distanceMotion,
						100,noise*noise);

		RefinePerspectiveNPoint refineMotion = FactoryEpipolar.refinePnpEfficient(5,0.1);

		MonocularSimpleVo<T> mono = new MonocularSimpleVo<T>(minTracks,minTracks*2,minPixelChange,tracker,pixelToNormalized,
				epipolarMotion,refineE,computeMotion,refineMotion);

		return new WrapMonocularSimpleVo<T>(mono);
	}

	public static <T extends ImageBase>
	MonocularVisualOdometry<T> monoSeparated( int minTracks , double minPixelChange ,
											  double pixelNoise , double triangulateAngle ,
											  ImagePointTracker<T> tracker ,
											  PointTransform_F64 pixelToNormalized )
	{
		// translate from pixel to normalized coordinate error
		Point2D_F64 tempA = new Point2D_F64();
		Point2D_F64 tempB = new Point2D_F64();
		pixelToNormalized.compute(pixelNoise,pixelNoise,tempA);
		pixelToNormalized.compute(0,0,tempB);
		double noise = (Math.abs(tempA.x-tempB.x) + Math.abs(tempA.y-tempB.y))/2;

		EpipolarMatrixEstimator essentialAlg = FactoryEpipolar.computeFundamentalOne(7,false,1);
		TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();

		ModelGenerator<Se3_F64,AssociatedPair> generateEpipolarMotion =
				new Se3FromEssentialGenerator(essentialAlg,triangulate);

		DistanceFromModel<Se3_F64,AssociatedPair> distanceSe3 =
				new DistanceSe3SymmetricSq(triangulate,1,1,0,1,1,0); // TODO use intrinsic

		int N = generateEpipolarMotion.getMinimumPoints();

		ModelMatcher<Se3_F64,AssociatedPair> epipolarMotion =
				new Ransac<Se3_F64,AssociatedPair>(2323,generateEpipolarMotion,distanceSe3,
						2000,2*noise*noise);

		ModelMatcherTranGivenRot estimateTran = new ModelMatcherTranGivenRot(234,2000,noise*noise);


		MonocularSeparatedMotion<T> mono =
				new MonocularSeparatedMotion<T>(tracker,pixelToNormalized,epipolarMotion,estimateTran,
						4*noise,minPixelChange,triangulateAngle);

		return new WrapMonocularSeparatedMotion<T>(mono);
	}

	public static <T extends ImageSingleBand>
	StereoVisualOdometry<T> stereoDepth(int minTracks, double inlierPixelTol,
										ImagePointTracker<T> tracker,
										StereoParameters stereoParam,
										StereoDisparitySparse<T> sparseDisparity,
										Class<T> imageType) {

		// motion estimation using essential matrix
		EpipolarMatrixEstimator essentialAlg = FactoryEpipolar.computeFundamentalOne(5, false, 2);
		TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();
		ModelGenerator<Se3_F64, AssociatedPair> generateEpipolarMotion =
				new Se3FromEssentialGenerator(essentialAlg, triangulate);

		DistanceFromModel<Se3_F64, AssociatedPair> distanceSe3 =
				new DistanceSe3SymmetricSq(triangulate,
						stereoParam.left.fx, stereoParam.left.fy, stereoParam.left.skew,
						stereoParam.left.fx, stereoParam.left.fy, stereoParam.left.skew);

		// 1/2 a pixel tolerance for RANSAC inliers
		double ransacTOL = inlierPixelTol * inlierPixelTol * 2.0;

		ModelMatcher<Se3_F64, AssociatedPair> epipolarMotion =
				new Ransac<Se3_F64, AssociatedPair>(2323, generateEpipolarMotion, distanceSe3,
						200, ransacTOL);

		// Range from sparse disparity
		StereoSparse3D<T> pixelTo3D = new StereoSparse3D<T>(sparseDisparity,stereoParam,imageType);

		// transform to go from pixel coordinates to normalized coordinates
		PointTransform_F64 leftPixelToNorm = LensDistortionOps.transformRadialToNorm_F64(stereoParam.left);

		// setup the tracker
		KeyFramePointTracker<T,PointPoseTrack> keyTracker =
				new KeyFramePointTracker<T,PointPoseTrack>(tracker,leftPixelToNorm,PointPoseTrack.class);

		PixelDepthVoEpipolar<T> alg = new PixelDepthVoEpipolar<T>(minTracks,epipolarMotion,pixelTo3D,keyTracker,triangulate);

		return new WrapPixelDepthVoEpipolar<T>(alg,pixelTo3D);
	}

	public static <T extends ImageSingleBand>
	StereoVisualOdometry<T> stereoEpipolar(int minTracks, double inlierPixelTol,
										   ImagePointTracker<T> tracker,
										   StereoParameters stereoParam,
										   StereoDisparitySparse<T> sparseDisparity,
										   Class<T> imageType)
	{
		int maxIterations = 200;

		// motion estimation using essential matrix
		EpipolarMatrixEstimator essentialAlg = FactoryEpipolar.computeFundamentalOne(5, false, 2);
		TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();
		ModelGenerator<Se3_F64, AssociatedPair> generateSe3 =
				new Se3FromEssentialGenerator(essentialAlg, triangulate);

		// Distance metric for estimating motion between left camera key and current
		DistanceFromModel<Se3_F64, AssociatedPair> distanceL2L =
				new DistanceSe3SymmetricSq(triangulate,
						stereoParam.left.fx, stereoParam.left.fy, stereoParam.left.skew,
						stereoParam.left.fx, stereoParam.left.fy, stereoParam.left.skew);

		// distance metric for estimating baseline between left and right camera
		DistanceFromModel<Se3_F64, AssociatedPair> distanceL2R =
				new DistanceSe3SymmetricSq(triangulate,
						stereoParam.left.fx, stereoParam.left.fy, stereoParam.left.skew,
						stereoParam.right.fx, stereoParam.right.fy, stereoParam.right.skew);

		// pixel tolerance for RANSAC inliers
		double ransacTOL = inlierPixelTol * inlierPixelTol * 2.0;

		ModelMatcher<Se3_F64, AssociatedPair> robustMotionL2L =
				new Ransac<Se3_F64, AssociatedPair>(2323, generateSe3, distanceL2L,
						maxIterations, ransacTOL);

		ModelMatcher<Se3_F64, AssociatedPair> robustMotionL2R =
				new Ransac<Se3_F64, AssociatedPair>(2323, generateSe3, distanceL2R,
						maxIterations, ransacTOL);

		EpipolarMatrixEstimator crudeEssential = FactoryEpipolar.computeFundamentalOne(8, false, 0);
		ModelGenerator<Se3_F64, AssociatedPair> crudeSe3 =
				new Se3FromEssentialGenerator(crudeEssential, triangulate);

		// used to match features in the right image
		AssociateStereoPoint<T> stereoAssociate = new AssociateStereoPoint<T>(sparseDisparity,stereoParam,imageType);

		// transform to go from pixel coordinates to normalized coordinates
		PointTransform_F64 leftPixelToNorm = LensDistortionOps.transformRadialToNorm_F64(stereoParam.left);
		PointTransform_F64 rightPixelToNorm = LensDistortionOps.transformRadialToNorm_F64(stereoParam.right);

		StereoVoEpipolar<T> alg = new StereoVoEpipolar<T>(robustMotionL2L,robustMotionL2R,crudeSe3,
				leftPixelToNorm,rightPixelToNorm,
				tracker,stereoAssociate,stereoParam.rightToLeft,minTracks);

		return new WrapStereoVoEpipolar<T>(alg,stereoAssociate);
	}
}
