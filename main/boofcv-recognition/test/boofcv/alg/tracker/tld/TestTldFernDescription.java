/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.tld;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTldFernDescription {

	@Test
	public void sanityTest() {
		TldFernDescription fern = new TldFernDescription(new Random(24),8);

		assertEquals(fern.pairs.length,8);

		int numNotZeroA = 0;
		for( int i = 0; i < fern.pairs.length; i++ ) {
			if( fern.pairs[i].a.x  != 0 )
				numNotZeroA++;

			assertTrue(fern.pairs[i].a.x >= -0.5);
			assertTrue(fern.pairs[i].a.x < 0.5);
			assertTrue(fern.pairs[i].b.x >= -0.5);
			assertTrue(fern.pairs[i].b.x < 0.5);
		}

		assertTrue(numNotZeroA > 0);
	}

}
