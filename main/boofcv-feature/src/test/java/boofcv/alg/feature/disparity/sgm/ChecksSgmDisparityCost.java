/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.sgm;

import boofcv.struct.image.GrayU8;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Common unit tests for implementations of {@link SgmDisparityCost}
 *
 * @author Peter Abeles
 */
abstract class ChecksSgmDisparityCost {

	// In some tests pixels are assigned two values and a check is done to see if the disparity is at the right
	// location or not
	int VALUE_LOW = 10;
	int VALUE_HIGH = 200;

	// TODO ensure cost does not exceed max

	abstract SgmDisparityCost<GrayU8> createAlg();

	/**
	 * Sanity checks the ordering of values in the cost image
	 */
	@Test
	void costOrder() {
		fail("Implement");
	}

	/**
	 * Does it reshape the output?
	 */
	@Test
	void reshape() {
		fail("Implement");
	}

	/**
	 * Is the min disparity parameter obeyed?
	 */
	@Test
	void minDisparity() {
		fail("Implement");
	}

	/**
	 * Is the min disparity parameter obeyed?
	 */
	@Test
	void maxDisparity() {
		fail("Implement");
	}
}