package boofcv.abst.sfm;

import georegression.struct.se.Se3_F64;

/**
 * @author Peter Abeles
 */
public interface VisualOdometry {

	/**
	 * Forget past history and tracking results, returning it to its initial state.
	 */
	public void reset();

	/**
	 * If a fatal error occurred while updating its state then this function will return true.
	 * Before more images can be processed {@link #reset()} must be called.
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
	public Se3_F64 getLeftToWorld();
}
