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

package boofcv.alg.disparity.block.score;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestDisparitySparseRectifiedScoreBM_F32 extends BoofStandardJUnit {
	@Test
	void array() {
		var alg = new DisparitySparseRectifiedScoreBM_F32(2, 3) {
			@Override protected void scoreDisparity( int disparityRange, boolean leftToRight ) {}
		};
		alg.setSampleRegion(1, 2);
		alg.configure(2, 10);

		// this is the only thing this class does really. The array needs to be able to store the entire range
		assertEquals(10, alg.scoreLtoR.length);
	}
}