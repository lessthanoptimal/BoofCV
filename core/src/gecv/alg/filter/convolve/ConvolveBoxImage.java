package gecv.alg.filter.convolve;

import gecv.alg.filter.convolve.impl.ConvolveBox_F32_F32;
import gecv.alg.filter.convolve.impl.ConvolveBox_I8_I16;
import gecv.alg.filter.convolve.impl.ConvolveBox_I8_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt32;
import gecv.struct.image.ImageInt8;

/**
 * @author Peter Abeles
 */
// TODO add description
public class ConvolveBoxImage {

	public static void horizontal(ImageFloat32 input, ImageFloat32 output, int radius, boolean includeBorder) {
		ConvolveBox_F32_F32.horizontal(input, output, radius, includeBorder);
	}

	public static void horizontal(ImageInt8 input, ImageInt16 output, int radius, boolean includeBorder) {
		ConvolveBox_I8_I16.horizontal(input, output, radius, includeBorder);
	}

	public static void horizontal(ImageInt8 input, ImageInt32 output, int radius, boolean includeBorder) {
		ConvolveBox_I8_I32.horizontal(input, output, radius, includeBorder);
	}

	public static void vertical(ImageFloat32 input, ImageFloat32 output, int radius, boolean includeBorder) {
		ConvolveBox_F32_F32.vertical(input, output, radius, includeBorder);
	}

	public static void vertical(ImageInt8 input, ImageInt16 output, int radius, boolean includeBorder) {
		ConvolveBox_I8_I16.vertical(input, output, radius, includeBorder);
	}

	public static void vertical(ImageInt8 input, ImageInt32 output, int radius, boolean includeBorder) {
		ConvolveBox_I8_I32.vertical(input, output, radius, includeBorder);
	}
}
