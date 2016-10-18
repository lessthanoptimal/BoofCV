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

package boofcv.alg.geo.triangulate;

import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPixelDepthLinear {

	@Test
	public void depthNView() {
		// define the camera's motion
		Se3_F64 motion1 = new Se3_F64();
		motion1.getR().set(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.05, -0.03, 0.02, null));
		motion1.getT().set(0.1, -0.1, 0.01);
		Se3_F64 motion2 = new Se3_F64();
		motion2.getR().set(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, -0.15, -0.3, 0.08, null));
		motion2.getT().set(-0.2, -0.15, 0.2);

		// compute the point's location in each camera's view
		Point3D_F64 A = new Point3D_F64(2,3,2);
		Point3D_F64 B = SePointOps_F64.transform(motion1,A,null);
		Point3D_F64 C = SePointOps_F64.transform(motion2,A,null);

		// projected points
		Point2D_F64 x1 = new Point2D_F64(A.x/A.z,A.y/A.z);
		Point2D_F64 x2 = new Point2D_F64(B.x/B.z,B.y/B.z);
		Point2D_F64 x3 = new Point2D_F64(C.x/C.z,C.y/C.z);

		// setup data structures
		List<Se3_F64> listMotion = new ArrayList<>();
		List<Point2D_F64> listPoint = new ArrayList<>();

		listMotion.add(motion1);
		listMotion.add(motion2);

		listPoint.add(x1);
		listPoint.add(x2);
		listPoint.add(x3);


		PixelDepthLinear alg = new PixelDepthLinear();

		double depth = alg.depthNView(listPoint,listMotion);

		// see if the origin point was recomputed
		assertEquals(x1.x*depth,A.x,1e-8);
		assertEquals(x1.y*depth,A.y,1e-8);
		assertEquals(depth,A.z,1e-8);
	}

	@Test
	public void depth2View() {
		// define the camera's motion
		Se3_F64 motion1 = new Se3_F64();
		motion1.getR().set(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.05, -0.03, 0.02, null));
		motion1.getT().set(0.1, -0.1, 0.01);


		// compute the point's location in each camera's view
		Point3D_F64 A = new Point3D_F64(2,3,2);
		Point3D_F64 B = SePointOps_F64.transform(motion1,A,null);

		// projected points
		Point2D_F64 x1 = new Point2D_F64(A.x/A.z,A.y/A.z);
		Point2D_F64 x2 = new Point2D_F64(B.x/B.z,B.y/B.z);

		PixelDepthLinear alg = new PixelDepthLinear();

		double depth = alg.depth2View(x1, x2, motion1);

		// see if the origin point was recomputed
		assertEquals(x1.x*depth,A.x,1e-8);
		assertEquals(x1.y*depth,A.y,1e-8);
		assertEquals(depth,A.z,1e-8);
	}
}
