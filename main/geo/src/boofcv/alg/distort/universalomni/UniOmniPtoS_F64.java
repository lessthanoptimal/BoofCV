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

package boofcv.alg.distort.universalomni;

import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.distort.Point2Transform3_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import static boofcv.alg.distort.radtan.RemoveRadialNtoN_F64.removeRadial;

/**
 * @author Peter Abeles
 */
public class UniOmniPtoS_F64 implements Point2Transform3_F64 {
	public CameraUniversalOmni params;

	Point2D_F64 p2 = new Point2D_F64();

	private double tol=1e-10;

		// inverse of camera calibration matrix
	protected DenseMatrix64F K_inv = new DenseMatrix64F(3,3);

	public UniOmniPtoS_F64(CameraUniversalOmni params) {
		this.params = params;
	}

	public UniOmniPtoS_F64() {
	}

	public CameraUniversalOmni getParams() {
		return params;
	}

	public void setParams(CameraUniversalOmni params) {
		this.params = params;

		K_inv.set(0,0,params.fx);
		K_inv.set(1,1,params.fy);
		K_inv.set(0,1,params.skew);
		K_inv.set(0,2,params.cx);
		K_inv.set(1,2,params.cy);
		K_inv.set(2,2,1);

		CommonOps.invert(K_inv);
	}

	@Override
	public void compute(double x, double y, Point3D_F64 out) {
		p2.x = x;
		p2.y = y;

		// initial estimate of undistorted point
		GeometryMath_F64.mult(K_inv, p2, p2);

		// find the undistorted normalized image coordinate
		removeRadial(p2.x, p2.y, params.radial, params.t1, params.t2, p2, tol );

		// put into unit sphere coordinates
		out.x = p2.x;
		out.y = p2.y;
//		out.z = 1.0 + ;
		// crap this step isn't going to be trivial
	}
}
