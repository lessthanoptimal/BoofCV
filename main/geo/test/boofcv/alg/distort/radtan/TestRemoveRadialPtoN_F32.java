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

import boofcv.alg.distort.Transform2ThenPixel_F32;
import boofcv.alg.geo.PerspectiveOps;
import georegression.geometry.GeometryMath_F32;
import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F32;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestRemoveRadialPtoN_F32 {

	@Test
	public void checkAgainstAdd() {
		checkAgainstAdd(0,0);
		checkAgainstAdd(0.1f,-0.05f);
	}

	public void checkAgainstAdd( float t1 , float t2 ) {
		float fx = 600;
		float fy = 500;
		float skew = 2;
		float xc = 300;
		float yc = 350;

		/**/double radial[]= new /**/double[]{0.12f,-0.13f};

		Point2D_F32 point = new Point2D_F32();

		float undistX = 19.5f;
		float undistY = 200.1f;

		AddRadialPtoN_F32 p_to_n = new AddRadialPtoN_F32().setK(fx, fy, skew, xc, yc).setDistortion(radial,t1,t2);
		new Transform2ThenPixel_F32(p_to_n).set(fx, fy, skew, xc, yc).compute(undistX, undistY, point);

		float distX = point.x;
		float distY = point.y;

		RemoveRadialPtoN_F32 alg = new RemoveRadialPtoN_F32().setK(fx,fy,skew,xc,yc).setDistortion(radial,t1,t2);

		alg.compute(distX, distY, point);

		/// go from calibrated coordinates to pixel
		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(fx, fy, skew, xc, yc);

		GeometryMath_F32.mult(K,point,point);

		assertEquals(undistX,point.x, GrlConstants.FLOAT_TEST_TOL_SQRT);
		assertEquals(undistY,point.y, GrlConstants.FLOAT_TEST_TOL_SQRT);
	}
}
