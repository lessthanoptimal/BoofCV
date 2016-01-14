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

package boofcv.abst.geo;

import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class GeneralTestTriangulateTwoViewsCalibrated {

	public abstract TriangulateTwoViewsCalibrated createAlg();

	/**
	 * See if it can triangulate perfect observations
	 */
	@Test
	public void triangulate() {
		Point3D_F64 world = new Point3D_F64(0.5,-0.1,4);

		Se3_F64 worldToA = new Se3_F64();
		Se3_F64 worldToB = new Se3_F64();
		worldToB.getT().set(2,0.1,-0.5);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, 0.05, 0,worldToB.getR());

		Point3D_F64 pointA = SePointOps_F64.transform(worldToA,world,null);
		Point2D_F64 viewA = new Point2D_F64();
		viewA.x = pointA.x/pointA.z;
		viewA.y = pointA.y/pointA.z;
		
		Point3D_F64 pointB = SePointOps_F64.transform(worldToB,world,null);
		Point2D_F64 viewB = new Point2D_F64();
		viewB.x = pointB.x/pointB.z;
		viewB.y = pointB.y/pointB.z;

		
		TriangulateTwoViewsCalibrated alg = createAlg();
		Point3D_F64 found = new Point3D_F64();
		
		alg.triangulate(viewA,viewB,worldToB,found);
		assertEquals(found.x, world.x, 1e-8);
		assertEquals(found.y, world.y, 1e-8);
		assertEquals(found.z, world.z, 1e-8);
	}
}
