package gecv.alg.detect.corner;

import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;

/**
 * @author Peter Abeles
 */
public interface GradientCornerDetector<T extends ImageBase> {

	/**
	 * Returns the radius of the feature being computed.  Features are square in shape with a width = 2*radius+1.
	 *
	 * @return Radius of detected features.
	 */
	public int getRadius();

	/**
	 * <p>
	 * This is the output of the feature detector.  It is an intensity image where higher values indicate
	 * if a feature is more desirable as a feature for tracking.
	 * </p>
	 * <p/>
	 * <p>
	 * NOTE: All image types output a floating point image to allow the same code to be used for selecting
	 * the features.
	 * </p>
	 */
	public ImageFloat32 getIntensity();

	/**
	 * Computes feature intensity image.
	 *
	 * @param derivX Image derivative along the x-axis.
	 * @param derivY Image derivative along the y-axis.
	 */
	public void process(T derivX, T derivY);
}
