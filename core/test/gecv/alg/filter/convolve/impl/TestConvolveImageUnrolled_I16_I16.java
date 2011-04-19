package gecv.alg.filter.convolve.impl;

import gecv.alg.filter.convolve.TestConvolveImage;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageInt16;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestConvolveImageUnrolled_I16_I16 {
	@Test
	public void horizontal() throws NoSuchMethodException {
		Random rand = new Random(234);

		for (int i = 0; i < GenerateConvolvedUnrolled.numUnrolled; i++) {
			Method m = ConvolveImageUnrolled_I16_I16.class.getMethod("horizontal",
					Kernel1D_I32.class, ImageInt16.class, ImageInt16.class, boolean.class);

			TestConvolveImage.checkAgainstStandard(m, "horizontal", 5, 7, i + 1, rand);
		}
	}

	@Test
	public void vertical() throws NoSuchMethodException {
		Random rand = new Random(234);

		for (int i = 0; i < GenerateConvolvedUnrolled.numUnrolled; i++) {
			Method m = ConvolveImageUnrolled_I16_I16.class.getMethod("vertical",
					Kernel1D_I32.class, ImageInt16.class, ImageInt16.class, boolean.class);

			TestConvolveImage.checkAgainstStandard(m, "vertical", 5, 7, i + 1, rand);
		}
	}

	@Test
	public void horizontal_divide() {
		fail("implement");
	}

	@Test
	public void vertical_divide() {
		fail("implement");
	}
}
