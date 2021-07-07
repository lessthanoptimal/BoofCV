/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort.brown;

import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestAddRadialPtoN_F64 extends BoofStandardJUnit {
	/**
	 * Manually compute the distorted coordinate for a point and see if it matches
	 */
	@Test void againstManual() {
		againstManual(0,0);
		againstManual(-0.5,0.03);
	}

	public void againstManual( double t1, double t2 ) {
		double fx = 600;
		double fy = 500;
		double skew = 2;
		double xc = 300;
		double yc = 350;

		/**/double[] radial= new /**/double[]{0.01,-0.03};

		Point2D_F64 orig = new Point2D_F64(19.5,400.1); // undistorted pixel coordinates

		Point2D_F64 normPt = new Point2D_F64();

		DMatrixRMaj K = new DMatrixRMaj(3,3,true,fx,skew,xc,0,fy,yc,0,0,1);
		DMatrixRMaj K_inv = new DMatrixRMaj(3,3);
		CommonOps_DDRM.invert(K, K_inv);

		// compute normalized image coordinate
		GeometryMath_F64.mult(K_inv, orig, normPt);

		double nx = normPt.x; // undistorted normalized image coordinates
		double ny = normPt.y;

		double r2 = nx*nx + ny*ny;
		double ri2 = 1;
		double sum = 0;
		for( int i = 0; i < radial.length; i++ ) {
			ri2 *= r2;
			sum += (double) radial[i]*ri2;
		}

		// distorted normalized image coordinates
		double dnx = nx + nx*sum + 2*t1*nx*ny + t2*(r2 + 2*nx*nx);
		double dny = ny + ny*sum + t1*(r2 + 2*ny*ny) + 2*t2*nx*ny;

		AddBrownPtoN_F64 alg = new AddBrownPtoN_F64().setK(fx, fy, skew, xc, yc).
				setDistortion(radial, t1, t2);

		Point2D_F64 found = new Point2D_F64();

		alg.compute(orig.x,orig.y,found);

		assertEquals(dnx,found.x,1e-4);
		assertEquals(dny,found.y,1e-4);
	}
}
