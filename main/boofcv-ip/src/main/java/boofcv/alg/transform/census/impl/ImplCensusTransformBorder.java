/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.GrayU8;

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

	public static void region3x3(final ImageBorder_S32 input, final GrayU8 output ) {
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

	public static void region5x5(final ImageBorder_S32 input, final GrayS32 output ) {
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
}
