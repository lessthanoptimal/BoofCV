package gecv.core.image;

import gecv.struct.image.ImageInterleavedInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestUtilImageInterleavedInt8 {

	Random rand = new Random(234234);

	@Test
	public void fill() {
		ImageInterleavedInt8 image = new ImageInterleavedInt8(10, 20, 3);

		GecvTesting.checkSubImage(this, "checkFill", true, image);
	}

	public void checkFill(ImageInterleavedInt8 image) {
		UtilImageInterleavedInt8.fill(image, (byte) 6, (byte) 7, (byte) 8);

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				assertEquals(image.getBand(x, y, 0), (byte) 6);
				assertEquals(image.getBand(x, y, 1), (byte) 7);
				assertEquals(image.getBand(x, y, 2), (byte) 8);
			}
		}
	}

	@Test
	public void randomize() {
		ImageInterleavedInt8 image = new ImageInterleavedInt8(10, 20, 3);

		GecvTesting.checkSubImage(this, "checkRandomize", false, image);
	}

	public void checkRandomize(ImageInterleavedInt8 image) {
		UtilImageInterleavedInt8.randomize(image, rand);

		int totalZero = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				for (int k = 0; k < 3; k++)
					if (image.getBand(x, y, k) == 0)
						totalZero++;
			}
		}
		assertTrue(totalZero < 10);
	}
}
