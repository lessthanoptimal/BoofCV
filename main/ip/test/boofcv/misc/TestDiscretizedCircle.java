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

package boofcv.misc;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDiscretizedCircle {
	/**
	 * Make sure the circles have the expected properties.
	 * <p/>
	 * Right now it just checks the lengths.  I can do a better job..
	 */
	@Test
	public void testCircles() {

		int pts[];

		pts = DiscretizedCircle.imageOffsets(1, 100);
		assertEquals(4, pts.length);
		pts = DiscretizedCircle.imageOffsets(2, 100);
		assertEquals(12, pts.length);
		pts = DiscretizedCircle.imageOffsets(3, 100);
		assertEquals(16, pts.length);
	}
}
