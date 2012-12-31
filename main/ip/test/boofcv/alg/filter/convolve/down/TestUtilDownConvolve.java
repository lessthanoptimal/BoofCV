/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.convolve.down;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestUtilDownConvolve {

	@Test
	public void computeMaxSide() {
		assertEquals(8,UtilDownConvolve.computeMaxSide(10,1,1));
		assertEquals(7,UtilDownConvolve.computeMaxSide(10,1,2));
		assertEquals(8,UtilDownConvolve.computeMaxSide(10,2,1));
		assertEquals(6,UtilDownConvolve.computeMaxSide(10,2,2));
		assertEquals(6,UtilDownConvolve.computeMaxSide(10,2,3));
		assertEquals(4,UtilDownConvolve.computeMaxSide(10,2,4));
		assertEquals(6,UtilDownConvolve.computeMaxSide(10,3,1));
		assertEquals(6,UtilDownConvolve.computeMaxSide(10,3,2));
		assertEquals(6,UtilDownConvolve.computeMaxSide(10,3,3));
		assertEquals(3,UtilDownConvolve.computeMaxSide(10,3,4));
		assertEquals(4,UtilDownConvolve.computeMaxSide(11,4,2));
	}

	@Test
	public void computeOffset() {
		assertEquals(1,UtilDownConvolve.computeOffset(1,1));
		assertEquals(2,UtilDownConvolve.computeOffset(1,2));
		assertEquals(3,UtilDownConvolve.computeOffset(1,3));
		assertEquals(2,UtilDownConvolve.computeOffset(2,1));
		assertEquals(2,UtilDownConvolve.computeOffset(2,2));
		assertEquals(4,UtilDownConvolve.computeOffset(2,3));
	}
}
