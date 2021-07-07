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

package boofcv.abst.feature.detect.extract;

import boofcv.alg.feature.detect.extract.NonMaxBlock;
import boofcv.alg.feature.detect.extract.NonMaxBlockSearchRelaxed;
import boofcv.alg.feature.detect.extract.NonMaxBlockSearchStrict;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */
public class TestWrapperNonMaximumBlock extends BoofStandardJUnit {

	@Test void checkStrict_Max() {
		GeneralNonMaxSuppressionChecks checks = new GeneralNonMaxSuppressionChecks() {

			@Override
			public NonMaxSuppression createAlg() {
				NonMaxBlock alg = new NonMaxBlock(new NonMaxBlockSearchStrict.Max());
				return new WrapperNonMaximumBlock(alg);
			}
		};
		checks.testAll();
	}

	@Test void checkStrict_Min() {
		GeneralNonMaxSuppressionChecks checks = new GeneralNonMaxSuppressionChecks() {

			@Override
			public NonMaxSuppression createAlg() {
				NonMaxBlock alg = new NonMaxBlock(new NonMaxBlockSearchStrict.Min());
				return new WrapperNonMaximumBlock(alg);
			}
		};
		checks.testAll();
	}

	@Test void checkStrict_MinMax() {
		GeneralNonMaxSuppressionChecks checks = new GeneralNonMaxSuppressionChecks() {

			@Override
			public NonMaxSuppression createAlg() {
				NonMaxBlock alg = new NonMaxBlock(new NonMaxBlockSearchStrict.MinMax());
				return new WrapperNonMaximumBlock(alg);
			}
		};
		checks.testAll();
	}

	@Test void checkRelaxed_Max() {
		GeneralNonMaxSuppressionChecks checks = new GeneralNonMaxSuppressionChecks() {

			@Override
			public NonMaxSuppression createAlg() {
				NonMaxBlock alg = new NonMaxBlock(new NonMaxBlockSearchRelaxed.Max());
				return new WrapperNonMaximumBlock(alg);
			}
		};
		checks.testAll();
	}

	@Test void checkRelaxed_Min() {
		GeneralNonMaxSuppressionChecks checks = new GeneralNonMaxSuppressionChecks() {

			@Override
			public NonMaxSuppression createAlg() {
				NonMaxBlock alg = new NonMaxBlock(new NonMaxBlockSearchRelaxed.Min());
				return new WrapperNonMaximumBlock(alg);
			}
		};
		checks.testAll();
	}

	@Test void checkRelaxed_MinMax() {
		GeneralNonMaxSuppressionChecks checks = new GeneralNonMaxSuppressionChecks() {

			@Override
			public NonMaxSuppression createAlg() {
				NonMaxBlock alg = new NonMaxBlock(new NonMaxBlockSearchRelaxed.MinMax());
				return new WrapperNonMaximumBlock(alg);
			}
		};
		checks.testAll();
	}
}
