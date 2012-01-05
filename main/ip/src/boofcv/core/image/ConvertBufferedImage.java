/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
	public static ImageUInt8 extractImageInt8(BufferedImage img) {
		if (img.getRaster() instanceof ByteInterleavedRaster) {
			ByteInterleavedRaster raster = (ByteInterleavedRaster) img.getRaster();
			if (raster.getNumBands() != 1)
				throw new IllegalArgumentException("Input image has more than one channel");
			ImageUInt8 ret = new ImageUInt8();

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
	 * Converts a buffered image into an image of the specified type.  In a 'dst' image is provided
	 * it will be used for output.
	 * 
	 * @param src Input BufferedImage which is to be converted
	 * @param dst The image which it is being converted into   
	 */
	public static <T extends ImageBase> void convertFrom(BufferedImage src, T dst ) {
		if( dst instanceof ImageSingleBand ) {
			ImageSingleBand sb = (ImageSingleBand)dst;
			convertFromSingle(src, sb, (Class<ImageSingleBand>) sb.getClass());
		} else if( dst instanceof MultiSpectral ) {
			MultiSpectral ms = (MultiSpectral)dst;
			convertFromMulti(src,ms,ms.getType());
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
			if (src.getRaster() instanceof ByteInterleavedRaster) {
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
					src.getType() == BufferedImage.TYPE_BYTE_GRAY ) {
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
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new unsigned image is created.
	 * @return Converted image.
	 */
	public static <T extends ImageSingleBand> MultiSpectral<T>
	convertFromMulti(BufferedImage src, MultiSpectral<T> dst , Class<T> type )
	{
		if (dst != null) {
			if (src.getWidth() != dst.getWidth() || src.getHeight() != dst.getHeight()) {
				throw new IllegalArgumentException("image dimension are different");
			}
		} else {
			dst = new MultiSpectral<T>(type,src.getWidth(),src.getHeight(),3);
		}

		if( type == ImageUInt8.class ) {
			try {
				if (src.getRaster() instanceof ByteInterleavedRaster &&
						src.getType() != BufferedImage.TYPE_BYTE_GRAY ) {
					ConvertRaster.bufferedToMulti_U8((ByteInterleavedRaster) src.getRaster(), (MultiSpectral<ImageUInt8>)dst);
				} else if (src.getRaster() instanceof IntegerInterleavedRaster) {
					ConvertRaster.bufferedToMulti_U8((IntegerInterleavedRaster) src.getRaster(), (MultiSpectral<ImageUInt8>)dst);
				} else {
					ConvertRaster.bufferedToMulti_U8(src, (MultiSpectral<ImageUInt8>)dst);
				}
			} catch( java.security.AccessControlException e) {
				// Applets don't allow access to the raster()
				ConvertRaster.bufferedToMulti_U8(src, (MultiSpectral<ImageUInt8>)dst);
			}
		} else if( type == ImageFloat32.class ) {
			try {
				if (src.getRaster() instanceof ByteInterleavedRaster &&
						src.getType() != BufferedImage.TYPE_BYTE_GRAY ) {
					ConvertRaster.bufferedToMulti_F32((ByteInterleavedRaster) src.getRaster(), (MultiSpectral<ImageFloat32>)dst);
				} else if (src.getRaster() instanceof IntegerInterleavedRaster) {
					ConvertRaster.bufferedToMulti_F32((IntegerInterleavedRaster) src.getRaster(), (MultiSpectral<ImageFloat32>)dst);
				} else {
					ConvertRaster.bufferedToMulti_F32(src, (MultiSpectral<ImageFloat32>)dst);
				}
			} catch( java.security.AccessControlException e) {
				// Applets don't allow access to the raster()
				ConvertRaster.bufferedToMulti_F32(src, (MultiSpectral<ImageFloat32>)dst);
			}
		} else {
			throw new IllegalArgumentException("Band type not supported yet");
		}

		return dst;
	}

	/**
	 * Converts an image which extends {@link boofcv.struct.image.ImageSingleBand} into a BufferedImage.
	 *
	 * @param src Input image.  Pixels must have a value from 0 to 255.
	 * @param dst Where the converted image is written to.  If null a new image is created.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo( ImageBase src, BufferedImage dst) {
		if( src instanceof ImageSingleBand ) {
			if( ImageUInt8.class == src.getClass() ) {
				return convertTo((ImageUInt8)src,dst);
			} else if( ImageSInt16.class == src.getClass() ) {
				return convertTo((ImageSInt16)src,dst);
			} else if( ImageFloat32.class == src.getClass() ) {
				return convertTo((ImageFloat32)src,dst);
			} else {
				throw new IllegalArgumentException("ImageSingleBand type is not yet supported: "+src.getClass().getSimpleName());
			}
		} else if( src instanceof MultiSpectral ) {
			MultiSpectral ms = (MultiSpectral)src;
			if( ImageUInt8.class == ms.getType() ) {
				return convertTo_U8((MultiSpectral<ImageUInt8>) ms, dst);
			} else if( ImageFloat32.class == ms.getType() ) {
				return convertTo_F32((MultiSpectral<ImageFloat32>) ms, dst);
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
			if (dst.getRaster() instanceof ByteInterleavedRaster) {
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
	 * Converts a {@link boofcv.struct.image.ImageSInt16} into a BufferedImage.  If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new image is created.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo(ImageSInt16 src, BufferedImage dst) {
		dst = checkInputs(src, dst);

		try {
			if (dst.getRaster() instanceof ByteInterleavedRaster) {
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
			if (dst.getRaster() instanceof ByteInterleavedRaster) {
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
	 * @return Converted image.
	 */
	public static BufferedImage convertTo_U8(MultiSpectral<ImageUInt8> src, BufferedImage dst) {
		dst = checkInputs(src, dst);

		try {
			if (dst.getRaster() instanceof ByteInterleavedRaster) {
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
	 * @return Converted image.
	 */
	public static BufferedImage convertTo_F32(MultiSpectral<ImageFloat32> src, BufferedImage dst) {
		dst = checkInputs(src, dst);

		try {
			if (dst.getRaster() instanceof ByteInterleavedRaster) {
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
			dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
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
	 * If a MultiSpectral was created from a BufferedImage its colors might not be in the expected order.
	 * Invoking this function ensures that the image will have the expected ordering.
	 */
	public static <T extends ImageSingleBand>
	void orderBandsIntoRGB( MultiSpectral<T> image , BufferedImage input ) {
		
		boolean swap = false;

		// see if access to the raster is restricted or not
		try {
			WritableRaster raster = input.getRaster();
			if( raster instanceof ByteInterleavedRaster ) {
				((ByteInterleavedRaster)raster).getDataStorage();
			} else if( raster instanceof IntegerInterleavedRaster ) {
				((IntegerInterleavedRaster)raster).getDataStorage();
			}

			int bufferedImageType = input.getType();
			if( bufferedImageType == BufferedImage.TYPE_3BYTE_BGR ||
					bufferedImageType == BufferedImage.TYPE_INT_BGR ) {
				swap = true;
			}
		} catch( java.security.AccessControlException e) {
			// its in an applet or something and will need to use getRGB() to read/write from the image
		}

		
		if( swap ) {
			T[] temp = (T[])Array.newInstance(image.getType(),3);

			temp[0] = image.getBand(2);
			temp[1] = image.getBand(1);
			temp[2] = image.getBand(0);

			image.bands[0] = temp[0];
			image.bands[1] = temp[1];
			image.bands[2] = temp[2];
		}
	}
}
