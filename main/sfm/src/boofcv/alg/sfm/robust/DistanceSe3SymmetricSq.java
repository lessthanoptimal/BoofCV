package boofcv.alg.sfm.robust;

import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.numerics.fitting.modelset.DistanceFromModel;
import boofcv.struct.geo.AssociatedPair;
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
 * Error units can be in either pixels<sup>2</sup> or unit less (normalized pixel coordinates).  To compute
 * the error in pixels pass in the correct intrinsic calibration parameters in the constructor.  Otherwise
 * pass in fx=1.fy=1,skew=0 for normalized.
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
	private double key_fx; // focal length x
	private double key_fy; // focal length y
	private double key_skew; // pixel skew
	private double curr_fx; // focal length x
	private double curr_fy; // focal length y
	private double curr_skew; // pixel skew

	/**
	 * Configure distance calculation.   See comment above about how to specify error units using
	 * intrinsic parameters.
	 *
	 * @param triangulate Triangulates the intersection of two observations
	 * @param key_fx intrinsic parameter: focal length x for key camera
	 * @param key_fy intrinsic parameter: focal length y for key camera
	 * @param key_skew intrinsic parameter: skew for key camera (usually zero)
	 * @param curr_fx intrinsic parameter: focal length x for curr camera
	 * @param curr_fy intrinsic parameter: focal length y for curr camera
	 * @param curr_skew intrinsic parameter: skew for curr camera (usually zero)
	 */
	public DistanceSe3SymmetricSq(TriangulateTwoViewsCalibrated triangulate,
								  double key_fx, double key_fy , double key_skew ,
								  double curr_fx, double curr_fy , double curr_skew) {
		this.triangulate = triangulate;
		this.key_fx = key_fx;
		this.key_fy = key_fy;
		this.key_skew = key_skew;
		this.curr_fx = curr_fx;
		this.curr_fy = curr_fy;
		this.curr_skew = curr_skew;
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
		double dy1 = (obs.keyLoc.y - p.y/p.z);
		double dx1 = (obs.keyLoc.x - p.x/p.z)*key_fx + dy1*key_skew;
		dy1 *= key_fy;

		SePointOps_F64.transform(keyToCurr,p,p);
		if( p.z < 0 )
			return Double.MAX_VALUE;

		double dy2 = (obs.currLoc.y - p.y/p.z);
		double dx2 = (obs.currLoc.x - p.x/p.z)*curr_fx + dy2*curr_skew;
		dy2 *= curr_fy;

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
