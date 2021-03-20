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
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

import static boofcv.alg.filter.binary.ThresholdNiblackFamily.Variant.*;

class TestThresholdNiblackFamily_MT extends BoofStandardJUnit {
	@Nested class Niblack extends GenericInputToBinaryCompare<GrayF32>{
		Niblack() {
			var target = new ThresholdNiblackFamily_MT(ConfigLength.fixed(12), 0.5f, true, NIBLACK);
			var reference = new ThresholdNiblackFamily(ConfigLength.fixed(12), 0.5f, true, NIBLACK);

			initialize(target, reference);
		}
	}

	@Nested class Sauvola extends GenericInputToBinaryCompare<GrayF32>{
		Sauvola() {
			var target = new ThresholdNiblackFamily_MT(ConfigLength.fixed(12), 0.5f, true, SAUVOLA);
			var reference = new ThresholdNiblackFamily(ConfigLength.fixed(12), 0.5f, true, SAUVOLA);

			initialize(target, reference);
		}
	}

	@Nested class Wolf extends GenericInputToBinaryCompare<GrayF32>{
		Wolf() {
			var target = new ThresholdNiblackFamily_MT(ConfigLength.fixed(12), 0.5f, true, WOLF_JOLION);
			var reference = new ThresholdNiblackFamily(ConfigLength.fixed(12), 0.5f, true, WOLF_JOLION);

			initialize(target, reference);
		}
	}
}

