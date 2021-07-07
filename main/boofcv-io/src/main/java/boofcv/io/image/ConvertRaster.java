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

import boofcv.concurrency.BoofConcurrency;
import boofcv.io.image.impl.ImplConvertRaster;
import boofcv.io.image.impl.ImplConvertRaster_MT;
import boofcv.struct.image.*;

import java.awt.image.*;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Routines for converting to and from {@link BufferedImage} that use its internal
 * raster for better performance.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ConvertRaster {

	public static void bufferedToGray(DataBufferUShort buffer , WritableRaster src, GrayI16 dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.bufferedToGray(buffer,src,dst);
		} else {
			ImplConvertRaster.bufferedToGray(buffer,src,dst);
		}
	}

	public static void bufferedToGray(BufferedImage src, GrayI16 dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.bufferedToGray(src,dst);
		} else {
			ImplConvertRaster.bufferedToGray(src,dst);
		}
	}

	public static void bufferedToGray(DataBufferInt buffer, WritableRaster src, GrayF32 dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.bufferedToGray(buffer,src,dst);
		} else {
			ImplConvertRaster.bufferedToGray(buffer,src,dst);
		}
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToGray(DataBufferByte buffer, WritableRaster src, GrayU8 dst) {
		byte[] srcData = buffer.getData();


		int numBands = src.getNumBands();

		int size = dst.getWidth() * dst.getHeight();

		int srcStride = stride(src);
		int srcOffset = getOffset(src);
		int srcStrideDiff = srcStride-src.getNumDataElements()*dst.width;

		if(BoofConcurrency.USE_CONCURRENT ) {
			if (numBands == 3) {
				ImplConvertRaster_MT.from_3BU8_to_U8(srcData, srcStride, srcOffset, dst);
			} else if (numBands == 1) {
				ImplConvertRaster_MT.from_1BU8_to_U8(srcData, size, srcStride, srcOffset, srcStrideDiff, dst);
			} else if (numBands == 4) {
				ImplConvertRaster_MT.from_4BU8_to_U8(srcData, srcStride, srcOffset, dst);
			} else {
				throw new RuntimeException("Unexpected number of bands found. Bands = " + numBands);
			}
		} else {
			if (numBands == 3) {
				ImplConvertRaster.from_3BU8_to_U8(srcData, srcStride, srcOffset, dst);
			} else if (numBands == 1) {
				ImplConvertRaster.from_1BU8_to_U8(srcData, size, srcStride, srcOffset, srcStrideDiff, dst);
			} else if (numBands == 4) {
				ImplConvertRaster.from_4BU8_to_U8(srcData, srcStride, srcOffset, dst);
			} else {
				throw new RuntimeException("Unexpected number of bands found. Bands = " + numBands);
			}
		}
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToGray(DataBufferByte buffer, WritableRaster src, GrayF32 dst) {
		byte[] srcData = buffer.getData();

		int numBands = src.getNumBands();

		int srcStride = stride(src);
		int srcOffset = getOffset(src);

		if(BoofConcurrency.USE_CONCURRENT ) {
			if (numBands == 3) {
				ImplConvertRaster_MT.from_3BU8_to_F32(srcData, srcStride, srcOffset, dst);
			} else if (numBands == 1) {
				ImplConvertRaster_MT.from_1BU8_to_F32(srcData, srcStride, srcOffset, dst);
			} else if (numBands == 4) {
				ImplConvertRaster_MT.from_4BU8_to_F32(srcData, srcStride, srcOffset, dst);
			} else {
				throw new RuntimeException("Write more code here.");
			}
		} else {
			if (numBands == 3) {
				ImplConvertRaster.from_3BU8_to_F32(srcData, srcStride, srcOffset, dst);
			} else if (numBands == 1) {
				ImplConvertRaster.from_1BU8_to_F32(srcData, srcStride, srcOffset, dst);
			} else if (numBands == 4) {
				ImplConvertRaster.from_4BU8_to_F32(srcData, srcStride, srcOffset, dst);
			} else {
				throw new RuntimeException("Write more code here.");
			}
		}
	}

	public static int stride(WritableRaster raster) {
		while( raster.getWritableParent() != null ) {
			raster = raster.getWritableParent();
		}
		return raster.getWidth()*raster.getNumDataElements();
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	static void bufferedToPlanar_U8(DataBufferByte buffer , WritableRaster src, Planar<GrayU8> dst) {
		byte[] srcData = buffer.getData();

		int numBands = src.getNumBands();

		int srcStride = stride(src);
		int srcOffset = getOffset(src);

		if(BoofConcurrency.USE_CONCURRENT ) {
			if (numBands == 3) {
				ImplConvertRaster_MT.from_3BU8_to_PLU8(srcData, srcStride, srcOffset, dst);
			} else if (numBands == 1) {
				ImplConvertRaster_MT.from_1BU8_to_PLU8(srcData, srcStride, srcOffset, dst);
			} else if (numBands == 4) {
				ImplConvertRaster_MT.from_4BU8_to_PLU8(srcData, srcStride, srcOffset, dst);
			} else {
				throw new RuntimeException("Write more code here.");
			}
		} else {
			if (numBands == 3) {
				ImplConvertRaster.from_3BU8_to_PLU8(srcData, srcStride, srcOffset, dst);
			} else if (numBands == 1) {
				ImplConvertRaster.from_1BU8_to_PLU8(srcData, srcStride, srcOffset, dst);
			} else if (numBands == 4) {
				ImplConvertRaster.from_4BU8_to_PLU8(srcData, srcStride, srcOffset, dst);
			} else {
				throw new RuntimeException("Write more code here.");
			}
		}
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	static void bufferedToPlanar_F32(DataBufferByte buffer, WritableRaster src, Planar<GrayF32> dst) {
		byte[] srcData = buffer.getData();

		int numBands = src.getNumBands();

		int srcStride = stride(src);
		int srcOffset = getOffset(src);

		if(BoofConcurrency.USE_CONCURRENT ) {
			if (numBands == 3) {
				ImplConvertRaster_MT.from_3BU8_to_PLF32(srcData, srcStride, srcOffset, dst);
			} else if (numBands == 1) {
				ImplConvertRaster_MT.from_1BU8_to_PLF32(srcData, srcStride, srcOffset, dst);
			} else if (numBands == 4) {
				ImplConvertRaster_MT.from_4BU8_to_PLF32(srcData, srcStride, srcOffset, dst);
			} else {
				throw new RuntimeException("Write more code here.");
			}
		} else {
			if (numBands == 3) {
				ImplConvertRaster.from_3BU8_to_PLF32(srcData, srcStride, srcOffset, dst);
			} else if (numBands == 1) {
				ImplConvertRaster.from_1BU8_to_PLF32(srcData, srcStride, srcOffset, dst);
			} else if (numBands == 4) {
				ImplConvertRaster.from_4BU8_to_PLF32(srcData, srcStride, srcOffset, dst);
			} else {
				throw new RuntimeException("Write more code here.");
			}
		}
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void planarToBuffered_F32(Planar<GrayF32> src, DataBuffer buffer, BufferedImage dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			try {
				if (buffer.getDataType() == DataBuffer.TYPE_BYTE && isKnownByteFormat(dst)) {
					ImplConvertRaster_MT.planarToBuffered_F32(src, (DataBufferByte) buffer, dst.getRaster());
				} else if (buffer.getDataType() == DataBuffer.TYPE_INT) {
					ImplConvertRaster_MT.planarToBuffered_F32(src, (DataBufferInt) buffer, dst.getRaster());
				} else {
					ImplConvertRaster_MT.planarToBuffered_F32(src, dst);
				}
				// hack so that it knows the buffer has been modified
				dst.setRGB(0, 0, dst.getRGB(0, 0));
			} catch (java.security.AccessControlException e) {
				ImplConvertRaster_MT.planarToBuffered_F32(src, dst);
			}
		} else {
			try {
				if (buffer.getDataType() == DataBuffer.TYPE_BYTE && isKnownByteFormat(dst)) {
					ImplConvertRaster.planarToBuffered_F32(src, (DataBufferByte) buffer, dst.getRaster());
				} else if (buffer.getDataType() == DataBuffer.TYPE_INT) {
					ImplConvertRaster.planarToBuffered_F32(src, (DataBufferInt) buffer, dst.getRaster());
				} else {
					ImplConvertRaster.planarToBuffered_F32(src, dst);
				}
				// hack so that it knows the buffer has been modified
				dst.setRGB(0, 0, dst.getRGB(0, 0));
			} catch (java.security.AccessControlException e) {
				ImplConvertRaster.planarToBuffered_F32(src, dst);
			}
		}
	}

	public static void interleavedToBuffered(InterleavedU8 src, DataBuffer buffer, BufferedImage dst, boolean orderRgb) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			try {
				if (buffer.getDataType() == DataBuffer.TYPE_BYTE && isKnownByteFormat(dst)) {
					ImplConvertRaster_MT.interleavedToBuffered(src, (DataBufferByte) buffer, dst.getRaster());
					if (orderRgb)
						ImplConvertRaster_MT.orderBandsBufferedFromRGB((DataBufferByte) buffer, dst.getRaster(), dst.getType());
				} else if (buffer.getDataType() == DataBuffer.TYPE_INT) {
					ImplConvertRaster_MT.interleavedToBuffered(src, (DataBufferInt) buffer, dst.getRaster());
					if (orderRgb)
						ImplConvertRaster_MT.orderBandsBufferedFromRGB((DataBufferInt) buffer, dst.getRaster(), dst.getType());
				} else {
					ImplConvertRaster_MT.interleavedToBuffered(src, dst);
				}
				// hack so that it knows the buffer has been modified
				dst.setRGB(0, 0, dst.getRGB(0, 0));
			} catch (java.security.AccessControlException e) {
				ImplConvertRaster_MT.interleavedToBuffered(src, dst);
			}
		} else {
			try {
				if (buffer.getDataType() == DataBuffer.TYPE_BYTE && isKnownByteFormat(dst)) {
					ImplConvertRaster.interleavedToBuffered(src, (DataBufferByte) buffer, dst.getRaster());
					if (orderRgb)
						ImplConvertRaster.orderBandsBufferedFromRGB((DataBufferByte) buffer, dst.getRaster(), dst.getType());
				} else if (buffer.getDataType() == DataBuffer.TYPE_INT) {
					ImplConvertRaster.interleavedToBuffered(src, (DataBufferInt) buffer, dst.getRaster());
					if (orderRgb)
						ImplConvertRaster.orderBandsBufferedFromRGB((DataBufferInt) buffer, dst.getRaster(), dst.getType());
				} else {
					ImplConvertRaster.interleavedToBuffered(src, dst);
				}
				// hack so that it knows the buffer has been modified
				dst.setRGB(0, 0, dst.getRGB(0, 0));
			} catch (java.security.AccessControlException e) {
				ImplConvertRaster.interleavedToBuffered(src, dst);
			}
		}
	}

	public static int getOffset( WritableRaster raster ) {

		if( raster.getWritableParent() == null )
			return 0;

		try {
			Method m = raster.getClass().getMethod("getDataOffset",int.class);
			int min = Integer.MAX_VALUE;
			for (int i = 0; i < raster.getNumDataElements(); i++) {
				min = Math.min(min,(Integer)m.invoke(raster,i));
			}
			return min;

		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalArgumentException("BufferedImage subimages are not supported in Java 9 and beyond");
		}
	}

	/**
	 * If a Planar was created from a BufferedImage its colors might not be in the expected order.
	 * Invoking this function ensures that the image will have the expected ordering. For images with
	 * 3 bands it will be RGB and for 4 bands it will be ARGB.
	 */
	public static <T extends ImageGray<T>>
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
				T[] temp = (T[]) Array.newInstance(image.getBandType(),4);

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

	public static <T extends ImageGray<T>>
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

	public static void bufferedToGray(BufferedImage src, float[] data, int dstStartIndex , int dstStride ) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.bufferedToGray(src, data, dstStartIndex, dstStride);
		} else {
			ImplConvertRaster.bufferedToGray(src, data, dstStartIndex, dstStride);
		}
	}

	public static void bufferedToGray(BufferedImage src, byte[] data, int dstStartIndex , int dstStride ) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.bufferedToGray(src, data, dstStartIndex, dstStride);
		} else {
			ImplConvertRaster.bufferedToGray(src, data, dstStartIndex, dstStride);
		}
	}

	public static void bufferedToGray(DataBufferInt buffer, WritableRaster src, GrayU8 dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.bufferedToGray(buffer, src, dst);
		} else {
			ImplConvertRaster.bufferedToGray(buffer, src, dst);
		}
	}

	public static void orderBandsIntoRGB( ImageInterleaved image , BufferedImage input ) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			if (image instanceof InterleavedU8) {
				ImplConvertRaster_MT.orderBandsIntoRGB((InterleavedU8) image, input);
			} else if (image instanceof InterleavedF32) {
				ImplConvertRaster_MT.orderBandsIntoRGB((InterleavedF32) image, input);
			} else {
				throw new IllegalArgumentException("Unsupported interleaved type");
			}
		} else {
			if (image instanceof InterleavedU8) {
				ImplConvertRaster.orderBandsIntoRGB((InterleavedU8) image, input);
			} else if (image instanceof InterleavedF32) {
				ImplConvertRaster.orderBandsIntoRGB((InterleavedF32) image, input);
			} else {
				throw new IllegalArgumentException("Unsupported interleaved type");
			}
		}
	}

	public static boolean swapBandOrder(BufferedImage input) {
		// see if access to the raster is restricted or not
		int bufferedImageType = input.getType();
		return bufferedImageType == BufferedImage.TYPE_3BYTE_BGR ||
				bufferedImageType == BufferedImage.TYPE_INT_BGR ||
				bufferedImageType == BufferedImage.TYPE_INT_ARGB ||
				bufferedImageType == BufferedImage.TYPE_4BYTE_ABGR;
	}

	public static void interleavedToBuffered( InterleavedU8 src, BufferedImage dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.interleavedToBuffered(src,dst);
		} else {
			ImplConvertRaster.interleavedToBuffered(src,dst);
		}
	}

	public static void orderBandsBufferedFromRGB( DataBufferByte buffer , WritableRaster raster , int type ) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.orderBandsBufferedFromRGB(buffer,raster,type);
		} else {
			ImplConvertRaster.orderBandsBufferedFromRGB(buffer,raster,type);
		}
	}

	public static void orderBandsBufferedFromRGB( DataBufferInt buffer, WritableRaster raster , int type ) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.orderBandsBufferedFromRGB(buffer,raster,type);
		} else {
			ImplConvertRaster.orderBandsBufferedFromRGB(buffer,raster,type);
		}
	}

	public static void interleavedToBuffered(InterleavedU8 src, DataBufferByte buffer , WritableRaster dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.interleavedToBuffered(src,buffer,dst);
		} else {
			ImplConvertRaster.interleavedToBuffered(src,buffer,dst);
		}
	}

	public static void interleavedToBuffered(InterleavedU8 src, DataBufferInt buffer, WritableRaster dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.interleavedToBuffered(src,buffer,dst);
		} else {
			ImplConvertRaster.interleavedToBuffered(src,buffer,dst);
		}
	}

	public static void interleavedToBuffered(InterleavedF32 src, DataBufferByte buffer , WritableRaster dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.interleavedToBuffered(src,buffer,dst);
		} else {
			ImplConvertRaster.interleavedToBuffered(src,buffer,dst);
		}
	}

	public static void interleavedToBuffered(InterleavedF32 src, DataBufferInt buffer, WritableRaster dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.interleavedToBuffered(src,buffer,dst);
		} else {
			ImplConvertRaster.interleavedToBuffered(src,buffer,dst);
		}
	}
	public static void interleavedToBuffered( InterleavedF32 src, BufferedImage dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.interleavedToBuffered(src,dst);
		} else {
			ImplConvertRaster.interleavedToBuffered(src,dst);
		}
	}

	public static void planarToBuffered_U8(Planar<GrayU8> src, DataBufferByte buffer , WritableRaster dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.planarToBuffered_U8(src,buffer,dst);
		} else {
			ImplConvertRaster.planarToBuffered_U8(src,buffer,dst);
		}
	}

	public static void planarToBuffered_U8(Planar<GrayU8> src, DataBufferInt buffer, WritableRaster dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.planarToBuffered_U8(src,buffer,dst);
		} else {
			ImplConvertRaster.planarToBuffered_U8(src,buffer,dst);
		}
	}

	public static void planarToBuffered_U8(Planar<GrayU8> src, BufferedImage dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.planarToBuffered_U8(src,dst);
		} else {
			ImplConvertRaster.planarToBuffered_U8(src,dst);
		}
	}

	public static void grayToBuffered(GrayF32 src, DataBufferByte buffer , WritableRaster dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.grayToBuffered(src,buffer,dst);
		} else {
			ImplConvertRaster.grayToBuffered(src,buffer,dst);
		}
	}

	public static void grayToBuffered(GrayF32 src, DataBufferInt buffer, WritableRaster dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.grayToBuffered(src,buffer,dst);
		} else {
			ImplConvertRaster.grayToBuffered(src,buffer,dst);
		}
	}

	public static void grayToBuffered(GrayF32 src, BufferedImage dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.grayToBuffered(src,dst);
		} else {
			ImplConvertRaster.grayToBuffered(src,dst);
		}
	}

	public static void bufferedToPlanar_U8(DataBufferInt buffer, WritableRaster src, Planar<GrayU8> dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.bufferedToPlanar_U8(buffer,src,dst);
		} else {
			ImplConvertRaster.bufferedToPlanar_U8(buffer,src,dst);
		}
	}

	public static void bufferedToPlanar_U8(BufferedImage src, Planar<GrayU8> dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.bufferedToPlanar_U8(src,dst);
		} else {
			ImplConvertRaster.bufferedToPlanar_U8(src,dst);
		}
	}

	public static void bufferedToPlanar_F32(DataBufferInt buffer, WritableRaster src, Planar<GrayF32> dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.bufferedToPlanar_F32(buffer,src,dst);
		} else {
			ImplConvertRaster.bufferedToPlanar_F32(buffer,src,dst);
		}
	}

	public static void bufferedToPlanar_F32(BufferedImage src, Planar<GrayF32> dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.bufferedToPlanar_F32(src,dst);
		} else {
			ImplConvertRaster.bufferedToPlanar_F32(src,dst);
		}
	}

	public static void bufferedToInterleaved(DataBufferByte buffer, WritableRaster src, InterleavedU8 dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.bufferedToInterleaved(buffer,src,dst);
		} else {
			ImplConvertRaster.bufferedToInterleaved(buffer,src,dst);
		}
	}

	public static void bufferedToInterleaved(BufferedImage src, InterleavedU8 dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.bufferedToInterleaved(src,dst);
		} else {
			ImplConvertRaster.bufferedToInterleaved(src,dst);
		}
	}

	public static void bufferedToInterleaved(DataBufferInt buffer, WritableRaster src, InterleavedU8 dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.bufferedToInterleaved(buffer,src,dst);
		} else {
			ImplConvertRaster.bufferedToInterleaved(buffer,src,dst);
		}
	}

	public static void bufferedToInterleaved(DataBufferByte buffer, WritableRaster src, InterleavedF32 dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.bufferedToInterleaved(buffer,src,dst);
		} else {
			ImplConvertRaster.bufferedToInterleaved(buffer,src,dst);
		}
	}

	public static void bufferedToInterleaved(BufferedImage src, InterleavedF32 dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.bufferedToInterleaved(src,dst);
		} else {
			ImplConvertRaster.bufferedToInterleaved(src,dst);
		}
	}

	public static void bufferedToInterleaved(DataBufferInt buffer, WritableRaster src, InterleavedF32 dst ) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.bufferedToInterleaved(buffer,src,dst);
		} else {
			ImplConvertRaster.bufferedToInterleaved(buffer,src,dst);
		}
	}

	public static void grayToBuffered(GrayU8 src, DataBufferByte buffer , WritableRaster dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.grayToBuffered(src,buffer,dst);
		} else {
			ImplConvertRaster.grayToBuffered(src,buffer,dst);
		}
	}

	public static void grayToBuffered(GrayU8 src, DataBufferInt buffer, WritableRaster dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.grayToBuffered(src,buffer,dst);
		} else {
			ImplConvertRaster.grayToBuffered(src,buffer,dst);
		}
	}

	public static void grayToBuffered(GrayU8 src, BufferedImage dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.grayToBuffered(src,dst);
		} else {
			ImplConvertRaster.grayToBuffered(src,dst);
		}
	}
	public static void grayToBuffered(GrayI16 src, DataBufferByte buffer , WritableRaster dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.grayToBuffered(src,buffer,dst);
		} else {
			ImplConvertRaster.grayToBuffered(src,buffer,dst);
		}
	}

	public static void grayToBuffered(GrayI16 src, DataBufferUShort buffer , WritableRaster dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.grayToBuffered(src,buffer,dst);
		} else {
			ImplConvertRaster.grayToBuffered(src,buffer,dst);
		}
	}

	public static void grayToBuffered(GrayI16 src, DataBufferInt buffer, WritableRaster dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.grayToBuffered(src,buffer,dst);
		} else {
			ImplConvertRaster.grayToBuffered(src,buffer,dst);
		}
	}

	public static void grayToBuffered(GrayI16 src, BufferedImage dst) {
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertRaster_MT.grayToBuffered(src,dst);
		} else {
			ImplConvertRaster.grayToBuffered(src,dst);
		}
	}


	/**
	 * Checks to see if it is a known byte format
	 */
	public static boolean isKnownByteFormat( BufferedImage image ) {
		int type = image.getType();
		return type != BufferedImage.TYPE_BYTE_INDEXED &&
				type != BufferedImage.TYPE_BYTE_BINARY &&
				type != BufferedImage.TYPE_CUSTOM;
	}

//	public static int getOffset( ByteComponentRaster raster ) {
//		int min = Integer.MAX_VALUE;
//		for (int i = 0; i < raster.getNumDataElements(); i++) {
//			min = Math.min(raster.getDataOffset(i),min);
//		}
//		return min;
//	}
//
//	public static int getOffset( IntegerInterleavedRaster raster ) {
//		int min = Integer.MAX_VALUE;
//		for (int i = 0; i < raster.getNumDataElements(); i++) {
//			min = Math.min(raster.getDataOffset(i),min);
//		}
//		return min;
//	}
//
//	public static int getOffset( ShortInterleavedRaster raster ) {
//		int min = Integer.MAX_VALUE;
//		for (int i = 0; i < raster.getNumDataElements(); i++) {
//			min = Math.min(raster.getDataOffset(i),min);
//		}
//		return min;
//	}
}
