package gecv.core.image;

import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class UtilImageInt16 {

	/**
	 * Fills the whole image with the specified pixel value
	 *
	 * @param img   An image.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill(ImageInt16 img, int value) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		short[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] = (short) value;
			}
		}
	}

	/**
	 * Fills the whole image with random values
	 *
	 * @param img  An image.
	 * @param rand The value that the image is being filled with.
	 */
	public static void randomize(ImageInt16 img, Random rand, int min, int max) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		short[] data = img.data;
		int range = max - min;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] = (short) (rand.nextInt(range) + min);
			}
		}
	}
}
