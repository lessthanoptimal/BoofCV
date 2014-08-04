/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import georegression.geometry.GeometryMath_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestZhang99ComputeTargetHomography {

	/**
	 * Give it a grid and see if it computed a legitimate homography
	 */
	@Test
	public void basicTest() {
		// create a grid an apply an arbitrary transform to it
		PlanarCalibrationTarget config = GenericCalibrationGrid.createStandardConfig();

		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(0.02,-0.05,0.01,null);
		Vector3D_F64 T = new Vector3D_F64(0,0,-1000);
		Se3_F64 motion = new Se3_F64(R,T);

		List<Point2D_F64> observations = GenericCalibrationGrid.observations(motion,config);

		// compute the homography
		Zhang99ComputeTargetHomography alg = new Zhang99ComputeTargetHomography(config.points);

		assertTrue(alg.computeHomography(observations));

		DenseMatrix64F H = alg.getHomography();

		// test this homography property: x2 = H*x1
		List<Point2D_F64> gridPoints = config.points;
		for( int i = 0; i < observations.size(); i++ ) {
			Point2D_F64 a = GeometryMath_F64.mult(H, gridPoints.get(i), new Point2D_F64());

			double diff = a.distance(observations.get(i));
			assertEquals(0,diff,1e-8);
		}
	}
}
