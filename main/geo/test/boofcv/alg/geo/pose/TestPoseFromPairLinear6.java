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

import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class TestPoseFromPairLinear6 extends ChecksMotionNPoint {

	/**
	 * Standard test using only the minimum number of observation
	 */
	@Test
	public void minimalObservationTest() {
		standardTest(6);
	}

	/**
	 * Standard test with an over determined system
	 */
	@Test
	public void overdetermined() {
		standardTest(20);
	}

	@Override
	public Se3_F64 compute(List<AssociatedPair> obs, List<Point3D_F64> locations) {
		PoseFromPairLinear6 alg = new PoseFromPairLinear6();

		alg.process(obs,locations);

		return alg.getMotion();
	}
}
