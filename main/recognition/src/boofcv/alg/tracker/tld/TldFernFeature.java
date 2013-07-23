package boofcv.alg.tracker.tld;

/**
 * Contains information on a fern feature value pair.
 *
 * @author Peter Abeles
 */
public class TldFernFeature {

	/**
	 * Numerical value of this descriptor
	 */
	public int value;
	/**
	 * Number of times P-constraint has been applied
	 */
	public int numP;
	/**
	 * Number of times N-constraint has been applied
	 */
	public int numN;

	/**
	 * Posterior probability.  P/(P+N)
	 */
	public double posterior;

	public void init( int value ) {
		this.value = value;
		numP = numN = 0;
	}

	public void incrementP() {
		numP++;
		computePosterior();
	}

	public void incrementN() {
		numN++;
		computePosterior();
	}

	private void computePosterior() {
		posterior = numP/(double)(numN + numP);
	}

	public double getPosterior() {
		return posterior;
	}

	public int getValue() {
		return value;
	}
}
