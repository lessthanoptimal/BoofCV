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
	DenseMatrix64F K = new DenseMatrix64F(3,3,true,60,0.01,-200,0,80,-150,0,0,1);

	// camera locations
	Se3_F64 se2,se3;

	// camera matrix for views 2 and 3
	DenseMatrix64F P2,P3;

	// trifocal tensor for these views
	TrifocalTensor tensor;


	public TestMultiViewOps() {
		se2 = new Se3_F64();
		se3 = new Se3_F64();

		RotationMatrixGenerator.eulerXYZ(0.2,0.001,-0.02,se2.R);
		se2.getT().set(0.3,0,0.05);

		RotationMatrixGenerator.eulerXYZ(0.8,-0.02,0.003,se3.R);
		se3.getT().set(0.5, 0.2, -0.02);

		P2 = MultiViewOps.createCameraMatrix(se2.R,se2.T,K,null);
		P3 = MultiViewOps.createCameraMatrix(se3.R,se3.T,K,null);
		tensor = MultiViewOps.createTrifocal(P2, P3, null);
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
				assertEquals(0,A.get(i,j),1e-8);
			}
		}
	}
}
