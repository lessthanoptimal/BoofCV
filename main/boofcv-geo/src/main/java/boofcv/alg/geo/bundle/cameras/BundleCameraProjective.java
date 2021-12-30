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
import georegression.struct.point.Point2D_F64;
import org.jetbrains.annotations.Nullable;

/**
 * Projective camera model. pixel (x,y) = (X/Z , Y/Z)
 *
 * @author Peter Abeles
 */
public class BundleCameraProjective implements BundleAdjustmentCamera {
	@Override
	public void setIntrinsic( double[] parameters, int offset ) {}

	@Override
	public void getIntrinsic( double[] parameters, int offset ) {}

	@Override
	public void project( double camX, double camY, double camZ, Point2D_F64 output ) {
		output.x = camX/camZ;
		output.y = camY/camZ;
	}

	@Override
	public void jacobian( double camX, double camY, double camZ, double[] pointX, double[] pointY, boolean computeIntrinsic, @Nullable double[] calibX, @Nullable double[] calibY ) {
		pointX[0] = 1/camZ;
		pointX[1] = 0;
		pointX[2] = -camX/(camZ*camZ);
		pointY[0] = 0;
		pointY[1] = 1/camZ;
		pointY[2] = -camY/(camZ*camZ);

		// there are no calibration parameters to differentiate
	}

	@Override
	public int getIntrinsicCount() {
		return 0;
	}
}
