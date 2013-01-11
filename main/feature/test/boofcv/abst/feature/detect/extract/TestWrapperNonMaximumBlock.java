/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.detect.extract.NonMaxBlockRelaxed;
import boofcv.alg.feature.detect.extract.NonMaxBlockStrict;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestWrapperNonMaximumBlock {

	@Test
	public void checkStrict_Max() {
		GeneralNonMaxSuppressionChecks checks = new GeneralNonMaxSuppressionChecks() {

			@Override
			public NonMaxSuppression createAlg() {
				return new WrapperNonMaximumBlock(new NonMaxBlockStrict.Max());
			}
		};
		checks.testAll();
	}

	@Test
	public void checkStrict_Min() {
		GeneralNonMaxSuppressionChecks checks = new GeneralNonMaxSuppressionChecks() {

			@Override
			public NonMaxSuppression createAlg() {
				return new WrapperNonMaximumBlock(new NonMaxBlockStrict.Min());
			}
		};
		checks.testAll();
	}

	@Test
	public void checkStrict_MinMax() {
		GeneralNonMaxSuppressionChecks checks = new GeneralNonMaxSuppressionChecks() {

			@Override
			public NonMaxSuppression createAlg() {
				return new WrapperNonMaximumBlock(new NonMaxBlockStrict.MinMax());
			}
		};
		checks.testAll();
	}

	@Test
	public void checkRelaxed_Max() {
		GeneralNonMaxSuppressionChecks checks = new GeneralNonMaxSuppressionChecks() {

			@Override
			public NonMaxSuppression createAlg() {
				return new WrapperNonMaximumBlock(new NonMaxBlockRelaxed.Max());
			}
		};
		checks.testAll();
	}

	@Test
	public void checkRelaxed_Min() {
		GeneralNonMaxSuppressionChecks checks = new GeneralNonMaxSuppressionChecks() {

			@Override
			public NonMaxSuppression createAlg() {
				return new WrapperNonMaximumBlock(new NonMaxBlockRelaxed.Min());
			}
		};
		checks.testAll();
	}

	@Test
	public void checkRelaxed_MinMax() {
		GeneralNonMaxSuppressionChecks checks = new GeneralNonMaxSuppressionChecks() {

			@Override
			public NonMaxSuppression createAlg() {
				return new WrapperNonMaximumBlock(new NonMaxBlockRelaxed.MinMax());
			}
		};
		checks.testAll();
	}
}
