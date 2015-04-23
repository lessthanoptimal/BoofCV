/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.robust;

import boofcv.struct.geo.Point2D3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class DistanceTranGivenRotSq implements DistanceFromModel<Vector3D_F64,Point2D3D> {

	Se3_F64 motion = new Se3_F64();

	Point3D_F64 localX = new Point3D_F64();

	public void setRotation( DenseMatrix64F R ) {
		motion.getR().set(R);
	}

	@Override
	public void setModel(Vector3D_F64 translation) {
		motion.getT().set(translation);
	}

	@Override
	public double computeDistance(Point2D3D pt) {
		Point3D_F64 X = pt.location;
		Point2D_F64 obs = pt.observation;

		SePointOps_F64.transform(motion,X,localX);

		double dx = obs.x - localX.x/localX.z;
		double dy = obs.y - localX.y/localX.z;

		return dx*dx + dy*dy;
	}

	@Override
	public void computeDistance(List<Point2D3D> data, double[] distance) {
		for( int i = 0; i < data.size(); i++ ) {
			distance[i] = computeDistance(data.get(i));
		}
	}
}
