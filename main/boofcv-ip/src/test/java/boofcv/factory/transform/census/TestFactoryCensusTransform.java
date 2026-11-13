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

package boofcv.factory.transform.census;

import boofcv.abst.transform.census.FilterCensusTransform;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.*;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"rawtypes", "unchecked"})
class TestFactoryCensusTransform extends BoofStandardJUnit {
	/// Each variant samples one point per bit, so on a random image every bit of the descriptor is used and no more
	@Test void variant_descriptorBits() {
		var input = new GrayU8(60, 50);
		ImageMiscOps.fillUniform(input, rand, 0, 256);

		for (CensusVariants variant : CensusVariants.values()) {
			FilterCensusTransform alg = FactoryCensusTransform.variant(variant, BorderType.REFLECT, GrayU8.class);
			ImageGray output = (ImageGray)alg.getOutputType().createImage(1, 1);
			alg.process(input, output);

			long union = 0;
			for (int y = 0; y < output.height; y++) {
				for (int x = 0; x < output.width; x++) {
					union |= descriptor(output, x, y);
				}
			}
			assertEquals(variant.getBits(), Long.bitCount(union), variant.name());
		}
	}

	private static long descriptor( ImageGray image, int x, int y ) {
		if (image instanceof GrayU8 gray)
			return gray.get(x, y);
		if (image instanceof GrayS32 gray)
			return gray.get(x, y) & 0xFFFFFFFFL;
		if (image instanceof GrayS64 gray)
			return gray.get(x, y);
		throw new IllegalArgumentException("Unexpected census image " + image.getClass().getSimpleName());
	}
}
