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

package boofcv.alg.distort.pinhole;

import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPinholePtoN_F64 {
	double fx = 200;
	double fy = 300;
	double skew = 1.2;
	double x_c = 400;
	double y_c = 450;

	/**
	 * Do the same calculation but using a different but equivalent equation
	 */
	@Test
	public void basic() {
		PinholePtoN_F64 alg = new PinholePtoN_F64();
		alg.set(fx,fy,skew,x_c,y_c);

		Point2D_F64 in = new Point2D_F64(100,120);
		Point2D_F64 out = new Point2D_F64();

		alg.compute(in.x,in.y,out);

		Point2D_F64 expected = new Point2D_F64();
		DenseMatrix64F K_inv = new DenseMatrix64F(3,3,true,fx,skew,x_c,0,fy,y_c,0,0,1);
		CommonOps.invert(K_inv);

		GeometryMath_F64.mult(K_inv, in, expected);

		assertEquals(expected.x,out.x,1e-5);
		assertEquals(expected.y, out.y, 1e-5);
	}
}
