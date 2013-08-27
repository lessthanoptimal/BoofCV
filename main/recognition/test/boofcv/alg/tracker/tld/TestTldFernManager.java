package boofcv.alg.tracker.tld;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTldFernManager {

	@Test
	public void constructor() {
		TldFernManager alg = new TldFernManager(10);
		assertEquals(1024,alg.table.length);
	}

	@Test
	public void lookupFern() {
		TldFernManager alg = new TldFernManager(10);

		TldFernFeature a = alg.lookupFern(345);
		assertTrue(a != null);

		assertTrue(a == alg.lookupFern(345));

		assertEquals(345,a.value);
	}

	@Test
	public void lookupPosterior() {
		TldFernManager alg = new TldFernManager(10);

		assertEquals(0,alg.lookupPosterior(234),1e-8);

		TldFernFeature a = alg.lookupFern(234);
		a.numN = 100;
		a.numP = 234;
		a.incrementP();

		double expected = a.getPosterior();
		assertEquals(expected,alg.lookupPosterior(234),1e-8);
	}

	@Test
	public void reset() {
		TldFernManager alg = new TldFernManager(10);

		alg.table[10] = new TldFernFeature();
		alg.table[800] = new TldFernFeature();

		alg.reset();

		for( int i = 0; i < alg.table.length; i++ ) {
			assertTrue(alg.table[i] == null);
		}

		assertEquals(2,alg.unusedFern.size());

	}
	@Test
	public void createFern() {
		TldFernManager alg = new TldFernManager(3);

		assertTrue(alg.createFern() != null);
		alg.unusedFern.push(new TldFernFeature());

		assertTrue(alg.createFern() != null);
		assertEquals(0,alg.unusedFern.size());
	}

}
