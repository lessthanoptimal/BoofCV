package gecv.core.image;

import gecv.struct.image.*;

import java.util.Random;

/**
 * Operations that return information about the specific image.  Useful when writing highly abstracted code
 * which is independent of the input image.
 *
 * @author Peter Abeles
 */
public class GeneralizedImageOps {

	public static void randomize(ImageBase img, int min, int max, Random rand) {
		if (img.getClass() == ImageInt8.class) {
			UtilImageInt8.randomize((ImageInt8) img, rand, min, max);
		} else if (img.getClass() == ImageInt16.class) {
			UtilImageInt16.randomize((ImageInt16) img, rand, (short) min, (short) max);
		} else if (img.getClass() == ImageFloat32.class) {
			UtilImageFloat32.randomize((ImageFloat32) img, rand, min, max);
		} else {
			throw new RuntimeException("Adsasd");
		}
	}

	/**
	 * Returns true if the input image is a floating point image.
	 *
	 * @param img A image of unknown type.
	 * @return true if floating point.
	 */
	public static boolean isFloatingPoint(ImageBase img) {
		return ImageFloat32.class.isAssignableFrom(img.getClass());
	}

	public static double get(ImageBase img, int x, int y) {
		if (img instanceof ImageInt8) {
			return ((ImageInt8) img).getU(x, y);
		} else if (img instanceof ImageInt16) {
			return ((ImageInt16) img).get(x, y);
		} else if (img instanceof ImageInt32) {
			return ((ImageInt32) img).get(x, y);
		} else if (img instanceof ImageFloat32) {
			return ((ImageFloat32) img).get(x, y);
		} else {
			throw new IllegalArgumentException("Unknown or incompatible image type: " + img.getClass().getSimpleName());
		}
	}

}
