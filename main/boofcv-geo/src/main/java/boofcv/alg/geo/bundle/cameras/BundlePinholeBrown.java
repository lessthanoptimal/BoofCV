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

package boofcv.alg.geo.bundle.cameras;

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.struct.calib.CameraPinholeBrown;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

/**
 * Formulas for {@link CameraPinholeBrown}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class BundlePinholeBrown implements BundleAdjustmentCamera {

	// if true skew is assumed to be zero
	public boolean zeroSkew = true;
	// if true tangential terms are assumed to be not zero
	public boolean tangential = true;
	public double fx, fy, skew, cx, cy;
	public double[] radial;
	public double t1, t2;

	public BundlePinholeBrown( boolean zeroSkew, boolean tangential ) {
		this.zeroSkew = zeroSkew;
		this.tangential = tangential;
		radial = new double[2];
	}

	public BundlePinholeBrown() {}

	public BundlePinholeBrown setK( double fx, double fy, double skew, double cx, double cy ) {
		this.fx = fx;
		this.fy = fy;
		this.skew = skew;
		this.cx = cx;
		this.cy = cy;
		this.zeroSkew = skew == 0.0;
		return this;
	}

	public BundlePinholeBrown setK( DMatrixRMaj K ) {
		fx = K.get(0, 0);
		fy = K.get(1, 1);
		cx = K.get(0, 2);
		cy = K.get(1, 2);
		if (zeroSkew)
			skew = 0;
		else
			skew = K.get(0, 1);
		return this;
	}

	public BundlePinholeBrown setRadial( double... radial ) {
		this.radial = radial.clone();
		return this;
	}

	public BundlePinholeBrown setTangential( double t1, double t2 ) {
		this.t1 = t1;
		this.t2 = t2;
		this.tangential = t1 != 0.0 || t2 != 0.0;
		return this;
	}

	@Override
	public void setIntrinsic( double[] parameters, int offset ) {
		fx = parameters[offset++];
		fy = parameters[offset++];
		cx = parameters[offset++];
		cy = parameters[offset++];
		for (int i = 0; i < radial.length; i++) {
			radial[i] = parameters[offset++];
		}
		if (tangential) {
			t1 = parameters[offset++];
			t2 = parameters[offset++];
		}

		if (!zeroSkew) {
			skew = parameters[offset];
		} else {
			skew = 0;
		}
	}

	@Override
	public void getIntrinsic( double[] parameters, int offset ) {
		parameters[offset++] = fx;
		parameters[offset++] = fy;
		parameters[offset++] = cx;
		parameters[offset++] = cy;
		for (int i = 0; i < radial.length; i++) {
			parameters[offset++] = radial[i];
		}
		if (tangential) {
			parameters[offset++] = t1;
			parameters[offset++] = t2;
		}
		if (!zeroSkew) {
			parameters[offset] = skew;
		}
	}

	@Override
	public void project( double camX, double camY, double camZ, Point2D_F64 output ) {
		// compute normalized image coordinates
		double nx = camX/camZ;
		double ny = camY/camZ;

		// compute radial distortion
		double a = 0;
		double r2 = nx*nx + ny*ny;
		double r2i = r2;
		for (int i = 0; i < radial.length; i++) {
			a += radial[i]*r2i;
			r2i *= r2;
		}

		// Apply distortion
		double x, y;
		if (tangential) {
			x = nx*(1 + a) + 2*t1*nx*ny + t2*(r2 + 2*nx*nx);
			y = ny*(1 + a) + t1*(r2 + 2*ny*ny) + 2*t2*nx*ny;
		} else {
			x = nx*(1 + a);
			y = ny*(1 + a);
		}
		// Convert to pixels
		output.x = fx*x + skew*y + cx;
		output.y = fy*y + cy;
	}

	@Override
	public void jacobian( double camX, double camY, double camZ, double[] inputX, double[] inputY,
						  boolean computeIntrinsic, @Nullable double[] calibX, @Nullable double[] calibY ) {
		double nx = camX/camZ;
		double ny = camY/camZ;

		double Z = camZ;

		// Apply radial distortion
		double sum = 0;
		double sumdot = 0;

		double r2 = nx*nx + ny*ny;
		double r2i = r2;
		double rdev = 1;

		for (int i = 0; i < radial.length; i++) {
			sum += radial[i]*r2i;
			sumdot += radial[i]*(i + 1)*rdev;

			r2i *= r2;
			rdev *= r2;
		}

		// X
		double xdot = sumdot*2*nx*nx/Z + (1 + sum)/Z;
		double ydot = sumdot*2*nx*ny/Z;
		if (tangential) {
			xdot += (2*t1*ny + t2*6*nx)/Z;
			ydot += (2*t1*nx + 2*ny*t2)/Z;
		}
		inputX[0] = fx*xdot + skew*ydot;
		inputY[0] = fy*ydot;

		// Y
		xdot = sumdot*2*ny*nx/Z;
		ydot = sumdot*2*ny*ny/Z + (1 + sum)/Z;
		if (tangential) {
			xdot += (2*t1*nx + t2*2*ny)/Z;
			ydot += (6*t1*ny + 2*nx*t2)/Z;
		}
		inputX[1] = fx*xdot + skew*ydot;
		inputY[1] = fy*ydot;

		// Z
		xdot = -sumdot*2*r2*nx/Z;
		ydot = -sumdot*2*r2*ny/Z;

		xdot += -(1 + sum)*nx/Z;
		ydot += -(1 + sum)*ny/Z;

		if (tangential) {
			xdot += -(4*t1*nx*ny + 6*t2*nx*nx + 2*t2*ny*ny)/Z;
			ydot += -(2*t1*nx*nx + 6*t1*ny*ny + 4*nx*ny*t2)/Z;
		}
		inputX[2] = fx*xdot + skew*ydot;
		inputY[2] = fy*ydot;

		if (!computeIntrinsic)
			return;

		// compute distorted normalized image coordinates
		double x = nx + nx*sum + (tangential ? 2*t1*nx*ny + t2*(r2 + 2*nx*nx) : 0);
		double y = ny + ny*sum + (tangential ? t1*(r2 + 2*ny*ny) + 2*t2*ny*ny : 0);

		if (calibX != null && calibY != null)
			jacobianIntrinsic(calibX, calibY, nx, ny, x, y);
	}

	/**
	 * @param calibX storage for calibration jacobian
	 * @param calibY storage for calibration jacobian
	 * @param nx undistorted normalized image coordinates
	 * @param ny undistorted normalized image coordinates
	 * @param dnx distorted normalized image coordinates
	 * @param dny distorted normalized image coordinates
	 */
	private void jacobianIntrinsic( double[] calibX, double[] calibY,
									double nx, double ny,
									double dnx, double dny ) {
		// Intrinsic parameters
		int index = 0;
		calibX[index] = dnx;
		calibY[index++] = 0;   // fx
		calibX[index] = 0;
		calibY[index++] = dny; // fy
		calibX[index] = 1;
		calibY[index++] = 0;   // cx
		calibX[index] = 0;
		calibY[index++] = 1;   // cy

		// Radial
		double r2 = nx*nx + ny*ny;
		double r2i = r2;
		for (int i = 0; i < radial.length; i++) {
			double xdot = nx*r2i;
			double ydot = ny*r2i;

			calibX[index] = fx*xdot + skew*ydot;
			calibY[index++] = fy*ydot;
			r2i *= r2;
		}

		// Tangential
		if (tangential) {
			double xy2 = 2.0*nx*ny;
			double r2yy = r2 + 2*ny*ny;
			double r2xx = r2 + 2*nx*nx;

			calibX[index] = fx*xy2 + skew*r2yy;
			calibY[index++] = fy*r2yy;

			calibX[index] = fx*r2xx + skew*xy2;
			calibY[index++] = fy*xy2;
		}

		if (!zeroSkew) {
			calibX[index] = dny;
			calibY[index] = 0;
		}
	}

	@Override
	public int getIntrinsicCount() {
		return 4 + radial.length + (tangential ? 2 : 0) + (zeroSkew ? 0 : 1);
	}
}
