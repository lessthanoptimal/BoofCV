package gecv.alg.filter.derivative.three;

import gecv.struct.image.ImageFloat32;


/**
 * This is an attempt to improve the performance by minimizing the number of times arrays are accessed
 * and partially unrolling loops.
 * <p/>
 * While faster than the standard algorithm, the standard appears to be fast enough.
 *
 * @author Peter Abeles
 */
public class GradientThree_Share {


	/**
	 * Can only be used with images that are NOT sub-images.
	 */
	public static void derivX_F32(ImageFloat32 orig,
								  ImageFloat32 derivX) {
		final float[] data = orig.data;
		final float[] imgX = derivX.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight();

		for (int y = 0; y < height; y++) {
			int index = width * y + 1;
			int endX = index + width - 2;
			int endXAlt = endX - (width - 2) % 3;

			float x0 = data[index - 1];
			float x1 = data[index];

			for (; index < endXAlt;) {
				float x2 = data[index + 1];
				imgX[index++] = (x2 - x0) * 0.5f;
				x0 = data[index + 1];
				imgX[index++] = (x0 - x1) * 0.5f;
				x1 = data[index + 1];
				imgX[index++] = (x1 - x2) * 0.5f;
			}

			for (; index < endX; index++) {
				imgX[index] = (data[index + 1] - data[index - 1]) * 0.5f;
			}
		}
	}
}