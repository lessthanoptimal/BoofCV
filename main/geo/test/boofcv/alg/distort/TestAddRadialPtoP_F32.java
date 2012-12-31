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

import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestAddRadialPtoP_F32 {

	/**
	 * Manually compute the distorted coordinate for a point and see if it matches
	 */
	@Test
	public void againstManual() {
		double fx = 600;
		double fy = 500;
		double skew = 2;
		double xc = 300;
		double yc = 350;

		double radial[]= new double[]{0.01,-0.03};

		Point2D_F64 orig = new Point2D_F64(19.5,400.1);
		Point2D_F64 dist = new Point2D_F64();

		Point2D_F64 normPt = new Point2D_F64();

		DenseMatrix64F K = new DenseMatrix64F(3,3,true,fx,skew,xc,0,fy,yc,0,0,1);
		DenseMatrix64F K_inv = new DenseMatrix64F(3,3);
		CommonOps.invert(K, K_inv);

		// compute normalized image coordinate
		GeometryMath_F64.mult(K_inv, orig, normPt);

		double c2 = normPt.x*normPt.x + normPt.y*normPt.y;
		double r = 1;
		double sum = 0;
		for( int i = 0; i < radial.length; i++ ) {
			r *= c2;
			sum += radial[i]*r;
		}

		dist.x = orig.x + (orig.x-xc)*sum;
		dist.y = orig.y + (orig.y-yc)*sum;

		AddRadialPtoP_F32 alg = new AddRadialPtoP_F32(fx,fy,skew,xc,yc,radial);

		Point2D_F32 found = new Point2D_F32();

		alg.compute(19.5f,400.1f,found);

		assertEquals(dist.x,found.x,1e-4);
		assertEquals(dist.y,found.y,1e-4);
	}
}
