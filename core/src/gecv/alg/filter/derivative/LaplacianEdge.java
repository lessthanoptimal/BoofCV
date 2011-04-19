package gecv.alg.filter.derivative;

import gecv.alg.InputSanityCheck;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;

/**
 * <p>
 * The Laplacian is convolved across an image to find second derivative of the image.
 * It is often a faster way to compute the intensity of an edge than first derivative algorithms.
 * </p>
 * <p/>
 * <pre>
 * This implementation of the laplacian has a 3 by 3 kernel.
 * <p/>
 *            partial^2 f     partial^2 f
 * f(x,y) =   ~~~~~~~~~~~  +  ~~~~~~~~~~~
 *            partial x^2     partial x^2
 * <p/>
 *          Integer Images    Floating Point Images
 *          [ 0   1   0 ]      [  0   0.25  0   ]
 * kernel = [ 1  -4   1 ]  or  [ 0.25  -1  0.25 ]
 *          [ 0   1   0 ]      [  0   0.25  0   ]
 * </pre>
 * <p/>
 * <p>
 * DEVELOPER NOTE:  This is still a strong candidate for further optimizations due to redundant
 * array accesses.
 * </p>
 *
 * @author Peter Abeles
 */
public class LaplacianEdge {


	/**
	 * Computes the Laplacean of 'orig'.
	 *
	 * @param orig  Input image.  Not modified.
	 * @param deriv Where the Laplacian is written to. Modified.
	 */
	public static void process_I8(ImageInt8 orig, ImageInt16 deriv) {
		InputSanityCheck.checkSameShape(orig, deriv);

		final byte[] data = orig.data;
		final short[] out = deriv.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int stride = orig.stride;

		for (int y = 1; y < height; y++) {
			int index = orig.startIndex + stride * y + 1;
			int indexOut = deriv.startIndex + deriv.stride * y + 1;
			int endX = index + width - 2;

			for (; index < endX; index++) {

				int v = data[index - stride] & 0xFF;
				v += data[index - 1] & 0xFF;
				v += -4 * (data[index] & 0xFF);
				v += data[index + 1] & 0xFF;
				v += data[index + stride] & 0xFF;

				out[indexOut++] = (short) v;
			}
		}
	}

	/**
	 * Computes the Laplacean of 'orig'.
	 *
	 * @param orig  Input image.  Not modified.
	 * @param deriv Where the Laplacian is written to. Modified.
	 */
	public static void process_F32(ImageFloat32 orig, ImageFloat32 deriv) {
		InputSanityCheck.checkSameShape(orig, deriv);

		final float[] data = orig.data;
		final float[] out = deriv.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int stride = orig.stride;

		for (int y = 1; y < height; y++) {
			int index = orig.startIndex + stride * y + 1;
			int indexOut = deriv.startIndex + deriv.stride * y + 1;
			int endX = index + width - 2;

			for (; index < endX; index++) {

				float v = data[index - stride] + data[index - 1];
				v += data[index + 1];
				v += data[index + stride];

				out[indexOut++] = 0.25f * v - data[index];
			}
		}
	}
}
