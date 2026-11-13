/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.census.impl;

import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayS64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.InterleavedU16;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastAccess;

/// Operations for handling borders in a Census Transform.
public class ImplCensusTransformBorder {

	public static int regionNxN( final ImageBorder_S32 input , int cx , int cy , int radius ) {
		int center = input.get(cx, cy);
		int census = 0;

		int bit = 1;
		for (int row = -radius; row <= radius; row++) {
			for (int col = -radius; col <= radius; col++) {
				if( row == 0 && col == 0 ) continue;

				if( input.get(cx+col,cy+row) > center )
					census |= bit;
				bit <<= 1;
			}
		}

		return census;
	}

	public static int regionNxN(final ImageBorder_F32 input , int cx , int cy , int radius ) {
		float center = input.get(cx, cy);
		int census = 0;

		int bit = 1;
		for (int row = -radius; row <= radius; row++) {
			for (int col = -radius; col <= radius; col++) {
				if( row == 0 && col == 0 ) continue;

				if( input.get(cx+col,cy+row) > center )
					census |= bit;
				bit <<= 1;
			}
		}

		return census;
	}

	public static void dense3x3_U8(final ImageBorder_S32 input, final GrayU8 output ) {
		final int radius = 1;
		final int width = output.width;
		final int height = output.height;

		// Each border pixel is computed once. Rows within the radius of the top or bottom, and every row when the
		// image is too narrow to have an inner part, are entirely border. Other rows skip over the inner image.
		for (int y = 0; y < height; y++) {
			boolean wholeRow = y < radius || y >= height - radius || width <= 2*radius;
			int indexDst = output.startIndex + y*output.stride;
			for (int x = 0; x < width; x++) {
				if (!wholeRow && x == radius)
					x = width - radius;
				output.data[indexDst + x] = (byte)regionNxN(input, x, y, radius);
			}
		}
	}

	public static void dense3x3_F32(final ImageBorder_F32 input, final GrayU8 output ) {
		final int radius = 1;
		final int width = output.width;
		final int height = output.height;

		// Each border pixel is computed once. Rows within the radius of the top or bottom, and every row when the
		// image is too narrow to have an inner part, are entirely border. Other rows skip over the inner image.
		for (int y = 0; y < height; y++) {
			boolean wholeRow = y < radius || y >= height - radius || width <= 2*radius;
			int indexDst = output.startIndex + y*output.stride;
			for (int x = 0; x < width; x++) {
				if (!wholeRow && x == radius)
					x = width - radius;
				output.data[indexDst + x] = (byte)regionNxN(input, x, y, radius);
			}
		}
	}

	public static void dense5x5_U8(final ImageBorder_S32 input, final GrayS32 output ) {
		final int radius = 2;
		final int width = output.width;
		final int height = output.height;

		// Each border pixel is computed once. Rows within the radius of the top or bottom, and every row when the
		// image is too narrow to have an inner part, are entirely border. Other rows skip over the inner image.
		for (int y = 0; y < height; y++) {
			boolean wholeRow = y < radius || y >= height - radius || width <= 2*radius;
			int indexDst = output.startIndex + y*output.stride;
			for (int x = 0; x < width; x++) {
				if (!wholeRow && x == radius)
					x = width - radius;
				output.data[indexDst + x] = regionNxN(input, x, y, radius);
			}
		}
	}

	public static void dense5x5_F32(final ImageBorder_F32 input, final GrayS32 output ) {
		final int radius = 2;
		final int width = output.width;
		final int height = output.height;

		// Each border pixel is computed once. Rows within the radius of the top or bottom, and every row when the
		// image is too narrow to have an inner part, are entirely border. Other rows skip over the inner image.
		for (int y = 0; y < height; y++) {
			boolean wholeRow = y < radius || y >= height - radius || width <= 2*radius;
			int indexDst = output.startIndex + y*output.stride;
			for (int x = 0; x < width; x++) {
				if (!wholeRow && x == radius)
					x = width - radius;
				output.data[indexDst + x] = regionNxN(input, x, y, radius);
			}
		}
	}

	public static void sample_S64(final ImageBorder_S32 input, final int radius , final FastAccess<Point2D_I32> offsets,
								  GrayS64 output  )
	{
		final int width = output.width;
		final int height = output.height;

		// Each border pixel is computed once. Rows within the radius of the top or bottom, and every row when the
		// image is too narrow to have an inner part, are entirely border. Other rows skip over the inner image.
		for (int y = 0; y < height; y++) {
			boolean wholeRow = y < radius || y >= height - radius || width <= 2*radius;
			int indexDst = output.startIndex + y*output.stride;
			for (int x = 0; x < width; x++) {
				if (!wholeRow && x == radius)
					x = width - radius;
				output.data[indexDst + x] = sample_S64(input, x, y, offsets);
			}
		}
	}

