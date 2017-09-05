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

package boofcv.struct;

import org.ejml.UtilEjml;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestConfigMinimumSize {

	@Test
	public void byPixels() {
		ConfigMinimumSize c = ConfigMinimumSize.byPixels(3);

		assertEquals(3,c.pixels);
		assertEquals(-1,c.fraction, UtilEjml.TEST_F64);
	}

	@Test
	public void bySize() {
		ConfigMinimumSize c = ConfigMinimumSize.bySize(0.01);

		assertEquals(-1,c.pixels);
		assertEquals(0.01,c.fraction, UtilEjml.TEST_F64);
	}

	@Test
	public void compute() {
		ConfigMinimumSize c = ConfigMinimumSize.byPixels(3);

		assertEquals(3,c.compute(200,230), UtilEjml.TEST_F64);

		c = ConfigMinimumSize.bySize(0.1);

		assertEquals(20,c.compute(200,230), UtilEjml.TEST_F64);

		c = new ConfigMinimumSize(25,0.1);

		assertEquals(20,c.compute(200,230), UtilEjml.TEST_F64);

		c = new ConfigMinimumSize(15,0.1);

		assertEquals(15,c.compute(200,230), UtilEjml.TEST_F64);
	}

	@Test
	public void copy() {
		ConfigMinimumSize found = new ConfigMinimumSize(2,0.1).copy();

		assertEquals(2,found.pixels);
		assertEquals(0.1,found.fraction,UtilEjml.TEST_F64);
	}
}
