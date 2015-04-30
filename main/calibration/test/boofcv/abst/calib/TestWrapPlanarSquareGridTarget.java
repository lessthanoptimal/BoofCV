/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.calib;

import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestWrapPlanarSquareGridTarget {
	@Test
	public void stuff() {
		fail("Implement");
	}

	@Test
	public void createLayout() {
		List<Point2D_F64> l = WrapPlanarSquareGridTarget.createLayout(3,5,0.1,0.2);

		assertEquals(4*6,l.size());

		double w = l.get(1).x - l.get(0).x;
		double h = l.get(0).y - l.get(4).y ;

		assertEquals(0.1,w,1e-8);
		assertEquals(0.1,h,1e-8);

		double s = l.get(2).x - l.get(1).x;

		assertEquals(0.2, s, 1e-8);
	}
}