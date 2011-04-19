package gecv.core.image;

import gecv.struct.image.ImageInt8;
import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;

import java.awt.image.BufferedImage;

/**
 * Routines for converting to and from {@link BufferedImage} that use its internal
 * raster for better performance.
 *
 * @author Peter Abeles
 */
public class ConvertRaster {

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToGray(ByteInterleavedRaster src, ImageInt8 dst) {
		byte[] srcData = src.getDataStorage();

		byte[] data = dst.data;

		int numBands = src.getNumBands();

		int size = dst.getWidth() * dst.getHeight();

		if (numBands == 3) {
			int indexSrc = 0;
			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + dst.stride * y;
				int indexDstEnd = indexDst + dst.width;
				for (; indexDst < indexDstEnd; indexDst++) {
					int r = srcData[indexSrc++] & 0xFF;
					int g = srcData[indexSrc++] & 0xFF;
					int b = srcData[indexSrc++] & 0xFF;

					int ave = (r + g + b) / 3;

					data[indexDst] = (byte) ave;
				}
			}
		} else if (numBands == 1) {
			if (dst.startIndex == 0 && dst.width == dst.stride)
				System.arraycopy(srcData, 0, data, 0, size);
			else {
				for (int y = 0; y < dst.height; y++) {
					int indexDst = dst.startIndex + dst.stride * y;
					int indexSrc = dst.width * y;

					System.arraycopy(srcData, indexSrc, dst.data, indexDst, dst.width);
				}
			}
		} else {
			throw new RuntimeException("Write more code here.");
		}
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToGray(IntegerInterleavedRaster src, ImageInt8 dst) {
		int[] srcData = src.getDataStorage();

		byte[] data = dst.data;

		int indexSrc = 0;
		for (int y = 0; y < dst.height; y++) {
			int indexDst = dst.startIndex + y * dst.stride;
			for (int x = 0; x < dst.width; x++) {

				int rgb = srcData[indexSrc++];

				int r = (rgb >> 16) & 0xFF;
				int g = (rgb >> 8) & 0xFF;
				int b = rgb & 0xFF;

				int ave = (r + g + b) / 3;

				data[indexDst++] = (byte) ave;
			}
		}
	}

	/**
	 * <p>
	 * Converts a buffered image into an 8bit intensity image using the
	 * BufferedImage's RGB interface.
	 * </p>
	 * <p/>
	 * <p>
	 * This is much slower than working
	 * directly with the BufferedImage's internal raster and should be
	 * avoided if possible.
	 * </p>
	 *
	 * @param src Input image.
	 * @param dst Output image.
	 */
	public static void bufferedToGray(BufferedImage src, ImageInt8 dst) {
		final int width = src.getWidth();
		final int height = src.getHeight();

		byte[] data = dst.data;
		int index = dst.startIndex;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				int argb = src.getRGB(j, i);

				int r = (argb >> 16) & 0xFF;
				int g = (argb >> 8) & 0xFF;
				int b = argb & 0xFF;

				int ave = (r + g + b) / 3;

				data[index++] = (byte) ave;
			}
		}
	}

	public static void grayToBuffered(ImageInt8 src, ByteInterleavedRaster dst) {
//        dst.markDirty();

		final byte[] srcData = src.data;
		final byte[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		final int size = src.getWidth() * src.getHeight();


		if (numBands == 3) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					byte val = srcData[indexSrc];

					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
		} else if (numBands == 1) {
			if (src.startIndex == 0 && src.width == src.stride) {
				System.arraycopy(srcData, 0, dstData, 0, size);
			} else {
				for (int y = 0; y < src.height; y++) {
					int indexSrc = src.startIndex + src.stride * y;
					int indexDst = src.width * y;

					System.arraycopy(srcData, indexSrc, dstData, indexDst, src.width);
				}
			}
		} else {
			throw new RuntimeException("Code more here");
		}

	}

	public static void grayToBuffered(ImageInt8 src, IntegerInterleavedRaster dst) {
//        dst.markDirty();

		final byte[] srcData = src.data;
		final int[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		final int size = src.getWidth() * src.getHeight();

		if (numBands == 3) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;

				for (int x = 0; x < src.width; x++) {
					int v = srcData[indexSrc++] & 0xFF;

					dstData[indexDst++] = v << 16 | v << 8 | v;
				}
			}
		} else {
			throw new RuntimeException("Code more here");
		}

	}

	public static void grayToBuffered(ImageInt8 src, BufferedImage dst) {
//        dst.markDirty();

		final int width = dst.getWidth();
		final int height = dst.getHeight();

		byte[] data = src.data;
		for (int y = 0; y < height; y++) {
			int indexSrc = src.startIndex + src.stride * y;

			for (int x = 0; x < width; x++) {
				int v = data[indexSrc++] & 0xFF;

				int argb = v << 16 | v << 8 | v;

				dst.setRGB(x, y, argb);
			}
		}

	}
}
