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

package boofcv.alg.misc;

import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestImageCoverage extends BoofStandardJUnit {
	@Test
	void reset() {
		var alg = new ImageCoverage();
		alg.configUniform.regionScaleFactor = 1.0; // make the math simple
		alg.fraction = 10; // see if this is reset
		alg.reset(200,100,200);

		assertEquals(0.0, alg.fraction, UtilEjml.TEST_F64);
		assertEquals(10, alg.targetCellPixels);
		assertEquals(20, alg.grid.rows);
		assertEquals(10, alg.grid.cols);
	}

	@Test
	void markPixel() {
		var alg = new ImageCoverage();
		alg.configUniform.regionScaleFactor = 1.0; // make the math simple
		alg.reset(200,100,200);
		// mark the same pixel multiple times
		alg.markPixel(0,2);
		alg.markPixel(3,5);
		// mark this pixel only once
		alg.markPixel(31,105);

		// see if they are covered
		assertTrue(alg.grid.get(0,0).covered);
		assertTrue(alg.grid.get(10,3).covered);

		// see that they are the only ones covered
		int totalTrue = 0;
		for (int i = 0; i < alg.grid.cells.size; i++) {
			if( alg.grid.cells.get(i).covered )
				totalTrue++;
		}
		assertEquals(2,totalTrue);
	}

	@Test
	void process() {
		var alg = new ImageCoverage();
		alg.configUniform.regionScaleFactor = 1.0; // make the math simple
		alg.reset(200,100,200);
		alg.markPixel(0,2);
		alg.markPixel(31,105);
		alg.process();
		assertEquals(2.0/200.0, alg.fraction);
	}
}
