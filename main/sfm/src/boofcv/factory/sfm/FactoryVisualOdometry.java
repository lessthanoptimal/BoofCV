package boofcv.factory.sfm;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.geo.RefineEpipolarMatrix;
import boofcv.abst.geo.fitting.DistanceFromModelResidual;
import boofcv.abst.geo.fitting.DistanceFromModelResidualN;
import boofcv.abst.geo.fitting.GenerateEpipolarMatrix;
import boofcv.abst.geo.fitting.GenerateMotionPnP;
import boofcv.abst.sfm.MonocularVisualOdometry;
import boofcv.abst.sfm.WrapMonocularSimpleVo;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.PointPositionPair;
import boofcv.alg.geo.f.FundamentalResidualSimple;
import boofcv.alg.geo.pose.PnPResidualSimple;
import boofcv.alg.sfm.MonocularSimpleVo;
import boofcv.factory.geo.EpipolarError;
import boofcv.factory.geo.FactoryEpipolar;
import boofcv.numerics.fitting.modelset.DistanceFromModel;
import boofcv.numerics.fitting.modelset.ModelGenerator;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.SimpleInlierRansac;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageBase;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

/**
 * Factory for creating visual odometry algorithms.
 *
 * @author Peter Abeles
 */
public class FactoryVisualOdometry {

	public <T extends ImageBase>
	MonocularVisualOdometry<T> monoSimple( int minTracks , double minDistance ,
										   double pixelNoise ,
										   ImagePointTracker<T> tracker ,
										   PointTransform_F64 pixelToNormalized )
	{

		ModelGenerator<DenseMatrix64F,AssociatedPair> generateE =
				new GenerateEpipolarMatrix(FactoryEpipolar.computeFundamental(false, 7));
		DistanceFromModel<DenseMatrix64F,AssociatedPair> distanceE =
				new DistanceFromModelResidual<DenseMatrix64F,AssociatedPair>(new FundamentalResidualSimple());

		int N = generateE.getMinimumPoints();

		ModelMatcher<DenseMatrix64F,AssociatedPair> computeE =
				new SimpleInlierRansac<DenseMatrix64F,AssociatedPair>(2323,generateE,distanceE,
						100,N,N,100000,pixelNoise);
		RefineEpipolarMatrix refineE = FactoryEpipolar.refineFundamental(1e-8,400, EpipolarError.SIMPLE);

		ModelGenerator<Se3_F64,PointPositionPair> generateMotion =
				new GenerateMotionPnP( FactoryEpipolar.pnpEfficientPnP(5,0.1));
		DistanceFromModel<Se3_F64,PointPositionPair> distanceMotion =
				new DistanceFromModelResidualN<Se3_F64,PointPositionPair>(new PnPResidualSimple());
		N = generateMotion.getMinimumPoints();
		
		ModelMatcher<Se3_F64,PointPositionPair> computeMotion =
				new SimpleInlierRansac<Se3_F64,PointPositionPair>(2323,generateMotion,distanceMotion,
						100,N,N,100000,pixelNoise);

		MonocularSimpleVo<T> mono = new MonocularSimpleVo<T>(minTracks,minDistance,tracker,pixelToNormalized,
				computeE,refineE,computeMotion,null);

		return new WrapMonocularSimpleVo<T>(mono);
	}
}
