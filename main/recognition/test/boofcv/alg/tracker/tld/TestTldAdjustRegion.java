package boofcv.alg.tracker.tld;

import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.sfm.ScaleTranslate2D;
import georegression.struct.shapes.RectangleCorner2D_F64;
import org.ddogleg.struct.FastQueue;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTldAdjustRegion {

	Random rand = new Random(234);

	@Test
	public void process() {
		ScaleTranslate2D motion = new ScaleTranslate2D(1.5,2,3);

		FastQueue<AssociatedPair> pairs = new FastQueue<AssociatedPair>(AssociatedPair.class,true);

		for( int i = 0; i < 200; i++ ) {
			AssociatedPair p = pairs.grow();
			p.p1.x = rand.nextGaussian()*2;
			p.p1.y = rand.nextGaussian()*2;

			p.p2.x = p.p1.x*motion.scale + motion.transX;
			p.p2.y = p.p1.y*motion.scale + motion.transY;
		}

		RectangleCorner2D_F64 rect = new RectangleCorner2D_F64(10,20,30,40);

		TldAdjustRegion alg = new TldAdjustRegion(30);
		alg.init(300,400);

		assertTrue(alg.process(pairs, rect));

		assertEquals(17, rect.x0 , 1e-8);
		assertEquals(33, rect.y0, 1e-8);
		assertEquals(47, rect.x1, 1e-8);
		assertEquals(63, rect.y1, 1e-8);
	}

	@Test
	public void adjustRectangle() {
		TldAdjustRegion alg = new TldAdjustRegion(50);

		RectangleCorner2D_F64 rect = new RectangleCorner2D_F64(10,20,30,40);
		ScaleTranslate2D motion = new ScaleTranslate2D(1.5,2,3);

		alg.adjustRectangle(rect,motion);

		assertEquals(17, rect.x0 , 1e-8);
		assertEquals(33, rect.y0, 1e-8);
		assertEquals(47, rect.x1, 1e-8);
		assertEquals(63, rect.y1, 1e-8);
	}
}
