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

package boofcv.alg.distort;

import georegression.struct.point.Point2D_F32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestRemoveRadialPtoP_F32 {

	@Test
	public void checkAgainstAdd() {
		double fx = 600;
		double fy = 500;
		double skew = 2;
		double xc = 300;
		double yc = 350;

		double radial[]= new double[]{0.12,-0.13};

		Point2D_F32 point = new Point2D_F32();

		float undistX = 19.5f;
		float undistY = 200.1f;

		new AddRadialPtoP_F32(fx,fy,skew,xc,yc,radial).compute(undistX,undistY,point);

		float distX = point.x;
		float distY = point.y;

		RemoveRadialPtoP_F32 alg = new RemoveRadialPtoP_F32();
		alg.set(fx,fy,skew,xc,yc,radial);
		alg.compute(distX, distY, point);

		assertEquals(undistX,point.x,1e-4);
		assertEquals(undistY,point.y,1e-4);
	}
}
