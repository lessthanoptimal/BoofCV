package boofcv.abst.sfm;

import boofcv.struct.image.ImageBase;
import georegression.struct.se.Se3_F64;

/**
 * <P>
 * Interface for visual odometry from a single camera that provides 6-DOF pose.  The camera is assumed 
 * to be calibrated so that the "true" Euclidean motion can be extracted, up to a scale factor. 
 * Motion can only be found up to a scale factor because it is impossible to distinguish between a 
 * large motion and distant objects against a small motion and close objects when using a single camera.
 * </p>
 * 
 * <p>
 * Each time a new image arrives the function {@link #process} should be invoked and its return value
 * checked.  If false is returned then {@link #isFatal()} needs to be called to see if a fatal error
 * occurred.  If a fatal error occurred then the motion estimate has been reset relative to the first
 * frame in which {@Link isFatal} returns false.
 * </p>
 * 
 * @author Peter Abeles
 */
public interface MonocularVisualOdometry<T extends ImageBase> {

	/**
	 * Process the new image and update the motion estimate.  The return value must be checked
	 * to see if the estimate was actually updated.  If false is returned then {@link #isFatal}
	 * also needs to be checked to see if the pose estimate has been reset.
	 *
	 * @param input Next image in the sequence.
	 * @return If the motion estimate has been updated or not
	 */
	public boolean process( T input );

	/**
	 * Forget past history and tracking results, returning it to its initial state.
	 */
	public void reset();

	/**
	 * If {@link #process} returns false then this function needs to be called to see if a fatal
	 * error has occurred and the the motion estimate has lost track of where it is.
	 *
	 * @return true if a fatal error has occurred.
	 */
	public boolean isFatal();

	/**
	 * Returns the estimated motion relative to the first frame in which a fatal error
	 * does not happen.
	 *
	 * @return Found pose.
	 */
	public Se3_F64 getCameraToWorld();
}
