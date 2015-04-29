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
		checkAgainstAdd(0,0);
		checkAgainstAdd(-0.12f,0.03f);
	}

	public void checkAgainstAdd( float t1 , float t2) {
		float fx = 600;
		float fy = 500;
		float skew = 2;
		float xc = 300;
		float yc = 350;

		double radial[]= new double[]{0.12f,-0.13f};

		Point2D_F32 point = new Point2D_F32();

		float undistX = 19.5f;
		float undistY = 200.1f;

		new AddRadialPtoP_F32().setK(fx,fy,skew,xc,yc).setDistortion(radial,t1,t2).compute(undistX, undistY, point);

		float distX = point.x;
		float distY = point.y;

		RemoveRadialPtoP_F32 alg = new RemoveRadialPtoP_F32();
		alg.setK(fx,fy,skew,xc,yc).setDistortion(radial, t1, t2);

		alg.compute(distX, distY, point);

		assertEquals(undistX,point.x,1e-2);
		assertEquals(undistY,point.y,1e-2);
	}

}
