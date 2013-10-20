/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.core.image;

import boofcv.struct.image.*;
import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;
import sun.awt.image.ShortInterleavedRaster;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.lang.reflect.Array;

/**
 * Functions for converting to and from {@link BufferedImage}.
 *
 * @author Peter Abeles
 */
public class ConvertBufferedImage {

	/**
	 * For BufferedImage stored as a byte array internally it extracts an
	 * interleaved image.  The input image and the returned image will both
	 * share the same internal data array.  Using this function allows unnecessary
	 * memory copying to be avoided.
	 *
	 * @param img Image whose internal data is extracted and wrapped.
	 * @return An image whose internal data is the same as the input image.
	 */
	public static InterleavedU8 extractInterleavedU8(BufferedImage img) {

		if (img.getRaster() instanceof ByteInterleavedRaster &&
				img.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {
			ByteInterleavedRaster raster = (ByteInterleavedRaster) img.getRaster();

			InterleavedU8 ret = new InterleavedU8();

			ret.width = img.getWidth();
			ret.height = img.getHeight();
			ret.stride = raster.getScanlineStride();
			ret.startIndex = raster.getDataOffset(0)-raster.getPixelStride()+1;
			ret.numBands = raster.getNumBands();
			ret.data = raster.getDataStorage();

			return ret;
		}
		throw new IllegalArgumentException("Buffered image does not have an interleaved byte raster");
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
	public static ImageUInt8 extractImageUInt8(BufferedImage img) {
		if (img.getRaster() instanceof ByteInterleavedRaster &&
				img.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {

			ByteInterleavedRaster raster = (ByteInterleavedRaster) img.getRaster();
			if (raster.getNumBands() != 1)
				throw new IllegalArgumentException("Input image has more than one channel");
			ImageUInt8 ret = new ImageUInt8();

			ret.width = img.getWidth();
			ret.height = img.getHeight();
			ret.startIndex = raster.getDataOffset(0);
			ret.stride = raster.getScanlineStride();
			ret.data = raster.getDataStorage();

			return ret;
		}
		throw new IllegalArgumentException("Buffered image does not have a gray scale byte raster");
	}

	/**
	 * Creates a new BufferedImage that internally uses the same data as the provided
	 * {@link InterleavedU8}.  If 3 bands then the image will be of type TYPE_3BYTE_BGR
	 * or if 1 band TYPE_BYTE_GRAY.
	 *
	 * @param img Input image who's data will be wrapped by the returned BufferedImage.
	 * @return BufferedImage which shared data with the input image.
	 */
	public static BufferedImage extractBuffered(InterleavedU8 img) {
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
	 * ImageUInt8.  The returned BufferedImage will be of type TYPE_BYTE_GRAY.
	 * </p>
	 * <p/>
	 * <p>
	 * NOTE: This only works on images which are not subimages!
	 * </p>
	 *
	 * @param img Input image who's data will be wrapped by the returned BufferedImage.
	 * @return BufferedImage which shared data with the input image.
	 */
	public static BufferedImage extractBuffered(ImageUInt8 img) {
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
	 * Converts a buffered image into an image of the specified type.
	 * 
	 * @param src Input BufferedImage which is to be converted
	 * @param dst The image which it is being converted into
	 * @param orderRgb If applicable, should it adjust the ordering of each color band to maintain color consistency
	 */
	public static <T extends ImageBase> void convertFrom(BufferedImage src, T dst , boolean orderRgb) {
		if( dst instanceof ImageSingleBand ) {
			ImageSingleBand sb = (ImageSingleBand)dst;
			convertFromSingle(src, sb, (Class<ImageSingleBand>) sb.getClass());
		} else if( dst instanceof MultiSpectral ) {
			MultiSpectral ms = (MultiSpectral)dst;
			convertFromMulti(src,ms,orderRgb,ms.getType());
		} else {
			throw new IllegalArgumentException("Unknown type " + dst.getClass().getSimpleName());
		}
	}

	/**
	 * Converts a buffered image into an image of the specified type.  In a 'dst' image is provided
	 * it will be used for output, otherwise a new image will be created.
	 */
	public static <T extends ImageSingleBand> T convertFromSingle(BufferedImage src, T dst, Class<T> type) {
		if (type == ImageUInt8.class) {
			return (T) convertFrom(src, (ImageUInt8) dst);
		} else if( ImageInt16.class.isAssignableFrom(type) ) {
			return (T) convertFrom(src, (ImageInt16) dst,(Class)type);
		} else if (type == ImageFloat32.class) {
			return (T) convertFrom(src, (ImageFloat32) dst);
		} else {
			throw new IllegalArgumentException("Unknown type " + type);
		}
	}

	/**
	 * Converts the buffered image into an {@link boofcv.struct.image.ImageUInt8}.  If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new unsigned image is created.
	 * @return Converted image.
	 */
	public static ImageUInt8 convertFrom(BufferedImage src, ImageUInt8 dst) {
		if (dst != null) {
			if (src.getWidth() != dst.getWidth() || src.getHeight() != dst.getHeight()) {
				throw new IllegalArgumentException("image dimension are different");
			}
		} else {
			dst = new ImageUInt8(src.getWidth(), src.getHeight());
		}

		try {
			if (src.getRaster() instanceof ByteInterleavedRaster &&
					src.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {
				ConvertRaster.bufferedToGray((ByteInterleavedRaster) src.getRaster(), dst);
			} else if (src.getRaster() instanceof IntegerInterleavedRaster) {
				ConvertRaster.bufferedToGray((IntegerInterleavedRaster) src.getRaster(), dst);
			} else {
				ConvertRaster.bufferedToGray(src, dst);
			}
		} catch( java.security.AccessControlException e) {
			// Applets don't allow access to the raster()
			ConvertRaster.bufferedToGray(src, dst);
		}

		return dst;
	}

	/**
	 * Converts the buffered image into an {@link boofcv.struct.image.ImageInt16}.  If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new unsigned image is created.
	 * @return Converted image.
	 */
	public static <T extends ImageInt16>T convertFrom(BufferedImage src, T dst , Class<T> type ) {
		if (dst != null) {
			if (src.getWidth() != dst.getWidth() || src.getHeight() != dst.getHeight()) {
				throw new IllegalArgumentException("image dimension are different");
			}
		} else {
			dst = GeneralizedImageOps.createSingleBand(type,src.getWidth(), src.getHeight());
		}

		try {
			if (src.getRaster() instanceof ShortInterleavedRaster ) {
				ConvertRaster.bufferedToGray((ShortInterleavedRaster) src.getRaster(), dst);
				return dst;
			}
		} catch( java.security.AccessControlException e) {}

		// Applets don't allow access to the raster() or the image type wasn't supported
		ConvertRaster.bufferedToGray(src, dst);

		return dst;
	}

	/**
	 * Converts the buffered image into an {@link boofcv.struct.image.ImageFloat32}.  If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new unsigned image is created.
	 * @return Converted image.
	 */
	public static ImageFloat32 convertFrom(BufferedImage src, ImageFloat32 dst) {
		if (dst != null) {
			if (src.getWidth() != dst.getWidth() || src.getHeight() != dst.getHeight()) {
				throw new IllegalArgumentException("image dimension are different");
			}
		} else {
			dst = new ImageFloat32(src.getWidth(), src.getHeight());
		}

		try {
			if (src.getRaster() instanceof ByteInterleavedRaster &&
					src.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {
				ConvertRaster.bufferedToGray((ByteInterleavedRaster) src.getRaster(), dst);
			} else if (src.getRaster() instanceof IntegerInterleavedRaster) {
				ConvertRaster.bufferedToGray((IntegerInterleavedRaster) src.getRaster(), dst);
			} else {
				ConvertRaster.bufferedToGray(src, dst);
			}
		} catch( java.security.AccessControlException e) {
			// Applets don't allow access to the raster()
			ConvertRaster.bufferedToGray(src, dst);
		}

		return dst;
	}

	/**
	 * Converts the buffered image into an {@link boofcv.struct.image.MultiSpectral} image of the specified
	 * type. 
	 *
	 * @param src Input image. Not modified.
	 * @param dst Output. The converted image is written to.  If null a new unsigned image is created.
	 * @param orderRgb If applicable, should it adjust the ordering of each color band to maintain color consistency.
	 *                 Most of the time you want this to be true.
	 * @param type Which type of data structure is each band. (ImageUInt8 or ImageFloat32)
	 * @return Converted image.
	 */
	public static <T extends ImageSingleBand> MultiSpectral<T>
	convertFromMulti(BufferedImage src, MultiSpectral<T> dst , boolean orderRgb , Class<T> type )
	{
		if( src == null )
			throw new IllegalArgumentException("src is null!");

		if (dst != null) {
			if (src.getWidth() != dst.getWidth() || src.getHeight() != dst.getHeight()) {
				throw new IllegalArgumentException("image dimension are different");
			}
		}

		try {
			WritableRaster raster = src.getRaster();

			int numBands;
			if( src.getType() == BufferedImage.TYPE_BYTE_INDEXED )
				numBands = 3;
			else
				numBands = raster.getNumBands();

			if( dst == null)
				dst = new MultiSpectral<T>(type,src.getWidth(),src.getHeight(),numBands);
			else if( dst.getNumBands() != numBands )
				throw new IllegalArgumentException("Expected "+numBands+" bands in dst not "+dst.getNumBands());

			if( type == ImageUInt8.class ) {
				if (src.getRaster() instanceof ByteInterleavedRaster &&
						src.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {
					if( src.getType() == BufferedImage.TYPE_BYTE_GRAY)  {
						for( int i = 0; i < dst.getNumBands(); i++ )
							ConvertRaster.bufferedToGray(src, ((MultiSpectral<ImageUInt8>) dst).getBand(i));
					} else {
						ConvertRaster.bufferedToMulti_U8((ByteInterleavedRaster) src.getRaster(), (MultiSpectral<ImageUInt8>)dst);
					}
				} else if (src.getRaster() instanceof IntegerInterleavedRaster) {
					ConvertRaster.bufferedToMulti_U8((IntegerInterleavedRaster) src.getRaster(), (MultiSpectral<ImageUInt8>)dst);
				} else {
					ConvertRaster.bufferedToMulti_U8(src, (MultiSpectral<ImageUInt8>)dst);
				}
			} else if( type == ImageFloat32.class ) {
				if (src.getRaster() instanceof ByteInterleavedRaster &&
						src.getType() != BufferedImage.TYPE_BYTE_INDEXED  ) {
					if( src.getType() == BufferedImage.TYPE_BYTE_GRAY)  {
						for( int i = 0; i < dst.getNumBands(); i++ )
							ConvertRaster.bufferedToGray(src,((MultiSpectral<ImageFloat32>)dst).getBand(i));
					} else {
						ConvertRaster.bufferedToMulti_F32((ByteInterleavedRaster) src.getRaster(), (MultiSpectral<ImageFloat32>)dst);
					}
				} else if (src.getRaster() instanceof IntegerInterleavedRaster) {
					ConvertRaster.bufferedToMulti_F32((IntegerInterleavedRaster) src.getRaster(), (MultiSpectral<ImageFloat32>)dst);
				} else {
					ConvertRaster.bufferedToMulti_F32(src, (MultiSpectral<ImageFloat32>)dst);
				}
			} else {
				throw new IllegalArgumentException("Band type not supported yet");
			}

		} catch( java.security.AccessControlException e) {
			// Applets don't allow access to the raster()
			if( dst == null )
				dst = new MultiSpectral<T>(type,src.getWidth(),src.getHeight(),3);

			if( type == ImageUInt8.class ) {
				ConvertRaster.bufferedToMulti_U8(src, (MultiSpectral<ImageUInt8>)dst);
			} else if( type == ImageFloat32.class ) {
				ConvertRaster.bufferedToMulti_F32(src, (MultiSpectral<ImageFloat32>)dst);
			}
		}

		// if requested, ensure the ordering of the bands
		if( orderRgb ) {
			orderBandsIntoRGB(dst,src);
		}

		return dst;
	}

	/**
	 * Converts an image into a BufferedImage.
	 *
	 * @param src Input image.  Pixels must have a value from 0 to 255.
	 * @param dst Where the converted image is written to.  If null a new image is created.
	 * @param orderRgb If applicable, should it change the order of the color bands (assumed RGB or ARGB) into the
	 *                 order based on BufferedImage.TYPE. Most of the time you want this to be true.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo( ImageBase src, BufferedImage dst, boolean orderRgb ) {
		if( src instanceof ImageSingleBand ) {
			if( ImageUInt8.class == src.getClass() ) {
				return convertTo((ImageUInt8)src,dst);
			} else if( ImageInt16.class.isInstance(src) ) {
				return convertTo((ImageInt16)src,dst);
			} else if( ImageFloat32.class == src.getClass() ) {
				return convertTo((ImageFloat32)src,dst);
			} else {
				throw new IllegalArgumentException("ImageSingleBand type is not yet supported: "+src.getClass().getSimpleName());
			}
		} else if( src instanceof MultiSpectral ) {
			MultiSpectral ms = (MultiSpectral)src;

			if( ImageUInt8.class == ms.getType() ) {
				return convertTo_U8((MultiSpectral<ImageUInt8>) ms, dst, orderRgb);
			} else if( ImageFloat32.class == ms.getType() ) {
				return convertTo_F32((MultiSpectral<ImageFloat32>) ms, dst, orderRgb);
			} else {
				throw new IllegalArgumentException("MultiSpectral type is not yet supported: "+ ms.getType().getSimpleName());
			}
		}

		throw new IllegalArgumentException("Image type is not yet supported: "+src.getClass().getSimpleName());
	}

	/**
	 * Converts a {@link boofcv.struct.image.ImageUInt8} into a BufferedImage.  If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new image is created.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo(ImageUInt8 src, BufferedImage dst) {
		dst = checkInputs(src, dst);

		try {
			if (dst.getRaster() instanceof ByteInterleavedRaster &&
					dst.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {
				ConvertRaster.grayToBuffered(src, (ByteInterleavedRaster) dst.getRaster());
			} else if (dst.getRaster() instanceof IntegerInterleavedRaster) {
				ConvertRaster.grayToBuffered(src, (IntegerInterleavedRaster) dst.getRaster());
			} else {
				ConvertRaster.grayToBuffered(src, dst);
			}
			// hack so that it knows the buffer has been modified
			dst.setRGB(0,0,dst.getRGB(0,0));
		} catch( java.security.AccessControlException e) {
			ConvertRaster.grayToBuffered(src, dst);
		}

		return dst;
	}

	/**
	 * Converts a {@link boofcv.struct.image.ImageInt16} into a BufferedImage.  If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new image is created.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo(ImageInt16 src, BufferedImage dst) {
		dst = checkInputs(src, dst);

		try {
			if (dst.getRaster() instanceof ByteInterleavedRaster &&
					dst.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {
				ConvertRaster.grayToBuffered(src, (ByteInterleavedRaster) dst.getRaster());
			} else if (dst.getRaster() instanceof IntegerInterleavedRaster) {
				ConvertRaster.grayToBuffered(src, (IntegerInterleavedRaster) dst.getRaster());
			} else if( dst.getType() == BufferedImage.TYPE_USHORT_GRAY ) {
				ConvertRaster.grayToBuffered(src, (ShortInterleavedRaster) dst.getRaster());
			} else {
				ConvertRaster.grayToBuffered(src, dst);
			}
			// hack so that it knows the buffer has been modified
			dst.setRGB(0,0,dst.getRGB(0,0));
		} catch( java.security.AccessControlException e) {
			ConvertRaster.grayToBuffered(src, dst);
		}

		return dst;
	}

	/**
	 * Converts the buffered image into an {@link boofcv.struct.image.ImageFloat32}.  If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.  The floating
	 * point image is assumed to be between 0 and 255.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new image is created.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo(ImageFloat32 src, BufferedImage dst) {
		dst = checkInputs(src, dst);

		try {
			if (dst.getRaster() instanceof ByteInterleavedRaster &&
					dst.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {
				ConvertRaster.grayToBuffered(src, (ByteInterleavedRaster) dst.getRaster());
			} else if (dst.getRaster() instanceof IntegerInterleavedRaster) {
				ConvertRaster.grayToBuffered(src, (IntegerInterleavedRaster) dst.getRaster());
			} else {
				ConvertRaster.grayToBuffered(src, dst);
			}
			// hack so that it knows the buffer has been modified
			dst.setRGB(0,0,dst.getRGB(0,0));
		} catch( java.security.AccessControlException e) {
			ConvertRaster.grayToBuffered(src, dst);
		}

		return dst;
	}

	/**
	 * Converts a {@link boofcv.struct.image.ImageUInt8} into a BufferedImage.  If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new image is created.
	 * @param orderRgb If applicable, should it change the order of the color bands (assumed RGB or ARGB) into the
	 *                 order based on BufferedImage.TYPE. Most of the time you want this to be true.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo_U8(MultiSpectral<ImageUInt8> src, BufferedImage dst, boolean orderRgb ) {
		dst = checkInputs(src, dst);

		if( orderRgb ) {
			src = orderBandsIntoBuffered(src, dst);
		}

		try {
			if (dst.getRaster() instanceof ByteInterleavedRaster &&
					dst.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {
				ConvertRaster.multToBuffered_U8(src, (ByteInterleavedRaster) dst.getRaster());
			} else if (dst.getRaster() instanceof IntegerInterleavedRaster) {
				ConvertRaster.multToBuffered_U8(src, (IntegerInterleavedRaster) dst.getRaster());
			} else {
				ConvertRaster.multToBuffered_U8(src, dst);
			}
			// hack so that it knows the buffer has been modified
			dst.setRGB(0,0,dst.getRGB(0,0));
		} catch( java.security.AccessControlException e) {
			ConvertRaster.multToBuffered_U8(src, dst);
		}

		return dst;
	}

	/**
	 * Converts a {@link boofcv.struct.image.ImageUInt8} into a BufferedImage.  If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new image is created.
	 * @param orderRgb If applicable, should it change the order of the color bands (assumed RGB or ARGB) into the
	 *                 order based on BufferedImage.TYPE. Most of the time you want this to be true.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo_F32(MultiSpectral<ImageFloat32> src, BufferedImage dst, boolean orderRgb) {
		dst = checkInputs(src, dst);

		if( orderRgb ) {
			src = orderBandsIntoBuffered(src, dst);
		}

		try {
			if (dst.getRaster() instanceof ByteInterleavedRaster &&
					dst.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {
				ConvertRaster.multToBuffered_F32(src, (ByteInterleavedRaster) dst.getRaster());
			} else if (dst.getRaster() instanceof IntegerInterleavedRaster) {
				ConvertRaster.multToBuffered_F32(src, (IntegerInterleavedRaster) dst.getRaster());
			} else {
				ConvertRaster.multToBuffered_F32(src, dst);
			}
			// hack so that it knows the buffer has been modified
			dst.setRGB(0,0,dst.getRGB(0,0));
		} catch( java.security.AccessControlException e) {
			ConvertRaster.multToBuffered_F32(src, dst);
		}

		return dst;
	}

	/**
	 * If null the dst is declared, otherwise it checks to see if the 'dst' as the same shape as 'src'.
	 */
	public static BufferedImage checkInputs(ImageBase src, BufferedImage dst) {
		if (dst != null) {
			if (dst.getWidth() != src.getWidth() || dst.getHeight() != src.getHeight()) {
				throw new IllegalArgumentException("image dimension are different");
			}
		} else {
			if( ImageInt16.class.isInstance(src))
				dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_USHORT_GRAY);
			else
				dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
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

	/**
	 * Returns a new image with the color bands in the appropriate ordering.  The returned image will
	 * reference the original image's image arrays.
	 */
	public static MultiSpectral orderBandsIntoBuffered(MultiSpectral src, BufferedImage dst) {
		// see if no change is required
		if( dst.getType() == BufferedImage.TYPE_INT_RGB )
			return src;

		MultiSpectral tmp = new MultiSpectral(src.type, src.getNumBands());
		tmp.width = src.width;
		tmp.height = src.height;
		tmp.stride = src.stride;
		tmp.startIndex = src.startIndex;
		for( int i = 0; i < src.getNumBands(); i++ ) {
			tmp.bands[i] = src.bands[i];
		}
		orderBandsIntoRGB(tmp, dst);
		return tmp;
	}

	/**
	 * If a MultiSpectral was created from a BufferedImage its colors might not be in the expected order.
	 * Invoking this function ensures that the image will have the expected ordering.  For images with
	 * 3 bands it will be RGB and for 4 bands it will be ARGB.
	 */
	public static <T extends ImageSingleBand>
	void orderBandsIntoRGB( MultiSpectral<T> image , BufferedImage input ) {

		int bufferedImageType = -1;
		boolean swap = false;

		// see if access to the raster is restricted or not
		try {
			WritableRaster raster = input.getRaster();
			if( raster instanceof ByteInterleavedRaster ) {
				((ByteInterleavedRaster)raster).getDataStorage();
			} else if( raster instanceof IntegerInterleavedRaster ) {
				((IntegerInterleavedRaster)raster).getDataStorage();
			}

			bufferedImageType = input.getType();
			if( bufferedImageType == BufferedImage.TYPE_3BYTE_BGR ||
					bufferedImageType == BufferedImage.TYPE_INT_BGR ||
					bufferedImageType == BufferedImage.TYPE_INT_ARGB ||
					bufferedImageType == BufferedImage.TYPE_4BYTE_ABGR ) {
				swap = true;
			}
		} catch( java.security.AccessControlException e) {
			// its in an applet or something and will need to use getRGB() to read/write from the image
			// so no need to re-order the bands
		}

		// Output formats are: RGB and RGBA

		if( swap ) {
			if( image.getNumBands() == 3 ) {
				T[] temp = (T[])Array.newInstance(image.getType(),3);

				temp[0] = image.getBand(2);
				temp[1] = image.getBand(1);
				temp[2] = image.getBand(0);

				image.bands[0] = temp[0];
				image.bands[1] = temp[1];
				image.bands[2] = temp[2];
			} else if( image.getNumBands() == 4 ) {
				T[] temp = (T[])Array.newInstance(image.getType(),4);

				if( bufferedImageType == BufferedImage.TYPE_INT_ARGB ) {
					temp[0] = image.getBand(1);
					temp[1] = image.getBand(2);
					temp[2] = image.getBand(3);
					temp[3] = image.getBand(0);
				} else if( bufferedImageType == BufferedImage.TYPE_4BYTE_ABGR ) {
					temp[0] = image.getBand(3);
					temp[1] = image.getBand(2);
					temp[2] = image.getBand(1);
					temp[3] = image.getBand(0);
				}

				image.bands[0] = temp[0];
				image.bands[1] = temp[1];
				image.bands[2] = temp[2];
				image.bands[3] = temp[3];
			}
		}
	}

	/**
	 * Checks to see if the input image is a subImage().
	 * @param img
	 * @return
	 */
	public static boolean isSubImage( BufferedImage img ) {
		return img.getRaster().getParent() != null;
	}
}
