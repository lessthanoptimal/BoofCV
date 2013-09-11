package boofcv.alg.tracker.tld;

import org.ddogleg.struct.FastQueue;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestTldNonMaximalSuppression {

	/**
	 * Just tests the connections graph generated in process()
	 */
	@Test
	public void process() {
		TldNonMaximalSuppression alg = new TldNonMaximalSuppression(0.5);

		FastQueue<TldRegion> regions = new FastQueue<TldRegion>(TldRegion.class,true);
		regions.grow().rect.set(0,100,10,120);
		regions.grow().rect.set(2,3,8,33);
		regions.grow().rect.set(0,100,9,119);
		regions.grow().rect.set(0,100,2,102);
		regions.get(0).confidence = 100;
		regions.get(1).confidence = 200;
		regions.get(2).confidence = 300;
		regions.get(3).confidence = 400;

		FastQueue<TldRegion> output = new FastQueue<TldRegion>(TldRegion.class,true);

		alg.process(regions,output);
		assertEquals(output.size(), 3);

		FastQueue<TldNonMaximalSuppression.Connections> conn = alg.getConnections();

		assertFalse(conn.data[0].maximum);
		assertTrue(conn.data[1].maximum);
		assertTrue(conn.data[2].maximum);
		assertTrue(conn.data[3].maximum);
	}
}
