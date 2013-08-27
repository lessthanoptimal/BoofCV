package boofcv.alg.tracker.tld;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTldFernDescription {

	@Test
	public void sanityTest() {
		TldFernDescription fern = new TldFernDescription(new Random(24),8);

		assertEquals(fern.pairs.length,8);

		int numNotZeroA = 0;
		for( int i = 0; i < fern.pairs.length; i++ ) {
			if( fern.pairs[i].a.x  != 0 )
				numNotZeroA++;

			assertTrue(fern.pairs[i].a.x >= -0.5);
			assertTrue(fern.pairs[i].a.x < 0.5);
			assertTrue(fern.pairs[i].b.x >= -0.5);
			assertTrue(fern.pairs[i].b.x < 0.5);
		}

		assertTrue(numNotZeroA > 0);
	}

}
