package boofcv.alg.sfm.robust;

import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.AssociatedPair;
import boofcv.numerics.fitting.modelset.DistanceFromModel;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.List;

/**
 * <p>
 * Computes the error for a given camera motion from two calibrated views.  First a point
 * is triangulated from the two views and the motion.  Then the difference between
 * the observed and projected point is found at each view. Error is normalized pixel difference
 * squared.
 * </p>
 * <p>
 * error = &Delta;x<sub>1</sub><sup>2</sup> + &Delta;y<sub>1</sub><sup>2</sup> +
 * &Delta;x<sub>2</sub><sup>2</sup> + &Delta;y<sub>2</sub><sup>2</sup>
 * </p>
 *
 * <p>
 * NOTE: If a point does not pass the positive depth constraint then a very large error is returned.
 * </p>
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

	// ------- intrinsic camera parameters from calibration matrix
	private double fx; // focal length x
	private double fy; // focal length y
	private double skew; // pixel skew

	/**
	 * Configure distance calculation.
	 *
	 * @param triangulate Triangulates the intersection of two observations
	 * @param fx intrinsic parameter: focal length x
	 * @param fy intrinsic parameter: focal length y
	 * @param skew intrinsic parameter: skew  (usually zero)
	 */
	public DistanceSe3SymmetricSq(TriangulateTwoViewsCalibrated triangulate,
								  double fx, double fy , double skew ) {
		this.triangulate = triangulate;
		this.fx = fx;
		this.fy = fy;
		this.skew = skew;
	}

	@Override
	public void setModel(Se3_F64 keyToCurr) {
		this.keyToCurr = keyToCurr;
	}

	/**
	 * Computes the error given the motion model
	 *
	 * @param obs Observation in normalized pixel coordinates
	 * @return observation error
	 */
	@Override
	public double computeDistance(AssociatedPair obs) {

		// triangulate the point in 3D space
		triangulate.triangulate(obs.keyLoc,obs.currLoc,keyToCurr,p);

		if( p.z < 0 )
			return Double.MAX_VALUE;
		
		// compute observational error in each view
		double dy1 = (obs.keyLoc.y - p.y/p.z)*fy;
		double dx1 = (obs.keyLoc.x - p.x/p.z)*fx + dy1*skew;

		SePointOps_F64.transform(keyToCurr,p,p);
		if( p.z < 0 )
			return Double.MAX_VALUE;

		double dy2 = (obs.currLoc.y - p.y/p.z)*fy;
		double dx2 = (obs.currLoc.x - p.x/p.z)*fx + dy2*skew;

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
