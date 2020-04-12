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
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * @author Peter Abeles
 */
class TestFeatureSelectFirst extends ChecksFeatureSelectLimit {
	@Override
	public FeatureSelectFirst createAlgorithm() {
		return new FeatureSelectFirst();
	}

	/**
	 * The order should not change
	 */
	@Test
	void checkOrder() {
		QueueCorner detected = createRandom(15);

		QueueCorner found = new QueueCorner();
		FeatureSelectFirst alg = createAlgorithm();
		alg.select(intensity,true,null,detected,10,found);

		assertEquals(10,found.size);
		for (int i = 0; i < found.size; i++) {
			assertNotSame(found.get(i), detected.get(i));
			assertEquals(found.get(i), detected.get(i));
		}
	}
}