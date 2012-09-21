package boofcv.alg.sfm.robust;

import boofcv.alg.geo.PointPositionPair;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.Ransac;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class ModelMatcherTranGivenRot implements ModelMatcher<Vector3D_F64,PointPositionPair> {

	Ransac<Vector3D_F64,PointPositionPair> alg;
	DistanceTranGivenRotSq dist = new DistanceTranGivenRotSq();
	TranGivenRotGenerator gen = new TranGivenRotGenerator();

	public ModelMatcherTranGivenRot(long randSeed, int maxIterations,
									double thresholdFit) {
		alg = new Ransac<Vector3D_F64, PointPositionPair>(randSeed, gen, dist,
				maxIterations, thresholdFit);
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
	public int getInputIndex(int matchIndex) {
		return alg.getInputIndex(matchIndex);
	}

	@Override
	public double getError() {
		return alg.getError();
	}
}
