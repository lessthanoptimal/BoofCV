package boofcv.alg.sfm.robust;

import boofcv.numerics.fitting.modelset.DistanceFromModel;
import boofcv.struct.geo.PointPositionPair;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class DistanceTranGivenRotSq implements DistanceFromModel<Vector3D_F64,PointPositionPair> {

	Se3_F64 motion = new Se3_F64();

	Point3D_F64 localX = new Point3D_F64();

	public void setRotation( DenseMatrix64F R ) {
		motion.getR().set(R);
	}

	@Override
	public void setModel(Vector3D_F64 translation) {
		motion.getT().set(translation);
	}

	@Override
	public double computeDistance(PointPositionPair pt) {
		Point3D_F64 X = pt.location;
		Point2D_F64 obs = pt.observed;

		SePointOps_F64.transform(motion,X,localX);

		double dx = obs.x - localX.x/localX.z;
		double dy = obs.y - localX.y/localX.z;

		return dx*dx + dy*dy;
	}

	@Override
	public void computeDistance(List<PointPositionPair> data, double[] distance) {
		for( int i = 0; i < data.size(); i++ ) {
			distance[i] = computeDistance(data.get(i));
		}
	}
}
