package gecv.alg.detect.corner.impl;

import gecv.alg.detect.corner.impl.KltCorner_I16;
import gecv.alg.detect.corner.impl.SsdCornerNaive_I16;
import gecv.alg.filter.derivative.GradientSobel;
import gecv.core.image.UtilImageInt8;
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
public class TestKltCorner_I16 {
	int width = 15;
	int height = 15;

	/**
	 * Creates a random image and looks for corners in it.  Sees if the naive
	 * and fast algorithm produce exactly the same results.
	 */
	@Test
	public void compareToNaive() {
		ImageInt8 img = new ImageInt8(width, height);
		UtilImageInt8.randomize(img, new Random(0xfeed));

		ImageInt16 derivX = new ImageInt16(img.getWidth(), img.getHeight());
		ImageInt16 derivY = new ImageInt16(img.getWidth(), img.getHeight());

		GradientSobel.process_I8(img, derivX, derivY);

		GecvTesting.checkSubImage(this, "compareToNaive", true, derivX, derivY);
	}

	public void compareToNaive(ImageInt16 derivX, ImageInt16 derivY) {
		SsdCornerNaive_I16 naive = new SsdCornerNaive_I16(width, height, 3);
		naive.process(derivX, derivY);

		KltCorner_I16 fast = new KltCorner_I16(width, height, 3);
		fast.process(derivX, derivY);

		GecvTesting.assertEquals(naive.getIntensity(), fast.getIntensity());
	}
}
