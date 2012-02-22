package boofcv.alg.sfm;

import boofcv.struct.image.ImageSingleBand;
import georegression.struct.se.Se3_F64;

/**
 *
 *
 * <p>
 * [1] David Nister, Oleg Naroditsky, and James Bergen, "Visual Odometry" CVPR 2004
 * </p>
 *
 * @author Peter Abeles
 */
public class MonocularNister2004<T extends ImageSingleBand> {


	public void reset() {

	}

	public boolean process( T input ) {
		return false;
	}

	/**
	 * Cumulative motion estimate some the start.
	 *
	 * @return Camera motion.
	 */
	public Se3_F64 getMotion() {
		return null;
	}

	/**
	 * Estimated motion between the two most recent consecutive calls to {@link #process}.
	 *
	 * @return Camera motion.
	 */
	public Se3_F64 getMotionChange() {
		return null;
	}
}
