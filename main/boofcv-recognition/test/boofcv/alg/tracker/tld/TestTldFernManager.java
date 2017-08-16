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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTldFernManager {

	@Test
	public void constructor() {
		TldFernManager alg = new TldFernManager(10);
		assertEquals(1024,alg.table.length);
	}

	@Test
	public void lookupFern() {
		TldFernManager alg = new TldFernManager(10);

		TldFernFeature a = alg.lookupFern(345);
		assertTrue(a != null);

		assertTrue(a == alg.lookupFern(345));

		assertEquals(345,a.value);
	}

	@Test
	public void lookupPosterior() {
		TldFernManager alg = new TldFernManager(10);

		assertEquals(0,alg.lookupPosterior(234),1e-8);

		TldFernFeature a = alg.lookupFern(234);
		a.numN = 100;
		a.numP = 234;
		a.incrementP();

		double expected = a.getPosterior();
		assertEquals(expected,alg.lookupPosterior(234),1e-8);
	}

	@Test
	public void reset() {
		TldFernManager alg = new TldFernManager(10);

		alg.table[10] = new TldFernFeature();
		alg.table[800] = new TldFernFeature();

		alg.reset();

		for( int i = 0; i < alg.table.length; i++ ) {
			assertTrue(alg.table[i] == null);
		}

		assertEquals(2,alg.unusedFern.size());

	}
	@Test
	public void createFern() {
		TldFernManager alg = new TldFernManager(3);

		assertTrue(alg.createFern() != null);
		alg.unusedFern.push(new TldFernFeature());

		assertTrue(alg.createFern() != null);
		assertEquals(0,alg.unusedFern.size());
	}

}
