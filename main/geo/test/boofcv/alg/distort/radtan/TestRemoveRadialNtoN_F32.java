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

import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestRemoveRadialNtoN_F32 {

	@Test
	public void checkManual() {
		checkManual(0, 0);
		checkManual(0.1f, -0.05f);
	}

	public void checkManual(float t1, float t2) {

		/**/double radial[]= new /**/double[]{0.12f,-0.13f};

		// undisorted normalized image coordinate
		Point2D_F32 undistorted = new Point2D_F32(0.1f,-0.2f);

		// manually compute the distortion
		float x = undistorted.x, y = undistorted.y;
		float r2 = x*x + y*y;
		float mag = (float)radial[0]*r2 + (float)radial[1]*r2*r2;

		// distorted normalized image coordinate
		float distX = undistorted.x*(1+mag) + 2*t1*x*y + t2*(r2 + 2*x*x);
		float distY = undistorted.y*(1+mag) + t1*(r2 + 2*y*y) + 2*t2*x*y;

		RemoveRadialNtoN_F32 alg = new RemoveRadialNtoN_F32().setDistortion(radial,t1,t2);

		Point2D_F32 found = new Point2D_F32();
		alg.compute(distX, distY, found);

		assertEquals(undistorted.x,found.x, GrlConstants.FLOAT_TEST_TOL_SQRT);
		assertEquals(undistorted.y,found.y, GrlConstants.FLOAT_TEST_TOL_SQRT);
	}
}
