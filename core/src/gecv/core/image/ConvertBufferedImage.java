/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.core.image;

import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt8;
import gecv.struct.image.ImageInterleavedInt8;
import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;

/**
 * Functions for converting to and from {@link BufferedImage}.
 *
 * @author Peter Abeles
 */
public class ConvertBufferedImage {

	/**
	 * For BufferedImage stored as a byte array internally it extracts an
	 * interlaced image.  The input image and the returned image will both
	 * share the same internal data array.  Using this function allows unnecessary
	 * memory copying to be avoided.
	 *
	 * @param img Image whose internal data is extracted and wrapped.
	 * @return An image whose internal data is the same as the input image.
	 */
	public static ImageInterleavedInt8 extractInterlacedInt8(BufferedImage img) {
		if (img.getRaster() instanceof ByteInterleavedRaster) {
			ByteInterleavedRaster raster = (ByteInterleavedRaster) img.getRaster();
			ImageInterleavedInt8 ret = new ImageInterleavedInt8();

			ret.width = img.getWidth();
			ret.height = img.getHeight();
			ret.stride = ret.width;
			ret.numBands = raster.getNumBands();
			ret.data = raster.getDataStorage();

			return ret;
		}
		throw new IllegalArgumentException("Buffered image does not have a byte raster");
	}

	/**
	 * For BufferedImage stored as a byte array internally it extracts an
	 * image.  The input image and the returned image will both
	 * share the same internal data array.  Using this function allows unnecessary
	 * memory copying to be avoided.
	 *
	 * @param img Image whose internal data is extracted and wrapped.
	 * @return An image whose internal data is the same as the input image.
	 */
	public static ImageInt8 extractImageInt8(BufferedImage img) {
		if (img.getRaster() instanceof ByteInterleavedRaster) {
			ByteInterleavedRaster raster = (ByteInterleavedRaster) img.getRaster();
			if (raster.getNumBands() != 1)
				throw new IllegalArgumentException("Input image has more than one channel");
			ImageInt8 ret = new ImageInt8();

			ret.width = img.getWidth();
			ret.height = img.getHeight();
			ret.stride = ret.width;
			ret.data = raster.getDataStorage();

			return ret;
		}
		throw new IllegalArgumentException("Buffered image does not have a byte raster");
	}

	/**
	 * Creates a new BufferedImage that internally uses the same data as the provided
	 * ImageInterleavedInt8.  If 3 bands then the image will be of type TYPE_3BYTE_BGR
	 * or if 1 band TYPE_BYTE_GRAY.
	 *
	 * @param img Input image who's data will be wrapped by the returned BufferedImage.
	 * @return BufferedImage which shared data with the input image.
	 */
	public static BufferedImage extractBuffered(ImageInterleavedInt8 img) {
		if (img.isSubimage())
			throw new IllegalArgumentException("Sub-images are not supported for this operation");

		final int width = img.width;
		final int height = img.height;
		final int numBands = img.numBands;

		// wrap the byte array
		DataBuffer bufferByte = new DataBufferByte(img.data, width * height * numBands, 0);

		ColorModel colorModel;
		int[] bOffs = null;

		if (numBands == 3) {
			ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
			int[] nBits = {8, 8, 8};
			bOffs = new int[]{2, 1, 0};
			colorModel = new ComponentColorModel(cs, nBits, false, false,
					Transparency.OPAQUE,
					DataBuffer.TYPE_BYTE);
		} else if (numBands == 1) {
			ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
			int[] nBits = {8};
			bOffs = new int[]{0};
			colorModel = new ComponentColorModel(cs, nBits, false, true,
					Transparency.OPAQUE,
					DataBuffer.TYPE_BYTE);
		} else {
			throw new IllegalArgumentException("Only 1 or 3 bands supported");
		}

		// Create a raster using the sample model and data buffer
		WritableRaster raster = Raster.createInterleavedRaster(
				bufferByte, width, height, img.stride, numBands, bOffs, new Point(0, 0));

		// Combine the color model and raster into a buffered image

		return new BufferedImage(colorModel, raster, false, null);
	}

