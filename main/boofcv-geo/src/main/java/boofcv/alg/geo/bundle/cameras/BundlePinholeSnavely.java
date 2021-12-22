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

import georegression.struct.point.Point2D_F64;
import org.jetbrains.annotations.Nullable;

/**
 * Bundler and Bundle Adjustment in the Large use a different coordinate system. This
 * converts it into what BoofCV understands by applying a negative sign to the Z coordinate.
 *
 * @author Peter Abeles
 */
public class BundlePinholeSnavely extends BundlePinholeSimplified {
	@Override
	public void project( double camX, double camY, double camZ, Point2D_F64 output ) {
		super.project(camX, camY, -camZ, output);
	}

	@Override
	public void jacobian( double X, double Y, double Z,
						  double[] inputX, double[] inputY,
						  boolean computeIntrinsic, @Nullable double[] calibX, @Nullable double[] calibY ) {

		Z = -Z;

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
		inputX[2] = f*normX*(r/Z - r_Z); // you have no idea how many hours I lost before I realized the mistake here
		inputY[2] = f*normY*(r/Z - r_Z);

		if (!computeIntrinsic || calibX == null || calibY == null)
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
	public String toString() {
		return "BundlePinholeSnavely{" +
				"f=" + f +
				", k1=" + k1 +
				", k2=" + k2 +
				'}';
	}
}
