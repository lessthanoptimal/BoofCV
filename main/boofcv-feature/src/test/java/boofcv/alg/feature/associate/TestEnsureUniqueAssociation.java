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

package boofcv.alg.feature.associate;

import boofcv.struct.feature.AssociatedIndex;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestEnsureUniqueAssociation extends BoofStandardJUnit {
	@Test void basicTest() {
		DogArray<AssociatedIndex> matches = new DogArray<>(10, AssociatedIndex::new);

		matches.grow().setTo(0, 1, 10);
		matches.grow().setTo(1, 0, 20);
		matches.grow().setTo(2, 2, 30);
		// add a duplicate dst
		matches.grow().setTo(3, 1, 5);

		EnsureUniqueAssociation alg = new EnsureUniqueAssociation();

		alg.process(matches, 3);

		FastAccess<AssociatedIndex> found = alg.getMatches();

		assertEquals(3, found.size);
		// the other shouldn't matter but it is easier to test this way
		assertEquals(0, found.get(0).dst);
		assertEquals(1, found.get(0).src);
		assertEquals(1, found.get(1).dst);
		assertEquals(3, found.get(1).src);
		assertEquals(2, found.get(2).dst);
		assertEquals(2, found.get(2).src);
	}
}
