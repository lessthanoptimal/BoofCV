package boofcv.alg.sfm.robust;

import boofcv.alg.geo.pose.PositionFromPairLinear2;
import boofcv.numerics.fitting.modelset.ModelGenerator;
import boofcv.struct.geo.PointPosePair;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the translation component given the rotation and at least two point observations
 * 
 * @author Peter Abeles
 */
public class TranGivenRotGenerator implements ModelGenerator<Vector3D_F64,PointPosePair>
{
	PositionFromPairLinear2 alg = new PositionFromPairLinear2();
	
	// rotation matrix
	DenseMatrix64F R;

	// storage
	List<Point3D_F64> worldPts = new ArrayList<Point3D_F64>();
	List<Point2D_F64 > observed = new ArrayList<Point2D_F64>();

	public void setRotation(DenseMatrix64F r) {
		R = r;
	}

	@Override
	public Vector3D_F64 createModelInstance() {
		return new Vector3D_F64();
	}

	@Override
	public boolean generate(List<PointPosePair> dataSet, Vector3D_F64 model ) {
		worldPts.clear();
		observed.clear();
		
		for( int i = 0; i < dataSet.size(); i++ ) {
			PointPosePair p = dataSet.get(i);
			worldPts.add( p.location );
			observed.add( p.observed );
		}
		
		if( alg.process(R,worldPts,observed) ) {
			model.set(alg.getT());
		}

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return 2;
	}

}
