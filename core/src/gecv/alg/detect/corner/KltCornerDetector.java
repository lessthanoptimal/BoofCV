package gecv.alg.detect.corner;

import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;

/**
 * <p>
 * This corner detector is designed to select the best features for tracking inside of a Kanade-Lucas-Tomasi (KLT)
 * feature tracker [1].  It selects features which have a low self similarity in all directions.  The output
 * is an intensity image which indicates how corner like each pixel is.  Point features are extracted from the
 * feature intensity image using {@link gecv.alg.detect.extract.CornerExtractor}.
 * </p>
 * <p/>
 * <p>
 * An important consideration when using this detector in other applications than a KLT tracker is that the
 * selected corners will not lie on the actual corner.  They lie inside of the corner itself.  This can bias
 * measurements when used to extract the location of a physical object in an image.
 * </p>
 * <p/>
 * <p>
 * Detected features are square in shape.  The feature's radius is equal to the square's width divided by two and rounded
 * down.
 * </p>
 * <p>
 * [1] Jianbo Shi and Carlo Tomasi. Good Features to Track. IEEE Conference on Computer Vision and Pattern Recognition,
 * pages 593-600, 1994
 * </p>
 *
 * @author Peter Abeles
 */
public interface KltCornerDetector<T extends ImageBase> extends GradientCornerDetector<T> {

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
