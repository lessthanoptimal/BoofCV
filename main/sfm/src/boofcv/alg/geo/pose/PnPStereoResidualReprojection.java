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

package boofcv.alg.geo.pose;

import boofcv.alg.geo.ModelObservationResidualN;
import boofcv.struct.sfm.Stereo2D3D;
import boofcv.struct.sfm.StereoPose;
import georegression.struct.point.Point3D_F64;
import georegression.transform.se.SePointOps_F64;

/**
 * Computes the predicted residual as a simple geometric distance between the observed and predicted
 * point observation in normalized pixel coordinates.
 *
 * @author Peter Abeles
 */
public class PnPStereoResidualReprojection implements ModelObservationResidualN<StereoPose,Stereo2D3D>
{
	StereoPose motion;

	Point3D_F64 temp = new Point3D_F64();

	@Override
	public void setModel(StereoPose model) {
		this.motion = model;
	}

	@Override
	public int computeResiduals(Stereo2D3D data, double[] residuals, int index) {

		SePointOps_F64.transform(motion.worldToCam0, data.location, temp);

		double expectedX = temp.x / temp.z;
		double expectedY = temp.y / temp.z;

		residuals[index++] = expectedX - data.leftObs.x;
		residuals[index++] = expectedY - data.leftObs.y;

		SePointOps_F64.transform(motion.cam0ToCam1, temp, temp);

		expectedX = temp.x / temp.z;
		expectedY = temp.y / temp.z;

		residuals[index++] = expectedX - data.rightObs.x;
		residuals[index++] = expectedY - data.rightObs.y;

		return index;
	}

	@Override
	public int getN() {
		return 4;
	}
}