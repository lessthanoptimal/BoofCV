package boofcv.alg.tracker.tld;

import georegression.struct.point.Point2D_F32;

import java.util.Random;

/**
 * Fern descriptor used in {@link TldTracker}.  The number of features can be at most 32, enough to fit inside
 * an integer. The location of each point is from 0 to 1 and randomly selected.  When computed it is scaled
 * independently along x and y axis to the region's width and height, respectively.
 *
 * @author Peter Abeles
 */
public class TldFernDescription {

	/**
	 * Pairs used to compute fern.  Must be <= 32 to fit inside an integer
	 */
	SamplePair pairs[];

	/**
	 * Creates random fern.
	 *
	 * @param rand Random number generator used to select sample locations
	 * @param num Number of features/pairs
	 */
	public TldFernDescription(Random rand, int num) {
		if( num < 1 || num > 32 )
			throw new IllegalArgumentException("Number of pairs must be from 1 to 32, inclusive");

		pairs = new SamplePair[num];
		for( int i = 0; i < num; i++ ) {
			SamplePair p = new SamplePair();

			p.a.set( rand.nextFloat() , rand.nextFloat() );
			p.b.set( rand.nextFloat() , rand.nextFloat() );

			pairs[i] = p;
		}
	}

	public static class SamplePair
	{
		Point2D_F32 a = new Point2D_F32();
		Point2D_F32 b = new Point2D_F32();
	}
}
