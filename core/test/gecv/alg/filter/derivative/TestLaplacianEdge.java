package gecv.alg.filter.derivative;

import gecv.core.image.UtilImageFloat32;
import gecv.core.image.UtilImageInt8;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestLaplacianEdge {

	Random rand = new Random(0xfeed);

	private final int width = 4;
	private final int height = 5;

	@Test
	public void checkInputShape() {
		GecvTesting.checkImageDimensionValidation(new LaplacianEdge(), 2);
	}

	@Test
	public void process_I8() {
		ImageInt8 img = new ImageInt8(width, height);
		UtilImageInt8.randomize(img, rand);

		ImageInt16 deriv = new ImageInt16(width, height);
		GecvTesting.checkSubImage(this, "process_I8", true, img, deriv);
	}

	public void process_I8(ImageInt8 img, ImageInt16 deriv) {
		LaplacianEdge.process_I8(img, deriv);

		int expected = -4 * img.getU(1, 1) + img.getU(0, 1) + img.getU(1, 0)
				+ img.getU(2, 1) + img.getU(1, 2);

		assertEquals(expected, deriv.get(1, 1));
	}

	@Test
	public void process_F32() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0, 1);

		ImageFloat32 deriv = new ImageFloat32(width, height);
		GecvTesting.checkSubImage(this, "process_F32", true, img, deriv);
	}

	public void process_F32(ImageFloat32 img, ImageFloat32 deriv) {
		LaplacianEdge.process_F32(img, deriv);

		float expected = -img.get(1, 1) + (img.get(0, 1) + img.get(1, 0)
				+ img.get(2, 1) + img.get(1, 2)) * 0.25f;

		assertEquals(expected, deriv.get(1, 1), 1e-5);
	}
}
