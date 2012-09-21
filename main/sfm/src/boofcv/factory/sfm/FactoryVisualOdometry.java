package boofcv.factory.sfm;

import boofcv.abst.feature.disparity.DisparityTo3D;
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
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.PointPositionPair;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.UtilIntrinsic;
import boofcv.alg.geo.pose.PnPResidualSimple;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.sfm.MonocularSeparatedMotion;
import boofcv.alg.sfm.MonocularSimpleVo;
import boofcv.alg.sfm.PointPoseTrack;
import boofcv.alg.sfm.StereoSimpleVo;
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
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

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
				new DistanceSe3SymmetricSq(triangulate,1,1,0); // TODO use intrinsic

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
				new DistanceSe3SymmetricSq(triangulate,1,1,0); // TODO use intrinsic

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
	StereoVisualOdometry<T> stereoSimple( int minTracks , double inlierPixelError ,
										  ImagePointTracker<T> tracker ,
										  StereoParameters stereoParam ,
										  StereoDisparitySparse<T> sparseDisparity ,
										  Class<T> imageType )
	{
		// compute rectification
		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();
		Se3_F64 leftToRight = stereoParam.getRightToLeft().invert(null);

		// original camera calibration matrices
		DenseMatrix64F K1 = UtilIntrinsic.calibrationMatrix(stereoParam.getLeft(), null);
		DenseMatrix64F K2 = UtilIntrinsic.calibrationMatrix(stereoParam.getRight(),null);

		rectifyAlg.process(K1,new Se3_F64(),K2,leftToRight);

		// rectification matrix for each image
		DenseMatrix64F rect1 = rectifyAlg.getRect1();
		DenseMatrix64F rect2 = rectifyAlg.getRect2();
		// New calibration matrix,
		// Both cameras have the same one after rectification.
		DenseMatrix64F rectK = rectifyAlg.getCalibrationMatrix();

		// Adjust the rectification to make the view area more useful
		RectifyImageOps.fullViewLeft(stereoParam.left, rect1, rect2, rectK);

		// inlier error is relative to pixel error, approximate pixel error in normalized image coordinates
		double focalLength = rectK.get(0,0);
		double errorTolNormPixel =  inlierPixelError/focalLength;

		// Estimator for PnP problem
		ModelGenerator<Se3_F64,PointPositionPair> generateMotion =
				new GenerateMotionPnP( FactoryEpipolar.pnpEfficientPnP(5,0.1));
		DistanceFromModel<Se3_F64,PointPositionPair> distanceMotion =
				new DistanceFromModelResidualN<Se3_F64,PointPositionPair>(new PnPResidualSimple());

		ModelMatcher<Se3_F64,PointPositionPair> computeMotion =
				new Ransac<Se3_F64,PointPositionPair>(2323,generateMotion,distanceMotion,
						200,errorTolNormPixel*errorTolNormPixel);

		// Refine the PnP pose estimate
		RefinePerspectiveNPoint refineMotion = FactoryEpipolar.refinePnP(1e-12, 200);

		// Range from sparse disparity
		ImageDistort<T> rectLeft = RectifyImageOps.rectifyImage(stereoParam.left,rect1,imageType);
		ImageDistort<T> rectRight = RectifyImageOps.rectifyImage(stereoParam.right,rect2,imageType);
		PointTransform_F32 pixelToRectified = RectifyImageOps.rectifyTransform(stereoParam.left,rect1);
		DisparityTo3D<T> pixelTo3D = new DisparityTo3D<T>(sparseDisparity,rectLeft,rectRight,
				pixelToRectified,imageType);
		pixelTo3D.configure(stereoParam.getBaseline(),rectK);

		// transform to go from pixel coordinates to normalized coordinates
		PointTransform_F64 pixelToNormalized = RectifyImageOps.rectifyNormalized_F64(stereoParam.left,rect1,rectK);

		// setup the tracker
		KeyFramePointTracker<T,PointPoseTrack> keyTracker =
				new KeyFramePointTracker<T,PointPoseTrack>(tracker,pixelToNormalized,PointPoseTrack.class);

		StereoSimpleVo<T> alg = new StereoSimpleVo<T>(keyTracker,pixelTo3D,computeMotion, refineMotion);

		return new WrapStereoSimpleVo<T>(alg);
	}
}
