package gecv.core.image;

import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestUtilImageFloat32 {

	Random rand = new Random(234234);

	@Test
	public void fill() {
		ImageFloat32 image = new ImageFloat32(10, 20);

		GecvTesting.checkSubImage(this, "checkFill", true, image);
	}

	public void checkFill(ImageFloat32 image) {
		UtilImageFloat32.fill(image, 1.1f);

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				assertEquals(image.get(x, y), 1.1f);
			}
		}
	}

	@Test
	public void randomize() {
		ImageFloat32 image = new ImageFloat32(10, 20);

		GecvTesting.checkSubImage(this, "checkRandomize", false, image);
	}

	public void checkRandomize(ImageFloat32 image) {
		UtilImageFloat32.randomize(image, rand, -20f, 20f);

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				assertTrue(image.get(x, y) != 0.0);
			}
		}
	}
}
