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

package boofcv.abst.fiducial;

import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestFourPointSyntheticStability extends BoofStandardJUnit {

	CameraPinholeBrown intrinsic = new CameraPinholeBrown(300,300,0,300,300,600,600).fsetRadial(0.2,0.01);

	/**
	 * Try a few different known scenarios
	 */
	@Test
	void basic() {
		Point2Transform2_F64 p2n = new LensDistortionBrown(intrinsic).undistort_F64(true,false);
		Point2Transform2_F64 n2p = new LensDistortionBrown(intrinsic).distort_F64(false,true);

		FourPointSyntheticStability alg = new FourPointSyntheticStability();

		alg.setShape(0.2,0.2);
		alg.setTransforms(p2n,n2p);

		Se3_F64 f2c0 = SpecialEuclideanOps_F64.eulerXyz(0,0,1,0,0,0,null);
		Se3_F64 f2c1 = SpecialEuclideanOps_F64.eulerXyz(0,0,3,0,0,0,null);
		Se3_F64 f2c2 = SpecialEuclideanOps_F64.eulerXyz(0,0,1,0,0.5,0,null);

		FiducialStability found0 = new FiducialStability();
		FiducialStability found1 = new FiducialStability();
		FiducialStability found2 = new FiducialStability();

		alg.computeStability(f2c0,1,found0);
		alg.computeStability(f2c1,1,found1);
		alg.computeStability(f2c2,1,found2);

		// farther away, errors result in large pose errors
		assertTrue(found0.location*1.1 < found1.location);
		assertTrue(found0.orientation*1.1 < found1.orientation);

		// when viewed at an angle a small change results in a large change in pose, meaning its easier to estimate
		// when viewed head on a small change in pixel results in a similar pose making it insensitive to changes
		assertTrue(found0.location > 1.1*found2.location);
		assertTrue(found0.orientation  > 1.1*found2.orientation);

		// larger disturbance larger error
		alg.computeStability(f2c0,2,found1);
		assertTrue(found0.location*1.1 < found1.location);
		assertTrue(found0.orientation*1.1 < found1.orientation);
	}
}
