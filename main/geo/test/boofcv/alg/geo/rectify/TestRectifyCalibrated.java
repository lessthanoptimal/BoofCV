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

package boofcv.alg.geo.rectify;

import boofcv.alg.geo.PerspectiveOps;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRectifyCalibrated {

	/**
	 * Compare results from rectified transform and a set of camera which are already rectified.
	 */
	@Test
	public void compareWithKnown() {
		DenseMatrix64F K = new DenseMatrix64F(3,3,true,300,0,200,0,400,205,0,0,1);

		// transforms are world to camera, but I'm thinking camera to world, which is why invert
		Se3_F64 poseR1 = createPose(0,0,0,  0.1,0,0.1).invert(null);
		Se3_F64 poseR2 = createPose(0,0,0,  1  ,0,0.1).invert(null);

		// only rotate around the y-axis so that the rectified coordinate system will have to be
		// the same as the global
		Se3_F64 poseA1 = createPose(0,0.05,0,   0.1,0,0.1).invert(null);
		Se3_F64 poseA2 = createPose(0,-0.1,0,   1  ,0,0.1).invert(null);

		RectifyCalibrated alg = new RectifyCalibrated();
		alg.process(K,poseA1,K,poseA2);

		// original camera matrix
		DenseMatrix64F foundP1 = PerspectiveOps.createCameraMatrix(poseA1.getR(),poseA1.getT(),K,null);
		DenseMatrix64F foundP2 = PerspectiveOps.createCameraMatrix(poseA2.getR(),poseA2.getT(),K,null);

		// apply rectification transform
		DenseMatrix64F temp = new DenseMatrix64F(3,4);
		CommonOps.mult(alg.getRect1(),foundP1,temp);
		foundP1.set(temp);
		CommonOps.mult(alg.getRect2(),foundP2,temp);
		foundP2.set(temp);

		CommonOps.scale(0.1/Math.abs(foundP1.get(2,3)),foundP1);

		Point3D_F64 X = new Point3D_F64(0,0,3);

		// compare results, both should match because of rotation only being around y-axis
		assertEquals(PerspectiveOps.renderPixel(poseR1,K,X).x,PerspectiveOps.renderPixel(foundP1,X).x,1e-5);
		assertEquals(PerspectiveOps.renderPixel(poseR1,K,X).y,PerspectiveOps.renderPixel(foundP1,X).y,1e-5);
		assertEquals(PerspectiveOps.renderPixel(poseR2,K,X).x,PerspectiveOps.renderPixel(foundP2,X).x,1e-5);
		assertEquals(PerspectiveOps.renderPixel(poseR2,K,X).y,PerspectiveOps.renderPixel(foundP2,X).y,1e-5);
	}

	/**
	 * Check to see if epipoles are at infinity after rectification
	 */
	@Test
	public void checkEpipolarGeometry() {
		DenseMatrix64F K = new DenseMatrix64F(3,3,true,300,0,200,0,400,205,0,0,1);

		// only rotate around the y-axis so that the rectified coordinate system will have to be
		// the same as the global
		Se3_F64 poseA1 = createPose(-0.3,0.05,0.07,   0.1,0,0.1).invert(null);
		Se3_F64 poseA2 = createPose(0.2,-0.1,0.02,    1  ,0,0.1).invert(null);

		// project epipoles
		Point2D_F64 epi1 = PerspectiveOps.renderPixel(poseA1, K, new Point3D_F64(1, 0, 0.1));
		Point2D_F64 epi2 = PerspectiveOps.renderPixel(poseA2, K, new Point3D_F64(0.1, 0, 0.1));

		// compute transforms
		RectifyCalibrated alg = new RectifyCalibrated();
		alg.process(K,poseA1,K,poseA2);

		// apply transform
		Point3D_F64 epi1a = new Point3D_F64();
		GeometryMath_F64.mult(alg.getRect1(),epi1,epi1a);
		Point3D_F64 epi2a = new Point3D_F64();
		GeometryMath_F64.mult(alg.getRect2(),epi2,epi2a);

		// see if epipoles are now at infinity
		assertEquals(0,epi1a.getZ(),1e-8);
		assertEquals(0,epi2a.getZ(),1e-8);
	}

	/**
	 * See if the transform align an observation to the same y-axis
	 */
	@Test
	public void alignY() {
		// different calibration matrices
		DenseMatrix64F K1 = new DenseMatrix64F(3,3,true,300,0,200,0,400,205,0,0,1);
		DenseMatrix64F K2 = new DenseMatrix64F(3,3,true,180,0,195,0,370,210,0,0,1);

		// only rotate around the y-axis so that the rectified coordinate system will have to be
		// the same as the global
		Se3_F64 poseA1 = createPose(-0.3,0.05,0.07,   0.1,0,0.1);
		Se3_F64 poseA2 = createPose(0.2,-0.1,0.02,    1  ,0,0.1);
		alignY(K1, poseA1, K2, poseA2);

		// the (Y,Z) coordinates are now different
		poseA1 = createPose(-0.3,0.05,0.1,   0.1, 0.05, -0.1);
		poseA2 = createPose(0.2,-0.1,0.02,    1  ,-0.4, 0.2);
		alignY(K1, poseA1, K2, poseA2);

		poseA1 = createPose(0,0,0, 0  ,0, 0);
		poseA2 = createPose(0,0,0, -0.2  ,0, -0.1);
		alignY(K1, poseA1, K2, poseA2);
	}

	private void alignY(DenseMatrix64F K1, Se3_F64 poseA1, DenseMatrix64F K2 , Se3_F64 poseA2) {
		// point being observed
		Point3D_F64 X = new Point3D_F64(0,0,4);

		// unrectified observation
		Point2D_F64 o1 = PerspectiveOps.renderPixel(poseA1, K1, X);
		Point2D_F64 o2 = PerspectiveOps.renderPixel(poseA2, K2, X);

		// original observations should not line up
		assertTrue(Math.abs(o1.y - o2.y) > 1e-8 );

		// compute transforms
		RectifyCalibrated alg = new RectifyCalibrated();
		alg.process(K1,poseA1, K2,poseA2);

		// apply transform to create rectified observations
		Point2D_F64 r1 = new Point2D_F64();
		Point2D_F64 r2 = new Point2D_F64();

		GeometryMath_F64.mult(alg.getRect1(), o1, r1);
		GeometryMath_F64.mult(alg.getRect2(),o2,r2);

		// see if they line up
		assertEquals(r1.y,r2.y,1e-8);
	}

	private Se3_F64 createPose( double rotX , double rotY , double rotZ , double x , double y , double z )
	{
		Se3_F64 ret = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,rotX, rotY, rotZ, ret.getR());
		ret.getT().set(x,y,z);
		return ret;
	}

}
