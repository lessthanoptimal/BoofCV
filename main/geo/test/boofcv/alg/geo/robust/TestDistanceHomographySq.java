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
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestDistanceHomographySq extends StandardDistanceTest<Homography2D_F64, AssociatedPair> {

	Random rand = new Random(234);

	@Override
	public DistanceFromModel<Homography2D_F64, AssociatedPair> create() {
		return new DistanceHomographySq();
	}

	@Override
	public Homography2D_F64 createRandomModel() {
		Homography2D_F64 h = new Homography2D_F64();

		h.a11 = rand.nextDouble()*5;
		h.a12 = rand.nextDouble()*5;
		h.a13 = rand.nextDouble()*5;
		h.a21 = rand.nextDouble()*5;
		h.a22 = rand.nextDouble()*5;
		h.a23 = rand.nextDouble()*5;
		h.a31 = rand.nextDouble()*5;
		h.a32 = rand.nextDouble()*5;
		h.a33 = rand.nextDouble()*5;

		return h;
	}

	@Override
	public AssociatedPair createRandomData() {
		Point2D_F64 p1 = new Point2D_F64(rand.nextGaussian(),rand.nextGaussian());
		Point2D_F64 p2 = new Point2D_F64(rand.nextGaussian(),rand.nextGaussian());

		return new AssociatedPair(p1,p2,false);
	}

	@Override
	public double distance(Homography2D_F64 h, AssociatedPair associatedPair) {

		Point2D_F64 result = new Point2D_F64();

		HomographyPointOps_F64.transform(h, associatedPair.p1, result);
		return result.distance2(associatedPair.p2);
	}
}
