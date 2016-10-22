/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
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
		basicTest(false);
		basicTest(true);
	}

	public void basicTest( boolean removeAFew ) {
		// create a grid an apply an arbitrary transform to it
		List<Point2D_F64> layout = GenericCalibrationGrid.standardLayout();

		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.02,-0.05,0.01,null);
		Vector3D_F64 T = new Vector3D_F64(0,0,-1000);
		Se3_F64 motion = new Se3_F64(R,T);

		CalibrationObservation observations = GenericCalibrationGrid.observations(motion,layout);

		if( removeAFew ) {
			for (int i = 0; i < 6; i++) {
				observations.points.remove(5);
			}
		}

		// compute the homography
		Zhang99ComputeTargetHomography alg = new Zhang99ComputeTargetHomography(layout);

		assertTrue(alg.computeHomography(observations));

		DenseMatrix64F H = alg.getHomography();

		// test this homography property: x2 = H*x1
		for( int i = 0; i < observations.size(); i++ ) {
			int gridIndex = observations.get(i).index;
			Point2D_F64 p = observations.get(i);

			Point2D_F64 a = GeometryMath_F64.mult(H, layout.get(gridIndex), new Point2D_F64());

			double diff = a.distance(p);
			assertEquals(0,diff,1e-8);
		}
}
}
