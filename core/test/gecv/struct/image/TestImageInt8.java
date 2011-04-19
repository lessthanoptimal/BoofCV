package gecv.struct.image;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestImageInt8 extends StandardImageTests {


	@Override
	public ImageBase createImage(int width, int height) {
		return new ImageInt8(width, height);
	}

	@Override
	public Number randomNumber() {
		return (byte) (rand.nextInt(255) - 126);
	}

	@Test
	public void getU() {
		ImageInt8 a = new ImageInt8(2, 2);

		a.set(0, 1, 5);
		a.set(1, 1, 200);

		assertEquals(5, a.get(0, 1));
		assertEquals(5, a.getU(0, 1));
		assertEquals(200, a.getU(1, 1));
		assertTrue(200 != a.get(1, 1));
	}
}
