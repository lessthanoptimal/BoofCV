package boofcv.alg.tracker.tld;

import boofcv.struct.ImageRectangle;
import boofcv.struct.sfm.ScaleTranslate2D;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestTldAdjustRegion {
	@Test
	public void process() {
		fail("implement");
	}

	@Test
	public void adjustRectangle() {
		TldAdjustRegion alg = new TldAdjustRegion(50);

		ImageRectangle rect = new ImageRectangle(10,20,30,40);
		ScaleTranslate2D motion = new ScaleTranslate2D(1.5,2,3);

//		alg.adjustRectangle(rect,motion);

		assertEquals(17, rect.x0);
		assertEquals(33, rect.y0);
		assertEquals(47, rect.x1);
		assertEquals(63, rect.y1);

		fail("update test");
	}
}
