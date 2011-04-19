package gecv.alg.filter.derivative.sobel;

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
public class TestGradientEdge_Naive {

	Random rand = new Random(0xfeed);

	private final int width = 4;
	private final int height = 5;

	/**
	 * Compare the results to a hand computed value
	 */
	@Test
	public void compareToKnown_I8() {
		ImageInt8 img = new ImageInt8(width, height);
		UtilImageInt8.randomize(img, rand);

		ImageInt16 derivX = new ImageInt16(width, height);
		ImageInt16 derivY = new ImageInt16(width, height);

		GecvTesting.checkSubImage(this, "compareToKnown_I8", true, img, derivX, derivY);
	}

	public void compareToKnown_I8(ImageInt8 img, ImageInt16 derivX, ImageInt16 derivY) {
		GradientSobel_Naive.process_I8(img, derivX, derivY);

		int dX = -((img.getU(0, 2) + img.getU(0, 0)) + img.getU(0, 1) * 2);
		dX += (img.getU(2, 2) + img.getU(2, 0)) + img.getU(2, 1) * 2;

		int dY = -((img.getU(2, 0) + img.getU(0, 0)) + img.getU(1, 0) * 2);
		dY += (img.getU(2, 2) + img.getU(0, 2)) + img.getU(1, 2) * 2;

		assertEquals(dX, derivX.get(1, 1), 1e-6);
		assertEquals(dY, derivY.get(1, 1), 1e-6);
	}

	/**
	 * Compare the results to a hand computed value
	 */
	@Test
	public void compareToKnown_F32() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0, 255);

		ImageFloat32 derivX = new ImageFloat32(width, height);
		ImageFloat32 derivY = new ImageFloat32(width, height);

		GecvTesting.checkSubImage(this, "compareToKnown_F32", true, img, derivX, derivY);

	}

	public void compareToKnown_F32(ImageFloat32 img, ImageFloat32 derivX, ImageFloat32 derivY) {
		GradientSobel_Naive.process_F32(img, derivX, derivY);

		float dX = -((img.get(0, 2) + img.get(0, 0)) * 0.25f + img.get(0, 1) * 0.5f);
		dX += (img.get(2, 2) + img.get(2, 0)) * 0.25f + img.get(2, 1) * 0.5f;

		float dY = -((img.get(2, 0) + img.get(0, 0)) * 0.25f + img.get(1, 0) * 0.5f);
		dY += (img.get(2, 2) + img.get(0, 2)) * 0.25f + img.get(1, 2) * 0.5f;

		assertEquals(dX, derivX.get(1, 1), 1e-6);
		assertEquals(dY, derivY.get(1, 1), 1e-6);
	}
}
