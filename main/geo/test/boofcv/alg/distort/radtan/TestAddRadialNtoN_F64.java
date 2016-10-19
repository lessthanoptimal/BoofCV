/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort.radtan;

import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestAddRadialNtoN_F64 {
	/**
	 * Manually compute the distorted coordinate for a point and see if it matches
	 */
	@Test
	public void againstManual() {
		/**/double radial[]= new /**/double[]{0.01,-0.03};
		double t1 = 0.1, t2 = -0.05;

		Point2D_F64 orig = new Point2D_F64(0.1,-0.2);

		// manually compute the distortion
		double x = orig.x, y = orig.y;
		double r2 = x*x + y*y;
		double mag = (double)radial[0]*r2 + (double)radial[1]*r2*r2;

		double distX = orig.x*(1+mag) + 2*t1*x*y + t2*(r2 + 2*x*x);
		double distY = orig.y*(1+mag) + t1*(r2 + 2*y*y) + 2*t2*x*y;

		AddRadialNtoN_F64 alg = new AddRadialNtoN_F64().setDistortion(radial, t1, t2);

		Point2D_F64 found = new Point2D_F64();

		alg.compute(orig.x,orig.y,found);

		assertEquals(distX,found.x,1e-4);
		assertEquals(distY,found.y,1e-4);
	}
}
