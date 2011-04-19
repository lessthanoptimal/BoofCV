package gecv.alg.filter.convolve;

import gecv.core.image.UtilImageFloat32;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.image.ImageFloat32;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestConvolve1DBorders {

	Random rand = new Random(0xFF);

	int width = 3;
	int height = 4;
	int kernelRadius = 1;

	/**
	 * Tests the convolution in a simple case with and without a border
	 */
	@Test
	public void horizontal() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0, 1);
		ImageFloat32 dest = new ImageFloat32(width, height);

		GecvTesting.checkSubImage(this, "horizontal", true, img, dest);
	}

	public void horizontal(ImageFloat32 img, ImageFloat32 dest) {
		Kernel1D_F32 ker = KernelFactory.gaussian1D_F32(kernelRadius, true);

		Convolve1DBorders.horizontal(ker, img, dest, false);

		// the top border should not be convolved yet
		assertEquals(0, dest.get(1, 0), 1e-6);

		// check a point where the full kernel can be convolved
		float val = img.get(0, 1) * ker.get(0) + img.get(1, 1) * ker.get(1) + img.get(2, 1) * ker.get(2);

		assertEquals(val, dest.get(1, 1), 1e-6);

		// check a point on the left where only a partial kernel is convolved
		val = img.get(0, 1) * ker.get(1) + img.get(1, 1) * ker.get(2);
		val /= (ker.get(1) + ker.get(2));

		assertEquals(val, dest.get(0, 1), 1e-6);

		// check the right size
		val = img.get(1, 1) * ker.get(0) + img.get(2, 1) * ker.get(1);
		val /= (ker.get(0) + ker.get(1));

		assertEquals(val, dest.get(2, 1), 1e-6);

		// now let it process the vertical border
		Convolve1DBorders.horizontal(ker, img, dest, true);
		val = img.get(0, 1) * ker.get(0) + img.get(1, 1) * ker.get(1) + img.get(2, 1) * ker.get(2);
		assertEquals(val, dest.get(1, 1), 1e-6);
		assertTrue(0 != dest.get(1, 0));
	}

	/**
	 * Tests the convolution in a simple case with and without a border
	 */
	@Test
	public void vertical() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0, 1);
		ImageFloat32 dest = new ImageFloat32(width, height);

		GecvTesting.checkSubImage(this, "vertical", true, img, dest);
	}

	public void vertical(ImageFloat32 img, ImageFloat32 dest) {
		Kernel1D_F32 ker = KernelFactory.gaussian1D_F32(kernelRadius, true);

		Convolve1DBorders.vertical(ker, img, dest, false);

		// the left border should not be convolved yet
		assertEquals(0, dest.get(0, 1), 1e-6);

		float val = img.get(1, 0) * ker.get(0) + img.get(1, 1) * ker.get(1) + img.get(1, 2) * ker.get(2);
		assertEquals(val, dest.get(1, 1), 1e-6);

		// check a point on the top where only a partial kernel is convolved
		val = img.get(1, 0) * ker.get(1) + img.get(1, 1) * ker.get(2);
		val /= (ker.get(1) + ker.get(2));

		assertEquals(val, dest.get(1, 0), 1e-6);

		// check the bottom size
		val = img.get(1, 2) * ker.get(0) + img.get(1, 3) * ker.get(1);
		val /= (ker.get(0) + ker.get(1));

		assertEquals(val, dest.get(1, 3), 1e-6);

		// now let it process the horizontal border
		Convolve1DBorders.vertical(ker, img, dest, true);
		val = img.get(1, 0) * ker.get(0) + img.get(1, 1) * ker.get(1) + img.get(1, 2) * ker.get(2);
		assertEquals(val, dest.get(1, 1), 1e-6);
		assertTrue(0 != dest.get(0, 1));
	}
}
