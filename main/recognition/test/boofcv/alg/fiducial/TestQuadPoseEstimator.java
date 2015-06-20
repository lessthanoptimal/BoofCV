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

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.struct.calib.IntrinsicParameters;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
	public void estimate() {
		fail("imeplement");
	}

	@Test
	public void computeErrors() {
		fail("imeplement");
	}
}
