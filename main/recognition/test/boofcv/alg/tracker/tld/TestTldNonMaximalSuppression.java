package boofcv.alg.tracker.tld;

import boofcv.struct.FastQueue;
import boofcv.struct.GrowQueue_I32;
import boofcv.struct.ImageRectangle;
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
//		assertEquals(conn.data[0].indexes.size,2);
//		assertEquals(conn.data[1].indexes.size,1);
//		assertEquals(conn.data[2].indexes.size,2);
//		assertEquals(conn.data[3].indexes.size,1);

		assertFalse(conn.data[0].maximum);
		assertTrue(conn.data[1].maximum);
		assertTrue(conn.data[2].maximum);
		assertTrue(conn.data[3].maximum);
	}

	@Test
	public void computeOverlap() {
		fail("move");
//		TldNonMaximalSuppression alg = new TldNonMaximalSuppression(0.5);
//
//		ImageRectangle a = new ImageRectangle(0,100,10,120);
//		ImageRectangle b = new ImageRectangle(2,3,8,33);
//
//		// no overlap
//		assertEquals(0,alg.computeOverlap(a,b),1e-8);
//
//		// non-zero overlap
//
//		ImageRectangle c = new ImageRectangle(0,100,2,102);
//		double expected = (4.0)/(200.0);
//		assertEquals(expected,alg.computeOverlap(a,c),1e-8);
	}

	@Test
	public void computeAverage() {
		FastQueue<TldRegion> regions = new FastQueue<TldRegion>(TldRegion.class,true);
		TldRegion a = regions.grow();
		a.confidence = 0.75;
		a.rect.set(10,12,30,45);
		TldRegion b = regions.grow();
		b.confidence = 0.9;
		b.rect.set(29,30,33,1023);
		TldRegion c = regions.grow();
		c.confidence = 0.9;
		c.rect.set(10,12,30,45);

		// ignore the one region which is different
		GrowQueue_I32 conn = new GrowQueue_I32();
		conn.add(0);
		conn.add(2);

		// identical regions should average to be the same
		ImageRectangle output = new ImageRectangle();
		TldNonMaximalSuppression.computeAverage(regions,conn,output);

		assertEquals(output.x0,a.rect.x0);
		assertEquals(output.y0,a.rect.y0);
		assertEquals(output.x1,a.rect.x1);
		assertEquals(output.y1,a.rect.y1);
	}

}
