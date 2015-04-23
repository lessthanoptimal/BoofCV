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

import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestDistanceSe2Sq extends StandardDistanceTest<Se2_F64, AssociatedPair> {

	Random rand = new Random(234);

	@Override
	public DistanceFromModel<Se2_F64, AssociatedPair> create() {
		return new DistanceSe2Sq();
	}

	@Override
	public Se2_F64 createRandomModel() {

		double x = rand.nextDouble()*5;
		double y = rand.nextDouble()*5;
		double yaw = 2*rand.nextDouble()*Math.PI;

		return new Se2_F64(x,y,yaw);
	}

	@Override
	public AssociatedPair createRandomData() {
		Point2D_F64 p1 = new Point2D_F64(rand.nextGaussian(),rand.nextGaussian());
		Point2D_F64 p2 = new Point2D_F64(rand.nextGaussian(),rand.nextGaussian());

		return new AssociatedPair(p1,p2);
	}

	@Override
	public double distance(Se2_F64 h, AssociatedPair obs) {

		Point2D_F64 result = new Point2D_F64();

		SePointOps_F64.transform(h, obs.p1, result);
		return result.distance2(obs.p2);
	}

}
