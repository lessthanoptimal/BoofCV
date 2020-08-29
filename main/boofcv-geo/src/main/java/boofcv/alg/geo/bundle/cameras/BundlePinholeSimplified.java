/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import georegression.struct.point.Point2D_F64;
import org.ejml.FancyPrint;
import org.ejml.data.DMatrixRMaj;

/**
 * A pinhole camera with radial distortion that is fully described using three parameters. Focal length and two
 * radial distortion parameters.
 *
 * Assumptions:
 * <ul>
 *     <li>Zero skew</li>
 *     <li>fx and fy is the same, e.g. square pixels</li>
 *     <li>No tangential distortion</li>
 *     <li>Image center is at coordinate (0,0)</li>
 * </ul>
 * The image center being at (0,0) only matters if the camera's FOV is being enforced by filtering out pixels that
 * are outside the image. With this camera model pixels can have positive and negative values.
 *
 *
 * @author Peter Abeles
 */
public class BundlePinholeSimplified implements BundleAdjustmentCamera {
	// focal length
	public double f;
	// radial distortion parameters
	public double k1,k2;

	public BundlePinholeSimplified() {
	}

	public BundlePinholeSimplified(double f, double k1, double k2) {
		this.f = f;
		this.k1 = k1;
		this.k2 = k2;
	}

	@Override
	public void setIntrinsic(double[] parameters, int offset) {
		f = parameters[offset];
		k1 = parameters[offset+1];
		k2 = parameters[offset+2];
	}

	@Override
	public void getIntrinsic(double[] parameters, int offset) {
		parameters[offset] = f;
		parameters[offset+1] = k1;
		parameters[offset+2] = k2;
	}

	@Override
	public void project(double camX, double camY, double camZ, Point2D_F64 output) {
		double normX = camX/camZ;
		double normY = camY/camZ;

		double n2 = normX*normX + normY*normY;

		double r = 1.0 + (k1 + k2*n2)*n2;

		output.x = f*r*normX;
		output.y = f*r*normY;
	}

	@Override
	public void jacobian(double X, double Y, double Z,
						 double[] inputX, double[] inputY, boolean computeIntrinsic, double[] calibX, double[] calibY) {

		double normX = X/Z;
		double normY = Y/Z;

		double n2 = normX*normX + normY*normY;

		double n2_X = 2*normX/Z;
		double n2_Y = 2*normY/Z;
		double n2_Z = -2*n2/Z;


		double r = 1.0 + (k1 + k2*n2)*n2;
		double kk = k1 + 2*k2*n2;

		double r_Z = n2_Z*kk;

		// partial X
		inputX[0] = (f/Z)*(r + 2*normX*normX*kk);
		inputY[0] = f*normY*n2_X*kk;

		// partial Y
		inputX[1] = f*normX*n2_Y*kk;
		inputY[1] = (f/Z)*(r + 2*normY*normY*kk);

		// partial Z
		inputX[2] = f*normX*(r_Z - r/Z);
		inputY[2] = f*normY*(r_Z - r/Z);

		if(!computeIntrinsic)
			return;

		// partial f
		calibX[0] = r*normX;
		calibY[0] = r*normY;

		// partial k1
		calibX[1] = f*normX*n2;
		calibY[1] = f*normY*n2;

		// partial k2
		calibX[2] = f*normX*n2*n2;
		calibY[2] = f*normY*n2*n2;
	}

	@Override
	public int getIntrinsicCount() {
		return 3;
	}

	@Override
	public String toString() {
		FancyPrint fp = new FancyPrint();

		return "BundlePinholeSimplified{" +
				"f=" + fp.s(f) +
				", k1=" + fp.s(k1) +
				", k2=" + fp.s(k2) +
				'}';
	}

	public void reset() {
		k1 = k2 = f = 0.0;
	}

	public void set( BundlePinholeSimplified c ) {
		this.f  = c.f;
		this.k1 = c.k1;
		this.k2 = c.k2;
	}

	public void set( CameraPinhole c ) {
		this.f  = (c.fx+c.fy)/2.0;
		this.k1 = 0.0;
		this.k2 = 0.0;
	}

	public void set( DMatrixRMaj K ) {
		this.f  = (K.get(0,0)+K.get(1,1))/2.0;
		this.k1 = 0.0;
		this.k2 = 0.0;
	}

	public void convertTo(DMatrixRMaj K) {
		K.reshape(3,3);
		K.zero();
		K.unsafe_set(0,0,f);
		K.unsafe_set(1,1,f);
		K.unsafe_set(2,2,1);
	}

	public void convertTo(CameraPinhole out) {
		out.fx = out.fy = this.f;
		out.cx = out.cy = 0.0;
		out.skew = 0.0;
	}

	public void convertTo(CameraPinholeBrown out) {
		this.convertTo((CameraPinhole)out);
		out.fsetRadial(this.k1, this.k2);
	}

	public BundlePinholeSimplified copy() {
		return new BundlePinholeSimplified(f,k1,k2);
	}
}
