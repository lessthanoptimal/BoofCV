package boofcv.alg.tracker.tld;

import boofcv.struct.ImageRectangle;

/**
 * A high confidence region detected inside the image where
 *
 * @author Peter Abeles
 */
public class TldRegion {

	/**
	 * Number of connections found in non-maximum suppression
	 */
	public int connections;

	/**
	 * Score computed from NCC descriptor
	 */
	public double confidence;

	/**
	 * Description of the rectangular region
	 */
	public ImageRectangle rect = new ImageRectangle();
}
