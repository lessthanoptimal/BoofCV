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

package boofcv.alg.filter.binary;

import boofcv.BoofTesting;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

class TestThresholdBlock_MT extends BoofStandardJUnit {

	/**
	 * Compare single threaded vs multi threaded variant
	 */
	@Test
	void compare() {
		GrayF32 input = new GrayF32(100, 110);
		GrayU8 expected = new GrayU8(input.width, input.height);
		GrayU8 found = new GrayU8(input.width, input.height);

		ImageMiscOps.fillUniform(input, rand, -10, 10);
		ImageMiscOps.fillUniform(expected, rand, 0, 2);
		ImageMiscOps.fillUniform(found, rand, 0, 2);

		ThresholdBlock<GrayF32, GrayF32> algS = new ThresholdBlock(new Dummy(), ConfigLength.fixed(5), true, GrayF32.class);
		ThresholdBlock<GrayF32, GrayF32> algP = new ThresholdBlock_MT(new Dummy(), ConfigLength.fixed(5), true, GrayF32.class);

		algS.process(input, expected);
		algP.process(input, found);

		BoofTesting.assertEquals(expected, found, 0);
	}

	private class Dummy implements ThresholdBlock.BlockProcessor<GrayF32, GrayF32> {

		// make each of these produce
		int blockWidth, blockHeight;

		@Override
		public GrayF32 createStats() {
			return new GrayF32(1, 1);
		}

		@Override
		public void init( int blockWidth, int blockHeight, boolean thresholdFromLocalBlocks ) {
			this.blockWidth = blockWidth;
			this.blockHeight = blockHeight;
		}

		@Override
		public void computeBlockStatistics( int x0, int y0, int width, int height, int indexStats, GrayF32 input, GrayF32 stats ) {
			int x1 = Math.min(x0 + blockWidth, width);
			int y1 = Math.min(y0 + blockHeight, height);

			float total = 0;
			for (int y = y0; y < y1; y++) {
				for (int x = x0; x < x1; x++) {
					total += input.unsafe_get(x, y);
				}
			}
			total = total/((x1 - x0)*(y1 - y0));
			stats.data[indexStats] = total;
		}

		@Override
		public void thresholdBlock( int blockX0, int blockY0, GrayF32 input, GrayF32 stats, GrayU8 output ) {
			float thresh = stats.get(blockX0, blockY0);

			int x0 = blockX0*blockWidth;
			int y0 = blockY0*blockWidth;
			int x1 = Math.min(x0 + blockWidth, input.width);
			int y1 = Math.min(y0 + blockHeight, input.height);

			for (int y = y0; y < y1; y++) {
				for (int x = x0; x < x1; x++) {
					output.set(x, y, input.get(x, y) < thresh ? 0 : 1);
				}
			}
		}

		@Override
		public ThresholdBlock.BlockProcessor<GrayF32, GrayF32> copy() {
			return new Dummy();
		}
	}
}

