/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.bundle;

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeRadial;
import georegression.struct.point.Point2D_F64;

/**
 * Formulas for {@link CameraPinhole}.
 *
 * @author Peter Abeles
 */
public class BundleAdjustmentPinholeRadial extends BundleAdjustmentCamera {

	// parameters for the camera model
	private boolean zeroSkew=true;
	private double fx,fy,skew,cx,cy;
	private double r1,r2;
	private double t1,t2;

	public BundleAdjustmentPinholeRadial(boolean zeroSkew) {
		this.zeroSkew = zeroSkew;
	}

	public BundleAdjustmentPinholeRadial() {
	}

	public BundleAdjustmentPinholeRadial(CameraPinholeRadial intrinsic ) {
		if( intrinsic.radial.length > 2 )
			throw new RuntimeException("Radial is too long");

		zeroSkew = intrinsic.skew == 0;
		fx = intrinsic.fx;
		fy = intrinsic.fy;
		cx = intrinsic.cx;
		cy = intrinsic.cy;
		r1=r2=0;
		if( intrinsic.radial.length > 0 )
			r1 = intrinsic.radial[0];
		if( intrinsic.radial.length > 1 )
			r2 = intrinsic.radial[1];
		t1 = intrinsic.t1;
		t2 = intrinsic.t2;
		skew = intrinsic.skew;
	}

	@Override
	public void setParameters(double[] parameters, int offset) {
		fx = parameters[offset];
		fy = parameters[offset+1];
		cx = parameters[offset+2];
		cy = parameters[offset+3];
		r1 = parameters[offset+4];
		r2 = parameters[offset+5];
		t1 = parameters[offset+6];
		t2 = parameters[offset+7];

		if( !zeroSkew ) {
			skew = parameters[offset+8];
		} else {
			skew = 0;
		}
	}

	@Override
	public void getParameters(double[] parameters, int offset) {
		parameters[offset] = fx;
		parameters[offset+1] = fy;
		parameters[offset+2] = cx;
		parameters[offset+3] = cy;
		parameters[offset+4] = r1;
		parameters[offset+5] = r2;
		parameters[offset+6] = t1;
		parameters[offset+7] = t2;
		if( !zeroSkew ) {
			parameters[offset+8] = skew;
		}
	}

	@Override
	public void project(double camX, double camY, double camZ, Point2D_F64 output) {
		// compute normalized image coordinates
		double nx = camX/camZ;
		double ny = camY/camZ;

		// Apply radial distortion
		double rr = nx*nx + ny*ny;
		double sum = (r1 + r2*rr)*rr;
		double x = nx*( 1 + sum);
		double y = ny*( 1 + sum);

		// Apply tangential distortion
		x += 2*t1*nx*ny + t2*(rr + 2*nx*nx);
		y += t1*(rr + 2*ny*ny) + 2*t2*nx*ny;

		// Convert to pixels
		output.x = fx*x + skew*y + cx;
		output.y = fy*y + cy;
	}

