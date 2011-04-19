package gecv.alg;

import gecv.struct.image.ImageInt8;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestInputSanityCheck {

	int imgWidth = 10;
	int imgHeight = 20;

	@Test
	public void checkShape_two() {
		ImageInt8 a = new ImageInt8(imgWidth, imgHeight);
		ImageInt8 b = new ImageInt8(imgWidth, imgHeight);

		// InputSanityCheck test
		InputSanityCheck.checkSameShape(a, b);

		// negative test
		try {
			b = new ImageInt8(imgWidth + 1, imgHeight);
			InputSanityCheck.checkSameShape(a, b);
			fail("Didn't throw an exception");
		} catch (IllegalArgumentException e) {
		}

		try {
			b = new ImageInt8(imgWidth, imgHeight + 1);
			InputSanityCheck.checkSameShape(a, b);
			fail("Didn't throw an exception");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void checkShape_three() {
		ImageInt8 a = new ImageInt8(imgWidth, imgHeight);
		ImageInt8 b = new ImageInt8(imgWidth, imgHeight);
		ImageInt8 c = new ImageInt8(imgWidth, imgHeight);

		// InputSanityCheck test
		InputSanityCheck.checkSameShape(a, b, c);

		// negative test
		try {
			b = new ImageInt8(imgWidth + 1, imgHeight);
			InputSanityCheck.checkSameShape(a, b, c);
			fail("Didn't throw an exception");
		} catch (IllegalArgumentException e) {
		}

		try {
			b = new ImageInt8(imgWidth, imgHeight + 1);
			InputSanityCheck.checkSameShape(a, b, c);
			fail("Didn't throw an exception");
		} catch (IllegalArgumentException e) {
		}
		b = new ImageInt8(imgWidth, imgHeight);
		try {
			c = new ImageInt8(imgWidth + 1, imgHeight);
			InputSanityCheck.checkSameShape(a, b, c);
			fail("Didn't throw an exception");
		} catch (IllegalArgumentException e) {
		}

		try {
			c = new ImageInt8(imgWidth, imgHeight + 1);
			InputSanityCheck.checkSameShape(a, b, c);
			fail("Didn't throw an exception");
		} catch (IllegalArgumentException e) {
		}
	}
}
