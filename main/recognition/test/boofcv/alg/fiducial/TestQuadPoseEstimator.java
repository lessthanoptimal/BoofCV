/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial;

import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestQuadPoseEstimator {

	@Test
	public void basicTest() {
		IntrinsicParameters intrinsic = new IntrinsicParameters(500,550,0,400,300,800,600).fsetRadial(0.15,0.05);

		Se3_F64 expectedW2C = new Se3_F64();
		expectedW2C.T.set(0.1,-0.05,4);
		RotationMatrixGenerator.eulerXYZ(0.03,0,0,expectedW2C.R);

		Quadrilateral_F64 quadPlane = new Quadrilateral_F64(-0.5,0.5,0.5,0.5,0.5,-0.5,-0.5,-0.5);
		Quadrilateral_F64 quadViewed = new Quadrilateral_F64();

		WorldToCameraToPixel worldToPixel = PerspectiveOps.createWorldToPixel(intrinsic,expectedW2C);
		project(worldToPixel, quadPlane.a, quadViewed.a);
		project(worldToPixel, quadPlane.b, quadViewed.b);
		project(worldToPixel, quadPlane.c, quadViewed.c);
		project(worldToPixel, quadPlane.d, quadViewed.d);

		QuadPoseEstimator alg = new QuadPoseEstimator(1e-8,200);

		alg.setFiducial(-0.5,0.5,0.5,0.5,0.5,-0.5,-0.5,-0.5);
		alg.setIntrinsic(intrinsic);
		assertTrue(alg.process(quadViewed));

		Se3_F64 found = alg.getWorldToCamera();

		assertTrue(found.T.distance(expectedW2C.T)<1e-6);
		assertTrue(MatrixFeatures.isIdentical(found.R, expectedW2C.R,1e-6));
	}

	private void project( WorldToCameraToPixel worldToPixel, Point2D_F64 p , Point2D_F64 v ) {
		worldToPixel.transform(new Point3D_F64(p.x,p.y,0),v);
	}

	@Test
	public void estimateP3P() {
		IntrinsicParameters intrinsic = new IntrinsicParameters(500,550,0,400,300,800,600).fsetRadial(0.04,0.02);

		Se3_F64 fiducialToCamera = new Se3_F64();
		fiducialToCamera.getT().set(0.2,-0.15,2);
		RotationMatrixGenerator.eulerXYZ(0.05,0.015,0.001,fiducialToCamera.R);

		QuadPoseEstimator alg = new QuadPoseEstimator(1e-8,200);
		alg.setIntrinsic(intrinsic);
		double r = 1.5;
		alg.setFiducial(r,-r, r,r, -r,r, -r,-r);

		WorldToCameraToPixel worldToPixel = PerspectiveOps.createWorldToPixel(intrinsic, fiducialToCamera);

		alg.listObs.add(worldToPixel.transform(alg.points[0].location));
		alg.listObs.add(worldToPixel.transform(alg.points[1].location));
		alg.listObs.add(worldToPixel.transform(alg.points[2].location));
		alg.listObs.add(worldToPixel.transform(alg.points[3].location));

		PointTransform_F64 pixelToNorm = LensDistortionOps.transformPoint(intrinsic).undistort_F64(true, false);

		for (int i = 0; i < 4; i++) {
			Point2D_F64 pixel = alg.listObs.get(i);
			pixelToNorm.compute(pixel.x,pixel.y,alg.points[i].observation);
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
		IntrinsicParameters intrinsic = new IntrinsicParameters(500,550,0,400,300,800,600).fsetRadial(0.15,0.05);

		Se3_F64 fiducialToCamera = new Se3_F64();
		fiducialToCamera.getT().set(0.2,-0.15,2);
		RotationMatrixGenerator.eulerXYZ(0.05,0.015,0.001,fiducialToCamera.R);

		QuadPoseEstimator alg = new QuadPoseEstimator(1e-8,200);
		alg.setIntrinsic(intrinsic);
		double r = 1.5;
		alg.setFiducial(r,-r, r,r, -r,r, -r,-r);

		WorldToCameraToPixel worldToPixel = PerspectiveOps.createWorldToPixel(intrinsic, fiducialToCamera);

		alg.listObs.add(worldToPixel.transform(alg.points[0].location));
		alg.listObs.add(worldToPixel.transform(alg.points[1].location));
		alg.listObs.add(worldToPixel.transform(alg.points[2].location));
		alg.listObs.add(worldToPixel.transform(alg.points[3].location));

		// perfect
		assertEquals(0,alg.computeErrors(fiducialToCamera),1e-8);

		// now with known errors
		for (int i = 0; i < 4; i++) {
			alg.listObs.get(0).x += 1.5;

			assertEquals(1.5*1.5,alg.computeErrors(fiducialToCamera),1e-8);

			alg.listObs.get(0).x -= 1.5;
		}

	}
}
