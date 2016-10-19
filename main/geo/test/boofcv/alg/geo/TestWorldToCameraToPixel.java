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

import boofcv.alg.distort.LensDistortionOps;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestWorldToCameraToPixel {
	CameraPinholeRadial intrinsic = new CameraPinholeRadial(500,500,0,320,240,640,480).fsetRadial(-0.1,-0.05);
	Point2Transform2_F64 normToPixel = LensDistortionOps.transformPoint(intrinsic).distort_F64(false,true);

	Se3_F64 worldToCamera = new Se3_F64();

	Point3D_F64 infront = new Point3D_F64(-0.1,0.2,0);
	Point3D_F64 behind = new Point3D_F64(-0.1,0.2,-4);

	Point2D_F64 expectedInFront = new Point2D_F64();

	public TestWorldToCameraToPixel() {
		worldToCamera.getT().set(0,0,3);

		Point3D_F64 tmp = new Point3D_F64();
		SePointOps_F64.transform(worldToCamera,infront,tmp);
		normToPixel.compute(tmp.x/tmp.z,tmp.y/tmp.z,expectedInFront);
	}

	@Test
	public void transform_two() {

		WorldToCameraToPixel worldToPixel = new WorldToCameraToPixel();
		worldToPixel.configure(intrinsic,worldToCamera);

		Point2D_F64 found = new Point2D_F64();

		assertTrue(worldToPixel.transform(infront,found));
		assertTrue(found.distance(expectedInFront)<1e-8);
		assertFalse(worldToPixel.transform(behind, found));
	}

	@Test
	public void transform_one() {
		WorldToCameraToPixel worldToPixel = new WorldToCameraToPixel();
		worldToPixel.configure(intrinsic,worldToCamera);

		Point2D_F64 found = worldToPixel.transform(infront);

		assertTrue(found != null);
		assertTrue(found.distance(expectedInFront)<1e-8);
		assertTrue(null == worldToPixel.transform(behind));
	}
}