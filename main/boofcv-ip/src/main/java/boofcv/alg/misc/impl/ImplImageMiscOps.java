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

package boofcv.alg.misc.impl;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.misc.BoofLambdas;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_F64;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.border.ImageBorder_S64;
import boofcv.struct.image.*;

import javax.annotation.Generated;
import java.util.Arrays;
import java.util.Random;

/**
 * Implementations of functions for {@link ImageMiscOps}
 *
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateImplImageMiscOps</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.misc.impl.GenerateImplImageMiscOps")
public class ImplImageMiscOps {

	public static < T extends GrayI8<T>> void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 T input, ImageBorder_S32<T> border, GrayI8 output ) {
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained in the output image. w="+output.width+" < "+(dstX+width)+" or y="+output.height+" < "+(dstY+height));

		// Check to see if it's entirely contained inside the input image
		if (srcX >= 0 && srcX + width <= input.width && srcY >= 0 && srcY + height <= input.height) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
			for (int y = 0; y < height; y++) {
				int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX;
				int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;

				System.arraycopy(input.data, indexSrc, output.data, indexDst, width);
			}
			//CONCURRENT_ABOVE });
		} else {
			// If any part is outside use the border. This isn't terribly efficient. A better approach is to
			// handle all the possible outside regions independently. That code is significantly more complex so I'm
			// punting it for a future person to write since this is good enough as it.
			border.setImage(input);
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
			for (int y = 0; y < height; y++) {
				int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;
				for (int x = 0; x < width; x++) {
					output.data[indexDst++] = (byte)border.get(srcX + x, srcY + y);
				}
			}
			//CONCURRENT_ABOVE });
		}
	}

	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayI8 input, GrayI8 output ) {
		if (input.width < srcX + width || input.height < srcY + height)
			throw new IllegalArgumentException("Copy region must be contained in the input image");
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained in the output image");

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
		for (int y = 0; y < height; y++) {
			int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX;
			int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;

			System.arraycopy(input.data, indexSrc, output.data, indexDst, width);
		}
		//CONCURRENT_ABOVE });
	}

	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 InterleavedI8 input, InterleavedI8 output ) {
		if (input.width < srcX + width || input.height < srcY + height)
			throw new IllegalArgumentException("Copy region must be contained input image");
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained output image");
		if (output.numBands != input.numBands)
			throw new IllegalArgumentException("Number of bands must match. " + input.numBands + " != " + output.numBands);

		final int numBands = input.numBands;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
		for (int y = 0; y < height; y++) {
			int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX*numBands;
			int indexDst = output.startIndex + (dstY + y)*output.stride + dstX*numBands;

			System.arraycopy(input.data, indexSrc, output.data, indexDst, width*numBands);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fill( GrayI8 input, int value ) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride();
			Arrays.fill(input.data, index, index + input.width, (byte)value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fill( InterleavedI8 input, int value ) {
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride();
			int end = index + input.width*input.numBands;
			Arrays.fill(input.data, index, end, (byte)value);
		}
	}

	public static void fill( InterleavedI8 input, int[] values ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			for (int band = 0; band < numBands; band++) {
				int index = input.getStartIndex() + y*input.getStride() + band;
				int end = index + input.width*numBands - band;
				int value = values[band];
				for (; index < end; index += numBands) {
					input.data[index] = (byte)value;
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBand( InterleavedI8 input, int band, int value ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride() + band;
			int end = index + input.width*numBands - band;
			for (; index < end; index += numBands) {
				input.data[index] = (byte)value;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void insertBand( GrayI8 input, int band, InterleavedI8 output ) {
		final int numBands = output.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.getStartIndex() + y*input.getStride();
			int indexOut = output.getStartIndex() + y*output.getStride() + band;
			int end = indexOut + output.width*numBands - band;
			for (; indexOut < end; indexOut += numBands, indexIn++) {
				output.data[indexOut] = input.data[indexIn];
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void extractBand( InterleavedI8 input, int band, GrayI8 output ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.getStartIndex() + y*input.getStride() + band;
			int indexOut = output.getStartIndex() + y*output.getStride();
			int end = indexOut + output.width;
			for (; indexOut < end; indexIn += numBands, indexOut++) {
				output.data[indexOut] = input.data[indexIn];
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBorder( GrayI8 input, int value, int radius ) {
		// top and bottom
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,radius,y->{
		for (int y = 0; y < radius; y++) {
			int indexTop = input.startIndex + y*input.stride;
			int indexBottom = input.startIndex + (input.height - y - 1)*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[indexTop++] = (byte)value;
				input.data[indexBottom++] = (byte)value;
			}
		}
		//CONCURRENT_ABOVE });

		// left and right
		int h = input.height - radius;
		int indexStart = input.startIndex + radius*input.stride;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,radius,x->{
		for (int x = 0; x < radius; x++) {
			int indexLeft = indexStart + x;
			int indexRight = indexStart + input.width - 1 - x;
			for (int y = radius; y < h; y++) {
				input.data[indexLeft] = (byte)value;
				input.data[indexRight] = (byte)value;

				indexLeft += input.stride;
				indexRight += input.stride;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBorder( GrayI8 input, int value, int borderX0, int borderY0, int borderX1, int borderY1 ) {
		// top and bottom
		for (int y = 0; y < borderY0; y++) {
			int srcIdx = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[srcIdx++] = (byte)value;
			}
		}
		for (int y = input.height - borderY1; y < input.height; y++) {
			int srcIdx = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[srcIdx++] = (byte)value;
			}
		}

		// left and right
		int h = input.height - borderY1;
		for (int x = 0; x < borderX0; x++) {
			int srcIdx = input.startIndex + borderY0*input.stride + x;
			for (int y = borderY0; y < h; y++) {
				input.data[srcIdx] = (byte)value;
				srcIdx += input.stride;
			}
		}
		for (int x = input.width - borderX1; x < input.width; x++) {
			int srcIdx = input.startIndex + borderY0*input.stride + x;
			for (int y = borderY0; y < h; y++) {
				input.data[srcIdx] = (byte)value;
				srcIdx += input.stride;
			}
		}
	}

	public static void fillRectangle( GrayI8 image, int value, int x0, int y0, int width, int height ) {
		int x1 = x0 + width;
		int y1 = y0 + height;

		if (x0 < 0) x0 = 0; if (x1 > image.width) x1 = image.width;
		if (y0 < 0) y0 = 0; if (y1 > image.height) y1 = image.height;
		final int _x0 = x0;
		final int _x1 = x1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1 , y->{
		for (int y = y0; y < y1; y++) {
			int index = image.startIndex + y*image.stride + _x0;
			Arrays.fill(image.data, index, index + _x1 - _x0, (byte)value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillRectangle(InterleavedI8 image, int value, int x0, int y0, int width, int height ) {
		int x1 = x0 + width;
		int y1 = y0 + height;

		if (x0 < 0) x0 = 0; if (x1 > image.width) x1 = image.width;
		if (y0 < 0) y0 = 0; if (y1 > image.height) y1 = image.height;
		final int _x0 = x0;

		int length = (x1 - x0)*image.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1, y->{
		for (int y = y0; y < y1; y++) {
			int index = image.startIndex + y*image.stride + _x0*image.numBands;
			Arrays.fill(image.data, index, index + length, (byte)value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillUniform( GrayI8 image, Random rand, int min, int max ) {
		int range = max - min;

		byte[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				data[index++] = (byte)(rand.nextInt(range) + min);
			}
		}
	}

	public static void fillUniform( InterleavedI8 image, Random rand, int min, int max ) {
		int range = max - min;

		byte[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int end = index + image.width*image.numBands;
			for (; index < end; index++) {
				data[index] = (byte)(rand.nextInt(range) + min);
			}
		}
	}

	public static void fillGaussian( GrayI8 image, Random rand, double mean, double sigma, int lowerBound, int upperBound ) {
		byte[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				int value = (int)(rand.nextGaussian()*sigma + mean);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				data[index++] = (byte)value;
			}
		}
	}

	public static void fillGaussian( InterleavedI8 image, Random rand, double mean, double sigma, int lowerBound, int upperBound ) {
		byte[] data = image.data;
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int indexEnd = index + length;

			while (index < indexEnd) {
				int value = (int)(rand.nextGaussian()*sigma + mean);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				data[index++] = (byte)value;
			}
		}
	}

	public static void flipVertical( GrayI8 image ) {
		int h2 = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h2, y->{
		for (int y = 0; y < h2; y++) {
			int index1 = image.getStartIndex() + y*image.getStride();
			int index2 = image.getStartIndex() + (image.height - y - 1)*image.getStride();

			int end = index1 + image.width;

			while (index1 < end) {
				int tmp = image.data[index1];
				image.data[index1++] = image.data[index2];
				image.data[index2++] = (byte)tmp;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void flipHorizontal( GrayI8 image ) {
		int w2 = image.width/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, y->{
		for (int y = 0; y < image.height; y++) {
			int index1 = image.getStartIndex() + y*image.getStride();
			int index2 = index1 + image.width - 1;

			int end = index1 + w2;

			while (index1 < end) {
				int tmp = image.data[index1];
				image.data[index1++] = image.data[index2];
				image.data[index2--] = (byte)tmp;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void transpose( GrayI8 input, GrayI8 output ) {
		output.reshape(input.height, input.width);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(y, x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void transpose( InterleavedI8 input, InterleavedI8 output ) {
		output.reshape(input.height, input.width, input.numBands);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(y, x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( GrayI8 image ) {
		if (image.width != image.height)
			throw new IllegalArgumentException("Image must be square");

		int w = image.height/2 + image.height%2;
		int h = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h, y0->{
		for (int y0 = 0; y0 < h; y0++) {
			int y1 = image.height - y0 - 1;

			for (int x0 = 0; x0 < w; x0++) {
				int x1 = image.width - x0 - 1;

				int index0 = image.startIndex + y0*image.stride + x0;
				int index1 = image.startIndex + x0*image.stride + y1;
				int index2 = image.startIndex + y1*image.stride + x1;
				int index3 = image.startIndex + x1*image.stride + y0;

				int tmp3 = image.data[index3];

				image.data[index3] = image.data[index2];
				image.data[index2] = image.data[index1];
				image.data[index1] = image.data[index0];
				image.data[index0] = (byte)tmp3;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( GrayI8 input, GrayI8 output ) {
		output.reshape(input.height, input.width);

		int h = input.height - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(h - y, x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( InterleavedI8 input, InterleavedI8 output ) {
		output.reshape(input.height, input.width, input.numBands);

		int h = input.height - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(h - y, x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( GrayI8 image ) {
		if (image.width != image.height)
			throw new IllegalArgumentException("Image must be square");

		int w = image.height/2 + image.height%2;
		int h = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h, y0->{
		for (int y0 = 0; y0 < h; y0++) {
			int y1 = image.height - y0 - 1;

			for (int x0 = 0; x0 < w; x0++) {
				int x1 = image.width - x0 - 1;

				int index0 = image.startIndex + y0*image.stride + x0;
				int index1 = image.startIndex + x0*image.stride + y1;
				int index2 = image.startIndex + y1*image.stride + x1;
				int index3 = image.startIndex + x1*image.stride + y0;

				int tmp0 = image.data[index0];

				image.data[index0] = image.data[index1];
				image.data[index1] = image.data[index2];
				image.data[index2] = image.data[index3];
				image.data[index3] = (byte)tmp0;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( GrayI8 input, GrayI8 output ) {
		output.reshape(input.height, input.width);

		int w = input.width - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(y, w - x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( InterleavedI8 input, InterleavedI8 output ) {
		output.reshape(input.height, input.width, input.numBands);

		int w = input.width - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(y, w - x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static <T extends GrayI8<T>>
	void growBorder( T src, ImageBorder_S32<T> border, int borderX0, int borderY0, int borderX1, int borderY1, T dst ) {
		dst.reshape(src.width + borderX0 + borderX1, src.height + borderY0 + borderY1);
		border.setImage(src);

		// Copy src into the inner portion of dst
		ImageMiscOps.copy(0, 0, borderX0, borderY0, src.width, src.height, src, dst);

		// Top border
		for (int y = 0; y < borderY0; y++) {
			int idxDst = dst.startIndex + y*dst.stride;
			for (int x = 0; x < dst.width; x++) {
				dst.data[idxDst++] = (byte)border.get(x - borderX0, y - borderY0);
			}
		}
		// Bottom border
		for (int y = 0; y < borderY1; y++) {
			int idxDst = dst.startIndex + (dst.height - borderY1 + y)*dst.stride;
			for (int x = 0; x < dst.width; x++) {
				dst.data[idxDst++] = (byte)border.get(x - borderX0, src.height + y);
			}
		}
		// Left and right border
		for (int y = borderY0; y < dst.height - borderY1; y++) {
			int idxDst = dst.startIndex + y*dst.stride;
			for (int x = 0; x < borderX0; x++) {
				dst.data[idxDst++] = (byte)border.get(x - borderX0, y - borderY0);
			}
			idxDst = dst.startIndex + y*dst.stride + src.width + borderX0;
			for (int x = 0; x < borderX1; x++) {
				dst.data[idxDst++] = (byte)border.get(src.width + x, y - borderY0);
			}
		}
	}

	public static void findAndProcess( GrayI8 input, BoofLambdas.Match_I8 finder, BoofLambdas.ProcessIIB process ) {
		for (int y = 0; y < input.height; y++) {
			int index = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++, index++) {
				if (finder.process(input.data[index])) {
					if (!process.process(x, y))
						return;
				}
			}
		}
	}

	public static < T extends GrayI16<T>> void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 T input, ImageBorder_S32<T> border, GrayI16 output ) {
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained in the output image. w="+output.width+" < "+(dstX+width)+" or y="+output.height+" < "+(dstY+height));

		// Check to see if it's entirely contained inside the input image
		if (srcX >= 0 && srcX + width <= input.width && srcY >= 0 && srcY + height <= input.height) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
			for (int y = 0; y < height; y++) {
				int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX;
				int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;

				System.arraycopy(input.data, indexSrc, output.data, indexDst, width);
			}
			//CONCURRENT_ABOVE });
		} else {
			// If any part is outside use the border. This isn't terribly efficient. A better approach is to
			// handle all the possible outside regions independently. That code is significantly more complex so I'm
			// punting it for a future person to write since this is good enough as it.
			border.setImage(input);
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
			for (int y = 0; y < height; y++) {
				int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;
				for (int x = 0; x < width; x++) {
					output.data[indexDst++] = (short)border.get(srcX + x, srcY + y);
				}
			}
			//CONCURRENT_ABOVE });
		}
	}

	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayI16 input, GrayI16 output ) {
		if (input.width < srcX + width || input.height < srcY + height)
			throw new IllegalArgumentException("Copy region must be contained in the input image");
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained in the output image");

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
		for (int y = 0; y < height; y++) {
			int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX;
			int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;

			System.arraycopy(input.data, indexSrc, output.data, indexDst, width);
		}
		//CONCURRENT_ABOVE });
	}

	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 InterleavedI16 input, InterleavedI16 output ) {
		if (input.width < srcX + width || input.height < srcY + height)
			throw new IllegalArgumentException("Copy region must be contained input image");
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained output image");
		if (output.numBands != input.numBands)
			throw new IllegalArgumentException("Number of bands must match. " + input.numBands + " != " + output.numBands);

		final int numBands = input.numBands;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
		for (int y = 0; y < height; y++) {
			int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX*numBands;
			int indexDst = output.startIndex + (dstY + y)*output.stride + dstX*numBands;

			System.arraycopy(input.data, indexSrc, output.data, indexDst, width*numBands);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fill( GrayI16 input, int value ) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride();
			Arrays.fill(input.data, index, index + input.width, (short)value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fill( InterleavedI16 input, int value ) {
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride();
			int end = index + input.width*input.numBands;
			Arrays.fill(input.data, index, end, (short)value);
		}
	}

	public static void fill( InterleavedI16 input, int[] values ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			for (int band = 0; band < numBands; band++) {
				int index = input.getStartIndex() + y*input.getStride() + band;
				int end = index + input.width*numBands - band;
				int value = values[band];
				for (; index < end; index += numBands) {
					input.data[index] = (short)value;
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBand( InterleavedI16 input, int band, int value ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride() + band;
			int end = index + input.width*numBands - band;
			for (; index < end; index += numBands) {
				input.data[index] = (short)value;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void insertBand( GrayI16 input, int band, InterleavedI16 output ) {
		final int numBands = output.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.getStartIndex() + y*input.getStride();
			int indexOut = output.getStartIndex() + y*output.getStride() + band;
			int end = indexOut + output.width*numBands - band;
			for (; indexOut < end; indexOut += numBands, indexIn++) {
				output.data[indexOut] = input.data[indexIn];
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void extractBand( InterleavedI16 input, int band, GrayI16 output ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.getStartIndex() + y*input.getStride() + band;
			int indexOut = output.getStartIndex() + y*output.getStride();
			int end = indexOut + output.width;
			for (; indexOut < end; indexIn += numBands, indexOut++) {
				output.data[indexOut] = input.data[indexIn];
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBorder( GrayI16 input, int value, int radius ) {
		// top and bottom
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,radius,y->{
		for (int y = 0; y < radius; y++) {
			int indexTop = input.startIndex + y*input.stride;
			int indexBottom = input.startIndex + (input.height - y - 1)*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[indexTop++] = (short)value;
				input.data[indexBottom++] = (short)value;
			}
		}
		//CONCURRENT_ABOVE });

		// left and right
		int h = input.height - radius;
		int indexStart = input.startIndex + radius*input.stride;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,radius,x->{
		for (int x = 0; x < radius; x++) {
			int indexLeft = indexStart + x;
			int indexRight = indexStart + input.width - 1 - x;
			for (int y = radius; y < h; y++) {
				input.data[indexLeft] = (short)value;
				input.data[indexRight] = (short)value;

				indexLeft += input.stride;
				indexRight += input.stride;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBorder( GrayI16 input, int value, int borderX0, int borderY0, int borderX1, int borderY1 ) {
		// top and bottom
		for (int y = 0; y < borderY0; y++) {
			int srcIdx = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[srcIdx++] = (short)value;
			}
		}
		for (int y = input.height - borderY1; y < input.height; y++) {
			int srcIdx = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[srcIdx++] = (short)value;
			}
		}

		// left and right
		int h = input.height - borderY1;
		for (int x = 0; x < borderX0; x++) {
			int srcIdx = input.startIndex + borderY0*input.stride + x;
			for (int y = borderY0; y < h; y++) {
				input.data[srcIdx] = (short)value;
				srcIdx += input.stride;
			}
		}
		for (int x = input.width - borderX1; x < input.width; x++) {
			int srcIdx = input.startIndex + borderY0*input.stride + x;
			for (int y = borderY0; y < h; y++) {
				input.data[srcIdx] = (short)value;
				srcIdx += input.stride;
			}
		}
	}

	public static void fillRectangle( GrayI16 image, int value, int x0, int y0, int width, int height ) {
		int x1 = x0 + width;
		int y1 = y0 + height;

		if (x0 < 0) x0 = 0; if (x1 > image.width) x1 = image.width;
		if (y0 < 0) y0 = 0; if (y1 > image.height) y1 = image.height;
		final int _x0 = x0;
		final int _x1 = x1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1 , y->{
		for (int y = y0; y < y1; y++) {
			int index = image.startIndex + y*image.stride + _x0;
			Arrays.fill(image.data, index, index + _x1 - _x0, (short)value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillRectangle(InterleavedI16 image, int value, int x0, int y0, int width, int height ) {
		int x1 = x0 + width;
		int y1 = y0 + height;

		if (x0 < 0) x0 = 0; if (x1 > image.width) x1 = image.width;
		if (y0 < 0) y0 = 0; if (y1 > image.height) y1 = image.height;
		final int _x0 = x0;

		int length = (x1 - x0)*image.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1, y->{
		for (int y = y0; y < y1; y++) {
			int index = image.startIndex + y*image.stride + _x0*image.numBands;
			Arrays.fill(image.data, index, index + length, (short)value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillUniform( GrayI16 image, Random rand, int min, int max ) {
		int range = max - min;

		short[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				data[index++] = (short)(rand.nextInt(range) + min);
			}
		}
	}

	public static void fillUniform( InterleavedI16 image, Random rand, int min, int max ) {
		int range = max - min;

		short[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int end = index + image.width*image.numBands;
			for (; index < end; index++) {
				data[index] = (short)(rand.nextInt(range) + min);
			}
		}
	}

	public static void fillGaussian( GrayI16 image, Random rand, double mean, double sigma, int lowerBound, int upperBound ) {
		short[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				int value = (int)(rand.nextGaussian()*sigma + mean);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				data[index++] = (short)value;
			}
		}
	}

	public static void fillGaussian( InterleavedI16 image, Random rand, double mean, double sigma, int lowerBound, int upperBound ) {
		short[] data = image.data;
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int indexEnd = index + length;

			while (index < indexEnd) {
				int value = (int)(rand.nextGaussian()*sigma + mean);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				data[index++] = (short)value;
			}
		}
	}

	public static void flipVertical( GrayI16 image ) {
		int h2 = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h2, y->{
		for (int y = 0; y < h2; y++) {
			int index1 = image.getStartIndex() + y*image.getStride();
			int index2 = image.getStartIndex() + (image.height - y - 1)*image.getStride();

			int end = index1 + image.width;

			while (index1 < end) {
				int tmp = image.data[index1];
				image.data[index1++] = image.data[index2];
				image.data[index2++] = (short)tmp;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void flipHorizontal( GrayI16 image ) {
		int w2 = image.width/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, y->{
		for (int y = 0; y < image.height; y++) {
			int index1 = image.getStartIndex() + y*image.getStride();
			int index2 = index1 + image.width - 1;

			int end = index1 + w2;

			while (index1 < end) {
				int tmp = image.data[index1];
				image.data[index1++] = image.data[index2];
				image.data[index2--] = (short)tmp;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void transpose( GrayI16 input, GrayI16 output ) {
		output.reshape(input.height, input.width);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(y, x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void transpose( InterleavedI16 input, InterleavedI16 output ) {
		output.reshape(input.height, input.width, input.numBands);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(y, x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( GrayI16 image ) {
		if (image.width != image.height)
			throw new IllegalArgumentException("Image must be square");

		int w = image.height/2 + image.height%2;
		int h = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h, y0->{
		for (int y0 = 0; y0 < h; y0++) {
			int y1 = image.height - y0 - 1;

			for (int x0 = 0; x0 < w; x0++) {
				int x1 = image.width - x0 - 1;

				int index0 = image.startIndex + y0*image.stride + x0;
				int index1 = image.startIndex + x0*image.stride + y1;
				int index2 = image.startIndex + y1*image.stride + x1;
				int index3 = image.startIndex + x1*image.stride + y0;

				int tmp3 = image.data[index3];

				image.data[index3] = image.data[index2];
				image.data[index2] = image.data[index1];
				image.data[index1] = image.data[index0];
				image.data[index0] = (short)tmp3;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( GrayI16 input, GrayI16 output ) {
		output.reshape(input.height, input.width);

		int h = input.height - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(h - y, x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( InterleavedI16 input, InterleavedI16 output ) {
		output.reshape(input.height, input.width, input.numBands);

		int h = input.height - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(h - y, x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( GrayI16 image ) {
		if (image.width != image.height)
			throw new IllegalArgumentException("Image must be square");

		int w = image.height/2 + image.height%2;
		int h = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h, y0->{
		for (int y0 = 0; y0 < h; y0++) {
			int y1 = image.height - y0 - 1;

			for (int x0 = 0; x0 < w; x0++) {
				int x1 = image.width - x0 - 1;

				int index0 = image.startIndex + y0*image.stride + x0;
				int index1 = image.startIndex + x0*image.stride + y1;
				int index2 = image.startIndex + y1*image.stride + x1;
				int index3 = image.startIndex + x1*image.stride + y0;

				int tmp0 = image.data[index0];

				image.data[index0] = image.data[index1];
				image.data[index1] = image.data[index2];
				image.data[index2] = image.data[index3];
				image.data[index3] = (short)tmp0;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( GrayI16 input, GrayI16 output ) {
		output.reshape(input.height, input.width);

		int w = input.width - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(y, w - x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( InterleavedI16 input, InterleavedI16 output ) {
		output.reshape(input.height, input.width, input.numBands);

		int w = input.width - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(y, w - x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static <T extends GrayI16<T>>
	void growBorder( T src, ImageBorder_S32<T> border, int borderX0, int borderY0, int borderX1, int borderY1, T dst ) {
		dst.reshape(src.width + borderX0 + borderX1, src.height + borderY0 + borderY1);
		border.setImage(src);

		// Copy src into the inner portion of dst
		ImageMiscOps.copy(0, 0, borderX0, borderY0, src.width, src.height, src, dst);

		// Top border
		for (int y = 0; y < borderY0; y++) {
			int idxDst = dst.startIndex + y*dst.stride;
			for (int x = 0; x < dst.width; x++) {
				dst.data[idxDst++] = (short)border.get(x - borderX0, y - borderY0);
			}
		}
		// Bottom border
		for (int y = 0; y < borderY1; y++) {
			int idxDst = dst.startIndex + (dst.height - borderY1 + y)*dst.stride;
			for (int x = 0; x < dst.width; x++) {
				dst.data[idxDst++] = (short)border.get(x - borderX0, src.height + y);
			}
		}
		// Left and right border
		for (int y = borderY0; y < dst.height - borderY1; y++) {
			int idxDst = dst.startIndex + y*dst.stride;
			for (int x = 0; x < borderX0; x++) {
				dst.data[idxDst++] = (short)border.get(x - borderX0, y - borderY0);
			}
			idxDst = dst.startIndex + y*dst.stride + src.width + borderX0;
			for (int x = 0; x < borderX1; x++) {
				dst.data[idxDst++] = (short)border.get(src.width + x, y - borderY0);
			}
		}
	}

	public static void findAndProcess( GrayI16 input, BoofLambdas.Match_I16 finder, BoofLambdas.ProcessIIB process ) {
		for (int y = 0; y < input.height; y++) {
			int index = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++, index++) {
				if (finder.process(input.data[index])) {
					if (!process.process(x, y))
						return;
				}
			}
		}
	}

	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayS32 input, ImageBorder_S32 border, GrayS32 output ) {
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained in the output image. w="+output.width+" < "+(dstX+width)+" or y="+output.height+" < "+(dstY+height));

		// Check to see if it's entirely contained inside the input image
		if (srcX >= 0 && srcX + width <= input.width && srcY >= 0 && srcY + height <= input.height) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
			for (int y = 0; y < height; y++) {
				int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX;
				int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;

				System.arraycopy(input.data, indexSrc, output.data, indexDst, width);
			}
			//CONCURRENT_ABOVE });
		} else {
			// If any part is outside use the border. This isn't terribly efficient. A better approach is to
			// handle all the possible outside regions independently. That code is significantly more complex so I'm
			// punting it for a future person to write since this is good enough as it.
			border.setImage(input);
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
			for (int y = 0; y < height; y++) {
				int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;
				for (int x = 0; x < width; x++) {
					output.data[indexDst++] = border.get(srcX + x, srcY + y);
				}
			}
			//CONCURRENT_ABOVE });
		}
	}

	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayS32 input, GrayS32 output ) {
		if (input.width < srcX + width || input.height < srcY + height)
			throw new IllegalArgumentException("Copy region must be contained in the input image");
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained in the output image");

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
		for (int y = 0; y < height; y++) {
			int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX;
			int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;

			System.arraycopy(input.data, indexSrc, output.data, indexDst, width);
		}
		//CONCURRENT_ABOVE });
	}

	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 InterleavedS32 input, InterleavedS32 output ) {
		if (input.width < srcX + width || input.height < srcY + height)
			throw new IllegalArgumentException("Copy region must be contained input image");
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained output image");
		if (output.numBands != input.numBands)
			throw new IllegalArgumentException("Number of bands must match. " + input.numBands + " != " + output.numBands);

		final int numBands = input.numBands;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
		for (int y = 0; y < height; y++) {
			int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX*numBands;
			int indexDst = output.startIndex + (dstY + y)*output.stride + dstX*numBands;

			System.arraycopy(input.data, indexSrc, output.data, indexDst, width*numBands);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fill( GrayS32 input, int value ) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride();
			Arrays.fill(input.data, index, index + input.width, value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fill( InterleavedS32 input, int value ) {
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride();
			int end = index + input.width*input.numBands;
			Arrays.fill(input.data, index, end, value);
		}
	}

	public static void fill( InterleavedS32 input, int[] values ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			for (int band = 0; band < numBands; band++) {
				int index = input.getStartIndex() + y*input.getStride() + band;
				int end = index + input.width*numBands - band;
				int value = values[band];
				for (; index < end; index += numBands) {
					input.data[index] = value;
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBand( InterleavedS32 input, int band, int value ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride() + band;
			int end = index + input.width*numBands - band;
			for (; index < end; index += numBands) {
				input.data[index] = value;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void insertBand( GrayS32 input, int band, InterleavedS32 output ) {
		final int numBands = output.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.getStartIndex() + y*input.getStride();
			int indexOut = output.getStartIndex() + y*output.getStride() + band;
			int end = indexOut + output.width*numBands - band;
			for (; indexOut < end; indexOut += numBands, indexIn++) {
				output.data[indexOut] = input.data[indexIn];
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void extractBand( InterleavedS32 input, int band, GrayS32 output ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.getStartIndex() + y*input.getStride() + band;
			int indexOut = output.getStartIndex() + y*output.getStride();
			int end = indexOut + output.width;
			for (; indexOut < end; indexIn += numBands, indexOut++) {
				output.data[indexOut] = input.data[indexIn];
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBorder( GrayS32 input, int value, int radius ) {
		// top and bottom
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,radius,y->{
		for (int y = 0; y < radius; y++) {
			int indexTop = input.startIndex + y*input.stride;
			int indexBottom = input.startIndex + (input.height - y - 1)*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[indexTop++] = value;
				input.data[indexBottom++] = value;
			}
		}
		//CONCURRENT_ABOVE });

		// left and right
		int h = input.height - radius;
		int indexStart = input.startIndex + radius*input.stride;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,radius,x->{
		for (int x = 0; x < radius; x++) {
			int indexLeft = indexStart + x;
			int indexRight = indexStart + input.width - 1 - x;
			for (int y = radius; y < h; y++) {
				input.data[indexLeft] = value;
				input.data[indexRight] = value;

				indexLeft += input.stride;
				indexRight += input.stride;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBorder( GrayS32 input, int value, int borderX0, int borderY0, int borderX1, int borderY1 ) {
		// top and bottom
		for (int y = 0; y < borderY0; y++) {
			int srcIdx = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[srcIdx++] = value;
			}
		}
		for (int y = input.height - borderY1; y < input.height; y++) {
			int srcIdx = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[srcIdx++] = value;
			}
		}

		// left and right
		int h = input.height - borderY1;
		for (int x = 0; x < borderX0; x++) {
			int srcIdx = input.startIndex + borderY0*input.stride + x;
			for (int y = borderY0; y < h; y++) {
				input.data[srcIdx] = value;
				srcIdx += input.stride;
			}
		}
		for (int x = input.width - borderX1; x < input.width; x++) {
			int srcIdx = input.startIndex + borderY0*input.stride + x;
			for (int y = borderY0; y < h; y++) {
				input.data[srcIdx] = value;
				srcIdx += input.stride;
			}
		}
	}

	public static void fillRectangle( GrayS32 image, int value, int x0, int y0, int width, int height ) {
		int x1 = x0 + width;
		int y1 = y0 + height;

		if (x0 < 0) x0 = 0; if (x1 > image.width) x1 = image.width;
		if (y0 < 0) y0 = 0; if (y1 > image.height) y1 = image.height;
		final int _x0 = x0;
		final int _x1 = x1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1 , y->{
		for (int y = y0; y < y1; y++) {
			int index = image.startIndex + y*image.stride + _x0;
			Arrays.fill(image.data, index, index + _x1 - _x0, value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillRectangle(InterleavedS32 image, int value, int x0, int y0, int width, int height ) {
		int x1 = x0 + width;
		int y1 = y0 + height;

		if (x0 < 0) x0 = 0; if (x1 > image.width) x1 = image.width;
		if (y0 < 0) y0 = 0; if (y1 > image.height) y1 = image.height;
		final int _x0 = x0;

		int length = (x1 - x0)*image.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1, y->{
		for (int y = y0; y < y1; y++) {
			int index = image.startIndex + y*image.stride + _x0*image.numBands;
			Arrays.fill(image.data, index, index + length, value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillUniform( GrayS32 image, Random rand, int min, int max ) {
		int range = max - min;

		int[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				data[index++] = rand.nextInt((int)range) + min;
			}
		}
	}

	public static void fillUniform( InterleavedS32 image, Random rand, int min, int max ) {
		int range = max - min;

		int[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int end = index + image.width*image.numBands;
			for (; index < end; index++) {
				data[index] = rand.nextInt((int)range) + min;
			}
		}
	}

	public static void fillGaussian( GrayS32 image, Random rand, double mean, double sigma, int lowerBound, int upperBound ) {
		int[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				int value = (int)(rand.nextGaussian()*sigma + mean);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				data[index++] = value;
			}
		}
	}

	public static void fillGaussian( InterleavedS32 image, Random rand, double mean, double sigma, int lowerBound, int upperBound ) {
		int[] data = image.data;
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int indexEnd = index + length;

			while (index < indexEnd) {
				int value = (int)(rand.nextGaussian()*sigma + mean);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				data[index++] = value;
			}
		}
	}

	public static void flipVertical( GrayS32 image ) {
		int h2 = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h2, y->{
		for (int y = 0; y < h2; y++) {
			int index1 = image.getStartIndex() + y*image.getStride();
			int index2 = image.getStartIndex() + (image.height - y - 1)*image.getStride();

			int end = index1 + image.width;

			while (index1 < end) {
				int tmp = image.data[index1];
				image.data[index1++] = image.data[index2];
				image.data[index2++] = (int)tmp;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void flipHorizontal( GrayS32 image ) {
		int w2 = image.width/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, y->{
		for (int y = 0; y < image.height; y++) {
			int index1 = image.getStartIndex() + y*image.getStride();
			int index2 = index1 + image.width - 1;

			int end = index1 + w2;

			while (index1 < end) {
				int tmp = image.data[index1];
				image.data[index1++] = image.data[index2];
				image.data[index2--] = (int)tmp;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void transpose( GrayS32 input, GrayS32 output ) {
		output.reshape(input.height, input.width);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(y, x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void transpose( InterleavedS32 input, InterleavedS32 output ) {
		output.reshape(input.height, input.width, input.numBands);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(y, x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( GrayS32 image ) {
		if (image.width != image.height)
			throw new IllegalArgumentException("Image must be square");

		int w = image.height/2 + image.height%2;
		int h = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h, y0->{
		for (int y0 = 0; y0 < h; y0++) {
			int y1 = image.height - y0 - 1;

			for (int x0 = 0; x0 < w; x0++) {
				int x1 = image.width - x0 - 1;

				int index0 = image.startIndex + y0*image.stride + x0;
				int index1 = image.startIndex + x0*image.stride + y1;
				int index2 = image.startIndex + y1*image.stride + x1;
				int index3 = image.startIndex + x1*image.stride + y0;

				int tmp3 = image.data[index3];

				image.data[index3] = image.data[index2];
				image.data[index2] = image.data[index1];
				image.data[index1] = image.data[index0];
				image.data[index0] = (int)tmp3;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( GrayS32 input, GrayS32 output ) {
		output.reshape(input.height, input.width);

		int h = input.height - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(h - y, x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( InterleavedS32 input, InterleavedS32 output ) {
		output.reshape(input.height, input.width, input.numBands);

		int h = input.height - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(h - y, x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( GrayS32 image ) {
		if (image.width != image.height)
			throw new IllegalArgumentException("Image must be square");

		int w = image.height/2 + image.height%2;
		int h = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h, y0->{
		for (int y0 = 0; y0 < h; y0++) {
			int y1 = image.height - y0 - 1;

			for (int x0 = 0; x0 < w; x0++) {
				int x1 = image.width - x0 - 1;

				int index0 = image.startIndex + y0*image.stride + x0;
				int index1 = image.startIndex + x0*image.stride + y1;
				int index2 = image.startIndex + y1*image.stride + x1;
				int index3 = image.startIndex + x1*image.stride + y0;

				int tmp0 = image.data[index0];

				image.data[index0] = image.data[index1];
				image.data[index1] = image.data[index2];
				image.data[index2] = image.data[index3];
				image.data[index3] = (int)tmp0;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( GrayS32 input, GrayS32 output ) {
		output.reshape(input.height, input.width);

		int w = input.width - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(y, w - x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( InterleavedS32 input, InterleavedS32 output ) {
		output.reshape(input.height, input.width, input.numBands);

		int w = input.width - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(y, w - x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void growBorder( GrayS32 src, ImageBorder_S32 border, int borderX0, int borderY0, int borderX1, int borderY1, GrayS32 dst ) {
		dst.reshape(src.width + borderX0 + borderX1, src.height + borderY0 + borderY1);
		border.setImage(src);

		// Copy src into the inner portion of dst
		ImageMiscOps.copy(0, 0, borderX0, borderY0, src.width, src.height, src, dst);

		// Top border
		for (int y = 0; y < borderY0; y++) {
			int idxDst = dst.startIndex + y*dst.stride;
			for (int x = 0; x < dst.width; x++) {
				dst.data[idxDst++] = border.get(x - borderX0, y - borderY0);
			}
		}
		// Bottom border
		for (int y = 0; y < borderY1; y++) {
			int idxDst = dst.startIndex + (dst.height - borderY1 + y)*dst.stride;
			for (int x = 0; x < dst.width; x++) {
				dst.data[idxDst++] = border.get(x - borderX0, src.height + y);
			}
		}
		// Left and right border
		for (int y = borderY0; y < dst.height - borderY1; y++) {
			int idxDst = dst.startIndex + y*dst.stride;
			for (int x = 0; x < borderX0; x++) {
				dst.data[idxDst++] = border.get(x - borderX0, y - borderY0);
			}
			idxDst = dst.startIndex + y*dst.stride + src.width + borderX0;
			for (int x = 0; x < borderX1; x++) {
				dst.data[idxDst++] = border.get(src.width + x, y - borderY0);
			}
		}
	}

	public static void findAndProcess( GrayS32 input, BoofLambdas.Match_S32 finder, BoofLambdas.ProcessIIB process ) {
		for (int y = 0; y < input.height; y++) {
			int index = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++, index++) {
				if (finder.process(input.data[index])) {
					if (!process.process(x, y))
						return;
				}
			}
		}
	}

	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayS64 input, ImageBorder_S64 border, GrayS64 output ) {
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained in the output image. w="+output.width+" < "+(dstX+width)+" or y="+output.height+" < "+(dstY+height));

		// Check to see if it's entirely contained inside the input image
		if (srcX >= 0 && srcX + width <= input.width && srcY >= 0 && srcY + height <= input.height) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
			for (int y = 0; y < height; y++) {
				int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX;
				int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;

				System.arraycopy(input.data, indexSrc, output.data, indexDst, width);
			}
			//CONCURRENT_ABOVE });
		} else {
			// If any part is outside use the border. This isn't terribly efficient. A better approach is to
			// handle all the possible outside regions independently. That code is significantly more complex so I'm
			// punting it for a future person to write since this is good enough as it.
			border.setImage(input);
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
			for (int y = 0; y < height; y++) {
				int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;
				for (int x = 0; x < width; x++) {
					output.data[indexDst++] = border.get(srcX + x, srcY + y);
				}
			}
			//CONCURRENT_ABOVE });
		}
	}

	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayS64 input, GrayS64 output ) {
		if (input.width < srcX + width || input.height < srcY + height)
			throw new IllegalArgumentException("Copy region must be contained in the input image");
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained in the output image");

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
		for (int y = 0; y < height; y++) {
			int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX;
			int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;

			System.arraycopy(input.data, indexSrc, output.data, indexDst, width);
		}
		//CONCURRENT_ABOVE });
	}

	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 InterleavedS64 input, InterleavedS64 output ) {
		if (input.width < srcX + width || input.height < srcY + height)
			throw new IllegalArgumentException("Copy region must be contained input image");
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained output image");
		if (output.numBands != input.numBands)
			throw new IllegalArgumentException("Number of bands must match. " + input.numBands + " != " + output.numBands);

		final int numBands = input.numBands;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
		for (int y = 0; y < height; y++) {
			int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX*numBands;
			int indexDst = output.startIndex + (dstY + y)*output.stride + dstX*numBands;

			System.arraycopy(input.data, indexSrc, output.data, indexDst, width*numBands);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fill( GrayS64 input, long value ) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride();
			Arrays.fill(input.data, index, index + input.width, value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fill( InterleavedS64 input, long value ) {
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride();
			int end = index + input.width*input.numBands;
			Arrays.fill(input.data, index, end, value);
		}
	}

	public static void fill( InterleavedS64 input, long[] values ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			for (int band = 0; band < numBands; band++) {
				int index = input.getStartIndex() + y*input.getStride() + band;
				int end = index + input.width*numBands - band;
				long value = values[band];
				for (; index < end; index += numBands) {
					input.data[index] = value;
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBand( InterleavedS64 input, int band, long value ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride() + band;
			int end = index + input.width*numBands - band;
			for (; index < end; index += numBands) {
				input.data[index] = value;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void insertBand( GrayS64 input, int band, InterleavedS64 output ) {
		final int numBands = output.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.getStartIndex() + y*input.getStride();
			int indexOut = output.getStartIndex() + y*output.getStride() + band;
			int end = indexOut + output.width*numBands - band;
			for (; indexOut < end; indexOut += numBands, indexIn++) {
				output.data[indexOut] = input.data[indexIn];
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void extractBand( InterleavedS64 input, int band, GrayS64 output ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.getStartIndex() + y*input.getStride() + band;
			int indexOut = output.getStartIndex() + y*output.getStride();
			int end = indexOut + output.width;
			for (; indexOut < end; indexIn += numBands, indexOut++) {
				output.data[indexOut] = input.data[indexIn];
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBorder( GrayS64 input, long value, int radius ) {
		// top and bottom
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,radius,y->{
		for (int y = 0; y < radius; y++) {
			int indexTop = input.startIndex + y*input.stride;
			int indexBottom = input.startIndex + (input.height - y - 1)*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[indexTop++] = value;
				input.data[indexBottom++] = value;
			}
		}
		//CONCURRENT_ABOVE });

		// left and right
		int h = input.height - radius;
		int indexStart = input.startIndex + radius*input.stride;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,radius,x->{
		for (int x = 0; x < radius; x++) {
			int indexLeft = indexStart + x;
			int indexRight = indexStart + input.width - 1 - x;
			for (int y = radius; y < h; y++) {
				input.data[indexLeft] = value;
				input.data[indexRight] = value;

				indexLeft += input.stride;
				indexRight += input.stride;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBorder( GrayS64 input, long value, int borderX0, int borderY0, int borderX1, int borderY1 ) {
		// top and bottom
		for (int y = 0; y < borderY0; y++) {
			int srcIdx = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[srcIdx++] = value;
			}
		}
		for (int y = input.height - borderY1; y < input.height; y++) {
			int srcIdx = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[srcIdx++] = value;
			}
		}

		// left and right
		int h = input.height - borderY1;
		for (int x = 0; x < borderX0; x++) {
			int srcIdx = input.startIndex + borderY0*input.stride + x;
			for (int y = borderY0; y < h; y++) {
				input.data[srcIdx] = value;
				srcIdx += input.stride;
			}
		}
		for (int x = input.width - borderX1; x < input.width; x++) {
			int srcIdx = input.startIndex + borderY0*input.stride + x;
			for (int y = borderY0; y < h; y++) {
				input.data[srcIdx] = value;
				srcIdx += input.stride;
			}
		}
	}

	public static void fillRectangle( GrayS64 image, long value, int x0, int y0, int width, int height ) {
		int x1 = x0 + width;
		int y1 = y0 + height;

		if (x0 < 0) x0 = 0; if (x1 > image.width) x1 = image.width;
		if (y0 < 0) y0 = 0; if (y1 > image.height) y1 = image.height;
		final int _x0 = x0;
		final int _x1 = x1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1 , y->{
		for (int y = y0; y < y1; y++) {
			int index = image.startIndex + y*image.stride + _x0;
			Arrays.fill(image.data, index, index + _x1 - _x0, value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillRectangle(InterleavedS64 image, long value, int x0, int y0, int width, int height ) {
		int x1 = x0 + width;
		int y1 = y0 + height;

		if (x0 < 0) x0 = 0; if (x1 > image.width) x1 = image.width;
		if (y0 < 0) y0 = 0; if (y1 > image.height) y1 = image.height;
		final int _x0 = x0;

		int length = (x1 - x0)*image.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1, y->{
		for (int y = y0; y < y1; y++) {
			int index = image.startIndex + y*image.stride + _x0*image.numBands;
			Arrays.fill(image.data, index, index + length, value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillUniform( GrayS64 image, Random rand, long min, long max ) {
		long range = max - min;

		long[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				data[index++] = (long)(rand.nextDouble()*0.9999*range) + min;
			}
		}
	}

	public static void fillUniform( InterleavedS64 image, Random rand, long min, long max ) {
		long range = max - min;

		long[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int end = index + image.width*image.numBands;
			for (; index < end; index++) {
				data[index] = (long)(rand.nextDouble()*0.9999*range) + min;
			}
		}
	}

	public static void fillGaussian( GrayS64 image, Random rand, double mean, double sigma, long lowerBound, long upperBound ) {
		long[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				long value = (long)(rand.nextGaussian()*sigma + mean);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				data[index++] = value;
			}
		}
	}

	public static void fillGaussian( InterleavedS64 image, Random rand, double mean, double sigma, long lowerBound, long upperBound ) {
		long[] data = image.data;
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int indexEnd = index + length;

			while (index < indexEnd) {
				long value = (long)(rand.nextGaussian()*sigma + mean);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				data[index++] = value;
			}
		}
	}

	public static void flipVertical( GrayS64 image ) {
		int h2 = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h2, y->{
		for (int y = 0; y < h2; y++) {
			int index1 = image.getStartIndex() + y*image.getStride();
			int index2 = image.getStartIndex() + (image.height - y - 1)*image.getStride();

			int end = index1 + image.width;

			while (index1 < end) {
				long tmp = image.data[index1];
				image.data[index1++] = image.data[index2];
				image.data[index2++] = (long)tmp;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void flipHorizontal( GrayS64 image ) {
		int w2 = image.width/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, y->{
		for (int y = 0; y < image.height; y++) {
			int index1 = image.getStartIndex() + y*image.getStride();
			int index2 = index1 + image.width - 1;

			int end = index1 + w2;

			while (index1 < end) {
				long tmp = image.data[index1];
				image.data[index1++] = image.data[index2];
				image.data[index2--] = (long)tmp;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void transpose( GrayS64 input, GrayS64 output ) {
		output.reshape(input.height, input.width);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(y, x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void transpose( InterleavedS64 input, InterleavedS64 output ) {
		output.reshape(input.height, input.width, input.numBands);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(y, x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( GrayS64 image ) {
		if (image.width != image.height)
			throw new IllegalArgumentException("Image must be square");

		int w = image.height/2 + image.height%2;
		int h = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h, y0->{
		for (int y0 = 0; y0 < h; y0++) {
			int y1 = image.height - y0 - 1;

			for (int x0 = 0; x0 < w; x0++) {
				int x1 = image.width - x0 - 1;

				int index0 = image.startIndex + y0*image.stride + x0;
				int index1 = image.startIndex + x0*image.stride + y1;
				int index2 = image.startIndex + y1*image.stride + x1;
				int index3 = image.startIndex + x1*image.stride + y0;

				long tmp3 = image.data[index3];

				image.data[index3] = image.data[index2];
				image.data[index2] = image.data[index1];
				image.data[index1] = image.data[index0];
				image.data[index0] = (long)tmp3;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( GrayS64 input, GrayS64 output ) {
		output.reshape(input.height, input.width);

		int h = input.height - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(h - y, x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( InterleavedS64 input, InterleavedS64 output ) {
		output.reshape(input.height, input.width, input.numBands);

		int h = input.height - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(h - y, x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( GrayS64 image ) {
		if (image.width != image.height)
			throw new IllegalArgumentException("Image must be square");

		int w = image.height/2 + image.height%2;
		int h = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h, y0->{
		for (int y0 = 0; y0 < h; y0++) {
			int y1 = image.height - y0 - 1;

			for (int x0 = 0; x0 < w; x0++) {
				int x1 = image.width - x0 - 1;

				int index0 = image.startIndex + y0*image.stride + x0;
				int index1 = image.startIndex + x0*image.stride + y1;
				int index2 = image.startIndex + y1*image.stride + x1;
				int index3 = image.startIndex + x1*image.stride + y0;

				long tmp0 = image.data[index0];

				image.data[index0] = image.data[index1];
				image.data[index1] = image.data[index2];
				image.data[index2] = image.data[index3];
				image.data[index3] = (long)tmp0;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( GrayS64 input, GrayS64 output ) {
		output.reshape(input.height, input.width);

		int w = input.width - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(y, w - x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( InterleavedS64 input, InterleavedS64 output ) {
		output.reshape(input.height, input.width, input.numBands);

		int w = input.width - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(y, w - x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void growBorder( GrayS64 src, ImageBorder_S64 border, int borderX0, int borderY0, int borderX1, int borderY1, GrayS64 dst ) {
		dst.reshape(src.width + borderX0 + borderX1, src.height + borderY0 + borderY1);
		border.setImage(src);

		// Copy src into the inner portion of dst
		ImageMiscOps.copy(0, 0, borderX0, borderY0, src.width, src.height, src, dst);

		// Top border
		for (int y = 0; y < borderY0; y++) {
			int idxDst = dst.startIndex + y*dst.stride;
			for (int x = 0; x < dst.width; x++) {
				dst.data[idxDst++] = border.get(x - borderX0, y - borderY0);
			}
		}
		// Bottom border
		for (int y = 0; y < borderY1; y++) {
			int idxDst = dst.startIndex + (dst.height - borderY1 + y)*dst.stride;
			for (int x = 0; x < dst.width; x++) {
				dst.data[idxDst++] = border.get(x - borderX0, src.height + y);
			}
		}
		// Left and right border
		for (int y = borderY0; y < dst.height - borderY1; y++) {
			int idxDst = dst.startIndex + y*dst.stride;
			for (int x = 0; x < borderX0; x++) {
				dst.data[idxDst++] = border.get(x - borderX0, y - borderY0);
			}
			idxDst = dst.startIndex + y*dst.stride + src.width + borderX0;
			for (int x = 0; x < borderX1; x++) {
				dst.data[idxDst++] = border.get(src.width + x, y - borderY0);
			}
		}
	}

	public static void findAndProcess( GrayS64 input, BoofLambdas.Match_S64 finder, BoofLambdas.ProcessIIB process ) {
		for (int y = 0; y < input.height; y++) {
			int index = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++, index++) {
				if (finder.process(input.data[index])) {
					if (!process.process(x, y))
						return;
				}
			}
		}
	}

	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayF32 input, ImageBorder_F32 border, GrayF32 output ) {
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained in the output image. w="+output.width+" < "+(dstX+width)+" or y="+output.height+" < "+(dstY+height));

		// Check to see if it's entirely contained inside the input image
		if (srcX >= 0 && srcX + width <= input.width && srcY >= 0 && srcY + height <= input.height) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
			for (int y = 0; y < height; y++) {
				int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX;
				int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;

				System.arraycopy(input.data, indexSrc, output.data, indexDst, width);
			}
			//CONCURRENT_ABOVE });
		} else {
			// If any part is outside use the border. This isn't terribly efficient. A better approach is to
			// handle all the possible outside regions independently. That code is significantly more complex so I'm
			// punting it for a future person to write since this is good enough as it.
			border.setImage(input);
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
			for (int y = 0; y < height; y++) {
				int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;
				for (int x = 0; x < width; x++) {
					output.data[indexDst++] = border.get(srcX + x, srcY + y);
				}
			}
			//CONCURRENT_ABOVE });
		}
	}

	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayF32 input, GrayF32 output ) {
		if (input.width < srcX + width || input.height < srcY + height)
			throw new IllegalArgumentException("Copy region must be contained in the input image");
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained in the output image");

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
		for (int y = 0; y < height; y++) {
			int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX;
			int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;

			System.arraycopy(input.data, indexSrc, output.data, indexDst, width);
		}
		//CONCURRENT_ABOVE });
	}

	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 InterleavedF32 input, InterleavedF32 output ) {
		if (input.width < srcX + width || input.height < srcY + height)
			throw new IllegalArgumentException("Copy region must be contained input image");
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained output image");
		if (output.numBands != input.numBands)
			throw new IllegalArgumentException("Number of bands must match. " + input.numBands + " != " + output.numBands);

		final int numBands = input.numBands;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
		for (int y = 0; y < height; y++) {
			int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX*numBands;
			int indexDst = output.startIndex + (dstY + y)*output.stride + dstX*numBands;

			System.arraycopy(input.data, indexSrc, output.data, indexDst, width*numBands);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fill( GrayF32 input, float value ) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride();
			Arrays.fill(input.data, index, index + input.width, value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fill( InterleavedF32 input, float value ) {
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride();
			int end = index + input.width*input.numBands;
			Arrays.fill(input.data, index, end, value);
		}
	}

	public static void fill( InterleavedF32 input, float[] values ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			for (int band = 0; band < numBands; band++) {
				int index = input.getStartIndex() + y*input.getStride() + band;
				int end = index + input.width*numBands - band;
				float value = values[band];
				for (; index < end; index += numBands) {
					input.data[index] = value;
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBand( InterleavedF32 input, int band, float value ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride() + band;
			int end = index + input.width*numBands - band;
			for (; index < end; index += numBands) {
				input.data[index] = value;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void insertBand( GrayF32 input, int band, InterleavedF32 output ) {
		final int numBands = output.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.getStartIndex() + y*input.getStride();
			int indexOut = output.getStartIndex() + y*output.getStride() + band;
			int end = indexOut + output.width*numBands - band;
			for (; indexOut < end; indexOut += numBands, indexIn++) {
				output.data[indexOut] = input.data[indexIn];
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void extractBand( InterleavedF32 input, int band, GrayF32 output ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.getStartIndex() + y*input.getStride() + band;
			int indexOut = output.getStartIndex() + y*output.getStride();
			int end = indexOut + output.width;
			for (; indexOut < end; indexIn += numBands, indexOut++) {
				output.data[indexOut] = input.data[indexIn];
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBorder( GrayF32 input, float value, int radius ) {
		// top and bottom
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,radius,y->{
		for (int y = 0; y < radius; y++) {
			int indexTop = input.startIndex + y*input.stride;
			int indexBottom = input.startIndex + (input.height - y - 1)*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[indexTop++] = value;
				input.data[indexBottom++] = value;
			}
		}
		//CONCURRENT_ABOVE });

		// left and right
		int h = input.height - radius;
		int indexStart = input.startIndex + radius*input.stride;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,radius,x->{
		for (int x = 0; x < radius; x++) {
			int indexLeft = indexStart + x;
			int indexRight = indexStart + input.width - 1 - x;
			for (int y = radius; y < h; y++) {
				input.data[indexLeft] = value;
				input.data[indexRight] = value;

				indexLeft += input.stride;
				indexRight += input.stride;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBorder( GrayF32 input, float value, int borderX0, int borderY0, int borderX1, int borderY1 ) {
		// top and bottom
		for (int y = 0; y < borderY0; y++) {
			int srcIdx = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[srcIdx++] = value;
			}
		}
		for (int y = input.height - borderY1; y < input.height; y++) {
			int srcIdx = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[srcIdx++] = value;
			}
		}

		// left and right
		int h = input.height - borderY1;
		for (int x = 0; x < borderX0; x++) {
			int srcIdx = input.startIndex + borderY0*input.stride + x;
			for (int y = borderY0; y < h; y++) {
				input.data[srcIdx] = value;
				srcIdx += input.stride;
			}
		}
		for (int x = input.width - borderX1; x < input.width; x++) {
			int srcIdx = input.startIndex + borderY0*input.stride + x;
			for (int y = borderY0; y < h; y++) {
				input.data[srcIdx] = value;
				srcIdx += input.stride;
			}
		}
	}

	public static void fillRectangle( GrayF32 image, float value, int x0, int y0, int width, int height ) {
		int x1 = x0 + width;
		int y1 = y0 + height;

		if (x0 < 0) x0 = 0; if (x1 > image.width) x1 = image.width;
		if (y0 < 0) y0 = 0; if (y1 > image.height) y1 = image.height;
		final int _x0 = x0;
		final int _x1 = x1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1 , y->{
		for (int y = y0; y < y1; y++) {
			int index = image.startIndex + y*image.stride + _x0;
			Arrays.fill(image.data, index, index + _x1 - _x0, value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillRectangle(InterleavedF32 image, float value, int x0, int y0, int width, int height ) {
		int x1 = x0 + width;
		int y1 = y0 + height;

		if (x0 < 0) x0 = 0; if (x1 > image.width) x1 = image.width;
		if (y0 < 0) y0 = 0; if (y1 > image.height) y1 = image.height;
		final int _x0 = x0;

		int length = (x1 - x0)*image.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1, y->{
		for (int y = y0; y < y1; y++) {
			int index = image.startIndex + y*image.stride + _x0*image.numBands;
			Arrays.fill(image.data, index, index + length, value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillUniform( GrayF32 image, Random rand, float min, float max ) {
		float range = max - min;

		float[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				data[index++] = rand.nextFloat()*range + min;
			}
		}
	}

	public static void fillUniform( InterleavedF32 image, Random rand, float min, float max ) {
		float range = max - min;

		float[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int end = index + image.width*image.numBands;
			for (; index < end; index++) {
				data[index] = rand.nextFloat()*range + min;
			}
		}
	}

	public static void fillGaussian( GrayF32 image, Random rand, double mean, double sigma, float lowerBound, float upperBound ) {
		float[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				float value = (float)(rand.nextGaussian()*sigma + mean);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				data[index++] = value;
			}
		}
	}

	public static void fillGaussian( InterleavedF32 image, Random rand, double mean, double sigma, float lowerBound, float upperBound ) {
		float[] data = image.data;
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int indexEnd = index + length;

			while (index < indexEnd) {
				float value = (float)(rand.nextGaussian()*sigma + mean);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				data[index++] = value;
			}
		}
	}

	public static void flipVertical( GrayF32 image ) {
		int h2 = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h2, y->{
		for (int y = 0; y < h2; y++) {
			int index1 = image.getStartIndex() + y*image.getStride();
			int index2 = image.getStartIndex() + (image.height - y - 1)*image.getStride();

			int end = index1 + image.width;

			while (index1 < end) {
				float tmp = image.data[index1];
				image.data[index1++] = image.data[index2];
				image.data[index2++] = (float)tmp;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void flipHorizontal( GrayF32 image ) {
		int w2 = image.width/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, y->{
		for (int y = 0; y < image.height; y++) {
			int index1 = image.getStartIndex() + y*image.getStride();
			int index2 = index1 + image.width - 1;

			int end = index1 + w2;

			while (index1 < end) {
				float tmp = image.data[index1];
				image.data[index1++] = image.data[index2];
				image.data[index2--] = (float)tmp;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void transpose( GrayF32 input, GrayF32 output ) {
		output.reshape(input.height, input.width);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(y, x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void transpose( InterleavedF32 input, InterleavedF32 output ) {
		output.reshape(input.height, input.width, input.numBands);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(y, x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( GrayF32 image ) {
		if (image.width != image.height)
			throw new IllegalArgumentException("Image must be square");

		int w = image.height/2 + image.height%2;
		int h = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h, y0->{
		for (int y0 = 0; y0 < h; y0++) {
			int y1 = image.height - y0 - 1;

			for (int x0 = 0; x0 < w; x0++) {
				int x1 = image.width - x0 - 1;

				int index0 = image.startIndex + y0*image.stride + x0;
				int index1 = image.startIndex + x0*image.stride + y1;
				int index2 = image.startIndex + y1*image.stride + x1;
				int index3 = image.startIndex + x1*image.stride + y0;

				float tmp3 = image.data[index3];

				image.data[index3] = image.data[index2];
				image.data[index2] = image.data[index1];
				image.data[index1] = image.data[index0];
				image.data[index0] = (float)tmp3;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( GrayF32 input, GrayF32 output ) {
		output.reshape(input.height, input.width);

		int h = input.height - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(h - y, x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( InterleavedF32 input, InterleavedF32 output ) {
		output.reshape(input.height, input.width, input.numBands);

		int h = input.height - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(h - y, x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( GrayF32 image ) {
		if (image.width != image.height)
			throw new IllegalArgumentException("Image must be square");

		int w = image.height/2 + image.height%2;
		int h = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h, y0->{
		for (int y0 = 0; y0 < h; y0++) {
			int y1 = image.height - y0 - 1;

			for (int x0 = 0; x0 < w; x0++) {
				int x1 = image.width - x0 - 1;

				int index0 = image.startIndex + y0*image.stride + x0;
				int index1 = image.startIndex + x0*image.stride + y1;
				int index2 = image.startIndex + y1*image.stride + x1;
				int index3 = image.startIndex + x1*image.stride + y0;

				float tmp0 = image.data[index0];

				image.data[index0] = image.data[index1];
				image.data[index1] = image.data[index2];
				image.data[index2] = image.data[index3];
				image.data[index3] = (float)tmp0;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( GrayF32 input, GrayF32 output ) {
		output.reshape(input.height, input.width);

		int w = input.width - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(y, w - x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( InterleavedF32 input, InterleavedF32 output ) {
		output.reshape(input.height, input.width, input.numBands);

		int w = input.width - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(y, w - x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void growBorder( GrayF32 src, ImageBorder_F32 border, int borderX0, int borderY0, int borderX1, int borderY1, GrayF32 dst ) {
		dst.reshape(src.width + borderX0 + borderX1, src.height + borderY0 + borderY1);
		border.setImage(src);

		// Copy src into the inner portion of dst
		ImageMiscOps.copy(0, 0, borderX0, borderY0, src.width, src.height, src, dst);

		// Top border
		for (int y = 0; y < borderY0; y++) {
			int idxDst = dst.startIndex + y*dst.stride;
			for (int x = 0; x < dst.width; x++) {
				dst.data[idxDst++] = border.get(x - borderX0, y - borderY0);
			}
		}
		// Bottom border
		for (int y = 0; y < borderY1; y++) {
			int idxDst = dst.startIndex + (dst.height - borderY1 + y)*dst.stride;
			for (int x = 0; x < dst.width; x++) {
				dst.data[idxDst++] = border.get(x - borderX0, src.height + y);
			}
		}
		// Left and right border
		for (int y = borderY0; y < dst.height - borderY1; y++) {
			int idxDst = dst.startIndex + y*dst.stride;
			for (int x = 0; x < borderX0; x++) {
				dst.data[idxDst++] = border.get(x - borderX0, y - borderY0);
			}
			idxDst = dst.startIndex + y*dst.stride + src.width + borderX0;
			for (int x = 0; x < borderX1; x++) {
				dst.data[idxDst++] = border.get(src.width + x, y - borderY0);
			}
		}
	}

	public static void findAndProcess( GrayF32 input, BoofLambdas.Match_F32 finder, BoofLambdas.ProcessIIB process ) {
		for (int y = 0; y < input.height; y++) {
			int index = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++, index++) {
				if (finder.process(input.data[index])) {
					if (!process.process(x, y))
						return;
				}
			}
		}
	}

	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayF64 input, ImageBorder_F64 border, GrayF64 output ) {
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained in the output image. w="+output.width+" < "+(dstX+width)+" or y="+output.height+" < "+(dstY+height));

		// Check to see if it's entirely contained inside the input image
		if (srcX >= 0 && srcX + width <= input.width && srcY >= 0 && srcY + height <= input.height) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
			for (int y = 0; y < height; y++) {
				int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX;
				int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;

				System.arraycopy(input.data, indexSrc, output.data, indexDst, width);
			}
			//CONCURRENT_ABOVE });
		} else {
			// If any part is outside use the border. This isn't terribly efficient. A better approach is to
			// handle all the possible outside regions independently. That code is significantly more complex so I'm
			// punting it for a future person to write since this is good enough as it.
			border.setImage(input);
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
			for (int y = 0; y < height; y++) {
				int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;
				for (int x = 0; x < width; x++) {
					output.data[indexDst++] = border.get(srcX + x, srcY + y);
				}
			}
			//CONCURRENT_ABOVE });
		}
	}

	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayF64 input, GrayF64 output ) {
		if (input.width < srcX + width || input.height < srcY + height)
			throw new IllegalArgumentException("Copy region must be contained in the input image");
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained in the output image");

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
		for (int y = 0; y < height; y++) {
			int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX;
			int indexDst = output.startIndex + (dstY + y)*output.stride + dstX;

			System.arraycopy(input.data, indexSrc, output.data, indexDst, width);
		}
		//CONCURRENT_ABOVE });
	}

	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 InterleavedF64 input, InterleavedF64 output ) {
		if (input.width < srcX + width || input.height < srcY + height)
			throw new IllegalArgumentException("Copy region must be contained input image");
		if (output.width < dstX + width || output.height < dstY + height)
			throw new IllegalArgumentException("Copy region must be contained output image");
		if (output.numBands != input.numBands)
			throw new IllegalArgumentException("Number of bands must match. " + input.numBands + " != " + output.numBands);

		final int numBands = input.numBands;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{
		for (int y = 0; y < height; y++) {
			int indexSrc = input.startIndex + (srcY + y)*input.stride + srcX*numBands;
			int indexDst = output.startIndex + (dstY + y)*output.stride + dstX*numBands;

			System.arraycopy(input.data, indexSrc, output.data, indexDst, width*numBands);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fill( GrayF64 input, double value ) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride();
			Arrays.fill(input.data, index, index + input.width, value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fill( InterleavedF64 input, double value ) {
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride();
			int end = index + input.width*input.numBands;
			Arrays.fill(input.data, index, end, value);
		}
	}

	public static void fill( InterleavedF64 input, double[] values ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			for (int band = 0; band < numBands; band++) {
				int index = input.getStartIndex() + y*input.getStride() + band;
				int end = index + input.width*numBands - band;
				double value = values[band];
				for (; index < end; index += numBands) {
					input.data[index] = value;
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBand( InterleavedF64 input, int band, double value ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int index = input.getStartIndex() + y*input.getStride() + band;
			int end = index + input.width*numBands - band;
			for (; index < end; index += numBands) {
				input.data[index] = value;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void insertBand( GrayF64 input, int band, InterleavedF64 output ) {
		final int numBands = output.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.getStartIndex() + y*input.getStride();
			int indexOut = output.getStartIndex() + y*output.getStride() + band;
			int end = indexOut + output.width*numBands - band;
			for (; indexOut < end; indexOut += numBands, indexIn++) {
				output.data[indexOut] = input.data[indexIn];
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void extractBand( InterleavedF64 input, int band, GrayF64 output ) {
		final int numBands = input.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.getStartIndex() + y*input.getStride() + band;
			int indexOut = output.getStartIndex() + y*output.getStride();
			int end = indexOut + output.width;
			for (; indexOut < end; indexIn += numBands, indexOut++) {
				output.data[indexOut] = input.data[indexIn];
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBorder( GrayF64 input, double value, int radius ) {
		// top and bottom
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,radius,y->{
		for (int y = 0; y < radius; y++) {
			int indexTop = input.startIndex + y*input.stride;
			int indexBottom = input.startIndex + (input.height - y - 1)*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[indexTop++] = value;
				input.data[indexBottom++] = value;
			}
		}
		//CONCURRENT_ABOVE });

		// left and right
		int h = input.height - radius;
		int indexStart = input.startIndex + radius*input.stride;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,radius,x->{
		for (int x = 0; x < radius; x++) {
			int indexLeft = indexStart + x;
			int indexRight = indexStart + input.width - 1 - x;
			for (int y = radius; y < h; y++) {
				input.data[indexLeft] = value;
				input.data[indexRight] = value;

				indexLeft += input.stride;
				indexRight += input.stride;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillBorder( GrayF64 input, double value, int borderX0, int borderY0, int borderX1, int borderY1 ) {
		// top and bottom
		for (int y = 0; y < borderY0; y++) {
			int srcIdx = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[srcIdx++] = value;
			}
		}
		for (int y = input.height - borderY1; y < input.height; y++) {
			int srcIdx = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				input.data[srcIdx++] = value;
			}
		}

		// left and right
		int h = input.height - borderY1;
		for (int x = 0; x < borderX0; x++) {
			int srcIdx = input.startIndex + borderY0*input.stride + x;
			for (int y = borderY0; y < h; y++) {
				input.data[srcIdx] = value;
				srcIdx += input.stride;
			}
		}
		for (int x = input.width - borderX1; x < input.width; x++) {
			int srcIdx = input.startIndex + borderY0*input.stride + x;
			for (int y = borderY0; y < h; y++) {
				input.data[srcIdx] = value;
				srcIdx += input.stride;
			}
		}
	}

	public static void fillRectangle( GrayF64 image, double value, int x0, int y0, int width, int height ) {
		int x1 = x0 + width;
		int y1 = y0 + height;

		if (x0 < 0) x0 = 0; if (x1 > image.width) x1 = image.width;
		if (y0 < 0) y0 = 0; if (y1 > image.height) y1 = image.height;
		final int _x0 = x0;
		final int _x1 = x1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1 , y->{
		for (int y = y0; y < y1; y++) {
			int index = image.startIndex + y*image.stride + _x0;
			Arrays.fill(image.data, index, index + _x1 - _x0, value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillRectangle(InterleavedF64 image, double value, int x0, int y0, int width, int height ) {
		int x1 = x0 + width;
		int y1 = y0 + height;

		if (x0 < 0) x0 = 0; if (x1 > image.width) x1 = image.width;
		if (y0 < 0) y0 = 0; if (y1 > image.height) y1 = image.height;
		final int _x0 = x0;

		int length = (x1 - x0)*image.numBands;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1, y->{
		for (int y = y0; y < y1; y++) {
			int index = image.startIndex + y*image.stride + _x0*image.numBands;
			Arrays.fill(image.data, index, index + length, value);
		}
		//CONCURRENT_ABOVE });
	}

	public static void fillUniform( GrayF64 image, Random rand, double min, double max ) {
		double range = max - min;

		double[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				data[index++] = rand.nextDouble()*range + min;
			}
		}
	}

	public static void fillUniform( InterleavedF64 image, Random rand, double min, double max ) {
		double range = max - min;

		double[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int end = index + image.width*image.numBands;
			for (; index < end; index++) {
				data[index] = rand.nextDouble()*range + min;
			}
		}
	}

	public static void fillGaussian( GrayF64 image, Random rand, double mean, double sigma, double lowerBound, double upperBound ) {
		double[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				double value = (rand.nextGaussian()*sigma + mean);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				data[index++] = value;
			}
		}
	}

	public static void fillGaussian( InterleavedF64 image, Random rand, double mean, double sigma, double lowerBound, double upperBound ) {
		double[] data = image.data;
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int indexEnd = index + length;

			while (index < indexEnd) {
				double value = (rand.nextGaussian()*sigma + mean);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				data[index++] = value;
			}
		}
	}

	public static void flipVertical( GrayF64 image ) {
		int h2 = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h2, y->{
		for (int y = 0; y < h2; y++) {
			int index1 = image.getStartIndex() + y*image.getStride();
			int index2 = image.getStartIndex() + (image.height - y - 1)*image.getStride();

			int end = index1 + image.width;

			while (index1 < end) {
				double tmp = image.data[index1];
				image.data[index1++] = image.data[index2];
				image.data[index2++] = (double)tmp;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void flipHorizontal( GrayF64 image ) {
		int w2 = image.width/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, y->{
		for (int y = 0; y < image.height; y++) {
			int index1 = image.getStartIndex() + y*image.getStride();
			int index2 = index1 + image.width - 1;

			int end = index1 + w2;

			while (index1 < end) {
				double tmp = image.data[index1];
				image.data[index1++] = image.data[index2];
				image.data[index2--] = (double)tmp;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void transpose( GrayF64 input, GrayF64 output ) {
		output.reshape(input.height, input.width);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(y, x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void transpose( InterleavedF64 input, InterleavedF64 output ) {
		output.reshape(input.height, input.width, input.numBands);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(y, x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( GrayF64 image ) {
		if (image.width != image.height)
			throw new IllegalArgumentException("Image must be square");

		int w = image.height/2 + image.height%2;
		int h = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h, y0->{
		for (int y0 = 0; y0 < h; y0++) {
			int y1 = image.height - y0 - 1;

			for (int x0 = 0; x0 < w; x0++) {
				int x1 = image.width - x0 - 1;

				int index0 = image.startIndex + y0*image.stride + x0;
				int index1 = image.startIndex + x0*image.stride + y1;
				int index2 = image.startIndex + y1*image.stride + x1;
				int index3 = image.startIndex + x1*image.stride + y0;

				double tmp3 = image.data[index3];

				image.data[index3] = image.data[index2];
				image.data[index2] = image.data[index1];
				image.data[index1] = image.data[index0];
				image.data[index0] = (double)tmp3;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( GrayF64 input, GrayF64 output ) {
		output.reshape(input.height, input.width);

		int h = input.height - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(h - y, x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCW( InterleavedF64 input, InterleavedF64 output ) {
		output.reshape(input.height, input.width, input.numBands);

		int h = input.height - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(h - y, x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( GrayF64 image ) {
		if (image.width != image.height)
			throw new IllegalArgumentException("Image must be square");

		int w = image.height/2 + image.height%2;
		int h = image.height/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h, y0->{
		for (int y0 = 0; y0 < h; y0++) {
			int y1 = image.height - y0 - 1;

			for (int x0 = 0; x0 < w; x0++) {
				int x1 = image.width - x0 - 1;

				int index0 = image.startIndex + y0*image.stride + x0;
				int index1 = image.startIndex + x0*image.stride + y1;
				int index2 = image.startIndex + y1*image.stride + x1;
				int index3 = image.startIndex + x1*image.stride + y0;

				double tmp0 = image.data[index0];

				image.data[index0] = image.data[index1];
				image.data[index1] = image.data[index2];
				image.data[index2] = image.data[index3];
				image.data[index3] = (double)tmp0;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( GrayF64 input, GrayF64 output ) {
		output.reshape(input.height, input.width);

		int w = input.width - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				output.unsafe_set(y, w - x, input.data[indexIn++]);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rotateCCW( InterleavedF64 input, InterleavedF64 output ) {
		output.reshape(input.height, input.width, input.numBands);

		int w = input.width - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{
		for (int y = 0; y < input.height; y++) {
			int indexSrc = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++) {
				int indexDst = output.getIndex(y, w - x);

				int end = indexSrc + input.numBands;
				while (indexSrc != end) {
					output.data[indexDst++] = input.data[indexSrc++];
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void growBorder( GrayF64 src, ImageBorder_F64 border, int borderX0, int borderY0, int borderX1, int borderY1, GrayF64 dst ) {
		dst.reshape(src.width + borderX0 + borderX1, src.height + borderY0 + borderY1);
		border.setImage(src);

		// Copy src into the inner portion of dst
		ImageMiscOps.copy(0, 0, borderX0, borderY0, src.width, src.height, src, dst);

		// Top border
		for (int y = 0; y < borderY0; y++) {
			int idxDst = dst.startIndex + y*dst.stride;
			for (int x = 0; x < dst.width; x++) {
				dst.data[idxDst++] = border.get(x - borderX0, y - borderY0);
			}
		}
		// Bottom border
		for (int y = 0; y < borderY1; y++) {
			int idxDst = dst.startIndex + (dst.height - borderY1 + y)*dst.stride;
			for (int x = 0; x < dst.width; x++) {
				dst.data[idxDst++] = border.get(x - borderX0, src.height + y);
			}
		}
		// Left and right border
		for (int y = borderY0; y < dst.height - borderY1; y++) {
			int idxDst = dst.startIndex + y*dst.stride;
			for (int x = 0; x < borderX0; x++) {
				dst.data[idxDst++] = border.get(x - borderX0, y - borderY0);
			}
			idxDst = dst.startIndex + y*dst.stride + src.width + borderX0;
			for (int x = 0; x < borderX1; x++) {
				dst.data[idxDst++] = border.get(src.width + x, y - borderY0);
			}
		}
	}

	public static void findAndProcess( GrayF64 input, BoofLambdas.Match_F64 finder, BoofLambdas.ProcessIIB process ) {
		for (int y = 0; y < input.height; y++) {
			int index = input.startIndex + y*input.stride;
			for (int x = 0; x < input.width; x++, index++) {
				if (finder.process(input.data[index])) {
					if (!process.process(x, y))
						return;
				}
			}
		}
	}

	public static void addUniform( GrayU8 image, Random rand, int min, int max ) {
		int range = max - min;

		byte[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				int value = (data[index] & 0xFF) + rand.nextInt(range) + min;
				if (value < 0) value = 0;
				if (value > 255) value = 255;

				data[index++] = (byte)value;
			}
		}
	}

	public static void addUniform( InterleavedU8 image, Random rand, int min, int max ) {
		int range = max - min;

		byte[] data = image.data;
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();

			int indexEnd = index + length;
			while (index < indexEnd) {
				int value = (data[index] & 0xFF) + rand.nextInt(range) + min;
				if (value < 0) value = 0;
				if (value > 255) value = 255;

				data[index++] = (byte)value;
			}
		}
	}

	public static void addGaussian( GrayU8 image, Random rand, double sigma, int lowerBound, int upperBound ) {

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				int value = (image.data[index] & 0xFF) + (int)(rand.nextGaussian()*sigma);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				image.data[index++] = (byte)value;
			}
		}
	}

	public static void addGaussian( InterleavedU8 image, Random rand, double sigma, int lowerBound, int upperBound ) {
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int indexEnd = index + length;
			while (index < indexEnd) {
				int value = (image.data[index] & 0xFF) + (int)(rand.nextGaussian()*sigma);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				image.data[index++] = (byte)value;
			}
		}
	}

	public static void addUniform( GrayS8 image, Random rand, int min, int max ) {
		int range = max - min;

		byte[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				int value = (data[index]) + rand.nextInt(range) + min;
				if (value < -128) value = -128;
				if (value > 127) value = 127;

				data[index++] = (byte)value;
			}
		}
	}

	public static void addUniform( InterleavedS8 image, Random rand, int min, int max ) {
		int range = max - min;

		byte[] data = image.data;
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();

			int indexEnd = index + length;
			while (index < indexEnd) {
				int value = (data[index]) + rand.nextInt(range) + min;
				if (value < -128) value = -128;
				if (value > 127) value = 127;

				data[index++] = (byte)value;
			}
		}
	}

	public static void addGaussian( GrayS8 image, Random rand, double sigma, int lowerBound, int upperBound ) {

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				int value = (image.data[index]) + (int)(rand.nextGaussian()*sigma);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				image.data[index++] = (byte)value;
			}
		}
	}

	public static void addGaussian( InterleavedS8 image, Random rand, double sigma, int lowerBound, int upperBound ) {
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int indexEnd = index + length;
			while (index < indexEnd) {
				int value = (image.data[index]) + (int)(rand.nextGaussian()*sigma);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				image.data[index++] = (byte)value;
			}
		}
	}

	public static void addUniform( GrayU16 image, Random rand, int min, int max ) {
		int range = max - min;

		short[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				int value = (data[index] & 0xFFFF) + rand.nextInt(range) + min;
				if (value < 0) value = 0;
				if (value > 65535) value = 65535;

				data[index++] = (short)value;
			}
		}
	}

	public static void addUniform( InterleavedU16 image, Random rand, int min, int max ) {
		int range = max - min;

		short[] data = image.data;
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();

			int indexEnd = index + length;
			while (index < indexEnd) {
				int value = (data[index] & 0xFFFF) + rand.nextInt(range) + min;
				if (value < 0) value = 0;
				if (value > 65535) value = 65535;

				data[index++] = (short)value;
			}
		}
	}

	public static void addGaussian( GrayU16 image, Random rand, double sigma, int lowerBound, int upperBound ) {

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				int value = (image.data[index] & 0xFFFF) + (int)(rand.nextGaussian()*sigma);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				image.data[index++] = (short)value;
			}
		}
	}

	public static void addGaussian( InterleavedU16 image, Random rand, double sigma, int lowerBound, int upperBound ) {
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int indexEnd = index + length;
			while (index < indexEnd) {
				int value = (image.data[index] & 0xFFFF) + (int)(rand.nextGaussian()*sigma);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				image.data[index++] = (short)value;
			}
		}
	}

	public static void addUniform( GrayS16 image, Random rand, int min, int max ) {
		int range = max - min;

		short[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				int value = (data[index]) + rand.nextInt(range) + min;
				if (value < -32768) value = -32768;
				if (value > 32767) value = 32767;

				data[index++] = (short)value;
			}
		}
	}

	public static void addUniform( InterleavedS16 image, Random rand, int min, int max ) {
		int range = max - min;

		short[] data = image.data;
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();

			int indexEnd = index + length;
			while (index < indexEnd) {
				int value = (data[index]) + rand.nextInt(range) + min;
				if (value < -32768) value = -32768;
				if (value > 32767) value = 32767;

				data[index++] = (short)value;
			}
		}
	}

	public static void addGaussian( GrayS16 image, Random rand, double sigma, int lowerBound, int upperBound ) {

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				int value = (image.data[index]) + (int)(rand.nextGaussian()*sigma);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				image.data[index++] = (short)value;
			}
		}
	}

	public static void addGaussian( InterleavedS16 image, Random rand, double sigma, int lowerBound, int upperBound ) {
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int indexEnd = index + length;
			while (index < indexEnd) {
				int value = (image.data[index]) + (int)(rand.nextGaussian()*sigma);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				image.data[index++] = (short)value;
			}
		}
	}

	public static void addUniform( GrayS32 image, Random rand, int min, int max ) {
		int range = max - min;

		int[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				int value = (data[index]) + rand.nextInt(range) + min;
				data[index++] = value;
			}
		}
	}

	public static void addUniform( InterleavedS32 image, Random rand, int min, int max ) {
		int range = max - min;

		int[] data = image.data;
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();

			int indexEnd = index + length;
			while (index < indexEnd) {
				int value = (data[index]) + rand.nextInt(range) + min;
				data[index++] = value;
			}
		}
	}

	public static void addGaussian( GrayS32 image, Random rand, double sigma, int lowerBound, int upperBound ) {

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				int value = (image.data[index]) + (int)(rand.nextGaussian()*sigma);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				image.data[index++] = value;
			}
		}
	}

	public static void addGaussian( InterleavedS32 image, Random rand, double sigma, int lowerBound, int upperBound ) {
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int indexEnd = index + length;
			while (index < indexEnd) {
				int value = (image.data[index]) + (int)(rand.nextGaussian()*sigma);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				image.data[index++] = value;
			}
		}
	}

	public static void addUniform( GrayS64 image, Random rand, long min, long max ) {
		long range = max - min;

		long[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				long value = data[index] + rand.nextInt((int)range) + min;
				data[index++] = value;
			}
		}
	}

	public static void addUniform( InterleavedS64 image, Random rand, long min, long max ) {
		long range = max - min;

		long[] data = image.data;
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();

			int indexEnd = index + length;
			while (index < indexEnd) {
				long value = data[index] + rand.nextInt((int)range) + min;
				data[index++] = value;
			}
		}
	}

	public static void addGaussian( GrayS64 image, Random rand, double sigma, long lowerBound, long upperBound ) {

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				long value = (image.data[index]) + (long)(rand.nextGaussian()*sigma);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				image.data[index++] = value;
			}
		}
	}

	public static void addGaussian( InterleavedS64 image, Random rand, double sigma, long lowerBound, long upperBound ) {
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int indexEnd = index + length;
			while (index < indexEnd) {
				long value = (image.data[index]) + (long)(rand.nextGaussian()*sigma);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				image.data[index++] = value;
			}
		}
	}

	public static void addUniform( GrayF32 image, Random rand, float min, float max ) {
		float range = max - min;

		float[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				float value = data[index] + rand.nextFloat()*range + min;
				data[index++] = value;
			}
		}
	}

	public static void addUniform( InterleavedF32 image, Random rand, float min, float max ) {
		float range = max - min;

		float[] data = image.data;
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();

			int indexEnd = index + length;
			while (index < indexEnd) {
				float value = data[index] + rand.nextFloat()*range + min;
				data[index++] = value;
			}
		}
	}

	public static void addGaussian( GrayF32 image, Random rand, double sigma, float lowerBound, float upperBound ) {

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				float value = (image.data[index]) + (float)(rand.nextGaussian()*sigma);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				image.data[index++] = value;
			}
		}
	}

	public static void addGaussian( InterleavedF32 image, Random rand, double sigma, float lowerBound, float upperBound ) {
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int indexEnd = index + length;
			while (index < indexEnd) {
				float value = (image.data[index]) + (float)(rand.nextGaussian()*sigma);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				image.data[index++] = value;
			}
		}
	}

	public static void addUniform( GrayF64 image, Random rand, double min, double max ) {
		double range = max - min;

		double[] data = image.data;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				double value = data[index] + rand.nextDouble()*range + min;
				data[index++] = value;
			}
		}
	}

	public static void addUniform( InterleavedF64 image, Random rand, double min, double max ) {
		double range = max - min;

		double[] data = image.data;
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();

			int indexEnd = index + length;
			while (index < indexEnd) {
				double value = data[index] + rand.nextDouble()*range + min;
				data[index++] = value;
			}
		}
	}

	public static void addGaussian( GrayF64 image, Random rand, double sigma, double lowerBound, double upperBound ) {

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.width; x++) {
				double value = (image.data[index]) + (rand.nextGaussian()*sigma);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				image.data[index++] = value;
			}
		}
	}

	public static void addGaussian( InterleavedF64 image, Random rand, double sigma, double lowerBound, double upperBound ) {
		int length = image.width*image.numBands;

		for (int y = 0; y < image.height; y++) {
			int index = image.getStartIndex() + y*image.getStride();
			int indexEnd = index + length;
			while (index < indexEnd) {
				double value = (image.data[index]) + (rand.nextGaussian()*sigma);
				if (value < lowerBound) value = lowerBound;
				if (value > upperBound) value = upperBound;
				image.data[index++] = value;
			}
		}
	}

}
