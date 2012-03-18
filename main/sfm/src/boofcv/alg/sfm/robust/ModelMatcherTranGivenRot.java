package boofcv.alg.sfm.robust;

import boofcv.alg.geo.PointPositionPair;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.SimpleInlierRansac;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class ModelMatcherTranGivenRot implements ModelMatcher<Vector3D_F64,PointPositionPair> {

	SimpleInlierRansac<Vector3D_F64,PointPositionPair> alg;
	DistanceTranGivenRotSq dist = new DistanceTranGivenRotSq();
	TranGivenRotGenerator gen = new TranGivenRotGenerator();

	public ModelMatcherTranGivenRot(long randSeed, int maxIterations,
									int exitFitPoints, double thresholdFit) {
		int minPts = gen.getMinimumPoints();
		alg = new SimpleInlierRansac<Vector3D_F64, PointPositionPair>(randSeed, gen, dist,
				maxIterations, minPts, minPts*3, exitFitPoints, thresholdFit);
	}

	public void setRotation( DenseMatrix64F R ) {
		dist.setRotation(R);
		gen.setRotation(R);
	}

	@Override
	public boolean process(List<PointPositionPair> dataSet) {
		return alg.process(dataSet);
	}

	@Override
	public Vector3D_F64 getModel() {
		return alg.getModel();
	}

	@Override
	public List<PointPositionPair> getMatchSet() {
		return alg.getMatchSet();
	}

	@Override
	public double getError() {
		return alg.getError();
	}
}
