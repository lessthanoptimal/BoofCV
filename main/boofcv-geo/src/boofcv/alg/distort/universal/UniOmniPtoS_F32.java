/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort.universal;

import boofcv.alg.distort.radtan.RadialTangential_F32;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.distort.Point2Transform3_F32;
import georegression.geometry.GeometryMath_F32;
import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F32;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_FDRM;

import static boofcv.alg.distort.radtan.RemoveRadialNtoN_F32.removeRadial;

/**
 * Backwards project from a distorted 2D pixel to 3D unit sphere coordinate using the {@link CameraUniversalOmni} model.
 *
 * @author Peter Abeles
 */
public class UniOmniPtoS_F32 implements Point2Transform3_F32 {
	float mirrorOffset;
	protected RadialTangential_F32 distortion = new RadialTangential_F32();

	// work space for internal calculations
	private Point2D_F32 p2 = new Point2D_F32();

	private float tol = GrlConstants.FCONV_TOL_A;

		// inverse of camera calibration matrix
	protected FMatrixRMaj K_inv = new FMatrixRMaj(3,3);

	public UniOmniPtoS_F32(CameraUniversalOmni model) {
		this.setModel(model);
	}

	public UniOmniPtoS_F32() {
	}

	public float getTol() {
		return tol;
	}

	public void setTol(float tol) {
		this.tol = tol;
	}

	public void setModel(CameraUniversalOmni model) {
		this.mirrorOffset = (float)model.mirrorOffset;

		distortion.set(model.radial,model.t1,model.t2);

		K_inv.set(0,0, (float)model.fx);
		K_inv.set(1,1, (float)model.fy);
		K_inv.set(0,1, (float)model.skew);
		K_inv.set(0,2, (float)model.cx);
		K_inv.set(1,2, (float)model.cy);
		K_inv.set(2,2,1);

		CommonOps_FDRM.invert(K_inv);
	}

	@Override
	public void compute(float x, float y, Point3D_F32 out) {
		p2.x = x;
		p2.y = y;

		// initial estimate of undistorted point
		GeometryMath_F32.mult(K_inv, p2, p2);

		// find the undistorted normalized image coordinate
		removeRadial(p2.x, p2.y, distortion.radial, distortion.t1, distortion.t2, p2, tol );

		// put into unit sphere coordinates
		float u = p2.x;
		float v = p2.y;

		// compute adjustment to go from normalized image coordinate to unit sphere
		// This is done by finding the intersection of a line with slop X going through the
		// origin and lying on the sphere's surface, i.e. distance of 1 from the center
		// X = (u, v , 1)
		// P = (t*u, t*v, t)  and ||P-C|| = 1
		// C = (0, 0, xi)
		// There will be two solutions.  It selects the one farther down the line (top of the
		// sphere)  If xi is > 1 then it's possible for two pixels to have the same value slope
		float xi = mirrorOffset;

		// solve for the quadratic equation
		float a = u*u + v*v + 1.0f;
		float b = -2.0f*xi;
		float c = xi*xi - 1.0f;
		float t = (-b + (float)Math.sqrt(b*b - 4.0f*a*c))/(2.0f*a);

		out.x = u*t;
		out.y = v*t;
		out.z = t - xi;
	}
}
