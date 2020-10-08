/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.extract;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */
public class TestNonMaxBlockSearchRelaxed extends BoofStandardJUnit {

	@Test
	void checkMax() {
		GenericNonMaxBlockTests checks = new GenericNonMaxBlockTests(false,new NonMaxBlockSearchRelaxed.Max()){};
		checks.allStandard();
	}

	@Test
	void checkMin() {
		GenericNonMaxBlockTests checks = new GenericNonMaxBlockTests(false,new NonMaxBlockSearchRelaxed.Min()){};
		checks.allStandard();
	}

	@Test
	void checkMinMax() {
		GenericNonMaxBlockTests checks = new GenericNonMaxBlockTests(false,new NonMaxBlockSearchRelaxed.MinMax()){};
		checks.allStandard();
	}

	@Test
	void checkMax_MT() {
		GenericNonMaxBlockTests checks = new GenericNonMaxBlockTests(false,new NonMaxBlockSearchRelaxed.Max(),true){};
		// increase image size to stress threads more and make it more likely to be processed out of order
		checks.setImageShape(100,120);
		checks.allStandard();
	}

	@Test
	void checkMin_MT() {
		GenericNonMaxBlockTests checks = new GenericNonMaxBlockTests(false,new NonMaxBlockSearchRelaxed.Min(),true){};
		// increase image size to stress threads more and make it more likely to be processed out of order
		checks.setImageShape(100,120);
		checks.allStandard();
	}

	@Test
	void checkMinMax_MT() {
		GenericNonMaxBlockTests checks = new GenericNonMaxBlockTests(false,new NonMaxBlockSearchRelaxed.MinMax(),true){};
		// increase image size to stress threads more and make it more likely to be processed out of order
		checks.setImageShape(100,120);
		checks.allStandard();
	}
}
