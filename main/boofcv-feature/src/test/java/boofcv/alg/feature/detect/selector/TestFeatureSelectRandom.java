/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.selector;

import boofcv.struct.QueueCorner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestFeatureSelectRandom extends ChecksFeatureSelectLimit {

	@Override
	public FeatureSelectRandom createAlgorithm() {
		return new FeatureSelectRandom(0xFEED);
	}

	/**
	 * Makes sure the order changes between calls
	 */
	@Test
	void checkOrderChanges() {
		QueueCorner detected = createRandom(15);

		FeatureSelectRandom alg = createAlgorithm();
		QueueCorner foundA = new QueueCorner();
		alg.select(intensity,true,null,detected,10,foundA);
		QueueCorner foundB = new QueueCorner();
		alg.select(intensity,true,null,detected,10,foundB);

		assertEquals(10,foundA.size);
		assertEquals(10,foundB.size);
		boolean different = false;
		for (int i = 0; i < foundA.size; i++) {
			if( !foundA.get(i).equals(foundB.get(i))) {
				different = true;
				break;
			}
		}
		assertTrue(different);
	}
}