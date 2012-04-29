/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F32;
import georegression.geometry.GeometryMath_F32;
import georegression.struct.point.Point2D_F32;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestUtilIntrinsic {

	@Test
	public void adjustDistortion_F32() {

		DenseMatrix64F A = new DenseMatrix64F(3,3,true,1,2,3,10,4,8,2,4,9);
		DenseMatrix64F B = new DenseMatrix64F(3,3,true,2,0,1,0,3,2,0,0,1);

		IntrinsicParameters param = new IntrinsicParameters(200,300,2,250,260,200,300, true, null);
		IntrinsicParameters paramAdj = new IntrinsicParameters();

		PointTransformHomography_F32 firstTran = new PointTransformHomography_F32(A);

		// test forward case
		PointTransform_F32 foundTran = UtilIntrinsic.adjustIntrinsic_F32(firstTran, true, param, B, paramAdj);

		Point2D_F32 X = new Point2D_F32(1,3);

		Point2D_F32 foundPt = new Point2D_F32();
		Point2D_F32 expectedPt = new Point2D_F32();

		foundTran.compute(1,3,foundPt);

		Point2D_F32 temp = new Point2D_F32();
		GeometryMath_F32.mult(A, X, temp);
		GeometryMath_F32.mult(B, temp, expectedPt);

		assertEquals(expectedPt.x,foundPt.x,1e-4);
		assertEquals(expectedPt.y,foundPt.y,1e-4);

		// check the new intrinsic parameters
		DenseMatrix64F K = UtilIntrinsic.calibrationMatrix(param,null);
		DenseMatrix64F Kfound = UtilIntrinsic.calibrationMatrix(paramAdj,null);
		DenseMatrix64F Kexpected = new DenseMatrix64F(3,3);

		CommonOps.mult(B,K,Kexpected);
		assertTrue(MatrixFeatures.isIdentical(Kexpected,Kfound,1e-8));

		// test reverse case
		foundTran = UtilIntrinsic.adjustIntrinsic_F32(firstTran, false, param, B, paramAdj);

		foundTran.compute(1,3,foundPt);

		GeometryMath_F32.mult(B, X, temp);
		GeometryMath_F32.mult(A, temp, expectedPt);

		assertEquals(expectedPt.x,foundPt.x,1e-4);
		assertEquals(expectedPt.y,foundPt.y,1e-4);

		// check the new intrinsic parameters
		Kfound = UtilIntrinsic.calibrationMatrix(paramAdj,null);
		CommonOps.invert(B);

		CommonOps.mult(B,K,Kexpected);
		assertTrue(MatrixFeatures.isIdentical(Kexpected,Kfound,1e-8));

	}

	@Test
	public void calibrationMatrix() {
		DenseMatrix64F K = UtilIntrinsic.calibrationMatrix(1, 2, 3, 4, 5);

		assertEquals(1,K.get(0,0),1e-3);
		assertEquals(2,K.get(1,1),1e-3);
		assertEquals(3,K.get(0,1),1e-3);
		assertEquals(4,K.get(0,2),1e-3);
		assertEquals(5,K.get(1,2),1e-3);
		assertEquals(1,K.get(2,2),1e-3);
	}

	@Test
	public void matrixToParam() {
		double fx = 1;
		double fy = 2;
		double skew = 3;
		double cx = 4;
		double cy = 5;

		DenseMatrix64F K = new DenseMatrix64F(3,3,true,fx,skew,cx,0,fy,cy,0,0,1);
		IntrinsicParameters ret = UtilIntrinsic.matrixToParam(K, 100, 200, true, null);

		assertTrue(ret.fx == fx);
		assertTrue(ret.fy == fy);
		assertTrue(ret.skew == skew);
		assertTrue(ret.cx == cx);
		assertTrue(ret.cy == cy);
		assertTrue(ret.width == 100);
		assertTrue(ret.height == 200);
		assertTrue(ret.leftHanded);
	}
}
