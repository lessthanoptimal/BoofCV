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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestFindUnassociated extends BoofStandardJUnit {
	@Test void checkSource() {
		var associations = new DogArray<>(AssociatedIndex::new);

		associations.grow().setTo(0, 1);
		associations.grow().setTo(3, 2);
		associations.grow().setTo(2, 10);
		associations.grow().setTo(6, 1);

		var alg = new FindUnassociated();

		alg.checkSource(associations, 7);

		assertEquals(3, alg.unassociatedSrc.size);
		assertTrue(alg.unassociatedSrc.contains(1));
		assertTrue(alg.unassociatedSrc.contains(4));
		assertTrue(alg.unassociatedSrc.contains(5));
	}

	@Test void checkDestination() {
		var associations = new DogArray<>(AssociatedIndex::new);

		associations.grow().setTo(1, 0);
		associations.grow().setTo(2, 3);
		associations.grow().setTo(10, 2);
		associations.grow().setTo(1, 6);

		var alg = new FindUnassociated();

		alg.checkDestination(associations, 7);

		assertEquals(3, alg.unassociatedDst.size);
		assertTrue(alg.unassociatedDst.contains(1));
		assertTrue(alg.unassociatedDst.contains(4));
		assertTrue(alg.unassociatedDst.contains(5));
	}
}
