/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.geo.AssociatedPair;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Peter Abeles
 */
class TestPoseFromPairLinear6 extends BoofStandardJUnit {

	@Nested
	class Calibrate extends ChecksMotionNPoint {
		/**
		 * Standard test using only the minimum number of observation
		 */
		@Test
		void minimalObservationTest() {
			standardTest(6);
		}

		/**
		 * Standard test with an over determined system
		 */
		@Test
		void overdetermined() {
			standardTest(20);
		}

		@Override
		public Se3_F64 compute(List<AssociatedPair> obs, List<Point3D_F64> locations) {
			PoseFromPairLinear6 alg = new PoseFromPairLinear6();

			alg.process(obs,locations);

			Se3_F64 se = new Se3_F64();

			DMatrixRMaj P = alg.getProjective();
			PerspectiveOps.projectionSplit(P,se.R,se.T);

			// Need to do this so that it gives the best fit SE(3)
			SpecialEuclideanOps_F64.bestFit(se);
			return se;
		}
	}

	@Nested
	class Projective extends ChecksMotionNPointHomogenous
	{
		@Override
		public DMatrixRMaj compute(List<AssociatedPair> obs, List<Point4D_F64> locations) {
			PoseFromPairLinear6 alg = new PoseFromPairLinear6();

			alg.processHomogenous(obs,locations);

			return alg.getProjective();
		}
	}
}
