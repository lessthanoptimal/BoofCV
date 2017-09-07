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

package boofcv.alg.filter.binary;

import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.InterleavedS32;

import java.util.Arrays;

/**
 * Block Otsu threshold implementation based on {@link ThresholdBlockCommon}. Computes a histogram in non-overlapping
 * square regions. Then thresholds a single region by combining histograms from its neighbors.
 *
 * @see GThresholdImageOps#computeOtsu(ImageGray, int, int)
 *
 * @author Peter Abeles
 */
public class ThresholdBlockOtsu extends ThresholdBlockCommon<GrayU8,InterleavedS32> {

	int histogram[] = new int[256];
	boolean down;
	double scale;
	/**
	 * Configures the detector
	 *
	 * @param requestedBlockWidth About how wide and tall you wish a block to be in pixels.
	 */
	public ThresholdBlockOtsu(int requestedBlockWidth, double scale, boolean down ) {
		super(requestedBlockWidth);
		this.down = down;
		this.scale = scale;
		stats = new InterleavedS32(1,1,256);
	}

	@Override
	protected void computeStatistics(GrayU8 input, int innerWidth, int innerHeight) {
		Arrays.fill(stats.data,0,stats.width*stats.height*256,0);
		super.computeStatistics(input, innerWidth, innerHeight);
	}

	@Override
	protected void computeBlockStatistics(int x0, int y0, int width, int height, int indexStats, GrayU8 input) {

		for (int y = 0; y < height; y++) {
			int indexInput = input.startIndex + (y0+y)*input.stride + x0;
			for (int x = 0; x < width; x++) {
				stats.data[indexStats+(input.data[indexInput++] & 0xFF)]++;
			}
		}
	}

	@Override
	protected void thresholdBlock(int blockX0, int blockY0, GrayU8 input, GrayU8 output) {

		int x0 = blockX0*blockWidth;
		int y0 = blockY0*blockHeight;

		int x1 = blockX0== stats.width-1 ? input.width : (blockX0+1)*blockWidth;
		int y1 = blockY0== stats.height-1 ? input.height: (blockY0+1)*blockHeight;

		// define the local 3x3 region in blocks, taking in account the image border
		int blockX1 = Math.min(stats.width-1,blockX0+1);
		int blockY1 = Math.min(stats.height-1,blockY0+1);

		blockX0 = Math.max(0,blockX0-1);
		blockY0 = Math.max(0,blockY0-1);


		// sum up histogram in local region
		Arrays.fill(histogram,0,256,0);

		for (int y = blockY0; y <= blockY1; y++) {
			for (int x = blockX0; x <= blockX1; x++) {
				int indexStats = stats.getIndex(x,y,0);
				for (int i = 0; i < 256; i++) {
					histogram[i] += stats.data[indexStats+i];
				}
			}
		}

		// this can vary across the image at the borders
		int total = 0;
		for (int i = 0; i < 256; i++) {
			total += histogram[i];
		}

		// compute threshold
		int threshold = GThresholdImageOps.computeOtsu(histogram,256,total);

		threshold = (int)(threshold*scale+0.5);


		for (int y = y0; y < y1; y++) {
			int indexInput = input.startIndex + y*input.stride + x0;
			int indexOutput = output.startIndex + y*output.stride + x0;
			int end = indexOutput + (x1-x0);
			for (; indexOutput < end; indexOutput++, indexInput++ ) {
				output.data[indexOutput] = down == (input.data[indexInput]&0xFF) <= threshold ?
						(byte)1 : 0;
			}
		}
	}
}
