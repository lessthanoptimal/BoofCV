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

package boofcv.misc;

import georegression.misc.GrlConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestMovingAverage {

	/**
	 * Basic check that makes sure the rate of decay changes the behavior in the expected way
	 */
	@Test
	public void basic() {
		MovingAverage a = new MovingAverage(0.95);
		MovingAverage b = new MovingAverage(0.8);

		assertEquals(2.0,a.update(2), GrlConstants.TEST_F64);
		assertEquals(2.0,b.update(2), GrlConstants.TEST_F64);

		a.update(1.5);
		b.update(1.5);

		assertTrue( a.average > b.average );
		assertTrue( a.average > 1.5 && a.average < 2 );
		assertTrue( b.average > 1.5 && b.average < 2 );
	}
}