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

package boofcv.alg.sfm.overhead;

import boofcv.alg.distort.LensDistortionOps;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestCameraPlaneProjection {

	int width = 800;
	int height = 850;
	CameraPinholeRadial param = new CameraPinholeRadial(200,201,0,width/2,height/2,width,height).fsetRadial(0.002, 0);
	Point2Transform2_F64 normToPixel = LensDistortionOps.transformPoint(param).distort_F64(false,true);

	Se3_F64 planeToCamera;

	Point3D_F64 worldPt = new Point3D_F64(0.3,0,3);
	Point3D_F64 cameraPt;
	Point2D_F64 normalizedPt = new Point2D_F64();
	Point2D_F64 pixelPt = new Point2D_F64();


	public TestCameraPlaneProjection() {
		// Easier to make up a plane in this direction
		Se3_F64 cameraToPlane = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,UtilAngle.degreeToRadian(-45),0,0.1,cameraToPlane.getR());
		cameraToPlane.getT().set(0,-5,0);

		planeToCamera = cameraToPlane.invert(null);

		cameraPt = SePointOps_F64.transform(planeToCamera,worldPt,null);
		normalizedPt.set(cameraPt.x/cameraPt.z,cameraPt.y/cameraPt.z);
		normToPixel.compute(normalizedPt.x,normalizedPt.y,pixelPt);
	}

	@Test
	public void planeToPixel() {

		CameraPlaneProjection alg = new CameraPlaneProjection();
		alg.setConfiguration(planeToCamera,param);

		Point2D_F64 found = new Point2D_F64();

		assertTrue(alg.planeToPixel(worldPt.z, -worldPt.x, found));

		assertEquals(found.x,pixelPt.x,1e-8);
		assertEquals(found.y,pixelPt.y,1e-8);

		// try giving it a point behind the camera
		assertFalse(alg.planeToPixel(-50,-worldPt.x,found));
	}

	@Test
	public void planeToNormalized() {

		CameraPlaneProjection alg = new CameraPlaneProjection();
		alg.setConfiguration(planeToCamera,param);

		Point2D_F64 found = new Point2D_F64();

		assertTrue(alg.planeToNormalized(worldPt.z, -worldPt.x, found));

		assertEquals(found.x,normalizedPt.x,1e-8);
		assertEquals(found.y,normalizedPt.y,1e-8);

		// try giving it a point behind the camera
		assertFalse(alg.planeToNormalized(-50,-worldPt.x,found));
	}

	@Test
	public void pixelToPlane() {
		CameraPlaneProjection alg = new CameraPlaneProjection();
		alg.setConfiguration(planeToCamera,param);

		Point2D_F64 found = new Point2D_F64();

		assertTrue(alg.pixelToPlane(pixelPt.x,pixelPt.y, found));

		assertEquals(found.x,worldPt.z,1e-6);
		assertEquals(found.y, -worldPt.x, 1e-6);

		// give it a point which won't intersect the plane
		assertFalse(alg.pixelToPlane(-10000, 0, found));
	}

	@Test
	public void normalToPlane() {
		CameraPlaneProjection alg = new CameraPlaneProjection();
		alg.setConfiguration(planeToCamera,param);

		Point2D_F64 found = new Point2D_F64();

		assertTrue(alg.normalToPlane(normalizedPt.x,normalizedPt.y, found));

		assertEquals(found.x,worldPt.z,1e-6);
		assertEquals(found.y, -worldPt.x, 1e-6);

		// give it a point which won't intersect the plane
		assertFalse(alg.pixelToPlane(-10000, 0, found));
	}

}
