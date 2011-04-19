package gecv.alg.detect.extract;

import gecv.struct.QueueCorner;
import gecv.struct.image.ImageFloat32;


/**
 * An interface used to get new corners.  This abstraction is done to make it easier
 * to attach more complex data structures to corners, but have the corner detection
 * algorithm unaware.
 *
 * @author Peter Abeles
 */
public interface CornerExtractor {

	/**
	 * Process a feature intensity image to extract the point features
	 *
	 * @param intenImg	Feature intensity image.
	 * @param features	List of feature indexes that were found.  Set to null if not provided.
	 * @param numFeatures Number of features that were set in the feature list.
	 * @param corners	 Where the corners that it found are written to.
	 */
	public void process(ImageFloat32 intenImg, int features[], int numFeatures,
						QueueCorner corners);
}
