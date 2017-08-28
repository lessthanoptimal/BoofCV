/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.javacv;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.*;

import java.nio.*;

import static org.bytedeco.javacpp.opencv_core.*;

/**
 * Functions for converting between JavaCV's IplImage data type and BoofCV image types
 *
 * @author Peter Abeles
 */
public class ConvertIplImage {

	public static <T extends ImageBase>T convertFrom( IplImage input ) {
		if( input.nChannels() == 1 ) {
			return (T)convertFrom(input,(ImageGray)null);
		} else {
			return (T)convertFrom(input,(ImageInterleaved) null);
		}
	}

	public static <T extends ImageGray<T>>T convertFrom( IplImage input , T output ) {
		if( input.nChannels() != 1 )
			throw new IllegalArgumentException("Expected 1 channel for gray scale images");

		ImageDataType dataType = depthToBoofType(input.depth());

		int width = input.width();
		int height = input.height();

		if( output != null ) {
			if( output.isSubimage() )
				throw new IllegalArgumentException("Can't handle sub-images");
			if( output.getDataType() != dataType )
				throw new IllegalArgumentException("Expected data type of "
						+dataType+" found "+output.getDataType()+" instead");
			output.reshape(width,height);
		} else {
			output = (T)GeneralizedImageOps.createSingleBand(dataType,width,height);
		}

		switch( dataType ) {
			case U8:
			case S8:
				convertFrom_G(input,(GrayI8)output); break;

			case S16: convertFrom_G(input,(GrayS16)output); break;
			case S32: convertFrom_G(input,(GrayS32)output); break;
			case F32: convertFrom_G(input,(GrayF32)output); break;
			case F64: convertFrom_G(input,(GrayF64)output); break;

			default:
				throw new RuntimeException("Add support for type "+dataType);
		}

		return output;
	}

	public static <T extends ImageInterleaved<T>>T convertFrom( IplImage input , T output ) {
		ImageDataType dataType = depthToBoofType(input.depth());

		int numBands = input.nChannels();
		int width = input.width();
		int height = input.height();

		if( output != null ) {
			if( output.isSubimage() )
				throw new IllegalArgumentException("Can't handle sub-images");
			if( output.getDataType() != dataType )
				throw new IllegalArgumentException("Expected data type of "
						+dataType+" found "+output.getDataType()+" instead");
			output.numBands = numBands;
			output.reshape(width,height);
		} else {
			output = (T)GeneralizedImageOps.createInterleaved(dataType,width,height,numBands);
		}

		switch( dataType ) {
			case U8:
			case S8:
				convertFrom_I(input,(InterleavedI8)output); break;

			case S16: convertFrom_I(input,(InterleavedS16)output); break;
			case S32: convertFrom_I(input,(InterleavedS32)output); break;
			case F32: convertFrom_I(input,(InterleavedF32)output); break;
			case F64: convertFrom_I(input,(InterleavedF64)output); break;

			default:
				throw new RuntimeException("Add support for type "+dataType);
		}

		return output;
	}

	private static void convertFrom_G( IplImage input , GrayI8 output ) {
		ByteBuffer buffer = input.createBuffer();

		int width = input.width();
		int height = input.height();
		int stride = input.widthStep();

		for (int y = 0; y < height; y++) {
			buffer.position(y*stride);
			buffer.get( output.data,  y * output.stride, width );
		}
	}

	private static void convertFrom_I( IplImage input , InterleavedI8 output ) {
		ByteBuffer buffer = input.createBuffer();

		int height = input.height();
		int stride = input.widthStep();
		int dataWidth = input.width()*input.nChannels();

		for (int y = 0; y < height; y++) {
			buffer.position(y*stride);
			buffer.get( output.data,  y * output.stride, dataWidth );
		}
	}

	private static void convertFrom_G( IplImage input , GrayS16 output ) {
		ShortBuffer buffer = input.createBuffer();

		int width = input.width();
		int height = input.height();
		int stride = input.widthStep()/2;

		for (int y = 0; y < height; y++) {
			buffer.position(y*stride);
			buffer.get( output.data,  y * output.stride, width );
		}
	}

	private static void convertFrom_I( IplImage input , InterleavedS16 output ) {
		ShortBuffer buffer = input.createBuffer();

		int height = input.height();
		int stride = input.widthStep()/2;
		int dataWidth = input.width()*input.nChannels();

		for (int y = 0; y < height; y++) {
			buffer.position(y*stride);
			buffer.get( output.data,  y * output.stride, dataWidth );
		}
	}

	private static void convertFrom_G( IplImage input , GrayS32 output ) {
		IntBuffer buffer = input.createBuffer();

		int width = input.width();
		int height = input.height();
		int stride = input.widthStep()/4;

		for (int y = 0; y < height; y++) {
			buffer.position(y*stride);
			buffer.get( output.data,  y * output.stride, width );
		}
	}

	private static void convertFrom_I( IplImage input , InterleavedS32 output ) {
		IntBuffer buffer = input.createBuffer();

		int height = input.height();
		int stride = input.widthStep()/4;
		int dataWidth = input.width()*input.nChannels();

		for (int y = 0; y < height; y++) {
			buffer.position(y*stride);
			buffer.get( output.data,  y * output.stride, dataWidth );
		}
	}

	private static void convertFrom_G( IplImage input , GrayF32 output ) {
		FloatBuffer buffer = input.createBuffer();

		int width = input.width();
		int height = input.height();
		int stride = input.widthStep()/4;

		for (int y = 0; y < height; y++) {
			buffer.position(y*stride);
			buffer.get( output.data,  y * output.stride, width );
		}
	}

	private static void convertFrom_I( IplImage input , InterleavedF32 output ) {
		FloatBuffer buffer = input.createBuffer();

		int height = input.height();
		int stride = input.widthStep()/4;
		int dataWidth = input.width()*input.nChannels();

		for (int y = 0; y < height; y++) {
			buffer.position(y*stride);
			buffer.get( output.data,  y * output.stride, dataWidth );
		}
	}

	private static void convertFrom_G( IplImage input , GrayF64 output ) {
		DoubleBuffer buffer = input.createBuffer();

		int width = input.width();
		int height = input.height();
		int stride = input.widthStep()/8;

		for (int y = 0; y < height; y++) {
			buffer.position(y*stride);
			buffer.get( output.data,  y * output.stride, width );
		}
	}

	private static void convertFrom_I( IplImage input , InterleavedF64 output ) {
		DoubleBuffer buffer = input.createBuffer();

		int height = input.height();
		int stride = input.widthStep()/8;
		int dataWidth = input.width()*input.nChannels();

		for (int y = 0; y < height; y++) {
			buffer.position(y*stride);
			buffer.get( output.data,  y * output.stride, dataWidth );
		}
	}

	public static ImageDataType depthToBoofType( int depth ) {
		switch( depth ) {
			case IPL_DEPTH_8U: return ImageDataType.U8;
			case IPL_DEPTH_8S: return ImageDataType.S8;
			case IPL_DEPTH_16S: return ImageDataType.S16;
			case IPL_DEPTH_32S: return ImageDataType.S32;
			case IPL_DEPTH_32F: return ImageDataType.F32;
			case IPL_DEPTH_64F: return ImageDataType.F64;
			default:
				throw new IllegalArgumentException("Unknown IPL depth "+depth);
		}
	}
}
