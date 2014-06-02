package boofcv.factory.flow;

import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.alg.flow.HornSchunck}
 *
 * @author Peter Abeles
 */
public class ConfigHornSchunck implements Configuration {

	/**
	 * Larger values place more importance on flow smoothness consistency over brightness consistency.  Try 20
	 */
	public float alpha = 20;

	/**
	 * Number of iterations.  Try 1000
	 */
	public int numIterations = 1000;

	@Override
	public void checkValidity() {

	}
}
