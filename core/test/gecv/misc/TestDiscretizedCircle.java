package gecv.misc;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestDiscretizedCircle {
	/**
	 * Make sure the circles have the expected properties.
	 * <p/>
	 * Right now it just checks the lengths.  I can do a better job..
	 */
	@Test
	public void testCircles() {

		int pts[];

		pts = DiscretizedCircle.imageOffsets(1, 100);
		assertEquals(4, pts.length);
		pts = DiscretizedCircle.imageOffsets(2, 100);
		assertEquals(12, pts.length);
		pts = DiscretizedCircle.imageOffsets(3, 100);
		assertEquals(16, pts.length);
	}
}
