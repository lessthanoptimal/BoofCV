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

package boofcv.alg.scene.bow;

import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestBowUtils extends BoofStandardJUnit {
	/**
	 * The filter accepts everything
	 */
	@Test void filterAndSortMatches_true() {
		var matches = new DogArray<>(BowMatch::new, BowMatch::reset);

		matches.resize(10);
		matches.forIdx(( idx, m ) -> m.identification = idx);
		matches.forIdx(( idx, m ) -> m.error = 10 - idx);

		// Limit is greater than the number of matches
		// All matches should be left, but the order changed
		BowUtils.filterAndSortMatches(matches, ( id ) -> true, 20);
		assertEquals(10, matches.size);
		matches.forIdx(( idx, m ) -> assertEquals(9 - idx, m.identification));

		// Limit is less than the number of matches
		matches.forIdx(( idx, m ) -> m.identification = idx);
		matches.forIdx(( idx, m ) -> m.error = 10 - idx);
		BowUtils.filterAndSortMatches(matches, ( id ) -> true, 4);
		assertEquals(4, matches.size);
		matches.forIdx(( idx, m ) -> assertEquals(9 - idx, m.identification));
	}

	/**
	 * The filter will reject even numbers
	 */
	@Test void filterAndSortMatches_Filtered() {
		var matches = new DogArray<>(BowMatch::new, BowMatch::reset);

		// Limit is greater than the number of matches, before filtering
		// The filter will remove all odd ID and return half
		matches.resize(50);
		matches.forIdx(( idx, m ) -> m.identification = idx);
		matches.forIdx(( idx, m ) -> m.error = 50 - idx);
		BowUtils.filterAndSortMatches(matches, ( id ) -> id%2==0, 100);
		assertEquals(25, matches.size);
		matches.forIdx(( idx, m ) -> assertEquals(48 - idx*2, m.identification));

		// Limit is greater than the number of matches, after filtering
		matches.resize(50);
		matches.forIdx(( idx, m ) -> m.identification = idx);
		matches.forIdx(( idx, m ) -> m.error = 50 - idx);
		BowUtils.filterAndSortMatches(matches, ( id ) -> id%2==0, 27);
		assertEquals(25, matches.size);
		matches.forIdx(( idx, m ) -> assertEquals(48 - idx*2, m.identification));

		// Limit is less than the number of matches, after filtering
		for (int limit = 5; limit < 20; limit++) {
			matches.resize(50);
			matches.forIdx(( idx, m ) -> m.identification = idx);
			matches.forIdx(( idx, m ) -> m.error = 50 - idx);
			BowUtils.filterAndSortMatches(matches, ( id ) -> id%2==0, limit);
			assertEquals(limit, matches.size);
			matches.forIdx(( idx, m ) -> assertEquals(48 - idx*2, m.identification));
		}
	}

	/**
	 * Null is passed in as the filter
	 */
	@Test void filterAndSortMatches_noFilter() {
		var matches = new DogArray<>(BowMatch::new, BowMatch::reset);

		matches.resize(10);
		matches.forIdx(( idx, m ) -> m.identification = idx);
		matches.forIdx(( idx, m ) -> m.error = 10 - idx);

		// Limit is greater than the number of matches
		// All matches should be left, but the order changed
		BowUtils.filterAndSortMatches(matches, null, 20);
		assertEquals(10, matches.size);
		matches.forIdx(( idx, m ) -> assertEquals(9 - idx, m.identification));

		// Limit is less than the number of matches
		matches.forIdx(( idx, m ) -> m.identification = idx);
		matches.forIdx(( idx, m ) -> m.error = 10 - idx);
		BowUtils.filterAndSortMatches(matches, null, 4);
		assertEquals(4, matches.size);
		matches.forIdx(( idx, m ) -> assertEquals(9 - idx, m.identification));
	}
}
