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

package boofcv.alg.filter.binary.impl;

import boofcv.alg.filter.binary.ThresholdBlockMean;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;

/**
 * Implementation of {@link ThresholdBlockMean} for input images of type {@link GrayU8}
 *
 * @author Peter Abeles
 */
public class ThresholdBlockMean_F32
	extends ThresholdBlockMean<GrayF32>
{
	float scale;
	boolean down;

	public ThresholdBlockMean_F32(int requestedBlockWidth, double scale , boolean down ) {
		super(requestedBlockWidth,GrayF32.class);
		this.stats = new GrayF32(1,1);
		this.scale = (float)scale;
		this.down = down;
	}

	@Override
	protected void thresholdBlock(int blockX0 , int blockY0 , GrayF32 input, GrayU8 output ) {

		int x0 = blockX0*blockWidth;
		int y0 = blockY0*blockHeight;

		int x1 = blockX0==stats.width-1 ? input.width : (blockX0+1)*blockWidth;
		int y1 = blockY0==stats.height-1 ? input.height: (blockY0+1)*blockHeight;

		// define the local 3x3 region in blocks, taking in account the image border
		int blockX1 = Math.min(stats.width-1,blockX0+1);
		int blockY1 = Math.min(stats.height-1,blockY0+1);

		blockX0 = Math.max(0,blockX0-1);
		blockY0 = Math.max(0,blockY0-1);

		// Average the mean across local blocks
		float mean = 0;

		for (int y = blockY0; y <= blockY1; y++) {
			for (int x = blockX0; x <= blockX1; x++) {
				mean += stats.unsafe_get(x,y);
			}
		}
		mean /= (blockY1-blockY0+1)*(blockX1-blockX0+1);


		// apply threshold
		for (int y = y0; y < y1; y++) {
			int indexInput = input.startIndex + y*input.stride + x0;
			int indexOutput = output.startIndex + y*output.stride + x0;
			for (int x = x0; x < x1; x++, indexOutput++, indexInput++ ) {
				if( down == input.data[indexInput] <= mean ) {
					output.data[indexOutput] = 1;
				} else {
					output.data[indexOutput] = 0;
				}
			}
		}
	}

	@Override
	protected void computeBlockStatistics(int x0, int y0, int width, int height, int indexStats, GrayF32 input) {

		float sum = 0;

		for (int y = 0; y < height; y++) {
			int indexInput = input.startIndex + (y0+y)*input.stride + x0;
			for (int x = 0; x < width; x++) {
				sum += input.data[indexInput++];
			}
		}
		sum = scale*sum/(width*height);

		stats.data[indexStats]   = sum;
	}
}
