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

import boofcv.alg.geo.GeoTestingOps;
import boofcv.alg.geo.PointPositionPair;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPoseResidualsSimple extends CommonMotionNPoint {

	@Test
	public void perfect() {
		Se3_F64 motion = new Se3_F64();
		motion.getR().set(RotationMatrixGenerator.eulerArbitrary(0, 1, 2, 0.05, -0.03, 0.02));
		motion.getT().set(0.1,-0.1,0.01);

		generateScene(10, motion, false);
		PointPositionPair p = pointPose.get(0);

		PoseResidualsSimple alg = new PoseResidualsSimple();

		alg.setModel(motion);
		double residuals[] = new double[2];
		alg.computeResiduals(p,residuals,0);
		assertEquals(0, GeoTestingOps.residualError(residuals), 1e-8);
	}

	@Test
	public void error() {
		Se3_F64 motion = new Se3_F64();
		motion.getR().set(RotationMatrixGenerator.eulerArbitrary(0, 1, 2, 0.05, -0.03, 0.02));
		motion.getT().set(0.1,-0.1,0.01);

		generateScene(10, motion, false);
		PointPositionPair p = pointPose.get(0);

		PoseResidualsSimple alg = new PoseResidualsSimple();

		motion.getT().setX(0.5);
		alg.setModel(motion);
		double residuals[] = new double[2];
		alg.computeResiduals(p,residuals,0);
		assertTrue(GeoTestingOps.residualError(residuals) > 0);
	}
}
