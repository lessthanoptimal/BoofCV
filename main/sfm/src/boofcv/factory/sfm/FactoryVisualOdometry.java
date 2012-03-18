package boofcv.factory.sfm;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.geo.*;
import boofcv.abst.geo.fitting.DistanceFromModelResidualN;
import boofcv.abst.geo.fitting.GenerateMotionPnP;
import boofcv.abst.sfm.MonocularVisualOdometry;
import boofcv.abst.sfm.WrapMonocularSeparatedMotion;
import boofcv.abst.sfm.WrapMonocularSimpleVo;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.PointPositionPair;
import boofcv.alg.geo.pose.PnPResidualSimple;
import boofcv.alg.sfm.MonocularSeparatedMotion;
import boofcv.alg.sfm.MonocularSimpleVo;
import boofcv.alg.sfm.robust.DistanceSe3SymmetricSq;
import boofcv.alg.sfm.robust.ModelMatcherTranGivenRot;
import boofcv.alg.sfm.robust.Se3FromEssentialGenerator;
import boofcv.factory.geo.EpipolarError;
import boofcv.factory.geo.FactoryEpipolar;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.numerics.fitting.modelset.DistanceFromModel;
import boofcv.numerics.fitting.modelset.ModelGenerator;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.SimpleInlierRansac;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageBase;
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
		
		EpipolarMatrixEstimator essentialAlg = FactoryEpipolar.computeFundamental(false, 7);
		TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();
				
		ModelGenerator<Se3_F64,AssociatedPair> generateEpipolarMotion =
				new Se3FromEssentialGenerator(essentialAlg,triangulate);

		DistanceFromModel<Se3_F64,AssociatedPair> distanceSe3 =
				new DistanceSe3SymmetricSq(triangulate);
		
		int N = generateEpipolarMotion.getMinimumPoints();

		ModelMatcher<Se3_F64,AssociatedPair> epipolarMotion =
				new SimpleInlierRansac<Se3_F64,AssociatedPair>(2323,generateEpipolarMotion,distanceSe3,
						100,N,N,100000,2*noise*noise);

		RefineEpipolarMatrix refineE = FactoryEpipolar.refineFundamental(1e-8,400, EpipolarError.SIMPLE);

		ModelGenerator<Se3_F64,PointPositionPair> generateMotion =
				new GenerateMotionPnP( FactoryEpipolar.pnpEfficientPnP(5,0.1));
		DistanceFromModel<Se3_F64,PointPositionPair> distanceMotion =
				new DistanceFromModelResidualN<Se3_F64,PointPositionPair>(new PnPResidualSimple());
		N = generateMotion.getMinimumPoints();
		
		ModelMatcher<Se3_F64,PointPositionPair> computeMotion =
				new SimpleInlierRansac<Se3_F64,PointPositionPair>(2323,generateMotion,distanceMotion,
						100,N,N,100000,noise*noise);

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

		EpipolarMatrixEstimator essentialAlg = FactoryEpipolar.computeFundamental(false, 7);
		TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();

		ModelGenerator<Se3_F64,AssociatedPair> generateEpipolarMotion =
				new Se3FromEssentialGenerator(essentialAlg,triangulate);

		DistanceFromModel<Se3_F64,AssociatedPair> distanceSe3 =
				new DistanceSe3SymmetricSq(triangulate);

		int N = generateEpipolarMotion.getMinimumPoints();

		ModelMatcher<Se3_F64,AssociatedPair> epipolarMotion =
				new SimpleInlierRansac<Se3_F64,AssociatedPair>(2323,generateEpipolarMotion,distanceSe3,
						300,N,N,100000,2*noise*noise);

		ModelMatcherTranGivenRot estimateTran = new ModelMatcherTranGivenRot(234,200,10000,noise*noise);

		BundleAdjustmentCalibrated ba = null;//FactoryEpipolar.bundleCalibrated(1e-8,50);

		MonocularSeparatedMotion<T> mono =
				new MonocularSeparatedMotion<T>(tracker,pixelToNormalized,epipolarMotion,estimateTran,
						ba,4*noise,minTracks,minPixelChange,triangulateAngle);

		return new WrapMonocularSeparatedMotion<T>(mono);
	}
}
