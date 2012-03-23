package boofcv.alg.sfm.robust;

import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.AssociatedPair;
import boofcv.numerics.fitting.modelset.DistanceFromModel;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.List;

/**
 * Computes the error for a given camera motion from two calibrated views.  First a point
 * is triangulated from the two views and the motion.  Then the difference between
 * the observed and projected point is found at each view. Error is normalized pixel difference
 * squared.
 *
 * If a point does not pass the positive depth constraint a very large error is returned.
 *
 * @author Peter Abeles
 */
public class DistanceSe3SymmetricSq implements DistanceFromModel<Se3_F64,AssociatedPair> {

	// transform from key frame to current frame
	private Se3_F64 keyToCurr;
	// triangulation algorithm
	private TriangulateTwoViewsCalibrated triangulate;
	// working storage
	private Point3D_F64 p = new Point3D_F64();

	public DistanceSe3SymmetricSq(TriangulateTwoViewsCalibrated triangulate) {
		this.triangulate = triangulate;
	}

	@Override
	public void setModel(Se3_F64 keyToCurr) {
		this.keyToCurr = keyToCurr;
	}

	@Override
	public double computeDistance(AssociatedPair obs) {

		// triangulate the point in 3D space
		triangulate.triangulate(obs.keyLoc,obs.currLoc,keyToCurr,p);

		if( p.z < 0 )
			return Double.MAX_VALUE;
		
		// compute observational error in each view
		double dx1 = obs.keyLoc.x - p.x/p.z;
		double dy1 = obs.keyLoc.y - p.y/p.z;

		SePointOps_F64.transform(keyToCurr,p,p);
		if( p.z < 0 )
			return Double.MAX_VALUE;

		double dx2 = obs.currLoc.x - p.x/p.z;
		double dy2 = obs.currLoc.y - p.y/p.z;
		
		// symmetric error
		return dx1*dx1 + dy1*dy1 + dx2*dx2 + dy2*dy2;
	}

	@Override
	public void computeDistance(List<AssociatedPair> associatedPairs, double[] distance) {
		for( int i = 0; i < associatedPairs.size(); i++ ) {
			AssociatedPair obs = associatedPairs.get(i);
			distance[i] = computeDistance(obs);
		}
	}
}