	@Override
	public void jacobian(double camX, double camY, double camZ, double[] inputX, double[] inputY, double[] calibX, double[] calibY) {
		double nx = camX/camZ;
		double ny = camY/camZ;

		double X = camX;
		double Y = camY;
		double Z = camZ;
		double ZZ = Z*Z;

		// Apply radial distortion
		double rr = nx*nx + ny*ny;
		double sum = (r1 + r2*rr)*rr;
		double x = nx*( 1 + sum);
		double y = ny*( 1 + sum);

		// Apply tangential distortion
		x += 2*t1*nx*ny + t2*(rr + 2*nx*nx);
		y += t1*(rr + 2*ny*ny) + 2*t2*nx*ny;

		calibX[0] = x;  calibY[0] = 0; // fx
		calibX[1] = 0;  calibY[1] = y; // fy
		calibX[2] = 1;  calibY[2] = 0; // cx
		calibX[3] = 0;  calibY[3] = 1; // cy

		// r1
		calibX[4] = (nx*fx + ny*skew)*rr;
		calibY[4] = ny*fy*rr;

		// r2
		calibX[5] = (nx*fx + ny*skew)*rr*rr;
		calibY[5] = ny*fy*rr*rr;

		// t1
		calibX[6] = skew*(rr + 2*ny*ny) + 2*nx*ny*fx;
		calibY[6] = fy*(rr + 2*ny*ny);

		// t2
		calibX[7] = fx*(rr + 2*nx*nx) + 2*nx*ny*skew;
		calibY[7] = 2*nx*ny*fy;

		if( !zeroSkew ) {
			calibX[8] = ny; calibY[8] = 0;
		}

		double A0 = r1 + r2*rr;
		double B0 = nx*(r2*rr + A0);
		double B1 = ny*(r2*rr + A0);

		// X
		inputX[0] = (fx*(2*nx*B0 + (sum + 1) + 2*ny*t1 + 6*nx*t2) + 2*skew*(ny*B0 + nx*t1 + ny*t2))/Z;
		inputY[0] = 2*fy*(ny*B0 + nx*t1 + ny*t2)/Z;

		// Y
		inputX[1] = 2*fx*(nx*B1 + nx*t1 + ny*t2)/Z + skew*((2*ny*B1 + sum + 1)/Z + 6*ny*t1 + 2*nx*t2);
		inputY[1] = fy*(2*Y*Y*(r2*rr + A0)/Z + (sum + 1)*Z + 6*Y*t1 + 2*X*t2)/ZZ;

		// Z
		inputX[2] = -(2*t2*(2*nx*nx + rr) + 2*(r2*rr*rr + sum)*nx + (sum + 1)*nx + 4*nx*ny*t1)*fx/Z - (2*t1*(nx*nx + 3*ny*ny) + 2*(r2*rr*rr/Z + sum)*ny + (sum + 1)*ny + 4*nx*ny*t2)*skew/Z;
		inputY[2] = -(2*t1*(rr + 2*ny*ny) + 2*(r2*rr + A0)*rr*Y/Z + (sum + 1)*ny + 4*nx*ny*t2)*fy/Z;
	}

	@Override
	public void jacobian(double camX, double camY, double camZ, double[] inputX, double[] inputY)
	{
		// compute normalized image coordinates
		double nx = camX/camZ;
		double ny = camY/camZ;

		// Apply radial distortion
		double rr = nx*nx + ny*ny;
		double sum = (r1 + r2*rr)*rr;

		double X = camX;
		double Y = camY;
		double Z = camZ;
		double ZZ = Z*Z;

		double A0 = r1 + r2*rr;
		double B0 = nx*(r2*rr + A0);
		double B1 = ny*(r2*rr + A0);

		// X
		inputX[0] = (fx*(2*nx*B0 + (sum + 1) + 2*ny*t1 + 6*nx*t2) + 2*skew*(ny*B0 + nx*t1 + ny*t2))/Z;
		inputY[0] = 2*fy*(ny*B0 + nx*t1 + ny*t2)/Z;

		// Y
		inputX[1] = 2*fx*(nx*B1 + nx*t1 + ny*t2)/Z + skew*((2*ny*B1 + sum + 1)/Z + 6*ny*t1 + 2*nx*t2);
		inputY[1] = fy*(2*Y*Y*(r2*rr + A0)/Z + (sum + 1)*Z + 6*Y*t1 + 2*X*t2)/ZZ;

		// Z
		inputX[2] = -(2*t2*(2*nx*nx + rr) + 2*(r2*rr*rr + sum)*nx + (sum + 1)*nx + 4*nx*ny*t1)*fx/Z - (2*t1*(nx*nx + 3*ny*ny) + 2*(r2*rr*rr/Z + sum)*ny + (sum + 1)*ny + 4*nx*ny*t2)*skew/Z;
		inputY[2] = -(2*t1*(rr + 2*ny*ny) + 2*(r2*rr + A0)*rr*Y/Z + (sum + 1)*ny + 4*nx*ny*t2)*fy/Z;
	}


	@Override
	public int getParameterCount() {
		return zeroSkew ? 8 : 9;
	}
}
