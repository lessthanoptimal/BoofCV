/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.color.ColorFormat;
import boofcv.struct.image.*;

import java.nio.ByteBuffer;

// TODO comment out current conversion code and unroll it. Runs significantly faster, see below.
// maybe autogenerate the code some how so that fixes are propagated throughout the code base
//		Benchmark                              Mode  Cnt  Score   Error  Units
//		BenchmarkYuv420_888.yuvToPlanarRgbU8   avgt   10  4.023 ± 0.323  ms/op
//		BenchmarkYuv420_888.yuvToPlanarRgbU8A  avgt   10  3.460 ± 0.141  ms/op
// A = original code that was unrolled.

/**
 * Functions for converting YUV 420 888 into BoofCV imgae types.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class ConvertYuv420_888
{
	public static byte[] declareWork( int strideY , int strideUV , byte[] work ) {

		int workLength = strideY + 2*strideUV;

		if( work == null || work.length < workLength )
			return new byte[workLength];
		return work;
	}

	public static void yuvToBoof(ByteBuffer bufferY, ByteBuffer bufferU , ByteBuffer bufferV  ,
								 int width, int height, int strideY , int strideUV , int stridePixelUV,
								 ColorFormat colorOutput, ImageBase output, byte[] work)
	{
		if( output instanceof GrayU8 ) {
			yuvToGray(bufferY,width,height,strideY,(GrayU8)output);
			return;
		} else if( output instanceof  GrayF32 ) {
			yuvToGray(bufferY,width,height,strideY,(GrayF32)output, work);
			return;
		} else if( output.getImageType().getFamily() == ImageType.Family.PLANAR ) {
			switch (colorOutput) {
				case RGB:{
					switch( output.getImageType().getDataType()) {
						case U8:
							yuvToPlanarRgbU8(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV,(Planar<GrayU8>)output,work);
							return;
						case F32:
							yuvToPlanarRgbF32(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV,(Planar<GrayF32>)output,work);
							return;
					}
				}break;

				case YUV:{
					switch( output.getImageType().getDataType()) {
						case U8:
							yuvToPlanarYuvU8(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV,(Planar<GrayU8>)output,work);
							return;
					}
				}break;
			}
		} else if( output.getImageType().getFamily() == ImageType.Family.INTERLEAVED ) {
			switch (colorOutput) {
				case RGB:{
					switch( output.getImageType().getDataType()) {
						case U8:
							yuvToInterleavedRgbU8(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV, (InterleavedU8) output, work);
							return;
						case F32:
							yuvToInterleavedRgbF32(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV, (InterleavedF32) output, work);
							return;
					}
				}break;

				case YUV:{
					switch( output.getImageType().getDataType()) {
						case U8:
							yuvToInterleavedYuvU8(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV,(InterleavedU8)output,work);
							return;
					}
				}break;
			}
		}
		throw new RuntimeException("Not yet supported. format="+colorOutput+" out="+output.getImageType());
	}


	/**
	 * Converts an YUV 420 888 into gray
	 *
	 * @param output Output: Optional storage for output image.  Can be null.
	 * @param outputType  Output: Type of output image
	 * @param <T> Output image type
	 * @return Gray scale image
	 */
	public static <T extends ImageGray<T>>
	T yuvToGray(ByteBuffer bufferY , int width , int height, int strideRow , T output , byte work[], Class<T> outputType )
	{
		if( outputType == GrayU8.class ) {
			return (T) yuvToGray(bufferY,width,height,strideRow,(GrayU8)output);
		} else if( outputType == GrayF32.class ) {
			return (T) yuvToGray(bufferY,width,height,strideRow,(GrayF32)output,work);
		} else {
			throw new IllegalArgumentException("Unsupported BoofCV Image Type "+outputType.getSimpleName());
		}
	}


	/**
	 *
	 * @param output Output: Optional storage for output image.  Can be null.
	 * @return Gray scale image
	 */
	public static GrayF32 yuvToGray( ByteBuffer bufferY , int width , int height, int strideRow, GrayF32 output, byte work[] ) {
		if( output != null ) {
			output.reshape(width,height);
		} else {
			output = new GrayF32(width,height);
		}
		if( work.length < width )
			throw new IllegalArgumentException("work array must be at least width long");


		int indexDst = 0;
		for (int y = 0, indexRow=0; y < height; y++,indexRow += strideRow) {
			bufferY.position(indexRow);
			bufferY.get(work,0,width);
			for (int x = 0; x < width; x++) {
				output.data[indexDst++] = work[x]&0xFF;
			}
		}

		return output;
	}

	public static GrayU8 yuvToGray(ByteBuffer bufferY , int width , int height, int strideRow, GrayU8 output ) {
		if( output != null ) {
			output.reshape(width,height);
		} else {
			output = new GrayU8(width,height);
		}

		int indexDst = 0;
		for (int y = 0, indexRow=0; y < height; y++,indexRow += strideRow, indexDst += width) {
			bufferY.position(indexRow);
			bufferY.get(output.data,indexDst,width);
		}

		return output;
	}

	interface ProcessorYuv
	{
		void processYUV(final int y , final int u ,final int v );
	}

	abstract static class ProcessorYuvRgb implements ProcessorYuv
	{
		final public void processYUV(final int y , final int u ,final int v ) {
			int Y = 1191*(y - 16);
			int CR = u - 128;
			int CB = v - 128;

			// if( y < 0 ) y = 0;
			Y = ((Y >>> 31)^1)*Y;

			int r = (Y + 1836*CR) >> 10;
			int g = (Y - 547*CR - 218*CB) >> 10;
			int b = (Y + 2165*CB) >> 10;

//				if( r < 0 ) r = 0; else if( r > 255 ) r = 255;
//				if( g < 0 ) g = 0; else if( g > 255 ) g = 255;
//				if( b < 0 ) b = 0; else if( b > 255 ) b = 255;

			r *= ((r >>> 31)^1);
			g *= ((g >>> 31)^1);
			b *= ((b >>> 31)^1);

			// The bitwise code below isn't faster than than the if statement below
//				r |= (((255-r) >>> 31)*0xFF);
//				g |= (((255-g) >>> 31)*0xFF);
//				b |= (((255-b) >>> 31)*0xFF);

			if( r > 255 ) r = 255;
			if( g > 255 ) g = 255;
			if( b > 255 ) b = 255;

			processRGB(r,g,b);
		}

		public abstract void processRGB( final int r ,final int g , final int b );
	}

	public static void processYuv(ByteBuffer bufferY, ByteBuffer bufferU , ByteBuffer bufferV  ,
								  int width, int height, int strideY , int strideUV , int stridePixelUV,
								  byte work[], ProcessorYuv processor )
	{
		// U and V stride are the same by 420_888 specification

		// not sure the best way to compute this. The width of a plane should be used here and not the stride
		// but the plane's width isn't specified.
		int periodUV = (int)Math.round(width/(strideUV/(double)stridePixelUV));

		int workLength = strideY + strideUV + strideUV;
		if( work.length < workLength )
			throw new IllegalArgumentException("Work must be at least "+workLength);

		// Index of the start of the row in the buffer
		int positionY=0,positionUV=0;
		int rowBytesUV = ((width/periodUV)-1)*stridePixelUV+1;

		// start of each band in the work buffer
		int offsetU = strideY;
		int offsetV = strideY + strideUV;

		// save for debugging purposes
		int totalBytesY = bufferY.limit();
		int totalBytesU = bufferU.limit();
		int totalBytesV = bufferV.limit();

		int x=-1,y=-1,indexY=-1,indexU=-1,indexV=-1;
		try {
			for (y = 0; y < height; y++) {
				// Read all the data for this row from each plane
				bufferY.position(positionY);
				bufferY.get(work, 0, width);
				positionY += strideY;

				if (y % periodUV == 0) {
					bufferU.position(positionUV);
					bufferU.get(work, offsetU, rowBytesUV);
					bufferV.position(positionUV);
					bufferV.get(work, offsetV, rowBytesUV);
					positionUV += strideUV;
				}

				indexY = 0;
				indexU = offsetU;
				indexV = offsetV;

				for (x = 0; x < width; x++, indexY++) {
					processor.processYUV(work[indexY] & 0xFF, work[indexU] & 0xFF, work[indexV] & 0xFF);

					// this is intended to be a fast way to do if a == 0 ? 1 : 0
					int stepUV = stridePixelUV * ((x + 1) % periodUV == 0 ? 1 : 0);
					indexU += stepUV;
					indexV += stepUV;
				}
			}
		} catch( RuntimeException e ) {
			e.printStackTrace();

			String message = "Crashed in YUV. "+e.getMessage()+" bytes Y="+totalBytesY+" U="+totalBytesU+
					" V="+totalBytesV+" width="+width+" height="+height+" work.length="+work.length+
					" strideY="+strideY+" strideUV="+strideUV+" stridePixelUV="+stridePixelUV+" periodUV="+periodUV+
					" x="+x+" y="+y+" indexY="+indexY+" indexU="+indexU+" indexV="+indexV;
			throw new RuntimeException(message);
		}
	}

	public static Planar<GrayU8> yuvToPlanarRgbU8(ByteBuffer bufferY, ByteBuffer bufferU , ByteBuffer bufferV  ,
												  int width, int height, int strideY , int strideUV , int stridePixelUV,
												  Planar<GrayU8> output , byte work[] ) {
		if( output != null ) {
			output.reshape(width,height,3);
		} else {
			output = new  Planar<>(GrayU8.class,width,height,3);
		}
		final byte[] red = output.getBand(0).data;
		final byte[] green = output.getBand(1).data;
		final byte[] blue = output.getBand(2).data;

		ProcessorYuv processor = new ProcessorYuvRgb() {
			int indexOut = 0;

			@Override
			public void processRGB( final int r , final int g , final int b ) {
				red[indexOut] = (byte)r;
				green[indexOut] = (byte)g;
				blue[indexOut++] = (byte)b;
			}
		};

		processYuv(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV,work,processor);

		return output;
	}

	public static Planar<GrayF32> yuvToPlanarRgbF32(ByteBuffer bufferY, ByteBuffer bufferU , ByteBuffer bufferV  ,
													int width, int height, int strideY , int strideUV , int stridePixelUV,
													Planar<GrayF32> output , byte work[] )
	{
		if( output != null ) {
			output.reshape(width,height,3);
		} else {
			output = new  Planar<>(GrayF32.class,width,height,3);
		}
		final float[] red = output.getBand(0).data;
		final float[] green = output.getBand(1).data;
		final float[] blue = output.getBand(2).data;

		ProcessorYuv processor = new ProcessorYuvRgb() {
			int indexOut = 0;

			@Override
			public void processRGB( final int r , final int g , final int b ) {
				red[indexOut] = r;
				green[indexOut] = g;
				blue[indexOut++] = b;
			}
		};

		processYuv(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV,work,processor);

		return output;
	}

	public static InterleavedU8 yuvToInterleavedRgbU8(ByteBuffer bufferY, ByteBuffer bufferU , ByteBuffer bufferV  ,
													  int width, int height, int strideY , int strideUV , int stridePixelUV,
													  InterleavedU8 output , byte work[] )
	{
		if( output != null ) {
			output.reshape(width,height,3);
		} else {
			output = new InterleavedU8(width,height,3);
		}
		InterleavedU8 _output = output;

		ProcessorYuv processor = new ProcessorYuvRgb() {
			int indexOut = 0;

			@Override
			public void processRGB( final int r , final int g , final int b ) {
				_output.data[indexOut++] = (byte)r;
				_output.data[indexOut++] = (byte)g;
				_output.data[indexOut++] = (byte)b;
			}
		};

		processYuv(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV,work,processor);

		return output;
	}

	public static InterleavedF32 yuvToInterleavedRgbF32(ByteBuffer bufferY, ByteBuffer bufferU , ByteBuffer bufferV  ,
														int width, int height, int strideY , int strideUV , int stridePixelUV,
														InterleavedF32 output , byte work[] )
	{
		if( output != null ) {
			output.reshape(width,height,3);
		} else {
			output = new InterleavedF32(width,height,3);
		}
		InterleavedF32 _output = output;

		ProcessorYuv processor = new ProcessorYuvRgb() {
			int indexOut = 0;

			@Override
			public void processRGB( final int r , final int g , final int b ) {
				_output.data[indexOut++] = r;
				_output.data[indexOut++] = g;
				_output.data[indexOut++] = b;
			}
		};

		processYuv(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV,work,processor);

		return output;
	}

	public static Planar<GrayU8> yuvToPlanarYuvU8(ByteBuffer bufferY, ByteBuffer bufferU , ByteBuffer bufferV  ,
												  int width, int height, int strideY , int strideUV , int stridePixelUV,
												  Planar<GrayU8> output , byte work[] )
	{
		if( output != null ) {
			output.reshape(width,height,3);
		} else {
			output = new Planar(GrayU8.class,width,height,3);
		}

		final byte[] dataY = output.getBand(0).data;
		final byte[] dataU = output.getBand(1).data;
		final byte[] dataV = output.getBand(2).data;

		ProcessorYuv processor = new ProcessorYuv() {
			int indexOut = 0;
			@Override
			final public void processYUV(final int y, final int u, final int v) {
				dataY[indexOut] = (byte)y;
				dataU[indexOut] = (byte)u;
				dataV[indexOut++] = (byte)v;
			}
		};

		processYuv(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV,work,processor);

		return output;
	}

	public static InterleavedU8 yuvToInterleavedYuvU8(ByteBuffer bufferY, ByteBuffer bufferU , ByteBuffer bufferV  ,
													  int width, int height, int strideY , int strideUV , int stridePixelUV,
													  InterleavedU8 output , byte work[] )
	{
		if( output != null ) {
			output.reshape(width,height,3);
		} else {
			output = new InterleavedU8(width,height,3);
		}

		final byte[] data = output.data;

		ProcessorYuv processor = new ProcessorYuv() {
			int indexOut = 0;
			@Override
			final public void processYUV(final int y, final int u, final int v) {
				data[indexOut++] = (byte)y;
				data[indexOut++] = (byte)u;
				data[indexOut++] = (byte)v;
			}
		};

		processYuv(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV,work,processor);

		return output;
	}
}
