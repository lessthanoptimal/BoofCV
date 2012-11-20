package boofcv.alg.sfm.robust;

import boofcv.struct.geo.Point2D3D;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class ModelMatcherTranGivenRot implements ModelMatcher<Vector3D_F64,Point2D3D> {

	Ransac<Vector3D_F64,Point2D3D> alg;
	DistanceTranGivenRotSq dist = new DistanceTranGivenRotSq();
	TranGivenRotGenerator gen = new TranGivenRotGenerator();

	public ModelMatcherTranGivenRot(long randSeed, int maxIterations,
									double thresholdFit) {
		alg = new Ransac<Vector3D_F64, Point2D3D>(randSeed, gen, dist,
				maxIterations, thresholdFit);
	}

	public void setRotation( DenseMatrix64F R ) {
		dist.setRotation(R);
		gen.setRotation(R);
	}

	@Override
	public boolean process(List<Point2D3D> dataSet) {
		return alg.process(dataSet);
	}

	@Override
	public Vector3D_F64 getModel() {
		return alg.getModel();
	}

	@Override
	public List<Point2D3D> getMatchSet() {
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

	@Override
	public int getMinimumSize() {
		return alg.getMinimumSize();
	}
}
