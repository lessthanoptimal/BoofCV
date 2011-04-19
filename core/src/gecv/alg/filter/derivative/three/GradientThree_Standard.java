package gecv.alg.filter.derivative.three;

import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;


/**
 * <p>
 * Basic implementation of {@link gecv.alg.filter.derivative.GradientThree} with nothing fancy is done to improve its performance.
 * </p>
 *
 * @author Peter Abeles
 */
public class GradientThree_Standard {

	/**
	 * Computes the derivative along the x and y axes
	 */
	public static void deriv_F32(ImageFloat32 orig,
								 ImageFloat32 derivX,
								 ImageFloat32 derivY) {
		final float[] data = orig.data;
		final float[] imgX = derivX.data;
		final float[] imgY = derivY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int stride = orig.stride;

		for (int y = 1; y < height; y++) {
			int indexX = derivX.startIndex + derivX.stride * y + 1;
			int indexY = derivY.startIndex + derivY.stride * y + 1;
			int indexSrc = orig.startIndex + stride * y + 1;
			final int endX = indexSrc + width - 2;

			for (; indexSrc < endX; indexSrc++) {
				imgX[indexX++] = (data[indexSrc + 1] - data[indexSrc - 1]) * 0.5f;
				imgY[indexY++] = (data[indexSrc + stride] - data[indexSrc - stride]) * 0.5f;
			}
		}
	}

	/**
	 * Computes the derivative along the x and y axes
	 */
	public static void deriv_I8(ImageInt8 orig,
								ImageInt16 derivX,
								ImageInt16 derivY) {
		final byte[] data = orig.data;
		final short[] imgX = derivX.data;
		final short[] imgY = derivY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int stride = orig.stride;

		for (int y = 1; y < height; y++) {
			int indexX = derivX.startIndex + derivX.stride * y + 1;
			int indexY = derivY.startIndex + derivY.stride * y + 1;
			int indexSrc = orig.startIndex + stride * y + 1;
			final int endX = indexSrc + width - 2;

			for (; indexSrc < endX; indexSrc++) {
				imgX[indexX++] = (short) ((data[indexSrc + 1] & 0xFF) - (data[indexSrc - 1] & 0xFF));
				imgY[indexY++] = (short) ((data[indexSrc + stride] & 0xFF) - (data[indexSrc - stride] & 0xFF));
			}
		}
	}

	/**
	 * Computes the derivative along the x-axis only
	 */
	public static void derivX_F32(ImageFloat32 orig,
								  ImageFloat32 derivX) {
		final float[] data = orig.data;
		final float[] imgX = derivX.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight();
		final int stride = orig.stride;

		for (int y = 0; y < height; y++) {
			int indexX = derivX.startIndex + derivX.stride * y + 1;
			int indexSrc = orig.startIndex + stride * y + 1;
			final int endX = indexSrc + width - 2;

			for (; indexSrc < endX; indexSrc++) {
				imgX[indexX++] = (data[indexSrc + 1] - data[indexSrc - 1]) * 0.5F;
			}
		}
	}

	/**
	 * Computes the derivative along the y-axis only
	 */
	public static void derivY_F32(ImageFloat32 orig,
								  ImageFloat32 derivY) {
		final float[] data = orig.data;
		final float[] imgY = derivY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int stride = orig.stride;

		for (int y = 1; y < height; y++) {
			int indexY = derivY.startIndex + derivY.stride * y;
			int indexSrc = orig.startIndex + stride * y;
			final int endX = indexSrc + width;

			for (; indexSrc < endX; indexSrc++) {
				imgY[indexY++] = (data[indexSrc + stride] - data[indexSrc - stride]) * 0.5F;
			}
		}
	}

	/**
	 * Computes the derivative along the x axis
	 */
	public static void derivX_I8(ImageInt8 orig,
								 ImageInt16 derivX) {
		final byte[] data = orig.data;
		final short[] imgX = derivX.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int stride = orig.stride;

		for (int y = 1; y < height; y++) {
			int indexX = derivX.startIndex + derivX.stride * y + 1;
			int indexSrc = orig.startIndex + stride * y + 1;
			final int endX = indexSrc + width - 2;

			for (; indexSrc < endX; indexSrc++) {
				imgX[indexX++] = (short) ((data[indexSrc + 1] & 0xFF) - (data[indexSrc - 1] & 0xFF));
			}
		}
	}

	/**
	 * Computes the derivative along the y axis
	 */
	public static void derivY_I8(ImageInt8 orig,
								 ImageInt16 derivY) {
		final byte[] data = orig.data;
		final short[] imgY = derivY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int stride = orig.stride;

		for (int y = 1; y < height; y++) {
			int indexY = derivY.startIndex + derivY.stride * y + 1;
			int indexSrc = orig.startIndex + stride * y + 1;
			final int endX = indexSrc + width - 2;

			for (; indexSrc < endX; indexSrc++) {
				imgY[indexY++] = (short) ((data[indexSrc + stride] & 0xFF) - (data[indexSrc - stride] & 0xFF));
			}
		}
	}
}