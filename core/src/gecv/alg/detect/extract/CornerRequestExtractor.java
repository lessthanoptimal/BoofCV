package gecv.alg.detect.extract;


/**
 * A corner extractor that lets the user specify how many features it needs.
 *
 * @author Peter Abeles
 */
public interface CornerRequestExtractor extends CornerExtractor {
	/**
	 * This used to request a certain number of features be returned.  this is just
	 * a suggestion and more or less features can be returned.  It might even be
	 * ignored by some algorithms.  If it can the implementing algorithm should return
	 * at least this many features.
	 *
	 * @param numFeatures Number of features it should try to return.
	 */
	public void requestNumFeatures(int numFeatures);
}
