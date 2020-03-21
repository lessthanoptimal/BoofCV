/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayS64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.InterleavedU16;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastAccess;

/**
 * @author Peter Abeles
 */
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

	public static void dense3x3_U8(final ImageBorder_S32 input, final GrayU8 output ) {
		final int width = output.width;
		final int height = output.height;

		int indexDst0 = output.startIndex;
		int indexDst1 = output.startIndex + (output.height-1)*output.stride;
		final int h = output.height-1;

		for (int x = 0; x < width; x++) {
			output.data[indexDst0++] = (byte) regionNxN(input,x,0,1);
			output.data[indexDst1++] = (byte) regionNxN(input,x,h,1);
		}

		indexDst0 = output.startIndex + output.stride;
		indexDst1 = output.startIndex + output.stride + width-1;
		for (int y = 1; y < height-1; y++) {
			output.data[indexDst0] = (byte) regionNxN(input,0,y,1);
			output.data[indexDst1] = (byte) regionNxN(input,width-1,y,1);

			indexDst0 += output.stride;
			indexDst1 += output.stride;
		}
	}

	public static void dense5x5_U8(final ImageBorder_S32 input, final GrayS32 output ) {
		final int width = output.width;
		final int height = output.height;

		int indexDst0 = output.startIndex;
		int indexDst1 = output.startIndex + output.stride;
		int indexDst2 = output.startIndex + (output.height-2)*output.stride;
		int indexDst3 = output.startIndex + (output.height-1)*output.stride;

		for (int x = 0; x < width; x++) {
			output.data[indexDst0++] = regionNxN(input,x,0,2);
			output.data[indexDst1++] = regionNxN(input,x,1,2);
			output.data[indexDst2++] = regionNxN(input,x,height-2,2);
			output.data[indexDst3++] = regionNxN(input,x,height-1,2);
		}

		indexDst0 = output.startIndex + output.stride;
		indexDst1 = output.startIndex + output.stride + 1;
		indexDst2 = output.startIndex + output.stride + width-2;
		indexDst3 = output.startIndex + output.stride + width-1;

		for (int y = 1; y < height-1; y++) {
			output.data[indexDst0] = regionNxN(input,0,y,2);
			output.data[indexDst1] = regionNxN(input,1,y,2);
			output.data[indexDst2] = regionNxN(input,width-2,y,2);
			output.data[indexDst3] = regionNxN(input,width-1,y,2);

			indexDst0 += output.stride;
			indexDst1 += output.stride;
			indexDst2 += output.stride;
			indexDst3 += output.stride;
		}
	}

	public static void sample_S64(final ImageBorder_S32 input, final int radius , final FastAccess<Point2D_I32> offsets,
								  GrayS64 output  )
	{
		final int width = output.width;
		final int height = output.height;

		for (int r = 0; r < radius; r++) {
			int indexDst0 = output.startIndex + r*output.stride;
			int indexDst1 = output.startIndex + (height-r-1)*output.stride;
			for (int x = 0; x < width; x++) {
				output.data[indexDst0++] = sample_S64(input,x,r,offsets);
				output.data[indexDst1++] = sample_S64(input,x,height-r-1,offsets);
			}
		}

		for (int r = 0; r < radius; r++) {
			int indexDst0 = output.startIndex + radius*output.stride+r;
			int indexDst1 = output.startIndex + radius*output.stride+(width-r-1);
			for (int y = radius; y < height-radius; y++) {
				output.data[indexDst0] = sample_S64(input,r,y,offsets);
				output.data[indexDst1] = sample_S64(input,width-r-1,y,offsets);
				indexDst0 += output.stride;
				indexDst1 += output.stride;
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

	public static long sample_S64( final ImageBorder_S32 input , int cx , int cy ,
								   final FastAccess<Point2D_I32> offsets ) {
		int center = input.get(cx, cy);
		long census = 0;

		int bit = 1;
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
		final int width = output.width;
		final int height = output.height;

		int numBands = output.numBands;

		int fullBlocks = offsets.size/16;

		for (int r = 0; r < radius; r++) {
			int indexDst0 = output.startIndex + r*output.stride;
			int indexDst1 = output.startIndex + (height-r-1)*output.stride;
			for (int x = 0; x < width; x++) {
				for (int i = 0; i < fullBlocks; i++) {
					int idx0 = i*16;
					int idx1 = idx0+16;
					output.data[indexDst0++] = sample(input,x,r,offsets,idx0,idx1);
					output.data[indexDst1++] = sample(input,x,height-r-1,offsets,idx0,idx1);
				}
				if( numBands != fullBlocks) {
					output.data[indexDst0++] = sample(input,x,r,offsets,fullBlocks*16,offsets.size);
					output.data[indexDst1++] = sample(input,x,height-r-1,offsets,fullBlocks*16,offsets.size);
				}
			}
		}

		for (int r = 0; r < radius; r++) {
			for (int y = radius; y < height-radius; y++) {
				int indexDst0 = output.startIndex + y*output.stride+r*numBands;
				int indexDst1 = output.startIndex + y*output.stride+(width-r-1)*numBands;
				for (int i = 0; i < fullBlocks; i++) {
					int idx0 = i*16;
					int idx1 = idx0+16;
					output.data[indexDst0++] = sample(input,r,y,offsets,idx0,idx1);
					output.data[indexDst1++] = sample(input,width-r-1,y,offsets,idx0,idx1);
					}
				if( numBands != fullBlocks) {
					output.data[indexDst0++] = sample(input,r,y,offsets,fullBlocks*16,offsets.size);
					output.data[indexDst1++] = sample(input,width-r-1,y,offsets,fullBlocks*16,offsets.size);
				}
			}
		}
	}
}
