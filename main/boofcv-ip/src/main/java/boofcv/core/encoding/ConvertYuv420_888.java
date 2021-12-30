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

package boofcv.core.encoding;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.color.ColorFormat;
import boofcv.core.encoding.impl.ImplConvertYuv420_888;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_I8;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

import java.nio.ByteBuffer;

/**
 * Functions for converting YUV 420 888 into BoofCV imgae types.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class ConvertYuv420_888 {
	public static byte[] declareWork( int strideY, int strideUV, byte[] work ) {

		int workLength = strideY + 2*strideUV;

		if (work == null || work.length < workLength)
			return new byte[workLength];
		return work;
	}

	@SuppressWarnings({"MissingCasesInEnumSwitch"})
	public static void yuvToBoof( ByteBuffer bufferY, ByteBuffer bufferU, ByteBuffer bufferV,
								  int width, int height, int strideY, int strideUV, int stridePixelUV,
								  ColorFormat colorOutput, ImageBase output,
								  @Nullable GrowArray<DogArray_I8> workArrays ) {
		if (output instanceof GrayU8) {
			yuvToGray(bufferY, width, height, strideY, (GrayU8)output);
			return;
		} else if (output instanceof GrayF32) {
			yuvToGray(bufferY, width, height, strideY, (GrayF32)output, workArrays);
			return;
		} else if (output.getImageType().getFamily() == ImageType.Family.PLANAR) {
			switch (colorOutput) {
				case RGB -> {
					switch (output.getImageType().getDataType()) {
						case U8:
							yuvToPlanarRgbU8(bufferY, bufferU, bufferV, width, height, strideY, strideUV, stridePixelUV, (Planar<GrayU8>)output, workArrays);
							return;
						case F32:
							yuvToPlanarRgbF32(bufferY, bufferU, bufferV, width, height, strideY, strideUV, stridePixelUV, (Planar<GrayF32>)output, workArrays);
							return;
					}
				}
				case YUV -> {
					switch (output.getImageType().getDataType()) {
						case U8:
							yuvToPlanarYuvU8(bufferY, bufferU, bufferV, width, height, strideY, strideUV, stridePixelUV, (Planar<GrayU8>)output, workArrays);
							return;
					}
				}
			}
		} else if (output.getImageType().getFamily() == ImageType.Family.INTERLEAVED) {
			switch (colorOutput) {
				case RGB -> {
					switch (output.getImageType().getDataType()) {
						case U8:
							yuvToInterleavedRgbU8(bufferY, bufferU, bufferV, width, height, strideY, strideUV, stridePixelUV, (InterleavedU8)output, workArrays);
							return;
						case F32:
							yuvToInterleavedRgbF32(bufferY, bufferU, bufferV, width, height, strideY, strideUV, stridePixelUV, (InterleavedF32)output, workArrays);
							return;
					}
				}
				case YUV -> {
					switch (output.getImageType().getDataType()) {
						case U8:
							yuvToInterleavedYuvU8(bufferY, bufferU, bufferV, width, height, strideY, strideUV, stridePixelUV, (InterleavedU8)output, workArrays);
							return;
					}
				}
			}
		}
		throw new RuntimeException("Not yet supported. format=" + colorOutput + " out=" + output.getImageType());
	}

	/**
	 * Converts an YUV 420 888 into gray
	 *
	 * @param output Output: Optional storage for output image. Can be null.
	 * @param outputType Output: Type of output image
	 * @param <T> Output image type
	 * @return Gray scale image
	 */
	public static <T extends ImageGray<T>>
	T yuvToGray( ByteBuffer bufferY, int width, int height, int strideRow, T output,
				 @Nullable GrowArray<DogArray_I8> workArrays, Class<T> outputType ) {
		if (outputType == GrayU8.class) {
			return (T)yuvToGray(bufferY, width, height, strideRow, (GrayU8)output);
		} else if (outputType == GrayF32.class) {
			return (T)yuvToGray(bufferY, width, height, strideRow, (GrayF32)output, workArrays);
		} else {
			throw new IllegalArgumentException("Unsupported BoofCV Image Type " + outputType.getSimpleName());
		}
	}

	/**
	 * Converts a YUV buffer into a gray scale image.
	 *
	 * @param output Output: Optional storage for output image. Can be null.
	 * @return Gray scale image
	 */
	public static GrayF32 yuvToGray( ByteBuffer bufferY, int width, int height, int strideRow,
									 @Nullable GrayF32 output,
									 @Nullable GrowArray<DogArray_I8> workArrays ) {
		output = InputSanityCheck.declareOrReshape(output, width, height, GrayF32.class);
		workArrays = BoofMiscOps.checkDeclare(workArrays, DogArray_I8::new);

		byte[] work = BoofMiscOps.checkDeclare(workArrays.grow(), width, false);

		int indexDst = 0;
		for (int y = 0, indexRow = 0; y < height; y++, indexRow += strideRow) {
			bufferY.position(indexRow);
			bufferY.get(work, 0, width);
			for (int x = 0; x < width; x++) {
				output.data[indexDst++] = work[x] & 0xFF;
			}
		}

		return output;
	}

	/**
	 * Converts a YUV buffer into a gray scale image.
	 */
	public static GrayU8 yuvToGray( ByteBuffer bufferY, int width, int height, int strideRow, @Nullable GrayU8 output ) {
		output = InputSanityCheck.declareOrReshape(output, width, height, GrayU8.class);

		int indexDst = 0;
		for (int y = 0, indexRow = 0; y < height; y++, indexRow += strideRow, indexDst += width) {
			bufferY.position(indexRow);
			bufferY.get(output.data, indexDst, width);
		}

		return output;
	}

	public interface ProcessorYuv {
		void processYUV( final int y, final int u, final int v );
	}

	abstract static class ProcessorYuvRgb implements ProcessorYuv {
		@Override final public void processYUV( final int y, final int u, final int v ) {
			int Y = 1191*(y - 16);
			int CR = u - 128;
			int CB = v - 128;

			// if( y < 0 ) y = 0;
			Y = ((Y >>> 31) ^ 1)*Y;

			int r = (Y + 1836*CR) >> 10;
			int g = (Y - 547*CR - 218*CB) >> 10;
			int b = (Y + 2165*CB) >> 10;

//				if( r < 0 ) r = 0; else if( r > 255 ) r = 255;
//				if( g < 0 ) g = 0; else if( g > 255 ) g = 255;
//				if( b < 0 ) b = 0; else if( b > 255 ) b = 255;

			r *= ((r >>> 31) ^ 1);
			g *= ((g >>> 31) ^ 1);
			b *= ((b >>> 31) ^ 1);

			// The bitwise code below isn't faster than than the if statement below
//				r |= (((255-r) >>> 31)*0xFF);
//				g |= (((255-g) >>> 31)*0xFF);
//				b |= (((255-b) >>> 31)*0xFF);

			if (r > 255) r = 255;
			if (g > 255) g = 255;
			if (b > 255) b = 255;

			processRGB(r, g, b);
		}

		public abstract void processRGB( final int r, final int g, final int b );
	}

	public static Planar<GrayU8> yuvToPlanarRgbU8( ByteBuffer bufferY, ByteBuffer bufferU, ByteBuffer bufferV,
												   int width, int height, int strideY, int strideUV, int stridePixelUV,
												   @Nullable Planar<GrayU8> output, @Nullable GrowArray<DogArray_I8> workArrays ) {
		output = InputSanityCheck.declareOrReshape(output, width, height, 3, GrayU8.class);
		workArrays = BoofMiscOps.checkDeclare(workArrays, DogArray_I8::new);

		final byte[] red = output.getBand(0).data;
		final byte[] green = output.getBand(1).data;
		final byte[] blue = output.getBand(2).data;

		ProcessorYuv processor = new ProcessorYuvRgb() {
			int indexOut = 0;

			@Override
			public void processRGB( final int r, final int g, final int b ) {
				red[indexOut] = (byte)r;
				green[indexOut] = (byte)g;
				blue[indexOut++] = (byte)b;
			}
		};

		ImplConvertYuv420_888.processYuv(bufferY, bufferU, bufferV, width, height, strideY, strideUV, stridePixelUV, workArrays, processor);

		return output;
	}

	public static Planar<GrayF32> yuvToPlanarRgbF32( ByteBuffer bufferY, ByteBuffer bufferU, ByteBuffer bufferV,
													 int width, int height, int strideY, int strideUV, int stridePixelUV,
													 @Nullable Planar<GrayF32> output, @Nullable GrowArray<DogArray_I8> workArrays ) {
		output = InputSanityCheck.declareOrReshape(output, width, height, 3, GrayF32.class);
		workArrays = BoofMiscOps.checkDeclare(workArrays, DogArray_I8::new);

		final float[] red = output.getBand(0).data;
		final float[] green = output.getBand(1).data;
		final float[] blue = output.getBand(2).data;

		ProcessorYuv processor = new ProcessorYuvRgb() {
			int indexOut = 0;

			@Override
			public void processRGB( final int r, final int g, final int b ) {
				red[indexOut] = r;
				green[indexOut] = g;
				blue[indexOut++] = b;
			}
		};

//		if(BoofConcurrency.USE_CONCURRENT) {
//			ImplConvertYuv420_888_MT.processYuv(bufferY, bufferU, bufferV, width, height, strideY, strideUV, stridePixelUV, workArrays, processor);
//		} else {
		ImplConvertYuv420_888.processYuv(bufferY, bufferU, bufferV, width, height, strideY, strideUV, stridePixelUV, workArrays, processor);
//		}


		return output;
	}

	public static InterleavedU8 yuvToInterleavedRgbU8( ByteBuffer bufferY, ByteBuffer bufferU, ByteBuffer bufferV,
													   int width, int height, int strideY, int strideUV, int stridePixelUV,
													   @Nullable InterleavedU8 output, @Nullable GrowArray<DogArray_I8> workArrays ) {
		output = InputSanityCheck.declareOrReshape(output, width, height, 3, InterleavedU8.class);
		workArrays = BoofMiscOps.checkDeclare(workArrays, DogArray_I8::new);

		InterleavedU8 _output = output;

		ProcessorYuv processor = new ProcessorYuvRgb() {
			int indexOut = 0;

			@Override
			public void processRGB( final int r, final int g, final int b ) {
				_output.data[indexOut++] = (byte)r;
				_output.data[indexOut++] = (byte)g;
				_output.data[indexOut++] = (byte)b;
			}
		};

		ImplConvertYuv420_888.processYuv(bufferY, bufferU, bufferV, width, height, strideY, strideUV, stridePixelUV, workArrays, processor);

		return output;
	}

	public static InterleavedF32 yuvToInterleavedRgbF32( ByteBuffer bufferY, ByteBuffer bufferU, ByteBuffer bufferV,
														 int width, int height, int strideY, int strideUV, int stridePixelUV,
														 @Nullable InterleavedF32 output, @Nullable GrowArray<DogArray_I8> workArrays ) {
		output = InputSanityCheck.declareOrReshape(output, width, height, 3, InterleavedF32.class);
		workArrays = BoofMiscOps.checkDeclare(workArrays, DogArray_I8::new);

		InterleavedF32 _output = output;

		ProcessorYuv processor = new ProcessorYuvRgb() {
			int indexOut = 0;

			@Override
			public void processRGB( final int r, final int g, final int b ) {
				_output.data[indexOut++] = r;
				_output.data[indexOut++] = g;
				_output.data[indexOut++] = b;
			}
		};

		ImplConvertYuv420_888.processYuv(bufferY, bufferU, bufferV, width, height, strideY, strideUV, stridePixelUV, workArrays, processor);

		return output;
	}

	public static Planar<GrayU8> yuvToPlanarYuvU8( ByteBuffer bufferY, ByteBuffer bufferU, ByteBuffer bufferV,
												   int width, int height, int strideY, int strideUV, int stridePixelUV,
												   @Nullable Planar<GrayU8> output, @Nullable GrowArray<DogArray_I8> workArrays ) {
		output = InputSanityCheck.declareOrReshape(output, width, height, 3, GrayU8.class);
		workArrays = BoofMiscOps.checkDeclare(workArrays, DogArray_I8::new);

		final byte[] dataY = output.getBand(0).data;
		final byte[] dataU = output.getBand(1).data;
		final byte[] dataV = output.getBand(2).data;

		ProcessorYuv processor = new ProcessorYuv() {
			int indexOut = 0;

			@Override public void processYUV( final int y, final int u, final int v ) {
				dataY[indexOut] = (byte)y;
				dataU[indexOut] = (byte)u;
				dataV[indexOut++] = (byte)v;
			}
		};

		ImplConvertYuv420_888.processYuv(bufferY, bufferU, bufferV, width, height, strideY, strideUV, stridePixelUV, workArrays, processor);

		return output;
	}

	public static InterleavedU8 yuvToInterleavedYuvU8( ByteBuffer bufferY, ByteBuffer bufferU, ByteBuffer bufferV,
													   int width, int height, int strideY, int strideUV, int stridePixelUV,
													   @Nullable InterleavedU8 output, @Nullable GrowArray<DogArray_I8> workArrays ) {
		output = InputSanityCheck.declareOrReshape(output, width, height, 3, InterleavedU8.class);
		workArrays = BoofMiscOps.checkDeclare(workArrays, DogArray_I8::new);

		final byte[] data = output.data;

		ProcessorYuv processor = new ProcessorYuv() {
			int indexOut = 0;

			@Override final public void processYUV( final int y, final int u, final int v ) {
				data[indexOut++] = (byte)y;
				data[indexOut++] = (byte)u;
				data[indexOut++] = (byte)v;
			}
		};

		ImplConvertYuv420_888.processYuv(bufferY, bufferU, bufferV, width, height, strideY, strideUV, stridePixelUV, workArrays, processor);
		return output;
	}
}
