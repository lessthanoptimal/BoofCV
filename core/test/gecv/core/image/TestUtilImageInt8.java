package gecv.core.image;

import gecv.struct.image.ImageInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestUtilImageInt8 {

	Random rand = new Random(234234);

	@Test
	public void fill() {
		ImageInt8 image = new ImageInt8(10, 20);

		GecvTesting.checkSubImage(this, "checkFill", true, image);
	}

	public void checkFill(ImageInt8 image) {
		UtilImageInt8.fill(image, (byte) 6);

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				assertEquals(image.get(x, y), (byte) 6);
			}
		}
	}

	@Test
	public void randomize() {
		ImageInt8 image = new ImageInt8(10, 20);

		GecvTesting.checkSubImage(this, "checkRandomize", false, image);
	}

	public void checkRandomize(ImageInt8 image) {
		UtilImageInt8.randomize(image, rand);

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				assertTrue(image.get(x, y) != 0);
			}
		}
	}
}
