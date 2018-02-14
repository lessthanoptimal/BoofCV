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

package boofcv.alg.filter.binary.impl;

import boofcv.alg.filter.binary.ThresholdBlockMean;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayU8;

/**
 * Implementation of {@link ThresholdBlockMean} for input images of type {@link GrayU8}
 *
 * @author Peter Abeles
 */
public class ThresholdBlockMean_U8
	extends ThresholdBlockMean<GrayU8>
{
	double scale;
	boolean down;

	public ThresholdBlockMean_U8(ConfigLength requestedBlockWidth, double scale , boolean down,
								 boolean thresholdFromLocalBlocks ) {
		super(requestedBlockWidth,thresholdFromLocalBlocks,GrayU8.class);
		this.stats = new GrayU8(1,1);
		this.scale = scale;
		this.down = down;
	}

	@Override
	protected void thresholdBlock(int blockX0 , int blockY0 , GrayU8 input, GrayU8 output ) {

		int x0 = blockX0*blockWidth;
		int y0 = blockY0*blockHeight;

		int x1 = blockX0==stats.width-1 ? input.width : (blockX0+1)*blockWidth;
		int y1 = blockY0==stats.height-1 ? input.height: (blockY0+1)*blockHeight;

		// define the local 3x3 region in blocks, taking in account the image border
		int blockX1, blockY1;
		if(thresholdFromLocalBlocks) {
			blockX1 = Math.min(stats.width - 1, blockX0 + 1);
			blockY1 = Math.min(stats.height - 1, blockY0 + 1);

			blockX0 = Math.max(0, blockX0 - 1);
			blockY0 = Math.max(0, blockY0 - 1);
		} else {
			blockX1 = blockX0;
			blockY1 = blockY0;
		}

		// Average the mean across local blocks
		int mean = 0;

		for (int y = blockY0; y <= blockY1; y++) {
			for (int x = blockX0; x <= blockX1; x++) {
				mean += stats.unsafe_get(x,y);
			}
		}
		mean /= (blockY1-blockY0+1)*(blockX1-blockX0+1);


		// apply threshold
		final byte a,b;
		if( down ) {
			a = 1; b = 0;
		} else {
			a = 0; b = 1;
		}
		for (int y = y0; y < y1; y++) {
			int indexInput = input.startIndex + y * input.stride + x0;
			int indexOutput = output.startIndex + y * output.stride + x0;
			int end = indexOutput + (x1 - x0);
			for (; indexOutput < end; indexOutput++, indexInput++) {
				output.data[indexOutput] = (input.data[indexInput]&0xFF) <= mean ? (byte) a : b;
			}
		}
	}

	@Override
	protected void computeBlockStatistics(int x0, int y0, int width, int height, int indexStats, GrayU8 input) {

		int sum = 0;

		for (int y = 0; y < height; y++) {
			int indexInput = input.startIndex + (y0+y)*input.stride + x0;
			for (int x = 0; x < width; x++) {
				sum += input.data[indexInput++] & 0xFF;
			}
		}
		sum = (int)(scale*sum/(width*height)+0.5);


		stats.data[indexStats]   = (byte)sum;
	}
}
