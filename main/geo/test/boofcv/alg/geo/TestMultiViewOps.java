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

import boofcv.struct.geo.TrifocalTensor;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixFeatures;
import org.ejml.ops.RandomMatrices;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestMultiViewOps {

	Random rand = new Random(234);

	// camera calibration matrix
	DenseMatrix64F K = new DenseMatrix64F(3,3,true,60,0.01,200,0,80,150,0,0,1);
//	DenseMatrix64F K = CommonOps.identity(3);

	// camera locations
	Se3_F64 se2,se3;

	// camera matrix for views 2 and 3
	DenseMatrix64F P2,P3;

	// Fundamental matrix for views 2 and 3
	DenseMatrix64F F2,F3;

	// trifocal tensor for these views
	TrifocalTensor tensor;


	public TestMultiViewOps() {
		se2 = new Se3_F64();
		se3 = new Se3_F64();

		RotationMatrixGenerator.eulerXYZ(0.2,0.001,-0.02,se2.R);
		se2.getT().set(0.3,0,0.05);

		RotationMatrixGenerator.eulerXYZ(0.8,-0.02,0.003,se3.R);
		se3.getT().set(0.6, 0.2, -0.02);

		P2 = MultiViewOps.createCameraMatrix(se2.R,se2.T,K,null);
		P3 = MultiViewOps.createCameraMatrix(se3.R,se3.T,K,null);
		tensor = MultiViewOps.createTrifocal(P2, P3, null);

		F2 = UtilEpipolar.computeEssential(se2.getR(),se2.getT());
		F2 = UtilEpipolar.computeFundamental(F2,K);
		F3 = UtilEpipolar.computeEssential(se3.getR(),se3.getT());
		F3 = UtilEpipolar.computeFundamental(F3,K);
	}

	@Test
	public void createCameraMatrix() {
		SimpleMatrix R = SimpleMatrix.random(3, 3, -1, 1, rand);
		Vector3D_F64 T = new Vector3D_F64(2,3,-4);
		SimpleMatrix K = SimpleMatrix.wrap(RandomMatrices.createUpperTriangle(3, 0, -1, 1, rand));

		SimpleMatrix T_ = new SimpleMatrix(3,1,true,T.x,T.y,T.z);

		// test calibrated camera
		DenseMatrix64F found = MultiViewOps.createCameraMatrix(R.getMatrix(),T,null,null);
		for( int i = 0; i < 3; i++ ) {
			assertEquals(found.get(i,3),T_.get(i),1e-8);
			for( int j = 0; j < 3; j++ ) {
				assertEquals(found.get(i,j),R.get(i,j),1e-8);
			}
		}

		// test uncalibrated camera
		found = MultiViewOps.createCameraMatrix(R.getMatrix(),T,K.getMatrix(),null);

		SimpleMatrix expectedR = K.mult(R);
		SimpleMatrix expectedT = K.mult(T_);

		for( int i = 0; i < 3; i++ ) {
			assertEquals(found.get(i,3),expectedT.get(i),1e-8);
			for( int j = 0; j < 3; j++ ) {
				assertEquals(found.get(i,j),expectedR.get(i,j),1e-8);
			}
		}
	}

	/**
	 * Check the trifocal tensor using its definition
	 */
	@Test
	public void createTrifocal() {
		SimpleMatrix P1 = SimpleMatrix.random(3,4,-1,1,rand);
		SimpleMatrix P2 = SimpleMatrix.random(3,4,-1,1,rand);

		TrifocalTensor found = MultiViewOps.createTrifocal(P1.getMatrix(),P2.getMatrix(),null);

		for( int i = 0; i < 3; i++ ) {
			SimpleMatrix ai = P1.extractVector(false,i);
			SimpleMatrix b4 = P2.extractVector(false,3);
			SimpleMatrix a4 = P1.extractVector(false,3);
			SimpleMatrix bi = P2.extractVector(false,i);

			SimpleMatrix expected = ai.mult(b4.transpose()).minus(a4.mult(bi.transpose()));

			assertTrue(MatrixFeatures.isIdentical(expected.getMatrix(),found.getT(i),1e-8));
		}
	}

	@Test
	public void constraintTrifocal_lll() {
		fail("implement");
	}

	@Test
	public void constraintTrifocal_pll() {
		fail("implement");
	}

	@Test
	public void constraintTrifocal_plp() {
		fail("implement");
	}

	@Test
	public void constraintTrifocal_ppl() {
		fail("implement");
	}

	@Test
	public void constraintTrifocal_ppp() {
		// Point in 3D space being observed
		Point3D_F64 X = new Point3D_F64(0.1,0.5,3);

		// compute normalized image coordinates
		Point2D_F64 p1 = new Point2D_F64(X.x/X.z,X.y/X.z);

		Point3D_F64 temp = new Point3D_F64();
		SePointOps_F64.transform(se2,X,temp);
		Point2D_F64 p2 = new Point2D_F64(temp.x/temp.z,temp.y/temp.z);

		SePointOps_F64.transform(se3,X,temp);
		Point2D_F64 p3 = new Point2D_F64(temp.x/temp.z,temp.y/temp.z);

		// convert into pixel coordinates
		// don't convert the first view to pixels since it is assumed to be P1=[I|0]
		GeometryMath_F64.mult(K,p2,p2);
		GeometryMath_F64.mult(K,p3,p3);

		// check the constraint
		DenseMatrix64F A = MultiViewOps.constraintTrifocal(tensor,p1,p2,p3,null);

		for( int i = 0; i < 3; i++ ) {
			for( int j = 0; j < 3; j++ ) {
				assertEquals(0,A.get(i,j),1e-7);
			}
		}
	}

	@Test
	public void extractEpipoles() {
		Point3D_F64 found2 = new Point3D_F64();
		Point3D_F64 found3 = new Point3D_F64();

		TrifocalTensor input = tensor.copy();

		MultiViewOps.extractEpipoles(input,found2,found3);

		// make sure the input was not modified
		for( int i = 0; i < 3; i++ )
			assertTrue(MatrixFeatures.isIdentical(tensor.getT(i),input.getT(i),1e-8));

		Point3D_F64 space = new Point3D_F64();

		// check to see if it is the left-null space of their respective Fundamental matrices
		GeometryMath_F64.multTran(F2, found2, space);
		assertEquals(0,space.norm(),1e-8);

		GeometryMath_F64.multTran(F3, found3, space);
		assertEquals(0,space.norm(),1e-8);
	}

	@Test
	public void extractFundamental() {
		DenseMatrix64F found2 = new DenseMatrix64F(3,3);
		DenseMatrix64F found3 = new DenseMatrix64F(3,3);

		TrifocalTensor input = tensor.copy();
		MultiViewOps.extractFundamental(input, found2, found3);

		// make sure the input was not modified
		for( int i = 0; i < 3; i++ )
			assertTrue(MatrixFeatures.isIdentical(tensor.getT(i),input.getT(i),1e-8));

		CommonOps.scale(1.0/CommonOps.elementMaxAbs(found2),found2);
		CommonOps.scale(1.0/CommonOps.elementMaxAbs(found3),found3);

		Point3D_F64 X = new Point3D_F64(0.1,0.05,2);

		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(),K,X);
		Point2D_F64 x2 = PerspectiveOps.renderPixel(se2,K,X);
		Point2D_F64 x3 = PerspectiveOps.renderPixel(se3,K,X);

		assertEquals(0, GeometryMath_F64.innerProd(x2, found2, x1), 1e-8);
		assertEquals(0, GeometryMath_F64.innerProd(x3, found3, x1), 1e-8);
	}

	@Test
	public void extractCameraMatrices() {
		DenseMatrix64F P2 = new DenseMatrix64F(3,4);
		DenseMatrix64F P3 = new DenseMatrix64F(3,4);

		TrifocalTensor input = tensor.copy();
		MultiViewOps.extractCameraMatrices(input, P2, P3);

		// make sure the input was not modified
		for( int i = 0; i < 3; i++ )
			assertTrue(MatrixFeatures.isIdentical(tensor.getT(i),input.getT(i),1e-8));

		// Using found camera matrices render the point's location
		Point3D_F64 X = new Point3D_F64(0.1,0.05,2);

		Point2D_F64 x1 = new Point2D_F64(X.x/X.z,X.y/X.z);
		Point2D_F64 x2 = PerspectiveOps.renderPixel(P2,X);
		Point2D_F64 x3 = PerspectiveOps.renderPixel(P3,X);

		// validate correctness by testing a constraint on the points
		DenseMatrix64F A = new DenseMatrix64F(3,3);
		MultiViewOps.constraintTrifocal(tensor,x1,x2,x3,A);

		for( int i = 0; i < 3; i++ ) {
			for( int j = 0; j < 3; j++ ) {
				assertEquals(0,A.get(i,j),1e-7);
			}
		}
	}
}
