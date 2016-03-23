/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.binary.ThresholdSquareBlockMinMax;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.InterleavedF32;

/**
 *  * Implementation of {@link ThresholdSquareBlockMinMax} for input images of type {@link GrayF32}
 *
 * @author Peter Abeles
 */
public class ThresholdSquareBlockMinMax_F32
	extends ThresholdSquareBlockMinMax<GrayF32,InterleavedF32>
{
	float scale;
	boolean down;

	public ThresholdSquareBlockMinMax_F32(float minimumSpread, int requestedBlockWidth, float scale , boolean down ) {
		super(minimumSpread,requestedBlockWidth);
		minmax = new InterleavedF32(1,1,2);
		this.scale = scale;
		this.down = down;
	}

	@Override
	protected void thresholdBlock(int blockX0 , int blockY0 , GrayF32 input, GrayU8 output ) {

		int x0 = blockX0*blockWidth;
		int y0 = blockY0*blockHeight;

		int x1 = blockX0==minmax.width-1 ? input.width : (blockX0+1)*blockWidth;
		int y1 = blockY0==minmax.height-1 ? input.height: (blockY0+1)*blockHeight;

		// define the local 3x3 region in blocks, taking in account the image border
		int blockX1 = Math.min(minmax.width-1,blockX0+1);
		int blockY1 = Math.min(minmax.height-1,blockY0+1);

		blockX0 = Math.max(0,blockX0-1);
		blockY0 = Math.max(0,blockY0-1);

		// find the min and max pixel values inside this block region
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;

		for (int y = blockY0; y <= blockY1; y++) {
			for (int x = blockX0; x <= blockX1; x++) {
				float localMin = minmax.getBand(x,y,0);
				float localMax = minmax.getBand(x,y,1);

				if( localMin < min )
					min = localMin;
				if( localMax > max )
					max = localMax;
			}
		}

		// apply threshold
		float textureThreshold = (float)this.minimumSpread;
		for (int y = y0; y < y1; y++) {
			int indexInput = input.startIndex + y*input.stride + x0;
			int indexOutput = output.startIndex + y*output.stride + x0;
			for (int x = x0; x < x1; x++, indexOutput++, indexInput++ ) {

				if( max-min <= textureThreshold ) {
					output.data[indexOutput] = 1;
				} else {
					float average = scale*(max+min)/2.0f;
					if( down == input.data[indexInput] <= average ) {
						output.data[indexOutput] = 1;
					} else {
						output.data[indexOutput] = 0;
					}
				}
			}
		}
	}

	@Override
	protected void computeMinMaxBlock(int x0 , int y0 , int width , int height , int indexMinMax , GrayF32 input) {

		float min,max;
		min = max = input.unsafe_get(x0,y0);

		for (int y = 0; y < height; y++) {
			int indexInput = input.startIndex + (y0+y)*input.stride + x0;
			for (int x = 0; x < width; x++) {
				float value = input.data[indexInput++];
				if( value < min )
					min = value;
				else if( value > max )
					max = value;
			}
		}

		minmax.data[indexMinMax]   = min;
		minmax.data[indexMinMax+1] = max;
	}
}