	public static void sample_S64(final ImageBorder_F32 input, final int radius , final FastAccess<Point2D_I32> offsets,
								  GrayS64 output  )
	{
		final int width = output.width;
		final int height = output.height;

		// Each border pixel is computed once. Rows within the radius of the top or bottom, and every row when the
		// image is too narrow to have an inner part, are entirely border. Other rows skip over the inner image.
		for (int y = 0; y < height; y++) {
			boolean wholeRow = y < radius || y >= height - radius || width <= 2*radius;
			int indexDst = output.startIndex + y*output.stride;
			for (int x = 0; x < width; x++) {
				if (!wholeRow && x == radius)
					x = width - radius;
				output.data[indexDst + x] = sample_S64(input, x, y, offsets);
			}
		}
	}

	public static short sample( final ImageBorder_S32 input , int cx , int cy ,
								  final FastAccess<Point2D_I32> offsets , int idx0 , int idx1 ) {
		int center = input.get(cx, cy);
		int census = 0;

		int bit = 1;
		for (int i = idx0; i < idx1; i++) {
			Point2D_I32 p = offsets.data[i];
			if( input.get(cx+p.x,cy+p.y) > center )
				census |= bit;
			bit <<= 1;
		}

		return (short)census;
	}

	public static short sample( final ImageBorder_F32 input , int cx , int cy ,
								final FastAccess<Point2D_I32> offsets , int idx0 , int idx1 ) {
		float center = input.get(cx, cy);
		int census = 0;

		int bit = 1;
		for (int i = idx0; i < idx1; i++) {
			Point2D_I32 p = offsets.data[i];
			if( input.get(cx+p.x,cy+p.y) > center )
				census |= bit;
			bit <<= 1;
		}

		return (short)census;
	}

	public static long sample_S64( final ImageBorder_S32 input , int cx , int cy ,
								   final FastAccess<Point2D_I32> offsets ) {
		int center = input.get(cx, cy);
		long census = 0;

		long bit = 1;
		for (int i = 0; i < offsets.size; i++) {
			Point2D_I32 p = offsets.data[i];
			if( input.get(cx+p.x,cy+p.y) > center )
				census |= bit;
			bit <<= 1;
		}

		return census;
	}

	public static long sample_S64( final ImageBorder_F32 input , int cx , int cy ,
								   final FastAccess<Point2D_I32> offsets ) {
		float center = input.get(cx, cy);
		long census = 0;

		long bit = 1;
		for (int i = 0; i < offsets.size; i++) {
			Point2D_I32 p = offsets.data[i];
			if( input.get(cx+p.x,cy+p.y) > center )
				census |= bit;
			bit <<= 1;
		}

		return census;
	}

	public static void sample_IU16(final ImageBorder_S32 input , final int radius , final FastAccess<Point2D_I32> offsets,
								   final InterleavedU16 output ) {
		final int numBands = output.numBands;
		final int fullBlocks = offsets.size/16;

		final int width = output.width;
		final int height = output.height;

		// Each border pixel is computed once. Rows within the radius of the top or bottom, and every row when the
		// image is too narrow to have an inner part, are entirely border. Other rows skip over the inner image.
		for (int y = 0; y < height; y++) {
			boolean wholeRow = y < radius || y >= height - radius || width <= 2*radius;
			int indexDst = output.startIndex + y*output.stride;
			for (int x = 0; x < width; x++) {
				if (!wholeRow && x == radius)
					x = width - radius;
				int index = indexDst + x*numBands;
				for (int i = 0; i < fullBlocks; i++) {
					output.data[index++] = sample(input, x, y, offsets, i*16, i*16 + 16);
				}
				if (numBands != fullBlocks)
					output.data[index] = sample(input, x, y, offsets, fullBlocks*16, offsets.size);
			}
		}
	}

	public static void sample_IU16(final ImageBorder_F32 input , final int radius , final FastAccess<Point2D_I32> offsets,
								   final InterleavedU16 output ) {
		final int numBands = output.numBands;
		final int fullBlocks = offsets.size/16;

		final int width = output.width;
		final int height = output.height;

		// Each border pixel is computed once. Rows within the radius of the top or bottom, and every row when the
		// image is too narrow to have an inner part, are entirely border. Other rows skip over the inner image.
		for (int y = 0; y < height; y++) {
			boolean wholeRow = y < radius || y >= height - radius || width <= 2*radius;
			int indexDst = output.startIndex + y*output.stride;
			for (int x = 0; x < width; x++) {
				if (!wholeRow && x == radius)
					x = width - radius;
				int index = indexDst + x*numBands;
				for (int i = 0; i < fullBlocks; i++) {
					output.data[index++] = sample(input, x, y, offsets, i*16, i*16 + 16);
				}
				if (numBands != fullBlocks)
					output.data[index] = sample(input, x, y, offsets, fullBlocks*16, offsets.size);
			}
		}
	}
}
