package gecv.alg.filter.convolve;

import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.image.ImageFloat32;


/**
 * Covolves a 1D kernel in the horizontal or vertical direction across an image.  Unlike
 * {@link ConvolveImage} the borders along the image handled by normalizing the kernel
 * to one.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"ForLoopReplaceableByForEach"})
public class Convolve1DBorders {

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border
	 * is not processed and the vertical border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param processBorder Should the vertical border of the image be processed?
	 */
	public static void horizontal(Kernel1D_F32 kernel, ImageFloat32 image, ImageFloat32 dest,
								  boolean processBorder) {
		final float[] dataSrc = image.data;
		final float[] dataDst = dest.data;
		final float[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int yBorder = processBorder ? 0 : radius;

		final int width = image.getWidth();
		final int height = image.getHeight() - yBorder;

		for (int i = yBorder; i < height; i++) {
			int indexDest = dest.startIndex + i * dest.stride;
			int j = image.startIndex + i * image.stride;
			final int jStart = j;
			int jEnd = j + radius;
			final int jEnd2 = j + width - radius;

			for (; j < jEnd; j++) {
				float total = 0;
				float totalWeight = 0;
				int indexSrc = jStart;
				for (int k = kernelWidth - (radius + 1 + j - jStart); k < kernelWidth; k++) {
					float w = dataKer[k];
					totalWeight += w;
					total += dataSrc[indexSrc++] * w;
				}
				dataDst[indexDest++] = total / totalWeight;
			}

			for (; j < jEnd2; j++) {
				float total = 0;
				int indexSrc = j - radius;
				for (int k = 0; k < kernelWidth; k++) {
					total += dataSrc[indexSrc++] * dataKer[k];
				}
				dataDst[indexDest++] = total;
			}

			jEnd = jStart + width;
			for (; j < jEnd; j++) {
				float total = 0;
				float totalWeight = 0;
				int indexSrc = j - radius;
				final int kEnd = jEnd - indexSrc;

				for (int k = 0; k < kEnd; k++) {
					float w = dataKer[k];
					totalWeight += w;
					total += dataSrc[indexSrc++] * w;
				}
				dataDst[indexDest++] = total / totalWeight;
			}
		}
	}

	/**
	 * Performs a vertical 1D convolution across the image.  The vertical border
	 * is not processed and the horizontal border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param processBorder Should the horizontal border of the image be processed?
	 */
	public static void vertical(Kernel1D_F32 kernel, ImageFloat32 image, ImageFloat32 dest,
								boolean processBorder) {
		final float[] dataSrc = image.data;
		final float[] dataDst = dest.data;
		final float[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight - radius;

		final int xBorder = processBorder ? 0 : radius;

		for (int y = 0; y < radius; y++) {
			int indexDst = dest.startIndex + y * dest.stride + xBorder;
			int i = image.startIndex + y * image.stride;
			final int iEnd = i + imgWidth - xBorder;

			int kStart = radius - y;

			float weight = 0;
			for (int k = kStart; k < kernelWidth; k++) {
				weight += dataKer[k];
			}

			for (i += xBorder; i < iEnd; i++) {
				float total = 0;
				int indexSrc = i - y * image.stride;
				for (int k = kStart; k < kernelWidth; k++) {
					total += dataSrc[indexSrc] * dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = total / weight;
			}
		}

		for (int y = radius; y < yEnd; y++) {
			int indexDst = dest.startIndex + y * dest.stride + xBorder;
			int i = image.startIndex + y * image.stride;
			final int iEnd = i + imgWidth - xBorder;

			for (i += xBorder; i < iEnd; i++) {
				float total = 0;
				int indexSrc = i - radius * image.stride;
				for (int k = 0; k < kernelWidth; k++) {
					total += dataSrc[indexSrc] * dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = total;
			}
		}

		for (int y = yEnd; y < imgHeight; y++) {
			int indexDst = dest.startIndex + y * dest.stride + xBorder;
			int i = image.startIndex + y * image.stride;
			final int iEnd = i + imgWidth - xBorder;

			int kEnd = imgHeight - (y - radius);

			float weight = 0;
			for (int k = 0; k < kEnd; k++) {
				weight += dataKer[k];
			}

			for (i += xBorder; i < iEnd; i++) {
				float total = 0;
				int indexSrc = i - radius * image.stride;
				for (int k = 0; k < kEnd; k++) {
					total += dataSrc[indexSrc] * dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = total / weight;
			}
		}
	}
}