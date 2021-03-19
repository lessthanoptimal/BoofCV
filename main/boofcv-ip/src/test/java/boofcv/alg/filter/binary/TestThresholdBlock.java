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

import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestThresholdBlock extends BoofStandardJUnit {
	@Test void selectBlockSize() {
		int width = 30;

		ThresholdBlock alg = new ThresholdBlock(new Dummy(),
				ConfigLength.fixed(width), true, GrayU8.class);

		alg.selectBlockSize(300, 330, width);
		assertEquals(30, alg.blockWidth);
		assertEquals(30, alg.blockHeight);

		alg.selectBlockSize(329, 301, width);
		assertEquals(32, alg.blockWidth);
		assertEquals(30, alg.blockHeight);

		alg.selectBlockSize(301, 329, width);
		assertEquals(30, alg.blockWidth);
		assertEquals(32, alg.blockHeight);
	}

	// @formatter:off
	private class Dummy implements ThresholdBlock.BlockProcessor {
		@Override public ImageBase createStats() {return new GrayU8(1, 1);}
		@Override public void init( int blockWidth, int blockHeight, boolean thresholdFromLocalBlocks ) {}
		@Override public void computeBlockStatistics( int x0, int y0, int width, int height,
													  int indexStats, ImageGray input, ImageBase stats ) {}
		@Override public void thresholdBlock( int blockX0, int blockY0,
											  ImageGray input, ImageBase stats, GrayU8 output ) {}
		@Override public ThresholdBlock.BlockProcessor copy() {return null;}
	}
	// @formatter:on
}
