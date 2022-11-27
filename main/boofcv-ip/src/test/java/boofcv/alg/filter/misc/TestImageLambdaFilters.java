/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.misc;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

class TestImageLambdaFilters extends BoofStandardJUnit {
	/**
	 * Makes sure it passes in only pixels that are inside the image
	 */
	@Test void filterRectCenterInner() {
		// Image large enough that there is an inner region

		// image that is too narrow along x-axis

		// image that is too narrow along y-axis

		fail("Implement");
	}

	@Test void filterRectCenterEdge() {

		// Image large enough that there is an inner region

		// image that is too narrow along x-axis

		// image that is too narrow along y-axis

		// image that's (0,0) pixels

		fail("Implement");
	}
}