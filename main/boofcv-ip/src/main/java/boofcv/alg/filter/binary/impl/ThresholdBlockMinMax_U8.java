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

import boofcv.alg.filter.binary.ThresholdBlockMinMax;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.InterleavedU8;

/**
 * Implementation of {@link ThresholdBlockMinMax} for input images of type {@link GrayU8}
 *
 * @author Peter Abeles
 */
public class ThresholdBlockMinMax_U8
	extends ThresholdBlockMinMax<GrayU8,InterleavedU8>
{
	double scale;
	boolean down;

	public ThresholdBlockMinMax_U8(double minimumSpread, ConfigLength requestedBlockWidth, double scale , boolean down,
								   boolean thresholdFromLocalBlocks) {
		super(minimumSpread,requestedBlockWidth,thresholdFromLocalBlocks,GrayU8.class);
		stats = new InterleavedU8(1,1,2);
		this.scale = scale;
		this.down = down;
	}

	@Override
	protected void thresholdBlock(int blockX0 , int blockY0 , GrayU8 input, GrayU8 output ) {

		int x0 = blockX0*blockWidth;
		int y0 = blockY0*blockHeight;

		int x1 = blockX0== stats.width-1 ? input.width : (blockX0+1)*blockWidth;
		int y1 = blockY0== stats.height-1 ? input.height: (blockY0+1)*blockHeight;

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

		// find the min and max pixel values inside this block region
		int min = Integer.MAX_VALUE;
		int max = -Integer.MAX_VALUE;

		for (int y = blockY0; y <= blockY1; y++) {
			for (int x = blockX0; x <= blockX1; x++) {
				int localMin = stats.getBand(x,y,0);
				int localMax = stats.getBand(x,y,1);

				if( localMin < min )
					min = localMin;
				if( localMax > max )
					max = localMax;
			}
		}

		// apply threshold
		int textureThreshold = (int)this.minimumSpread;
		if( down ) {
			for (int y = y0; y < y1; y++) {
				int indexInput = input.startIndex + y * input.stride + x0;
				int indexOutput = output.startIndex + y * output.stride + x0;
				for (int x = x0; x < x1; x++, indexOutput++, indexInput++) {

					if (max - min <= textureThreshold) {
						output.data[indexOutput] = 1;
					} else {
						int average = (int)(scale * ((max+min)/2));
						output.data[indexOutput] = (input.data[indexInput]&0xFF) <= average ? (byte) 1 : 0;
					}
				}
			}
		} else {
			for (int y = y0; y < y1; y++) {
				int indexInput = input.startIndex + y * input.stride + x0;
				int indexOutput = output.startIndex + y * output.stride + x0;
				for (int x = x0; x < x1; x++, indexOutput++, indexInput++) {

					if (max - min <= textureThreshold) {
						output.data[indexOutput] = 1;
					} else {
						int average = (int)(scale * ((max+min)/2));
						output.data[indexOutput] = (input.data[indexInput]&0xFF) > average ? (byte) 1 : 0;
					}
				}
			}
		}
	}

	@Override
	protected void computeBlockStatistics(int x0 , int y0 , int width , int height , int indexMinMax , GrayU8 input) {

		int min,max;
		min = max = input.unsafe_get(x0,y0);

		for (int y = 0; y < height; y++) {
			int indexInput = input.startIndex + (y0+y)*input.stride + x0;
			for (int x = 0; x < width; x++) {
				int value = input.data[indexInput++] & 0xFF;
				if( value < min )
					min = value;
				else if( value > max )
					max = value;
			}
		}

		stats.data[indexMinMax]   = (byte)min;
		stats.data[indexMinMax+1] = (byte)max;
	}
}
