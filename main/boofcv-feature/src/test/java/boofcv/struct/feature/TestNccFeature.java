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

package boofcv.struct.feature;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestNccFeature extends BoofStandardJUnit {

	@Test void setTo() {
		NccFeature a = new NccFeature(4);
		a.mean = 0.5;
		a.sigma = 1.5;

		for( int i = 0; i < 4; i++ )
			a.data[i] = i+0.1;

		NccFeature b = new NccFeature(4);

		b.setTo(a);

		checkIdentical(a, b);
	}

	@Test void copy() {
		NccFeature a = new NccFeature(4);
		a.mean = 0.5;
		a.sigma = 1.5;

		for( int i = 0; i < 4; i++ )
			a.data[i] = i+0.1;

		NccFeature b = a.copy();

		checkIdentical(a, b);
	}

	private void checkIdentical(NccFeature a, NccFeature b) {
		assertEquals(a.mean,b.mean,1e-8);
		assertEquals(a.sigma,b.sigma,1e-8);

		for( int i = 0; i < 4; i++ )
			assertTrue(a.data[i] == b.data[i]);
	}
}
