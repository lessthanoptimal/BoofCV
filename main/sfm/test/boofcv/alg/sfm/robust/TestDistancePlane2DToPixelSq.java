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

package boofcv.alg.sfm.robust;

import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.sfm.overhead.CameraPlaneProjection;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.sfm.PlanePtPixel;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDistancePlane2DToPixelSq {

	CameraPinholeRadial intrinsic = new CameraPinholeRadial(200,210,0,320,240,640,480).fsetRadial(0,0);

	DistancePlane2DToPixelSq alg = new DistancePlane2DToPixelSq();
	Se3_F64 planeToCamera;
	Se2_F64 motion2D;

	Point2D_F64 planePtA = new Point2D_F64();
	Point2D_F64 planePtB = new Point2D_F64();

	Point2D_F64 pixelPtA = new Point2D_F64(150,75);
	Point2D_F64 pixelPtB = new Point2D_F64();

	Point2Transform2_F64 pixelToNorm = LensDistortionOps.transformPoint(intrinsic).undistort_F64(true,false);
	Point2D_F64 normPt = new Point2D_F64();

	public TestDistancePlane2DToPixelSq() {
		// Easier to make up a plane in this direction
		Se3_F64 cameraToPlane = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,UtilAngle.degreeToRadian(-75), 0.1, 0.0, cameraToPlane.getR());
		cameraToPlane.getT().set(0, -2, 0);

		planeToCamera = cameraToPlane.invert(null);
		motion2D = new Se2_F64(0.5,-0.05,0.15);

		alg.setExtrinsic(planeToCamera);
		alg.setIntrinsic(intrinsic.fx, intrinsic.fy, intrinsic.skew);
		alg.setModel(motion2D);

		// compute point on plane from pixel
		CameraPlaneProjection planeProjection = new CameraPlaneProjection();
		planeProjection.setConfiguration(planeToCamera, intrinsic);
		planeProjection.pixelToPlane(pixelPtA.x, pixelPtA.y, planePtA);

		// move the point on the plane and compute pixel observation
		SePointOps_F64.transform(motion2D,planePtA,planePtB);
		planeProjection.planeToPixel(planePtB.x, planePtB.y, pixelPtB);
	}

	@Test
	public void perfect() {

		pixelToNorm.compute(pixelPtB.x,pixelPtB.y,normPt);

		double error = alg.computeDistance(new PlanePtPixel(planePtA,normPt));

		assertEquals(0, error, 1e-8);
	}

	@Test
	public void noisy() {

		pixelToNorm.compute(pixelPtB.x+2,pixelPtB.y,normPt);

		double error = alg.computeDistance(new PlanePtPixel(planePtA,normPt));

		assertEquals(4, error, 1e-8);
	}

}
