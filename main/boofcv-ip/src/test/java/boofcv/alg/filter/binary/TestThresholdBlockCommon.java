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

import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestThresholdBlockCommon {
	@Test
	public void selectBlockSize() {

		int width = 30;

		ThresholdBlockCommon alg = new Dummy(width);

		alg.selectBlockSize(300,330,width);
		assertEquals(30,alg.blockWidth);
		assertEquals(30,alg.blockHeight);

		alg.selectBlockSize(329,301,width);
		assertEquals(32,alg.blockWidth);
		assertEquals(30,alg.blockHeight);

		alg.selectBlockSize(301,329,width);
		assertEquals(30,alg.blockWidth);
		assertEquals(32,alg.blockHeight);
	}

	private class Dummy extends ThresholdBlockCommon {

		public Dummy(int requestedBlockWidth) {
			super(ConfigLength.fixed(requestedBlockWidth),true,GrayU8.class);
		}

		@Override
		protected void computeBlockStatistics(int x0, int y0, int width, int height, int indexStats, ImageGray input) {

		}

		@Override
		protected void thresholdBlock(int blockX0, int blockY0, ImageGray input, GrayU8 output) {

		}
	}
}