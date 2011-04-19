package gecv.struct.image;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestImageInt16 extends StandardImageTests {


	@Override
	public ImageBase createImage(int width, int height) {
		return new ImageInt16(width, height);
	}

	@Override
	public Number randomNumber() {
		return (short) (rand.nextInt(Short.MAX_VALUE - Short.MIN_VALUE) - Short.MIN_VALUE);
	}

	@Test
	public void getU() {
		ImageInt16 a = new ImageInt16(2, 2);

		a.set(0, 1, 5);
		a.set(1, 1, Short.MAX_VALUE + 1);

		assertEquals(5, a.get(0, 1));
		assertEquals(5, a.getU(0, 1));
		assertEquals(Short.MAX_VALUE + 1, a.getU(1, 1));
		assertTrue(Short.MAX_VALUE + 1 != a.get(1, 1));
	}
}
