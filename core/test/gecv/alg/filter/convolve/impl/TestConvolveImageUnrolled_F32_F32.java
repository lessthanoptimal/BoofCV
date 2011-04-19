package gecv.alg.filter.convolve.impl;

import gecv.alg.filter.convolve.TestConvolveImage;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.image.ImageFloat32;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestConvolveImageUnrolled_F32_F32 {
	@Test
	public void horizontal() throws NoSuchMethodException {
		Random rand = new Random(234);

		for (int i = 0; i < GenerateConvolvedUnrolled.numUnrolled; i++) {
			Method m = ConvolveImageUnrolled_F32_F32.class.getMethod("horizontal",
					Kernel1D_F32.class, ImageFloat32.class, ImageFloat32.class, boolean.class);

			TestConvolveImage.checkAgainstStandard(m, "horizontal", 5, 7, i + 1, rand);
		}
	}

	@Test
	public void vertical() throws NoSuchMethodException {
		Random rand = new Random(234);

		for (int i = 0; i < GenerateConvolvedUnrolled.numUnrolled; i++) {
			Method m = ConvolveImageUnrolled_F32_F32.class.getMethod("vertical",
					Kernel1D_F32.class, ImageFloat32.class, ImageFloat32.class, boolean.class);

			TestConvolveImage.checkAgainstStandard(m, "vertical", 5, 7, i + 1, rand);
		}
	}
}
