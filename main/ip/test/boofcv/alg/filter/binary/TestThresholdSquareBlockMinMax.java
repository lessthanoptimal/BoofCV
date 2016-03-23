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

package boofcv.alg.filter.binary;

import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestThresholdSquareBlockMinMax {
	@Test
	public void selectBlockSize() {

		ThresholdSquareBlockMinMax alg = new Dummy(0,30);

		alg.selectBlockSize(300,330);
		assertEquals(30,alg.blockWidth);
		assertEquals(30,alg.blockHeight);

		alg.selectBlockSize(329,301);
		assertEquals(32,alg.blockWidth);
		assertEquals(30,alg.blockHeight);

		alg.selectBlockSize(301,329);
		assertEquals(30,alg.blockWidth);
		assertEquals(32,alg.blockHeight);
	}

	private class Dummy extends ThresholdSquareBlockMinMax {

		public Dummy(double textureThreshold, int requestedBlockWidth) {
			super(textureThreshold, requestedBlockWidth);
		}

		@Override
		protected void thresholdBlock(int blockX0, int blockY0, ImageGray input, GrayU8 output) {

		}

		@Override
		protected void computeMinMaxBlock(int x0, int y0, int width, int height, int indexMinMax, ImageGray input) {

		}
	}
}