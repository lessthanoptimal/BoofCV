/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.geo.PointPositionPair;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

/**
 * Give a set of 3D points and their position, computes the error of the proposed motion.
 * Camera observations must be calibrated.
 *
 * @author Peter Abeles
 */
public class PoseResidualsSimple implements ModelObservationResidualN<Se3_F64,PointPositionPair> {

	Se3_F64 worldToCamera;

	Point3D_F64 predicted = new Point3D_F64();

	@Override
	public void setModel(Se3_F64 worldToCamera ) {
		this.worldToCamera = worldToCamera;
	}

	@Override
	public int computeResiduals(PointPositionPair obs, double[] residuals, int index) {
		SePointOps_F64.transform(worldToCamera,obs.location,predicted);

		residuals[index++] = predicted.x/predicted.z - obs.observed.x;
		residuals[index++] = predicted.y/predicted.z - obs.observed.y;

		return index;
	}

	@Override
	public int getN() {
		return 2;
	}
}
