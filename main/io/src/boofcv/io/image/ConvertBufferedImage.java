/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;
import sun.awt.image.ShortInterleavedRaster;
import sun.awt.image.SunWritableRaster;

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
	 * Returns an image which doesn't have an alpha channel.  If the input image doesn't have an alpha
	 * channel to start then its returned as is.  Otherwise a new image is created and the RGB channels are
	 * copied and the new image returned.
	 *
	 * @param image Input image
	 * @return Image without an alpha channel
	 */
	public static BufferedImage stripAlphaChannel( BufferedImage image ) {
		int numBands = image.getRaster().getNumBands();

		if( numBands == 4 ) {
			BufferedImage output = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_RGB);
			output.createGraphics().drawImage(image,0,0,null);
			return output;
		} else {
			return image;
		}
	}

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
			ret.setNumBands( raster.getNumBands() );
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
	public static GrayU8 extractGrayU8(BufferedImage img) {
		if (img.getRaster() instanceof ByteInterleavedRaster &&
				img.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {

			ByteInterleavedRaster raster = (ByteInterleavedRaster) img.getRaster();
			if (raster.getNumBands() != 1)
				throw new IllegalArgumentException("Input image has more than one channel");
			GrayU8 ret = new GrayU8();

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
	 * GrayU8.  The returned BufferedImage will be of type TYPE_BYTE_GRAY.
	 * </p>
	 * <p/>
	 * <p>
	 * NOTE: This only works on images which are not subimages!
	 * </p>
	 *
	 * @param img Input image who's data will be wrapped by the returned BufferedImage.
	 * @return BufferedImage which shared data with the input image.
	 */
	public static BufferedImage extractBuffered(GrayU8 img) {
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
		if( dst instanceof ImageGray) {
			ImageGray sb = (ImageGray)dst;
			convertFromSingle(src, sb, (Class<ImageGray>) sb.getClass());
		} else if( dst instanceof Planar) {
			Planar ms = (Planar)dst;
			convertFromMulti(src,ms,orderRgb,ms.getBandType());
		} else if( dst instanceof ImageInterleaved ) {
			convertFromInterleaved(src, (ImageInterleaved) dst, orderRgb);
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
	public static <T extends ImageBase> T convertFrom(BufferedImage src , boolean orderRgb , ImageType<T> imageType) {

		T out = imageType.createImage(src.getWidth(),src.getHeight());

		switch( imageType.getFamily() ) {
			case GRAY:
				convertFromSingle(src, (ImageGray)out, imageType.getImageClass());
				break;

			case PLANAR:
				convertFromMulti(src, (Planar) out, orderRgb, imageType.getImageClass());
				break;

			case INTERLEAVED:
				convertFromInterleaved(src, (ImageInterleaved) out, orderRgb);
				break;

			default:
				throw new RuntimeException("Not supported yet");
		}

		return out;
	}

	public static <T extends ImageBase> T convertFrom(BufferedImage src , boolean orderRgb , T output ) {

		ImageType<T> imageType = output.getImageType();

		switch( imageType.getFamily() ) {
			case GRAY:
				convertFromSingle(src, (ImageGray)output, imageType.getImageClass());
				break;

			case PLANAR:
				convertFromMulti(src, (Planar) output, orderRgb, imageType.getImageClass());
				break;

			case INTERLEAVED:
				convertFromInterleaved(src, (ImageInterleaved) output, orderRgb);
				break;

			default:
				throw new RuntimeException("Not supported yet");
		}

		return output;
	}

	/**
	 * Converts a buffered image into an image of the specified type.  In a 'dst' image is provided
	 * it will be used for output, otherwise a new image will be created.
	 */
	public static <T extends ImageGray> T convertFromSingle(BufferedImage src, T dst, Class<T> type) {
		if (type == GrayU8.class) {
			return (T) convertFrom(src, (GrayU8) dst);
		} else if( GrayI16.class.isAssignableFrom(type) ) {
			return (T) convertFrom(src, (GrayI16) dst,(Class)type);
		} else if (type == GrayF32.class) {
			return (T) convertFrom(src, (GrayF32) dst);
		} else {
			throw new IllegalArgumentException("Unknown type " + type);
		}
	}

	/**
	 * Converts the buffered image into an {@link GrayU8}.  If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new unsigned image is created.
	 * @return Converted image.
	 */
	public static GrayU8 convertFrom(BufferedImage src, GrayU8 dst) {
		if (dst != null) {
			if (src.getWidth() != dst.getWidth() || src.getHeight() != dst.getHeight()) {
				throw new IllegalArgumentException("image dimension are different");
			}
		} else {
			dst = new GrayU8(src.getWidth(), src.getHeight());
		}

		try {
			if (src.getRaster() instanceof ByteInterleavedRaster ) {
				if( src.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {
					ConvertRaster.bufferedToGray((ByteInterleavedRaster) src.getRaster(), dst);
				} else {
					ConvertRaster.bufferedToGray(src, dst);
				}
			} else if (src.getRaster() instanceof IntegerInterleavedRaster) {
				ConvertRaster.bufferedToGray((IntegerInterleavedRaster) src.getRaster(), dst);
			} else if( src.getRaster().getClass() == SunWritableRaster.class ) {
				ConvertRaster.bufferedToGray((SunWritableRaster) src.getRaster(), dst);
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
	 * Converts the buffered image into an {@link GrayI16}.  If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new unsigned image is created.
	 * @return Converted image.
	 */
	public static <T extends GrayI16>T convertFrom(BufferedImage src, T dst , Class<T> type ) {
		if (dst != null) {
			if (src.getWidth() != dst.getWidth() || src.getHeight() != dst.getHeight()) {
				throw new IllegalArgumentException("image dimension are different");
			}
		} else {
			dst = GeneralizedImageOps.createSingleBand(type, src.getWidth(), src.getHeight());
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
	 * Converts the buffered image into an {@link GrayF32}.  If the buffered image
	 * has multiple channels the intensities of each channel are averaged together.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new unsigned image is created.
	 * @return Converted image.
	 */
	public static GrayF32 convertFrom(BufferedImage src, GrayF32 dst) {
		if (dst != null) {
			if (src.getWidth() != dst.getWidth() || src.getHeight() != dst.getHeight()) {
				String difference = "src = "+src.getWidth()+"x"+src.getHeight()+"  dst = "+dst.getWidth()+"x"+dst.getHeight();
				throw new IllegalArgumentException("image dimension are different. "+difference);
			}
		} else {
			dst = new GrayF32(src.getWidth(), src.getHeight());
		}

		try {
			if (src.getRaster() instanceof ByteInterleavedRaster ) {
				if( src.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {
					ConvertRaster.bufferedToGray((ByteInterleavedRaster) src.getRaster(), dst);
				} else {
					ConvertRaster.bufferedToGray(src, dst);
				}
			} else if (src.getRaster() instanceof IntegerInterleavedRaster) {
				ConvertRaster.bufferedToGray((IntegerInterleavedRaster) src.getRaster(), dst);
			} else if( src.getRaster().getClass() == SunWritableRaster.class ) {
				ConvertRaster.bufferedToGray((SunWritableRaster) src.getRaster(), dst);
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
	 * Converts the buffered image into an {@link Planar} image of the specified
	 * type. 
	 *
	 * @param src Input image. Not modified.
	 * @param dst Output. The converted image is written to.  If null a new unsigned image is created.
	 * @param orderRgb If applicable, should it adjust the ordering of each color band to maintain color consistency.
	 *                 Most of the time you want this to be true.
	 * @param type Which type of data structure is each band. (GrayU8 or GrayF32)
	 * @return Converted image.
	 */
	public static <T extends ImageGray> Planar<T>
	convertFromMulti(BufferedImage src, Planar<T> dst , boolean orderRgb , Class<T> type )
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
				dst = new Planar<>(type, src.getWidth(), src.getHeight(), numBands);
			else if( dst.getNumBands() != numBands )
				throw new IllegalArgumentException("Expected "+numBands+" bands in dst not "+dst.getNumBands());

			if( type == GrayU8.class ) {
				if (src.getRaster() instanceof ByteInterleavedRaster &&
						src.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {
					if( src.getType() == BufferedImage.TYPE_BYTE_GRAY)  {
						for( int i = 0; i < dst.getNumBands(); i++ )
							ConvertRaster.bufferedToGray(src, ((Planar<GrayU8>) dst).getBand(i));
					} else {
						ConvertRaster.bufferedToMulti_U8((ByteInterleavedRaster) src.getRaster(), (Planar<GrayU8>) dst);
					}
				} else if (src.getRaster() instanceof IntegerInterleavedRaster) {
					ConvertRaster.bufferedToMulti_U8((IntegerInterleavedRaster) src.getRaster(), (Planar<GrayU8>) dst);
				} else {
					ConvertRaster.bufferedToMulti_U8(src, (Planar<GrayU8>) dst);
				}
			} else if( type == GrayF32.class ) {
				if (src.getRaster() instanceof ByteInterleavedRaster &&
						src.getType() != BufferedImage.TYPE_BYTE_INDEXED  ) {
					if( src.getType() == BufferedImage.TYPE_BYTE_GRAY)  {
						for( int i = 0; i < dst.getNumBands(); i++ )
							ConvertRaster.bufferedToGray(src,((Planar<GrayF32>)dst).getBand(i));
					} else {
						ConvertRaster.bufferedToMulti_F32((ByteInterleavedRaster) src.getRaster(), (Planar<GrayF32>) dst);
					}
				} else if (src.getRaster() instanceof IntegerInterleavedRaster) {
					ConvertRaster.bufferedToMulti_F32((IntegerInterleavedRaster) src.getRaster(), (Planar<GrayF32>) dst);
				} else {
					ConvertRaster.bufferedToMulti_F32(src, (Planar<GrayF32>) dst);
				}
			} else {
				throw new IllegalArgumentException("Band type not supported yet");
			}

		} catch( java.security.AccessControlException e) {
			// Applets don't allow access to the raster()
			if( dst == null )
				dst = new Planar<>(type, src.getWidth(), src.getHeight(), 3);

			if( type == GrayU8.class ) {
				ConvertRaster.bufferedToMulti_U8(src, (Planar<GrayU8>) dst);
			} else if( type == GrayF32.class ) {
				ConvertRaster.bufferedToMulti_F32(src, (Planar<GrayF32>)dst);
			}
		}

		// if requested, ensure the ordering of the bands
		if( orderRgb ) {
			orderBandsIntoRGB(dst,src);
		}

		return dst;
	}

	public static void convertFromInterleaved(BufferedImage src, ImageInterleaved dst, boolean orderRgb)
	{
		if (src == null)
			throw new IllegalArgumentException("src is null!");

		if (src.getWidth() != dst.getWidth() || src.getHeight() != dst.getHeight()) {
			throw new IllegalArgumentException("image dimension are different");
		}

		try {
			WritableRaster raster = src.getRaster();

			int numBands;
			if( src.getType() == BufferedImage.TYPE_BYTE_INDEXED )
				numBands = 3;
			else
				numBands = raster.getNumBands();

			if( dst.getNumBands() != numBands )
				throw new IllegalArgumentException("Expected "+numBands+" bands in dst not "+dst.getNumBands());

			if( dst instanceof InterleavedU8 ) {
				if (src.getRaster() instanceof ByteInterleavedRaster ){
					if(src.getType() != BufferedImage.TYPE_BYTE_INDEXED) {
						if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) {
							ConvertRaster.bufferedToGray(src, (InterleavedU8) dst);
						} else {
							ConvertRaster.bufferedToInterleaved((ByteInterleavedRaster) src.getRaster(), (InterleavedU8) dst);
						}
					} else {
						ConvertRaster.bufferedToInterleaved(src, (InterleavedU8) dst);
					}
				} else if (src.getRaster() instanceof IntegerInterleavedRaster) {
					ConvertRaster.bufferedToInterleaved((IntegerInterleavedRaster) src.getRaster(), (InterleavedU8) dst);
				} else if (src.getRaster() instanceof SunWritableRaster) {
					ConvertRaster.bufferedToInterleaved((SunWritableRaster) src.getRaster(), (InterleavedU8) dst);
				} else {
					ConvertRaster.bufferedToInterleaved(src, (InterleavedU8) dst);
				}
			} else if( dst instanceof InterleavedF32 ) {
				if (src.getRaster() instanceof ByteInterleavedRaster ) {
					if(src.getType() != BufferedImage.TYPE_BYTE_INDEXED) {
						if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) {
							ConvertRaster.bufferedToGray(src, (InterleavedF32) dst);
						} else {
							ConvertRaster.bufferedToInterleaved((ByteInterleavedRaster) src.getRaster(), (InterleavedF32) dst);
						}
					} else {
						ConvertRaster.bufferedToInterleaved(src, (InterleavedF32) dst);
					}
				} else if (src.getRaster() instanceof IntegerInterleavedRaster) {
					ConvertRaster.bufferedToInterleaved((IntegerInterleavedRaster) src.getRaster(), (InterleavedF32) dst);
				} else if (src.getRaster() instanceof SunWritableRaster) {
					ConvertRaster.bufferedToInterleaved((SunWritableRaster) src.getRaster(), (InterleavedF32) dst);
				} else {
					ConvertRaster.bufferedToInterleaved(src, (InterleavedF32) dst);
				}
			} else {
				throw new IllegalArgumentException("Data type not supported yet");
			}

		} catch( java.security.AccessControlException e) {
			// Applets don't allow access to the raster()
			if( dst instanceof InterleavedU8 ) {
				ConvertRaster.bufferedToInterleaved(src, (InterleavedU8) dst);
			} else if( dst instanceof InterleavedF32 ) {
				ConvertRaster.bufferedToInterleaved(src, (InterleavedF32) dst);
			} else {
				throw new IllegalArgumentException("Unsupported dst image type");
			}
		}

		// if requested, ensure the ordering of the bands
		if( orderRgb ) {
			orderBandsIntoRGB(dst,src);
		}
	}

	/**
	 * <p>
	 * Converts an image into a BufferedImage. The best way to think of this function is that it's a mindless
	 * typecast.  If you don't provide an output image then it will create one.  However there isn't always a direct
	 * equivalent between a BoofCV image and BufferedImage internal type.  A "reasonable" choice will be made, but
	 * for your application it might not be a good choice.
	 * </p>
	 *
	 * @param src Input image.  Pixels must have a value from 0 to 255.
	 * @param dst Where the converted image is written to.  If null a new image is created.  See comment above about type.
	 * @param orderRgb If applicable, should it change the order of the color bands (assumed RGB or ARGB) into the
	 *                 order based on BufferedImage.TYPE. Most of the time you want this to be true.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo( ImageBase src, BufferedImage dst, boolean orderRgb ) {
		if( src instanceof ImageGray) {
			if( GrayU8.class == src.getClass() ) {
				return convertTo((GrayU8)src,dst);
			} else if( GrayI16.class.isInstance(src) ) {
				return convertTo((GrayI16)src,dst);
			} else if( GrayF32.class == src.getClass() ) {
				return convertTo((GrayF32)src,dst);
			} else {
				throw new IllegalArgumentException("ImageGray type is not yet supported: "+src.getClass().getSimpleName());
			}
		} else if( src instanceof Planar) {
			Planar ms = (Planar)src;

			if( GrayU8.class == ms.getBandType() ) {
				return convertTo_U8((Planar<GrayU8>) ms, dst, orderRgb);
			} else if( GrayF32.class == ms.getBandType() ) {
				return convertTo_F32((Planar<GrayF32>) ms, dst, orderRgb);
			} else {
				throw new IllegalArgumentException("Planar type is not yet supported: "+ ms.getBandType().getSimpleName());
			}
		} else if( src instanceof ImageInterleaved ) {
			if( InterleavedU8.class == src.getClass() ) {
				return convertTo((InterleavedU8)src,dst,orderRgb);
			} else if( InterleavedF32.class == src.getClass() ) {
				return convertTo((InterleavedF32)src,dst,orderRgb);
			} else {
				throw new IllegalArgumentException("ImageGray type is not yet supported: "+src.getClass().getSimpleName());
			}
		}

		throw new IllegalArgumentException("Image type is not yet supported: "+src.getClass().getSimpleName());
	}

	/**
	 * Converts a {@link GrayU8} into a BufferedImage.  If the buffered image
	 * has multiple channels then the input image is copied into each channel.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new image is created.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo(GrayU8 src, BufferedImage dst) {
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
	 * Converts a {@link GrayI16} into a BufferedImage.  If the buffered image
	 * has multiple channels then the input image is copied into each channel.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new image is created.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo(GrayI16 src, BufferedImage dst) {
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
	 * Converts the buffered image into an {@link GrayF32}.  If the buffered image
	 * has multiple channels then the input image is copied into each channel.  The floating
	 * point image is assumed to be between 0 and 255.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new image is created.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo(GrayF32 src, BufferedImage dst) {
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
	 * Converts a {@link Planar} {@link GrayU8} into a BufferedImage.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new image is created.
	 * @param orderRgb If applicable, should it change the order of the color bands (assumed RGB or ARGB) into the
	 *                 order based on BufferedImage.TYPE. Most of the time you want this to be true.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo_U8(Planar<GrayU8> src, BufferedImage dst, boolean orderRgb ) {
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
	 * Converts a {@link Planar} {@link GrayF32} into a BufferedImage.
	 *
	 * @param src Input image.
	 * @param dst Where the converted image is written to.  If null a new image is created.
	 * @param orderRgb If applicable, should it change the order of the color bands (assumed RGB or ARGB) into the
	 *                 order based on BufferedImage.TYPE. Most of the time you want this to be true.
	 * @return Converted image.
	 */
	public static BufferedImage convertTo_F32(Planar<GrayF32> src, BufferedImage dst, boolean orderRgb) {
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

	public static BufferedImage convertTo( InterleavedU8 src, BufferedImage dst, boolean orderRgb ) {
		dst = checkInputs(src, dst);

		try {
			if (dst.getRaster() instanceof ByteInterleavedRaster &&
					dst.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {
				ConvertRaster.interleavedToBuffered(src, (ByteInterleavedRaster) dst.getRaster());
				if( orderRgb )
					orderBandsBufferedFromRGB((ByteInterleavedRaster)dst.getRaster(),dst.getType());
			} else if (dst.getRaster() instanceof IntegerInterleavedRaster) {
				ConvertRaster.interleavedToBuffered(src, (IntegerInterleavedRaster) dst.getRaster());
				if( orderRgb )
					orderBandsBufferedFromRGB((IntegerInterleavedRaster) dst.getRaster(), dst.getType());
			} else {
				ConvertRaster.interleavedToBuffered(src, dst);
			}
			// hack so that it knows the buffer has been modified
			dst.setRGB(0,0,dst.getRGB(0,0));
		} catch( java.security.AccessControlException e) {
			ConvertRaster.interleavedToBuffered(src, dst);
		}

		return dst;
	}

	public static BufferedImage convertTo( InterleavedF32 src, BufferedImage dst, boolean orderRgb ) {
		dst = checkInputs(src, dst);

		try {
			if (dst.getRaster() instanceof ByteInterleavedRaster &&
					dst.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {
				ConvertRaster.interleavedToBuffered(src, (ByteInterleavedRaster) dst.getRaster());
				if( orderRgb )
					orderBandsBufferedFromRGB((ByteInterleavedRaster)dst.getRaster(),dst.getType());
			} else if (dst.getRaster() instanceof IntegerInterleavedRaster) {
				ConvertRaster.interleavedToBuffered(src, (IntegerInterleavedRaster) dst.getRaster());
				if( orderRgb )
					orderBandsBufferedFromRGB((IntegerInterleavedRaster) dst.getRaster(), dst.getType());
			} else {
				ConvertRaster.interleavedToBuffered(src, dst);
			}
			// hack so that it knows the buffer has been modified
			dst.setRGB(0,0,dst.getRGB(0,0));
		} catch( java.security.AccessControlException e) {
			ConvertRaster.interleavedToBuffered(src, dst);
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
			if( GrayI8.class.isInstance(src))
				dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			else if( GrayF.class.isInstance(src) )
				// no good equivalent.  Just assume the image is a regular gray scale image
				// with pixel values from 0 to 255
				dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
//			throw new RuntimeException("Fail!");
			else if( GrayI.class.isInstance(src))
				// no good equivalent.  I'm giving it the biggest pixel for the range
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
	public static Planar orderBandsIntoBuffered(Planar src, BufferedImage dst) {
		// see if no change is required
		if( dst.getType() == BufferedImage.TYPE_INT_RGB )
			return src;

		Planar tmp = new Planar(src.type, src.getNumBands());
		tmp.width = src.width;
		tmp.height = src.height;
		tmp.stride = src.stride;
		tmp.startIndex = src.startIndex;
		for( int i = 0; i < src.getNumBands(); i++ ) {
			tmp.bands[i] = src.bands[i];
		}
		orderBandsBufferedFromRgb(tmp, dst);
		return tmp;
	}

	/**
	 * The image the BufferedImage was created from had RGB or RGBA color order.  This swaps the bytes around
	 * to put it into the expected local format
	 */
	public static void orderBandsBufferedFromRGB( ByteInterleavedRaster raster , int type ) {
		int height = raster.getHeight();
		int width = raster.getWidth();
		int stride = raster.getScanlineStride();
		int offset = raster.getDataOffset(0)-raster.getPixelStride()+1;
		byte data[] = raster.getDataStorage();

		if( BufferedImage.TYPE_3BYTE_BGR == type ) {
			for (int y = 0; y < height; y++) {
				int index = offset + y*stride;
				for (int x = 0; x < width; x++) {
					byte tmp = data[index];
					data[index] = data[index+2];
					data[index+2] = tmp;
					index += 3;
				}
			}
		} else if( BufferedImage.TYPE_4BYTE_ABGR == type ) {
			for (int y = 0; y < height; y++) {
				int index = offset + y*stride;
				for (int x = 0; x < width; x++) {
					byte tmp0 = data[index];
					byte tmp1 = data[index+1];
					data[index] = data[index+3];
					data[index+1] = data[index+2];
					data[index+2] = tmp1;
					data[index+3] = tmp0;
					index += 4;
				}
			}
		} else {
			throw new IllegalArgumentException("Unsupported buffered image type");
		}
	}

	/**
	 * The image the BufferedImage was created from had RGB or RGBA color order.  This swaps the bytes around
	 * to put it into the expected local format
	 */
	public static void orderBandsBufferedFromRGB( IntegerInterleavedRaster raster , int type ) {
		if( BufferedImage.TYPE_INT_RGB == type )
			return;

		int height = raster.getHeight();
		int width = raster.getWidth();
		int stride = raster.getScanlineStride();
		int offset = raster.getDataOffset(0)-raster.getPixelStride()+1;
		int data[] = raster.getDataStorage();

		if( BufferedImage.TYPE_INT_BGR == type ) {
			for (int y = 0; y < height; y++) {
				int index = offset + y*stride;
				for (int x = 0; x < width; x++, index++) {
					int tmp = data[index];
					int c1 = tmp & 0xFF;
					int c2 = (tmp >> 8) & 0xFF;
					int c3 = (tmp >> 16) & 0xFF;

					data[index] = c1 << 16 | c2 << 8 | c3;
				}
			}
		} else if( BufferedImage.TYPE_INT_ARGB == type ) {
			for (int y = 0; y < height; y++) {
				int index = offset + y*stride;
				for (int x = 0; x < width; x++, index++) {
					int tmp = data[index];
					int c1 = tmp & 0xFF;
					int c2 = (tmp >> 8) & 0xFF;
					int c3 = (tmp >> 16) & 0xFF;
					int c4 = (tmp >> 24) & 0xFF;

					data[index] = c1 << 24 | c4 << 16 | c3 << 8 | c2;
				}
			}
		} else {
			throw new IllegalArgumentException("Unsupported buffered image type");
		}
	}

	/**
	 * If a Planar was created from a BufferedImage its colors might not be in the expected order.
	 * Invoking this function ensures that the image will have the expected ordering.  For images with
	 * 3 bands it will be RGB and for 4 bands it will be ARGB.
	 */
	public static <T extends ImageGray>
	void orderBandsIntoRGB(Planar<T> image , BufferedImage input ) {

		boolean swap = swapBandOrder(input);

		// Output formats are: RGB and RGBA

		if( swap ) {
			if( image.getNumBands() == 3 ) {
				int bufferedImageType = input.getType();
				if( bufferedImageType == BufferedImage.TYPE_3BYTE_BGR ||
						bufferedImageType == BufferedImage.TYPE_INT_BGR )
				{
					T tmp = image.getBand(0);
					image.bands[0] = image.getBand(2);
					image.bands[2] = tmp;
				}
			} else if( image.getNumBands() == 4 ) {
				T[] temp = (T[])Array.newInstance(image.getBandType(),4);

				int bufferedImageType = input.getType();
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

	public static <T extends ImageGray>
	void orderBandsBufferedFromRgb(Planar<T> image, BufferedImage input) {

		boolean swap = swapBandOrder(input);

		// Output formats are: RGB and RGBA

		if( swap ) {
			if( image.getNumBands() == 3 ) {
				int bufferedImageType = input.getType();
				if( bufferedImageType == BufferedImage.TYPE_3BYTE_BGR ||
						bufferedImageType == BufferedImage.TYPE_INT_BGR ) {
					T tmp = image.getBand(0);
					image.bands[0] = image.getBand(2);
					image.bands[2] = tmp;
				}
			} else if( image.getNumBands() == 4 ) {
				T[] temp = (T[])Array.newInstance(image.getBandType(),4);

				int bufferedImageType = input.getType();
				if( bufferedImageType == BufferedImage.TYPE_INT_ARGB ) {
					temp[0] = image.getBand(3);
					temp[1] = image.getBand(0);
					temp[2] = image.getBand(1);
					temp[3] = image.getBand(2);
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

	public static void orderBandsIntoRGB( ImageInterleaved image , BufferedImage input ) {
		if( image instanceof InterleavedU8 ) {
			orderBandsIntoRGB((InterleavedU8)image,input);
		} else if( image instanceof InterleavedF32 ) {
			orderBandsIntoRGB((InterleavedF32)image,input);
		} else {
			throw new IllegalArgumentException("Unsupported interleaved type");
		}
	}

	public static void orderBandsIntoRGB( InterleavedU8 image , BufferedImage input ) {
		boolean swap = swapBandOrder(input);

		// Output formats are: RGB and RGBA

		if( swap ) {
			if( image.getNumBands() == 3 ) {
				for (int y = 0; y < image.height; y++) {
					int index = image.startIndex + y*image.stride;
					int indexEnd = index + image.width*3;

					while( index < indexEnd ) {
						byte tmp = image.data[index+2];
						image.data[index+2] = image.data[index];
						image.data[index] = tmp;

						index += 3;
					}
				}

			} else if( image.getNumBands() == 4 ) {
				int bufferedImageType = input.getType();
				if( bufferedImageType == BufferedImage.TYPE_INT_ARGB ) {
					for (int y = 0; y < image.height; y++) {
						int index = image.startIndex + y*image.stride;
						int indexEnd = index + image.width*3;

						while( index < indexEnd ) {
							byte tmp = image.data[index];
							image.data[index] = image.data[index+1];
							image.data[index+1] = image.data[index+2];
							image.data[index+2] = image.data[index+3];
							image.data[index+3] = tmp;

							index += 4;
						}
					}
				} else if( bufferedImageType == BufferedImage.TYPE_4BYTE_ABGR ) {
					for (int y = 0; y < image.height; y++) {
						int index = image.startIndex + y*image.stride;
						int indexEnd = index + image.width*3;

						while( index < indexEnd ) {
							byte tmp1 = image.data[index+1];
							byte tmp0 = image.data[index];
							image.data[index] = image.data[index+3];
							image.data[index+1] = image.data[index+2];
							image.data[index+2] = tmp1;
							image.data[index+3] = tmp0;

							index += 4;
						}
					}
				}
			}
		}
	}

	public static void orderBandsIntoRGB( InterleavedF32 image , BufferedImage input ) {
		boolean swap = swapBandOrder(input);

		// Output formats are: RGB and RGBA

		if( swap ) {
			if( image.getNumBands() == 3 ) {
				for (int y = 0; y < image.height; y++) {
					int index = image.startIndex + y*image.stride;
					int indexEnd = index + image.width*3;

					while( index < indexEnd ) {
						float tmp = image.data[index+2];
						image.data[index+2] = image.data[index];
						image.data[index] = tmp;

						index += 3;
					}
				}

			} else if( image.getNumBands() == 4 ) {
				int bufferedImageType = input.getType();
				if( bufferedImageType == BufferedImage.TYPE_INT_ARGB ) {
					for (int y = 0; y < image.height; y++) {
						int index = image.startIndex + y*image.stride;
						int indexEnd = index + image.width*3;

						while( index < indexEnd ) {
							float tmp = image.data[index];
							image.data[index] = image.data[index+1];
							image.data[index+1] = image.data[index+2];
							image.data[index+2] = image.data[index+3];
							image.data[index+3] = tmp;

							index += 4;
						}
					}
				} else if( bufferedImageType == BufferedImage.TYPE_4BYTE_ABGR ) {
					for (int y = 0; y < image.height; y++) {
						int index = image.startIndex + y*image.stride;
						int indexEnd = index + image.width*3;

						while( index < indexEnd ) {
							float tmp1 = image.data[index+1];
							float tmp0 = image.data[index];
							image.data[index] = image.data[index+3];
							image.data[index+1] = image.data[index+2];
							image.data[index+2] = tmp1;
							image.data[index+3] = tmp0;

							index += 4;
						}
					}
				}
			}
		}
	}

	private static boolean swapBandOrder(BufferedImage input) {
		boolean swap = false;

		// see if access to the raster is restricted or not
		try {
			WritableRaster raster = input.getRaster();
			if( raster instanceof ByteInterleavedRaster) {
				((ByteInterleavedRaster)raster).getDataStorage();
			} else if( raster instanceof IntegerInterleavedRaster) {
				((IntegerInterleavedRaster)raster).getDataStorage();
			}

			int bufferedImageType = input.getType();
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
		return swap;
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