	/**
	 * <p>
	 * Creates a new BufferedImage that internally uses the same data as the provided
	 * ImageInt8.  The returned BufferedImage will be of type TYPE_BYTE_GRAY.
	 * </p>
	 * <p/>
	 * <p>
	 * NOTE: This only works on images which are not subimages!
	 * </p>
	 *
	 * @param img Input image who's data will be wrapped by the returned BufferedImage.
	 * @return BufferedImage which shared data with the input image.
	 */
	public static BufferedImage extractBuffered(ImageInt8 img) {
		if (img.isSubimage())
			throw new IllegalArgumentException("Sub-images are not supported for this operation");

		final int width = img.width;
		final int height = img.height;

		// wrap the byte array
		DataBuffer bufferByte = new DataBufferByte(img.data, width * height, 0);

		ColorModel colorModel;
		int[] bOffs = new int[]{0};

		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
		int[] nBits = {8};
		colorModel = new ComponentColorModel(cs, nBits, false, true,
				Transparency.OPAQUE,
				DataBuffer.TYPE_BYTE);

		// Create a raster using the sample model and data buffer
		WritableRaster raster = Raster.createInterleavedRaster(
				bufferByte, width, height, img.stride, 1, bOffs, new Point(0, 0));

		// Combine the color model and raster into a buffered image
		return new BufferedImage(colorModel, raster, false, null);
	}

	/**
	 * Converts a buffered image into an image of the specified type.  In a 'dst' image is provided
	 * it will be used for output, otherwise a new image will be created.
	 */
	public static <T extends ImageBase> T convertFrom(BufferedImage src, T dst, Class<T> type) {
		if (type == ImageInt8.class) {
			return (T) convertFrom(src, (ImageInt8) dst);
		} else if (type == ImageFloat32.class) {
			throw new RuntimeException("Not supported yet");
		} else {
			throw new IllegalArgumentException("Unknown type " + type);
		}
	}

	/**
	 * Converts the buffered image into an {@link ImageInt8}.  If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new image is created.
	 * @return Converted image.
	 */
	public static ImageInt8 convertFrom(BufferedImage src, ImageInt8 dst) {
		if (dst != null) {
			if (src.getWidth() != dst.getWidth() || src.getHeight() != dst.getHeight()) {
				throw new IllegalArgumentException("image dimension are different");
			}
		} else {
			dst = new ImageInt8(src.getWidth(), src.getHeight());
		}

		if (src.getRaster() instanceof ByteInterleavedRaster) {
			ConvertRaster.bufferedToGray((ByteInterleavedRaster) src.getRaster(), dst);
		} else if (src.getRaster() instanceof IntegerInterleavedRaster) {
			ConvertRaster.bufferedToGray((IntegerInterleavedRaster) src.getRaster(), dst);
		} else {
			ConvertRaster.bufferedToGray(src, dst);
		}

		return dst;
	}

	/**
	 * Converts the buffered image into an {@link ImageInt8}.  If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new image is created.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo(ImageInt8 src, BufferedImage dst) {
		if (dst != null) {
			if (dst.getWidth() != src.getWidth() || dst.getHeight() != src.getHeight()) {
				throw new IllegalArgumentException("image dimension are different");
			}
		} else {
			dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		}

		if (dst.getRaster() instanceof ByteInterleavedRaster) {
			ConvertRaster.grayToBuffered(src, (ByteInterleavedRaster) dst.getRaster());
		} else if (dst.getRaster() instanceof IntegerInterleavedRaster) {
			ConvertRaster.grayToBuffered(src, (IntegerInterleavedRaster) dst.getRaster());
		} else {
			ConvertRaster.grayToBuffered(src, dst);
		}

		return dst;
	}

	/**
	 * Draws the component into a BufferedImage.
	 *
	 * @param comp	The component being drawn into an image.
	 * @param storage if not null the component is drawn into it, if null a new BufferedImage is created.
	 * @return image of the component
	 */
	public static BufferedImage convertTo(JComponent comp, BufferedImage storage) {
		if (storage == null)
			storage = new BufferedImage(comp.getWidth(), comp.getHeight(), BufferedImage.TYPE_INT_RGB);

		Graphics2D g2 = storage.createGraphics();

		comp.paintComponents(g2);

		return storage;
	}


}
