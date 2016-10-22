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

package boofcv.alg.fiducial.square;

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.distort.radtan.LensDistortionRadialTangential;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestQuadPoseEstimator {

	private static LensDistortionNarrowFOV createDistortion() {
		CameraPinholeRadial intrinsic = new CameraPinholeRadial(500,550,0,400,300,800,600).fsetRadial(0.15,0.05);

		return new LensDistortionRadialTangential(intrinsic);
	}

	@Test
	public void basicTest() {
		LensDistortionNarrowFOV distortion = createDistortion();

		Se3_F64 expectedW2C = new Se3_F64();
		expectedW2C.T.set(0.1,-0.05,4);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.03,0,0,expectedW2C.R);

		Quadrilateral_F64 quadPlane = new Quadrilateral_F64(-0.5,0.5,0.5,0.5,0.5,-0.5,-0.5,-0.5);
		Quadrilateral_F64 quadViewed = new Quadrilateral_F64();

		project(expectedW2C, quadPlane.a, quadViewed.a);
		project(expectedW2C, quadPlane.b, quadViewed.b);
		project(expectedW2C, quadPlane.c, quadViewed.c);
		project(expectedW2C, quadPlane.d, quadViewed.d);

		QuadPoseEstimator alg = new QuadPoseEstimator(1e-8,200);

		alg.setFiducial(-0.5,0.5,0.5,0.5,0.5,-0.5,-0.5,-0.5);
		alg.setLensDistoriton(distortion);
		assertTrue(alg.process(quadViewed));

		Se3_F64 found = alg.getWorldToCamera();

		assertTrue(found.T.distance(expectedW2C.T)<1e-6);
		assertTrue(MatrixFeatures.isIdentical(found.R, expectedW2C.R,1e-6));
	}

	private void project( Se3_F64 worldToCamera, Point2D_F64 p , Point2D_F64 v ) {
		Point3D_F64 a = new Point3D_F64(p.x,p.y,0);
		Point3D_F64 b = new Point3D_F64();
		SePointOps_F64.transform(worldToCamera,a,b);
		v.x = b.x/b.z;
		v.y = b.y/b.z;
	}

	@Test
	public void estimateP3P() {
		LensDistortionNarrowFOV distortion = createDistortion();

		Se3_F64 fiducialToCamera = new Se3_F64();
		fiducialToCamera.getT().set(0.2,-0.15,2);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.05,0.015,0.001,fiducialToCamera.R);

		QuadPoseEstimator alg = new QuadPoseEstimator(1e-8,200);
		alg.setLensDistoriton(distortion);
		double r = 1.5;
		alg.setFiducial(r,-r, r,r, -r,r, -r,-r);

		WorldToCameraToPixel worldToPixel = PerspectiveOps.createWorldToPixel(distortion, fiducialToCamera);

		alg.listObs.add(worldToPixel.transform(alg.points.get(0).location));
		alg.listObs.add(worldToPixel.transform(alg.points.get(1).location));
		alg.listObs.add(worldToPixel.transform(alg.points.get(2).location));
		alg.listObs.add(worldToPixel.transform(alg.points.get(3).location));

		Point2Transform2_F64 pixelToNorm = distortion.undistort_F64(true, false);

		for (int i = 0; i < 4; i++) {
			Point2D_F64 pixel = alg.listObs.get(i);
			pixelToNorm.compute(pixel.x,pixel.y,alg.points.get(i).observation);
		}

		for (int i = 0; i < 4; i++) {
			alg.bestError = Double.MAX_VALUE;
			alg.estimateP3P(i);
			assertEquals(0,alg.bestError,1e-6);

			assertTrue(alg.bestPose.T.distance(fiducialToCamera.T)<1e-6);
			assertTrue(MatrixFeatures.isIdentical(alg.bestPose.R, fiducialToCamera.R,1e-6));
		}
	}

	@Test
	public void computeErrors() {
		LensDistortionNarrowFOV distortion = createDistortion();

		Se3_F64 fiducialToCamera = new Se3_F64();
		fiducialToCamera.getT().set(0.2,-0.15,2);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.05, 0.015, 0.001, fiducialToCamera.R);

		QuadPoseEstimator alg = new QuadPoseEstimator(1e-8,200);
		alg.setLensDistoriton(distortion);
		double r = 1.5;
		alg.setFiducial(r,-r, r,r, -r,r, -r,-r);

		WorldToCameraToPixel worldToPixel = PerspectiveOps.createWorldToPixel(distortion, fiducialToCamera);

		for (int i = 0; i < 4; i++) {
			Point3D_F64 X = alg.points.get(i).location;
			alg.listObs.add(worldToPixel.transform(X));
		}

		// perfect
		assertEquals(0,alg.computeErrors(fiducialToCamera),1e-8);

		// now with known errors
		for (int i = 0; i < 4; i++) {
			alg.listObs.get(i).x += 1.5;

			assertEquals(1.5,alg.computeErrors(fiducialToCamera),1e-8);

			alg.listObs.get(i).x -= 1.5;
		}

	}
}
