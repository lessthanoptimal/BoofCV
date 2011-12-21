package boofcv.alg.geo.d2.stabilization;

import boofcv.alg.geo.AssociatedPair;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestMotionMosaicPointKey {

	@Test
	public void imageCoverageFraction() {
		List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
		pairs.add( new AssociatedPair(0,10,15,20,25));
		pairs.add( new AssociatedPair(0,10,15,100,30));
		pairs.add( new AssociatedPair(0,10,15,29,120));
		// give it a useless pair which will not contribute to the solution
		pairs.add( new AssociatedPair(0,10,15,50,56));

		double area = 80*95;

		int width = 200;
		int height = 250;

		double expected = area/(width*height);
		double found = MotionMosaicPointKey.imageCoverageFraction(width,height,pairs);

		assertEquals(expected,found,1e-8);
	}
}
