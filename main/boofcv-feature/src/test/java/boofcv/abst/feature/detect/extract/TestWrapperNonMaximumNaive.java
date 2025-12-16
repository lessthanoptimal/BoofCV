/*
 * Copyright (c) 2025, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.extract;

import boofcv.alg.feature.detect.extract.NonMaxExtractorNaive;
import boofcv.struct.QueueCorner;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

public class TestWrapperNonMaximumNaive extends BoofStandardJUnit {

	@Test void checkStrict() {
		var checks = new GeneralNonMaxSuppressionChecks() {
			@Override public NonMaxSuppression<QueueCorner> createAlg() {
				return new WrapperNonMaximumNaive<>(new NonMaxExtractorNaive<QueueCorner>(true));
			}
		};
		checks.testAll();
	}

	@Test void checkRelaxed() {
		var checks = new GeneralNonMaxSuppressionChecks() {
			@Override public NonMaxSuppression<QueueCorner> createAlg() {
				return new WrapperNonMaximumNaive<>(new NonMaxExtractorNaive<QueueCorner>(false));
			}
		};
		checks.testAll();
	}
}
