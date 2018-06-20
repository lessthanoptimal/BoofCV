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
import georegression.struct.point.Point2D_F64;

/**
 * Formulas for {@link boofcv.struct.calib.CameraPinhole}.
 *
 * @author Peter Abeles
 */
public class BundleAdjustmentPinhole extends BundleAdjustmentCamera {

	// parameters for the camera model
	private boolean zeroSkew=true;
	private double fx,fy,skew,cx,cy;

	public BundleAdjustmentPinhole(boolean zeroSkew) {
		this.zeroSkew = zeroSkew;
	}

	public BundleAdjustmentPinhole() {
	}

	public BundleAdjustmentPinhole(CameraPinhole intrinsic ) {
		zeroSkew = intrinsic.skew == 0;
		fx = intrinsic.fx;
		fy = intrinsic.fy;
		cx = intrinsic.cx;
		cy = intrinsic.cy;
		skew = intrinsic.skew;
	}

	@Override
	public void setParameters(double[] parameters, int offset) {
		fx = parameters[offset];
		fy = parameters[offset+1];
		cx = parameters[offset+2];
		cy = parameters[offset+3];

		if( !zeroSkew ) {
			skew = parameters[offset+4];
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
		if( !zeroSkew ) {
			parameters[offset+4] = skew;
		}
	}

	@Override
	public void project(double camX, double camY, double camZ, Point2D_F64 output) {
		double nx = camX/camZ;
		double ny = camY/camZ;

		output.x = fx*nx + skew*ny + cx;
		output.y = fy*ny + cy;
	}

	@Override
	public void jacobian(double camX, double camY, double camZ, double[] inputX, double[] inputY, double[] calibX, double[] calibY) {
		double nx = camX/camZ;
		double ny = camY/camZ;

		calibX[0] = nx; calibY[0] = 0;
		calibX[1] = 0;  calibY[1] = ny;
		calibX[2] = 1;  calibY[2] = 0;
		calibX[3] = 0;  calibY[3] = 1;
		if( !zeroSkew ) {
			calibX[4] = ny; calibY[4] = 0;
		}

		inputX[0] = fx/camZ;           inputY[0] = 0;
		inputX[1] = skew/camZ;         inputY[1] = fy/camZ;
		inputX[2] = -(fx*camX + skew*camY)/(camZ*camZ);
		inputY[2] = -fy*camY/(camZ*camZ);
	}

	@Override
	public void jacobian(double camX, double camY, double camZ, double[] inputX, double[] inputY)
	{
		inputX[0] = fx/camZ;           inputY[0] = 0;
		inputX[1] = skew/camZ;         inputY[1] = fy/camZ;
		inputX[2] = -(fx*camX + skew*camY)/(camZ*camZ);
		inputY[2] = -fy*camY/(camZ*camZ);
	}


	@Override
	public int getParameterCount() {
		return zeroSkew ? 4 : 5;
	}
}
