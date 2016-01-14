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

package boofcv.alg.geo;

import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPositiveDepthConstraintCheck {

	/**
	 * Point a point in front of both cameras and see if it returns true
	 */
	@Test
	public void testPositive() {
		// create transform from A to B
		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0, -0.05, 0, null);
		Vector3D_F64 T = new Vector3D_F64(1,0,0);
		Se3_F64 fromAtoB = new Se3_F64(R,T);

		// point in front of both cameras
		Point3D_F64 pt = new Point3D_F64(0,0,2);

		// create observations of the point in calibrated coordinates
		Point2D_F64 obsA = new Point2D_F64(0,0);
		Point3D_F64 pt_inB = SePointOps_F64.transform(fromAtoB,pt,null);
		Point2D_F64 obsB = new Point2D_F64(pt_inB.x/pt_inB.z,pt_inB.y/pt_inB.z);

		PositiveDepthConstraintCheck alg = new PositiveDepthConstraintCheck();

		assertTrue(alg.checkConstraint(obsA,obsB,fromAtoB));
	}

	/**
	 * Point a point in behind the cameras
	 */
	@Test
	public void testNegative() {
		// create transform from A to B
		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0, -0.05, 0, null);
		Vector3D_F64 T = new Vector3D_F64(1,0,0);
		Se3_F64 fromAtoB = new Se3_F64(R,T);

		// point in front of both cameras
		Point3D_F64 pt = new Point3D_F64(0,0,-1);

		// create observations of the point in calibrated coordinates
		Point2D_F64 obsA = new Point2D_F64(0,0);
		Point3D_F64 pt_inB = SePointOps_F64.transform(fromAtoB,pt,null);
		Point2D_F64 obsB = new Point2D_F64(pt_inB.x/pt_inB.z,pt_inB.y/pt_inB.z);

		PositiveDepthConstraintCheck alg = new PositiveDepthConstraintCheck();

		assertFalse(alg.checkConstraint(obsA, obsB, fromAtoB));
	}
}
