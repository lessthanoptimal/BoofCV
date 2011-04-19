package gecv.core.image;

import gecv.struct.image.ImageInt8;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class UtilImageInt8 {

	/**
	 * Fills the whole image with the specified pixel value
	 *
	 * @param img   An image.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill(ImageInt8 img, byte value) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		byte[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] = value;
			}
		}
	}

	/**
	 * Fills the whole image with random values
	 *
	 * @param img  An image.
	 * @param rand The value that the image is being filled with.
	 */
	public static void randomize(ImageInt8 img, Random rand) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		byte[] data = img.data;
		int range = Byte.MAX_VALUE - Byte.MIN_VALUE;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] = (byte) (rand.nextInt(range) + Byte.MIN_VALUE);
			}
		}
	}

	public static void randomize(ImageInt8 img, Random rand, int min, int max) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		byte[] data = img.data;
		int range = max - min;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] = (byte) (rand.nextInt(range) + min);
			}
		}
	}
}
