/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.GrayU8;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestThresholdBlockMinMax_U8 extends GenericThresholdBlockMinMaxChecks<GrayU8> {

	public TestThresholdBlockMinMax_U8() {
		super(GrayU8.class);
	}

	@Override
	public ThresholdBlockMinMax<GrayU8, ?> createProcessor(
			double textureThreshold, int requestedBlockWidth, double scale, boolean down ) {
		return new ThresholdBlockMinMax_U8(textureThreshold, (float)scale, down);
	}

	@Test void computeTextureThreshold() {
		assertEquals(-1, threshold(-0.1));
		assertEquals(-1, threshold(-1e20));
		assertEquals(0, threshold(0));
		assertEquals(0, threshold(Math.nextDown(0.5)));
		assertEquals(2, threshold(2.4999999999));
		assertEquals(3, threshold(2.5));
		assertEquals(3, threshold(2.9999999999));
		assertEquals(Integer.MAX_VALUE, threshold(1e20));
	}

	private static int threshold( double minimumSpread ) {
		return new ThresholdBlockMinMax_U8(minimumSpread, 1.0, true).computeTextureThreshold();
	}
}
