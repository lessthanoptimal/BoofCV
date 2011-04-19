package gecv.alg.filter.derivative.sobel;

import gecv.core.image.UtilImageFloat32;
import gecv.core.image.UtilImageInt8;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestGradientEdge_Outer {

	Random rand = new Random(234);

	int width = 200;
	int height = 250;

	/**
	 * See if the same results are returned by ImageByte2D equivalent
	 */
	@Test
	public void process_I8_naive() {
		ImageInt8 img = new ImageInt8(width, height);
		UtilImageInt8.randomize(img, new Random(0xfeed));

		ImageInt16 derivX = new ImageInt16(width, height);
		ImageInt16 derivY = new ImageInt16(width, height);

		ImageInt16 derivX2 = new ImageInt16(width, height);
		ImageInt16 derivY2 = new ImageInt16(width, height);

		GradientSobel_Naive.process_I8(img, derivX2, derivY2);
		GradientSobel_Outer.process_I8(img, derivX, derivY);

		GecvTesting.assertEquals(derivX2, derivX, 0);
		GecvTesting.assertEquals(derivY2, derivY, 0);
	}

	@Test
	public void process_I8_sub_naive() {
		ImageInt8 img = new ImageInt8(width, height);
		UtilImageInt8.randomize(img, new Random(0xfeed));

		ImageInt16 derivX = new ImageInt16(width, height);
		ImageInt16 derivY = new ImageInt16(width, height);

		GecvTesting.checkSubImage(this, "process_I8_sub_naive", true, img, derivX, derivY);
	}

	public void process_I8_sub_naive(ImageInt8 img, ImageInt16 derivX, ImageInt16 derivY) {
		ImageInt16 derivX2 = new ImageInt16(width, height);
		ImageInt16 derivY2 = new ImageInt16(width, height);

		GradientSobel_Naive.process_I8(img, derivX2, derivY2);
		GradientSobel_Outer.process_I8_sub(img, derivX, derivY);

		GecvTesting.assertEquals(derivX2, derivX, 0);
		GecvTesting.assertEquals(derivY2, derivY, 0);
	}

	/**
	 * See if the same results are returned by ImageByte2D equivalent
	 */
	@Test
	public void process_F32_naive() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0f, 255f);

		ImageFloat32 derivX = new ImageFloat32(width, height);
		ImageFloat32 derivY = new ImageFloat32(width, height);

		ImageFloat32 derivX2 = new ImageFloat32(width, height);
		ImageFloat32 derivY2 = new ImageFloat32(width, height);

		GradientSobel_Naive.process_F32(img, derivX2, derivY2);
		GradientSobel_Outer.process_F32(img, derivX, derivY);

		GecvTesting.assertEquals(derivX2, derivX, 0, 1e-4f);
		GecvTesting.assertEquals(derivY2, derivY, 0, 1e-4f);
	}
}
