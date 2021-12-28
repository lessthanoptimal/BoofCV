/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.io.image;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.*;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;

import static boofcv.io.image.ConvertRaster.isKnownByteFormat;
import static boofcv.io.image.ConvertRaster.orderBandsIntoRGB;

/**
 * Functions for converting to and from {@link BufferedImage}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"Duplicates", "unchecked"})
public class ConvertBufferedImage {

	/**
	 * If the provided image does not have the same shape and same type a new one is declared and returned.
	 */
	public static BufferedImage checkDeclare( int width, int height, @Nullable BufferedImage image, int type ) {
		// webcam images can have type 0, which is unknown
		if (type == 0) {
			type = BufferedImage.TYPE_INT_RGB;
		}
		if (image == null)
			return new BufferedImage(width, height, type);
		if (image.getType() != type)
			return new BufferedImage(width, height, type);
		if (image.getWidth() != width || image.getHeight() != height)
			return new BufferedImage(width, height, type);
		return image;
	}

	public static BufferedImage checkDeclare( BufferedImage template, BufferedImage target ) {
		int width = template.getWidth();
		int height = template.getHeight();
		int type = template.getType();
		if (type == 0) {
			if (target != null)
				type = target.getType();
			else
				type = BufferedImage.TYPE_INT_RGB;
		}

		if (target == null)
			return new BufferedImage(width, height, type);
		if (target.getType() != type)
			return new BufferedImage(width, height, type);
		if (target.getWidth() != width || target.getHeight() != height)
			return new BufferedImage(width, height, type);
		return target;
	}

	/**
	 * Copies the original image into the output image. If it can't do a copy a new image is created and returned
	 *
	 * @param original Original image
	 * @param output (Optional) Storage for copy.
	 * @return The copied image. May be a new instance
	 */
	public static BufferedImage checkCopy( BufferedImage original, @Nullable BufferedImage output ) {
		ColorModel cm = original.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();

		if (output == null || original.getWidth() != output.getWidth() || original.getHeight() != output.getHeight() ||
				original.getType() != output.getType()) {
			WritableRaster raster = original.copyData(original.getRaster().createCompatibleWritableRaster());
			return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
		}

		original.copyData(output.getRaster());
		return output;
	}

	/**
	 * Returns the number of bands or channels that the BoofCV image needs to have for this iamge type
	 */
	public static int numChannels( BufferedImage image ) {
		return image.getRaster().getNumBands();
	}

	/**
	 * Returns an image which doesn't have an alpha channel. If the input image doesn't have an alpha
	 * channel to start then its returned as is. Otherwise a new image is created and the RGB channels are
	 * copied and the new image returned.
	 *
	 * @param image Input image
	 * @return Image without an alpha channel
	 */
	public static BufferedImage stripAlphaChannel( BufferedImage image ) {
		int numBands = image.getRaster().getNumBands();

		if (numBands == 4) {
			BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
			output.createGraphics().drawImage(image, 0, 0, null);
			return output;
		} else {
			return image;
		}
	}

	/**
	 * For BufferedImage stored as a byte array internally it extracts an
	 * interleaved image. The input image and the returned image will both
	 * share the same internal data array. Using this function allows unnecessary
	 * memory copying to be avoided.
	 *
	 * @param img Image whose internal data is extracted and wrapped.
	 * @return An image whose internal data is the same as the input image.
	 */
	public static InterleavedU8 extractInterleavedU8( BufferedImage img ) {

		DataBuffer buffer = img.getRaster().getDataBuffer();
		if (buffer.getDataType() == DataBuffer.TYPE_BYTE && isKnownByteFormat(img)) {
			WritableRaster raster = img.getRaster();

			InterleavedU8 ret = new InterleavedU8();

			ret.width = img.getWidth();
			ret.height = img.getHeight();
			ret.startIndex = ConvertRaster.getOffset(raster);
			ret.imageType.numBands = raster.getNumBands();
			ret.numBands = raster.getNumBands();
			ret.stride = ConvertRaster.stride(raster);
			ret.data = ((DataBufferByte)buffer).getData();
			ret.subImage = ret.startIndex != 0;

			return ret;
		}
		throw new IllegalArgumentException("Buffered image does not have an interleaved byte raster");
	}

	/**
	 * For BufferedImage stored as a byte array internally it extracts an
	 * image. The input image and the returned image will both
	 * share the same internal data array. Using this function allows unnecessary
	 * memory copying to be avoided.
	 *
	 * @param img Image whose internal data is extracted and wrapped.
	 * @return An image whose internal data is the same as the input image.
	 */
	public static GrayU8 extractGrayU8( BufferedImage img ) {
		WritableRaster raster = img.getRaster();
		DataBuffer buffer = raster.getDataBuffer();
		if (buffer.getDataType() == DataBuffer.TYPE_BYTE && isKnownByteFormat(img)) {

			if (raster.getNumBands() != 1)
				throw new IllegalArgumentException("Input image has more than one channel");
			GrayU8 ret = new GrayU8();

			ret.width = img.getWidth();
			ret.height = img.getHeight();
			ret.startIndex = ConvertRaster.getOffset(img.getRaster());
			ret.stride = ConvertRaster.stride(img.getRaster());
			ret.data = ((DataBufferByte)buffer).getData();

			return ret;
		}
		throw new IllegalArgumentException("Buffered image does not have a gray scale byte raster");
	}

	/**
	 * Creates a new BufferedImage that internally uses the same data as the provided
	 * {@link InterleavedU8}. If 3 bands then the image will be of type TYPE_3BYTE_BGR
	 * or if 1 band TYPE_BYTE_GRAY.
	 *
	 * @param img Input image who's data will be wrapped by the returned BufferedImage.
	 * @return BufferedImage which shared data with the input image.
	 */
	public static BufferedImage extractBuffered( InterleavedU8 img ) {
		if (img.isSubimage())
			throw new IllegalArgumentException("Sub-images are not supported for this operation");

		final int width = img.width;
		final int height = img.height;
		final int numBands = img.numBands;

		// wrap the byte array
		DataBuffer bufferByte = new DataBufferByte(img.data, width*height*numBands, 0);

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
	 * GrayU8. The returned BufferedImage will be of type TYPE_BYTE_GRAY.
	 * </p>
	 * <p/>
	 * <p>
	 * NOTE: This only works on images which are not subimages!
	 * </p>
	 *
	 * @param img Input image who's data will be wrapped by the returned BufferedImage.
	 * @return BufferedImage which shared data with the input image.
	 */
	public static BufferedImage extractBuffered( GrayU8 img ) {
		if (img.isSubimage())
			throw new IllegalArgumentException("Sub-images are not supported for this operation");

		final int width = img.width;
		final int height = img.height;

		// wrap the byte array
		DataBuffer bufferByte = new DataBufferByte(img.data, width*height, 0);

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
	 * Converts a buffered image into an image of the specified type.
	 *
	 * @param src Input BufferedImage which is to be converted
	 * @param dst The image which it is being converted into
	 * @param orderRgb If applicable, should it adjust the ordering of each color band to maintain color consistency
	 */
	public static <T extends ImageBase<T>> void convertFrom( BufferedImage src, T dst, boolean orderRgb ) {
		if (dst instanceof ImageGray) {
			ImageGray sb = (ImageGray)dst;
			convertFromSingle(src, sb, (Class<ImageGray>)sb.getClass());
		} else if (dst instanceof Planar) {
			Planar ms = (Planar)dst;
			convertFromPlanar(src, ms, orderRgb, ms.getBandType());
		} else if (dst instanceof ImageInterleaved) {
			convertFromInterleaved(src, (ImageInterleaved)dst, orderRgb);
		} else {
			throw new IllegalArgumentException("Unknown type " + dst.getClass().getSimpleName());
		}
	}

	/**
	 * Converts a buffered image into an image of the specified type.
	 *
	 * @param src Input BufferedImage which is to be converted
	 * @param orderRgb If applicable, should it adjust the ordering of each color band to maintain color consistency
	 * @param imageType Type of image it is to be converted into
	 * @return The image
	 */
	public static <T extends ImageBase<T>> T convertFrom( BufferedImage src, boolean orderRgb, ImageType<T> imageType ) {

		T out = imageType.createImage(src.getWidth(), src.getHeight());

		switch (imageType.getFamily()) {
			case GRAY -> convertFromSingle(src, (ImageGray)out, imageType.getImageClass());
			case PLANAR -> convertFromPlanar(src, (Planar)out, orderRgb, imageType.getImageClass());
			case INTERLEAVED -> convertFromInterleaved(src, (ImageInterleaved)out, orderRgb);
			default -> throw new RuntimeException("Not supported yet");
		}

		return out;
	}

	public static <T extends ImageBase<T>> T convertFrom( BufferedImage src, boolean orderRgb, T output ) {

		ImageType<T> imageType = output.getImageType();

		switch (imageType.getFamily()) {
			case GRAY -> convertFromSingle(src, (ImageGray)output, imageType.getImageClass());
			case PLANAR -> convertFromPlanar(src, (Planar)output, orderRgb, imageType.getImageClass());
			case INTERLEAVED -> convertFromInterleaved(src, (ImageInterleaved)output, orderRgb);
			default -> throw new RuntimeException("Not supported yet");
		}

		return output;
	}

	/**
	 * Converts a buffered image into an image of the specified type. In a 'dst' image is provided
	 * it will be used for output, otherwise a new image will be created.
	 */
	public static <T extends ImageGray<T>> T convertFromSingle( BufferedImage src, @Nullable T dst, Class<T> type ) {
		if (type == GrayU8.class) {
			return (T)convertFrom(src, (GrayU8)dst);
		} else if (GrayI16.class.isAssignableFrom(type)) {
			return (T)convertFrom(src, (GrayI16)dst, (Class)type);
		} else if (type == GrayF32.class) {
			return (T)convertFrom(src, (GrayF32)dst);
		} else {
			throw new IllegalArgumentException("Unknown type " + type);
		}
	}

	/**
	 * Converts the buffered image into an {@link GrayU8}. If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to. If null a new unsigned image is created.
	 * @return Converted image.
	 */
	public static GrayU8 convertFrom( BufferedImage src, @Nullable GrayU8 dst ) {
		if (dst != null) {
			dst.reshape(src.getWidth(), src.getHeight());
		} else {
			dst = new GrayU8(src.getWidth(), src.getHeight());
		}

		try {
			DataBuffer buff = src.getRaster().getDataBuffer();
			if (buff.getDataType() == DataBuffer.TYPE_BYTE) {
				if (isKnownByteFormat(src)) {
					ConvertRaster.bufferedToGray((DataBufferByte)buff, src.getRaster(), dst);
				} else {
					ConvertRaster.bufferedToGray(src, dst.data, dst.startIndex, dst.stride);
				}
			} else if (buff.getDataType() == DataBuffer.TYPE_INT) {
				ConvertRaster.bufferedToGray((DataBufferInt)buff, src.getRaster(), dst);
			} else {
				ConvertRaster.bufferedToGray(src, dst.data, dst.startIndex, dst.stride);
			}
		} catch (java.security.AccessControlException e) {
			// Applets don't allow access to the raster()
			ConvertRaster.bufferedToGray(src, dst.data, dst.startIndex, dst.stride);
		}

		return dst;
	}

	/**
	 * Converts the buffered image into an {@link GrayI16}. If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to. If null a new unsigned image is created.
	 * @return Converted image.
	 */
	public static <T extends GrayI16<T>> T convertFrom( BufferedImage src, @Nullable T dst, Class<T> type ) {
		if (dst != null) {
			dst.reshape(src.getWidth(), src.getHeight());
		} else {
			dst = GeneralizedImageOps.createSingleBand(type, src.getWidth(), src.getHeight());
		}

		DataBuffer buffer = src.getRaster().getDataBuffer();
		if (buffer.getDataType() == DataBuffer.TYPE_USHORT) {
			ConvertRaster.bufferedToGray((DataBufferUShort)buffer, src.getRaster(), dst);
			return dst;
		}

		ConvertRaster.bufferedToGray(src, dst);

		return dst;
	}

	/**
	 * Converts the buffered image into an {@link GrayF32}. If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to. If null a new unsigned image is created.
	 * @return Converted image.
	 */
	public static GrayF32 convertFrom( BufferedImage src, @Nullable GrayF32 dst ) {
		if (dst != null) {
			dst.reshape(src.getWidth(), src.getHeight());
		} else {
			dst = new GrayF32(src.getWidth(), src.getHeight());
		}

		try {
			DataBuffer buff = src.getRaster().getDataBuffer();

			if (buff.getDataType() == DataBuffer.TYPE_BYTE) {
				if (isKnownByteFormat(src)) {
					ConvertRaster.bufferedToGray((DataBufferByte)buff, src.getRaster(), dst);
				} else {
					ConvertRaster.bufferedToGray(src, dst.data, dst.startIndex, dst.stride);
				}
			} else if (buff.getDataType() == DataBuffer.TYPE_INT) {
				ConvertRaster.bufferedToGray((DataBufferInt)buff, src.getRaster(), dst);
			} else {
				ConvertRaster.bufferedToGray(src, dst.data, dst.startIndex, dst.stride);
			}
		} catch (java.security.AccessControlException e) {
			// Applets don't allow access to the raster()
			ConvertRaster.bufferedToGray(src, dst.data, dst.startIndex, dst.stride);
		}

		return dst;
	}

	/**
	 * Converts the buffered image into an {@link Planar} image of the specified ype.
	 *
	 * @param src Input image. Not modified.
	 * @param dst Output. The converted image is written to. If null a new unsigned image is created.
	 * @param orderRgb If applicable, should it adjust the ordering of each color band to maintain color consistency.
	 * Most of the time you want this to be true.
	 * @param type Which type of data structure is each band. (GrayU8 or GrayF32)
	 * @return Converted image.
	 */
	public static <T extends ImageGray<T>> Planar<T>
	convertFromPlanar( BufferedImage src, @Nullable Planar<T> dst, boolean orderRgb, Class<T> type ) {
		if (src == null)
			throw new IllegalArgumentException("src is null!");

		if (dst != null) {
			dst.reshape(src.getWidth(), src.getHeight());
		}

		try {
			WritableRaster raster = src.getRaster();

			int numBands;
			if (!isKnownByteFormat(src))
				numBands = 3;
			else
				numBands = raster.getNumBands();

			if (dst == null)
				dst = new Planar<>(type, src.getWidth(), src.getHeight(), numBands);
			else if (dst.getNumBands() != numBands)
				dst.setNumberOfBands(numBands);

			DataBuffer srcBuff = src.getRaster().getDataBuffer();
			if (type == GrayU8.class) {
				if (srcBuff.getDataType() == DataBuffer.TYPE_BYTE &&
						isKnownByteFormat(src)) {
					if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) {
						for (int i = 0; i < dst.getNumBands(); i++) {
							ConvertRaster.bufferedToGray((DataBufferByte)srcBuff, raster, ((Planar<GrayU8>)dst).getBand(i));
						}
					} else {
						ConvertRaster.bufferedToPlanar_U8((DataBufferByte)srcBuff, src.getRaster(), (Planar<GrayU8>)dst);
					}
				} else if (srcBuff.getDataType() == DataBuffer.TYPE_INT) {
					ConvertRaster.bufferedToPlanar_U8((DataBufferInt)srcBuff, src.getRaster(), (Planar<GrayU8>)dst);
				} else {
					ConvertRaster.bufferedToPlanar_U8(src, (Planar<GrayU8>)dst);
				}
			} else if (type == GrayF32.class) {
				if (srcBuff.getDataType() == DataBuffer.TYPE_BYTE &&
						isKnownByteFormat(src)) {
					if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) {
						for (int i = 0; i < dst.getNumBands(); i++)
							ConvertRaster.bufferedToGray((DataBufferByte)srcBuff, raster, ((Planar<GrayF32>)dst).getBand(i));
					} else {
						ConvertRaster.bufferedToPlanar_F32((DataBufferByte)srcBuff, src.getRaster(), (Planar<GrayF32>)dst);
					}
				} else if (srcBuff.getDataType() == DataBuffer.TYPE_INT) {
					ConvertRaster.bufferedToPlanar_F32((DataBufferInt)srcBuff, src.getRaster(), (Planar<GrayF32>)dst);
				} else {
					ConvertRaster.bufferedToPlanar_F32(src, (Planar<GrayF32>)dst);
				}
			} else {
				throw new IllegalArgumentException("Band type not supported yet");
			}
		} catch (java.security.AccessControlException e) {
			// Applets don't allow access to the raster()
			if (dst == null)
				dst = new Planar<>(type, src.getWidth(), src.getHeight(), 3);
			else {
				dst.setNumberOfBands(3);
			}

			if (type == GrayU8.class) {
				ConvertRaster.bufferedToPlanar_U8(src, (Planar<GrayU8>)dst);
			} else if (type == GrayF32.class) {
				ConvertRaster.bufferedToPlanar_F32(src, (Planar<GrayF32>)dst);
			}
		}

		// if requested, ensure the ordering of the bands
		if (orderRgb) {
			orderBandsIntoRGB(dst, src);
		}

		return dst;
	}

	public static <T extends ImageBase<T>> T convertFrom( BufferedImage src, Class type, boolean orderRgb ) {
		T dst;
		if (ImageGray.class.isAssignableFrom(type)) {
			dst = (T)convertFromSingle(src, null, type);
		} else if (ImageInterleaved.class.isAssignableFrom(type)) {
			dst = (T)GeneralizedImageOps.createInterleaved(type, 1, 1, 3);
			convertFromInterleaved(src, (ImageInterleaved)dst, orderRgb);
		} else {
			dst = (T)new Planar<>(GrayU8.class, 1, 1, 3);
			convertFrom(src, dst, orderRgb);
		}

		return dst;
	}

	public static void convertFromInterleaved( BufferedImage src, ImageInterleaved dst, boolean orderRgb ) {
		if (src == null)
			throw new IllegalArgumentException("src is null!");

		try {
			WritableRaster raster = src.getRaster();

			int numBands;
			if (!isKnownByteFormat(src))
				numBands = 3;
			else
				numBands = raster.getNumBands();

			dst.setNumberOfBands(numBands);
			dst.reshape(src.getWidth(), src.getHeight());

			DataBuffer buffer = src.getRaster().getDataBuffer();
			if (dst instanceof InterleavedU8) {
				if (buffer.getDataType() == DataBuffer.TYPE_BYTE) {
					if (isKnownByteFormat(src)) {
						if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) {
							ConvertRaster.bufferedToGray(src, ((InterleavedU8)dst).data, dst.startIndex, dst.stride);
						} else {
							ConvertRaster.bufferedToInterleaved((DataBufferByte)buffer, src.getRaster(), (InterleavedU8)dst);
						}
					} else {
						ConvertRaster.bufferedToInterleaved(src, (InterleavedU8)dst);
					}
				} else if (buffer.getDataType() == DataBuffer.TYPE_INT) {
					ConvertRaster.bufferedToInterleaved((DataBufferInt)buffer, src.getRaster(), (InterleavedU8)dst);
				} else {
					ConvertRaster.bufferedToInterleaved(src, (InterleavedU8)dst);
				}
			} else if (dst instanceof InterleavedF32) {
				if (buffer.getDataType() == DataBuffer.TYPE_BYTE) {
					if (isKnownByteFormat(src)) {
						if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) {
							ConvertRaster.bufferedToGray(src, ((InterleavedF32)dst).data, dst.startIndex, dst.stride);
						} else {
							ConvertRaster.bufferedToInterleaved((DataBufferByte)buffer, src.getRaster(), (InterleavedF32)dst);
						}
					} else {
						ConvertRaster.bufferedToInterleaved(src, (InterleavedF32)dst);
					}
				} else if (buffer.getDataType() == DataBuffer.TYPE_INT) {
					ConvertRaster.bufferedToInterleaved((DataBufferInt)buffer, src.getRaster(), (InterleavedF32)dst);
				} else {
					ConvertRaster.bufferedToInterleaved(src, (InterleavedF32)dst);
				}
			} else {
				throw new IllegalArgumentException("Data type not supported yet");
			}
		} catch (java.security.AccessControlException e) {
			// force the number of bands to be something valid
			if (dst.getNumBands() != 3 || dst.getNumBands() != 1) {
				dst.setNumberOfBands(3);
			}
			dst.reshape(src.getWidth(), src.getHeight());

			// Applets don't allow access to the raster()
			if (dst instanceof InterleavedU8) {
				ConvertRaster.bufferedToInterleaved(src, (InterleavedU8)dst);
			} else if (dst instanceof InterleavedF32) {
				ConvertRaster.bufferedToInterleaved(src, (InterleavedF32)dst);
			} else {
				throw new IllegalArgumentException("Unsupported dst image type");
			}
		}

		// if requested, ensure the ordering of the bands
		if (orderRgb) {
			orderBandsIntoRGB(dst, src);
		}
	}

	/**
	 * <p>
	 * Converts an image into a BufferedImage. The best way to think of this function is that it's a mindless
	 * typecast. If you don't provide an output image then it will create one. However there isn't always a direct
	 * equivalent between a BoofCV image and BufferedImage internal type. A "reasonable" choice will be made, but
	 * for your application it might not be a good choice.
	 * </p>
	 *
	 * @param src Input image. Pixels must have a value from 0 to 255.
	 * @param dst Where the converted image is written to. If null a new image is created. See comment above about type.
	 * @param orderRgb If applicable, should it change the order of the color bands (assumed RGB or ARGB) into the
	 * order based on BufferedImage.TYPE. Most of the time you want this to be true.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo( ImageBase src, @Nullable BufferedImage dst, boolean orderRgb ) {
		if (src instanceof ImageGray) {
			if (GrayU8.class == src.getClass()) {
				return convertTo((GrayU8)src, dst);
			} else if (GrayI16.class.isInstance(src)) {
				return convertTo((GrayI16)src, dst);
			} else if (GrayF32.class == src.getClass()) {
				return convertTo((GrayF32)src, dst);
			} else {
				throw new IllegalArgumentException("ImageGray type is not yet supported: " + src.getClass().getSimpleName());
			}
		} else if (src instanceof Planar) {
			Planar ms = (Planar)src;

			if (ms.getNumBands() == 1) {
				return convertTo(ms.getBand(0), dst, orderRgb);
			} else if (GrayU8.class == ms.getBandType()) {
				return convertTo_U8((Planar<GrayU8>)ms, dst, orderRgb);
			} else if (GrayF32.class == ms.getBandType()) {
				return convertTo_F32((Planar<GrayF32>)ms, dst, orderRgb);
			} else {
				throw new IllegalArgumentException("Planar type is not yet supported: " + ms.getBandType().getSimpleName());
			}
		} else if (src instanceof ImageInterleaved) {
			if (InterleavedU8.class == src.getClass()) {
				return convertTo((InterleavedU8)src, dst, orderRgb);
			} else if (InterleavedF32.class == src.getClass()) {
				return convertTo((InterleavedF32)src, dst, orderRgb);
			} else {
				throw new IllegalArgumentException("ImageGray type is not yet supported: " + src.getClass().getSimpleName());
			}
		}

		throw new IllegalArgumentException("Image type is not yet supported: " + src.getClass().getSimpleName());
	}

	/**
	 * Converts a {@link GrayU8} into a BufferedImage. If the buffered image
	 * has multiple channels then the input image is copied into each channel.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to. If null a new image is created.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo( GrayU8 src, @Nullable BufferedImage dst ) {
		dst = checkInputs(src, dst);

		DataBuffer buffer = dst.getRaster().getDataBuffer();
		try {
			if (buffer.getDataType() == DataBuffer.TYPE_BYTE && isKnownByteFormat(dst)) {
				ConvertRaster.grayToBuffered(src, (DataBufferByte)buffer, dst.getRaster());
			} else if (buffer.getDataType() == DataBuffer.TYPE_INT) {
				ConvertRaster.grayToBuffered(src, (DataBufferInt)buffer, dst.getRaster());
			} else {
				ConvertRaster.grayToBuffered(src, dst);
			}
			// hack so that it knows the buffer has been modified
			dst.setRGB(0, 0, dst.getRGB(0, 0));
		} catch (java.security.AccessControlException e) {
			ConvertRaster.grayToBuffered(src, dst);
		}

		return dst;
	}

	/**
	 * Converts a {@link GrayI16} into a BufferedImage. If the buffered image
	 * has multiple channels then the input image is copied into each channel.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to. If null a new image is created.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo( GrayI16 src, @Nullable BufferedImage dst ) {
		dst = checkInputs(src, dst);

		DataBuffer buffer = dst.getRaster().getDataBuffer();
		try {
			if (buffer.getDataType() == DataBuffer.TYPE_BYTE && isKnownByteFormat(dst)) {
				ConvertRaster.grayToBuffered(src, (DataBufferByte)buffer, dst.getRaster());
			} else if (buffer.getDataType() == DataBuffer.TYPE_INT) {
				ConvertRaster.grayToBuffered(src, (DataBufferInt)buffer, dst.getRaster());
			} else if (buffer.getDataType() == DataBuffer.TYPE_USHORT) {
				ConvertRaster.grayToBuffered(src, (DataBufferUShort)buffer, dst.getRaster());
			} else {
				ConvertRaster.grayToBuffered(src, dst);
			}
			// hack so that it knows the buffer has been modified
			dst.setRGB(0, 0, dst.getRGB(0, 0));
		} catch (java.security.AccessControlException e) {
			ConvertRaster.grayToBuffered(src, dst);
		}

		return dst;
	}

	/**
	 * Converts the buffered image into an {@link GrayF32}. If the buffered image
	 * has multiple channels then the input image is copied into each channel. The floating
	 * point image is assumed to be between 0 and 255.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to. If null a new image is created.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo( GrayF32 src, @Nullable BufferedImage dst ) {
		dst = checkInputs(src, dst);

		DataBuffer buffer = dst.getRaster().getDataBuffer();
		try {
			if (buffer.getDataType() == DataBuffer.TYPE_BYTE && isKnownByteFormat(dst)) {
				ConvertRaster.grayToBuffered(src, (DataBufferByte)buffer, dst.getRaster());
			} else if (buffer.getDataType() == DataBuffer.TYPE_INT) {
				ConvertRaster.grayToBuffered(src, (DataBufferInt)buffer, dst.getRaster());
			} else {
				ConvertRaster.grayToBuffered(src, dst);
			}
			// hack so that it knows the buffer has been modified
			dst.setRGB(0, 0, dst.getRGB(0, 0));
		} catch (java.security.AccessControlException e) {
			ConvertRaster.grayToBuffered(src, dst);
		}

		return dst;
	}

	/**
	 * Converts a {@link Planar} {@link GrayU8} into a BufferedImage.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to. If null a new image is created.
	 * @param orderRgb If applicable, should it change the order of the color bands (assumed RGB or ARGB) into the
	 * order based on BufferedImage.TYPE. Most of the time you want this to be true.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo_U8( Planar<GrayU8> src, @Nullable BufferedImage dst, boolean orderRgb ) {
		dst = checkInputs(src, dst);

		if (orderRgb) {
			src = orderBandsIntoBuffered(src, dst);
		}

		DataBuffer buffer = dst.getRaster().getDataBuffer();
		try {
			if (buffer.getDataType() == DataBuffer.TYPE_BYTE && isKnownByteFormat(dst)) {
				ConvertRaster.planarToBuffered_U8(src, (DataBufferByte)buffer, dst.getRaster());
			} else if (buffer.getDataType() == DataBuffer.TYPE_INT) {
				ConvertRaster.planarToBuffered_U8(src, (DataBufferInt)buffer, dst.getRaster());
			} else {
				ConvertRaster.planarToBuffered_U8(src, dst);
			}
			// hack so that it knows the buffer has been modified
			dst.setRGB(0, 0, dst.getRGB(0, 0));
		} catch (java.security.AccessControlException e) {
			ConvertRaster.planarToBuffered_U8(src, dst);
		}

		return dst;
	}

	/**
	 * Converts a {@link Planar} {@link GrayF32} into a BufferedImage.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to. If null a new image is created.
	 * @param orderRgb If applicable, should it change the order of the color bands (assumed RGB or ARGB) into the
	 * order based on BufferedImage.TYPE. Most of the time you want this to be true.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo_F32( Planar<GrayF32> src, @Nullable BufferedImage dst, boolean orderRgb ) {
		dst = checkInputs(src, dst);

		if (orderRgb) {
			src = orderBandsIntoBuffered(src, dst);
		}

		DataBuffer buffer = dst.getRaster().getDataBuffer();
		ConvertRaster.planarToBuffered_F32(src, buffer, dst);

		return dst;
	}

	public static BufferedImage convertTo( InterleavedU8 src, @Nullable BufferedImage dst, boolean orderRgb ) {
		dst = checkInputs(src, dst);

		DataBuffer buffer = dst.getRaster().getDataBuffer();
		try {
			if (buffer.getDataType() == DataBuffer.TYPE_BYTE && isKnownByteFormat(dst)) {
				ConvertRaster.interleavedToBuffered(src, (DataBufferByte)buffer, dst.getRaster());
				if (orderRgb)
					ConvertRaster.orderBandsBufferedFromRGB((DataBufferByte)buffer, dst.getRaster(), dst.getType());
			} else if (buffer.getDataType() == DataBuffer.TYPE_INT) {
				ConvertRaster.interleavedToBuffered(src, (DataBufferInt)buffer, dst.getRaster());
				if (orderRgb)
					ConvertRaster.orderBandsBufferedFromRGB((DataBufferInt)buffer, dst.getRaster(), dst.getType());
			} else {
				ConvertRaster.interleavedToBuffered(src, dst);
			}
			// hack so that it knows the buffer has been modified
			dst.setRGB(0, 0, dst.getRGB(0, 0));
		} catch (java.security.AccessControlException e) {
			ConvertRaster.interleavedToBuffered(src, dst);
		}

		return dst;
	}

	public static BufferedImage convertTo( InterleavedF32 src, @Nullable BufferedImage dst, boolean orderRgb ) {
		dst = checkInputs(src, dst);

		DataBuffer buffer = dst.getRaster().getDataBuffer();
		try {
			if (buffer.getDataType() == DataBuffer.TYPE_BYTE && isKnownByteFormat(dst)) {
				ConvertRaster.interleavedToBuffered(src, (DataBufferByte)buffer, dst.getRaster());
				if (orderRgb)
					ConvertRaster.orderBandsBufferedFromRGB((DataBufferByte)buffer, dst.getRaster(), dst.getType());
			} else if (buffer.getDataType() == DataBuffer.TYPE_INT) {
				ConvertRaster.interleavedToBuffered(src, (DataBufferInt)buffer, dst.getRaster());
				if (orderRgb)
					ConvertRaster.orderBandsBufferedFromRGB((DataBufferInt)buffer, dst.getRaster(), dst.getType());
			} else {
				ConvertRaster.interleavedToBuffered(src, dst);
			}
			// hack so that it knows the buffer has been modified
			dst.setRGB(0, 0, dst.getRGB(0, 0));
		} catch (java.security.AccessControlException e) {
			ConvertRaster.interleavedToBuffered(src, dst);
		}

		return dst;
	}

	/**
	 * If null the dst is declared, otherwise it checks to see if the 'dst' as the same shape as 'src'.
	 */
	public static BufferedImage checkInputs( ImageBase src, @Nullable BufferedImage dst ) {
		if (dst != null) {
			if (dst.getWidth() != src.getWidth() || dst.getHeight() != src.getHeight()) {
				throw new IllegalArgumentException("Shapes do not match: " +
						"src = ( " + src.width + " , " + src.height + " )  " +
						"dst = ( " + dst.getWidth() + " , " + dst.getHeight() + " )");
			}
		} else {
			if (src instanceof GrayI8)
				dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			else if (src instanceof GrayF)
				// no good equivalent. Just assume the image is a regular gray scale image
				// with pixel values from 0 to 255
				dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
//			throw new RuntimeException("Fail!");
			else if (src instanceof GrayI)
				// no good equivalent. I'm giving it the biggest pixel for the range
				dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_USHORT_GRAY);
			else
				dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
		}
		return dst;
	}

	/**
	 * Draws the component into a BufferedImage.
	 *
	 * @param comp The component being drawn into an image.
	 * @param storage if not null the component is drawn into it, if null a new BufferedImage is created.
	 * @return image of the component
	 */
	public static BufferedImage convertTo( JComponent comp, @Nullable BufferedImage storage ) {
		if (storage == null)
			storage = new BufferedImage(comp.getWidth(), comp.getHeight(), BufferedImage.TYPE_INT_RGB);

		Graphics2D g2 = storage.createGraphics();

		comp.paintComponents(g2);

		return storage;
	}

	/**
	 * Returns a new image with the color bands in the appropriate ordering. The returned image will
	 * reference the original image's image arrays.
	 */
	public static Planar orderBandsIntoBuffered( Planar src, BufferedImage dst ) {
		// see if no change is required
		if (dst.getType() == BufferedImage.TYPE_INT_RGB)
			return src;

		Planar tmp = new Planar(src.type, src.getNumBands());
		tmp.width = src.width;
		tmp.height = src.height;
		tmp.stride = src.stride;
		tmp.startIndex = src.startIndex;
		for (int i = 0; i < src.getNumBands(); i++) {
			tmp.bands[i] = src.bands[i];
		}
		ConvertRaster.orderBandsBufferedFromRgb(tmp, dst);
		return tmp;
	}

	/**
	 * Checks to see if the input image is a subImage().
	 */
	public static boolean isSubImage( BufferedImage img ) {
		return img.getRaster().getParent() != null;
	}
}
