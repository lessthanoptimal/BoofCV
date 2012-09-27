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
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixFeatures;
import org.ejml.ops.RandomMatrices;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestPerspectiveOps {

	Random rand = new Random(234);

	@Test
	public void adjustDistortion_F32() {

		DenseMatrix64F A = new DenseMatrix64F(3,3,true,1,2,3,10,4,8,2,4,9);
		DenseMatrix64F B = new DenseMatrix64F(3,3,true,2,0,1,0,3,2,0,0,1);

		IntrinsicParameters param = new IntrinsicParameters(200,300,2,250,260,200,300, true, null);
		IntrinsicParameters paramAdj = new IntrinsicParameters();

		PointTransformHomography_F32 firstTran = new PointTransformHomography_F32(A);

		// test forward case
		PointTransform_F32 foundTran = PerspectiveOps.adjustIntrinsic_F32(firstTran, true, param, B, paramAdj);

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
		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(param, null);
		DenseMatrix64F Kfound = PerspectiveOps.calibrationMatrix(paramAdj, null);
		DenseMatrix64F Kexpected = new DenseMatrix64F(3,3);

		CommonOps.mult(B,K,Kexpected);
		assertTrue(MatrixFeatures.isIdentical(Kexpected,Kfound,1e-8));

		// test reverse case
		foundTran = PerspectiveOps.adjustIntrinsic_F32(firstTran, false, param, B, paramAdj);

		foundTran.compute(1,3,foundPt);

		GeometryMath_F32.mult(B, X, temp);
		GeometryMath_F32.mult(A, temp, expectedPt);

		assertEquals(expectedPt.x,foundPt.x,1e-4);
		assertEquals(expectedPt.y,foundPt.y,1e-4);

		// check the new intrinsic parameters
		Kfound = PerspectiveOps.calibrationMatrix(paramAdj, null);
		CommonOps.invert(B);

		CommonOps.mult(B,K,Kexpected);
		assertTrue(MatrixFeatures.isIdentical(Kexpected,Kfound,1e-8));

	}

	@Test
	public void calibrationMatrix() {
		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(1, 2, 3, 4, 5);

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
		IntrinsicParameters ret = PerspectiveOps.matrixToParam(K, 100, 200, true, null);

		assertTrue(ret.fx == fx);
		assertTrue(ret.fy == fy);
		assertTrue(ret.skew == skew);
		assertTrue(ret.cx == cx);
		assertTrue(ret.cy == cy);
		assertTrue(ret.width == 100);
		assertTrue(ret.height == 200);
		assertTrue(ret.flipY);
	}

	@Test
	public void convertNormToPixel() {
		fail("implement");
	}

	@Test
	public void convertPixelToNorm() {
		fail("implement");
	}

	@Test
	public void renderPixel_SE() {
		fail("implement");
	}

	@Test
	public void renderPixel_cameramatrix() {
		fail("implement");
	}

	@Test
	public void computeNormalization() {

		List<AssociatedPair> list = new ArrayList<AssociatedPair>();
		for( int i = 0; i < 12; i++ ) {
			AssociatedPair p = new AssociatedPair();

			p.currLoc.set(rand.nextDouble()*5,rand.nextDouble()*5);
			p.keyLoc.set(rand.nextDouble() * 5, rand.nextDouble() * 5);

			list.add(p);
		}

		// compute statistics
		double meanX0 = 0;
		double meanY0 = 0;
		double meanX1 = 0;
		double meanY1 = 0;

		for( AssociatedPair p : list ) {
			meanX0 += p.keyLoc.x;
			meanY0 += p.keyLoc.y;
			meanX1 += p.currLoc.x;
			meanY1 += p.currLoc.y;
		}

		meanX0 /= list.size();
		meanY0 /= list.size();
		meanX1 /= list.size();
		meanY1 /= list.size();

		double sigmaX0 = 0;
		double sigmaY0 = 0;
		double sigmaX1 = 0;
		double sigmaY1 = 0;

		for( AssociatedPair p : list ) {
			sigmaX0 += Math.pow(p.keyLoc.x-meanX0,2);
			sigmaY0 += Math.pow(p.keyLoc.y-meanY0,2);
			sigmaX1 += Math.pow(p.currLoc.x-meanX1,2);
			sigmaY1 += Math.pow(p.currLoc.y-meanY1,2);
		}

		sigmaX0 = Math.sqrt(sigmaX0/list.size());
		sigmaY0 = Math.sqrt(sigmaY0/list.size());
		sigmaX1 = Math.sqrt(sigmaX1/list.size());
		sigmaY1 = Math.sqrt(sigmaY1/list.size());

		// test the output
		DenseMatrix64F N1 = new DenseMatrix64F(3,3);
		DenseMatrix64F N2 = new DenseMatrix64F(3,3);

		PerspectiveOps.computeNormalization(N1, N2, list);

		assertEquals(1/sigmaX0, N1.get(0,0),1e-8);
		assertEquals(1/sigmaY0, N1.get(1,1),1e-8);
		assertEquals(-meanX0/sigmaX0, N1.get(0,2),1e-8);
		assertEquals(-meanY0/sigmaY0, N1.get(1,2),1e-8);
		assertEquals(1, N1.get(2,2),1e-8);

		assertEquals(1/sigmaX1, N2.get(0,0),1e-8);
		assertEquals(1/sigmaY1, N2.get(1,1),1e-8);
		assertEquals(-meanX1/sigmaX1, N2.get(0,2),1e-8);
		assertEquals(-meanY1/sigmaY1, N2.get(1,2),1e-8);
		assertEquals(1, N2.get(2,2),1e-8);
	}

	/**
	 * Test it against a simple test case
	 */
	@Test
	public void pixelToNormalized() {
		DenseMatrix64F N = new DenseMatrix64F(3,3,true,1,2,3,4,5,6,7,8,9);

		Point2D_F64 a = new Point2D_F64(3,4);
		Point2D_F64 found = new Point2D_F64(3,4);
		Point2D_F64 expected = new Point2D_F64(3,4);

		expected.x = a.x * N.get(0,0) + N.get(0,2);
		expected.y = a.y * N.get(1,1) + N.get(1,2);


		PerspectiveOps.pixelToNormalized(a, found, N);

		assertEquals(found.x,expected.x,1e-8);
		assertEquals(found.y,expected.y,1e-8);
	}

	@Test
	public void createCameraMatrix() {
		SimpleMatrix R = SimpleMatrix.random(3, 3, -1, 1, rand);
		Vector3D_F64 T = new Vector3D_F64(2,3,-4);
		SimpleMatrix K = SimpleMatrix.wrap(RandomMatrices.createUpperTriangle(3, 0, -1, 1, rand));

		SimpleMatrix T_ = new SimpleMatrix(3,1,true,T.x,T.y,T.z);

		// test calibrated camera
		DenseMatrix64F found = PerspectiveOps.createCameraMatrix(R.getMatrix(), T, null, null);
		for( int i = 0; i < 3; i++ ) {
			assertEquals(found.get(i,3),T_.get(i),1e-8);
			for( int j = 0; j < 3; j++ ) {
				assertEquals(found.get(i,j),R.get(i,j),1e-8);
			}
		}

		// test uncalibrated camera
		found = PerspectiveOps.createCameraMatrix(R.getMatrix(), T, K.getMatrix(), null);

		SimpleMatrix expectedR = K.mult(R);
		SimpleMatrix expectedT = K.mult(T_);

		for( int i = 0; i < 3; i++ ) {
			assertEquals(found.get(i,3),expectedT.get(i),1e-8);
			for( int j = 0; j < 3; j++ ) {
				assertEquals(found.get(i,j),expectedR.get(i,j),1e-8);
			}
		}
	}
}
