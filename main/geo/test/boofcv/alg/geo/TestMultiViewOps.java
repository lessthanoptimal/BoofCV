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
import org.ejml.ops.NormOps;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestMultiViewOps {

	Random rand = new Random(234);

	// camera calibration matrix
	DenseMatrix64F K = new DenseMatrix64F(3,3,true,60,0.01,200,0,80,150,0,0,1);

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

		P2 = PerspectiveOps.createCameraMatrix(se2.R, se2.T, K, null);
		P3 = PerspectiveOps.createCameraMatrix(se3.R, se3.T, K, null);
		tensor = MultiViewOps.createTrifocal(P2, P3, null);

		F2 = MultiViewOps.createEssential(se2.getR(), se2.getT());
		F2 = MultiViewOps.createFundamental(F2, K);
		F3 = MultiViewOps.createEssential(se3.getR(), se3.getT());
		F3 = MultiViewOps.createFundamental(F3, K);
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
	public void constraint_Trifocal_lll() {
		fail("implement");
	}

	@Test
	public void constraint_Trifocal_pll() {
		fail("implement");
	}

	@Test
	public void constraint_Trifocal_plp() {
		fail("implement");
	}

	@Test
	public void constraint_Trifocal_ppl() {
		fail("implement");
	}

	@Test
	public void constraint_Trifocal_ppp() {
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
		DenseMatrix64F A = MultiViewOps.constraint(tensor, p1, p2, p3, null);

		for( int i = 0; i < 3; i++ ) {
			for( int j = 0; j < 3; j++ ) {
				assertEquals(0,A.get(i,j),1e-7);
			}
		}
	}

	@Test
	public void constraint_epipolar() {
		Point3D_F64 X = new Point3D_F64(0.1,-0.05,2);

		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(),K,X);
		Point2D_F64 p2 = PerspectiveOps.renderPixel(se2,K,X);

		DenseMatrix64F E = MultiViewOps.createEssential(se2.R,se2.T);
		DenseMatrix64F F = MultiViewOps.createFundamental(E, K);

		assertEquals(0,MultiViewOps.constraint(F,p1,p2),1e-8);
	}

	@Test
	public void constraint_homography() {
		fail("Implement");
	}

	@Test
	public void extractEpipoles_threeview() {
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
	public void extractFundamental_threeview() {
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

		// remember the first view is assumed to have a projection matrix of [I|0]
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(), null, X);
		Point2D_F64 x2 = PerspectiveOps.renderPixel(se2, K, X);
		Point2D_F64 x3 = PerspectiveOps.renderPixel(se3, K, X);

		assertEquals(0, MultiViewOps.constraint(found2, x1, x2), 1e-8);
		assertEquals(0, MultiViewOps.constraint(found3, x1, x3), 1e-8);
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
		Point2D_F64 x2 = PerspectiveOps.renderPixel(P2, X);
		Point2D_F64 x3 = PerspectiveOps.renderPixel(P3, X);

		// validate correctness by testing a constraint on the points
		DenseMatrix64F A = new DenseMatrix64F(3,3);
		MultiViewOps.constraint(tensor, x1, x2, x3, A);

		for( int i = 0; i < 3; i++ ) {
			for( int j = 0; j < 3; j++ ) {
				assertEquals(0,A.get(i,j),1e-7);
			}
		}
	}

	@Test
	public void createEssential() {
		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(0.05, -0.04, 0.1, null);
		Vector3D_F64 T = new Vector3D_F64(2,1,-3);
		T.normalize();

		DenseMatrix64F E = MultiViewOps.createEssential(R, T);

		// Test using the following theorem:  x2^T*E*x1 = 0
		Point3D_F64 X = new Point3D_F64(0.1,0.1,2);

		Point2D_F64 x0 = PerspectiveOps.renderPixel(new Se3_F64(),null,X);
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(R,T),null,X);

		double val = GeometryMath_F64.innerProd(x1,E,x0);
		assertEquals(0,val,1e-8);
	}

	@Test
	public void computeFundamental() {
		DenseMatrix64F E = MultiViewOps.createEssential(se2.R, se2.T);
		DenseMatrix64F F = MultiViewOps.createFundamental(E, K);

		Point3D_F64 X = new Point3D_F64(0.1,-0.1,2.5);
		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(),K,X);
		Point2D_F64 p2 = PerspectiveOps.renderPixel(se2,K,X);

		assertEquals(0,MultiViewOps.constraint(F,p1,p2),1e-8);
	}

	@Test
	public void createHomography_calibrated() {
		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(0.1,-0.01,0.2, null);
		Vector3D_F64 T = new Vector3D_F64(1,1,0.1);
		T.normalize();
		double d = 2;
		Vector3D_F64 N = new Vector3D_F64(0,0,1);

		DenseMatrix64F H = MultiViewOps.createHomography(R, T, d, N);

		// Test using the following theorem:  x2 = H*x1
		Point3D_F64 P = new Point3D_F64(0.1,0.1,d); // a point on the plane

		Point2D_F64 x0 = new Point2D_F64(P.x/P.z,P.y/P.z);
		SePointOps_F64.transform(new Se3_F64(R,T),P,P);
		Point2D_F64 x1 = new Point2D_F64(P.x/P.z,P.y/P.z);
		Point2D_F64 found = new Point2D_F64();

		GeometryMath_F64.mult(H, x0, found);
		assertEquals(x1.x,found.x,1e-8);
		assertEquals(x1.y,found.y,1e-8);
	}

	@Test
	public void createHomography_uncalibrated() {
		DenseMatrix64F K = new DenseMatrix64F(3,3,true,0.1,0.001,200,0,0.2,250,0,0,1);
		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(0.1,-0.01,0.2, null);
		Vector3D_F64 T = new Vector3D_F64(1,1,0.1);
		T.normalize();
		double d = 2;
		Vector3D_F64 N = new Vector3D_F64(0,0,1);

		DenseMatrix64F H = MultiViewOps.createHomography(R, T, d, N, K);

		// Test using the following theorem:  x2 = H*x1
		Point3D_F64 P = new Point3D_F64(0.1,0.1,d); // a point on the plane

		Point2D_F64 x0 = new Point2D_F64(P.x/P.z,P.y/P.z);
		GeometryMath_F64.mult(K,x0,x0);
		SePointOps_F64.transform(new Se3_F64(R,T),P,P);
		Point2D_F64 x1 = new Point2D_F64(P.x/P.z,P.y/P.z);
		GeometryMath_F64.mult(K,x1,x1);
		Point2D_F64 found = new Point2D_F64();

		GeometryMath_F64.mult(H, x0, found);
		assertEquals(x1.x,found.x,1e-8);
		assertEquals(x1.y,found.y,1e-8);
	}

	@Test
	public void extractEpipoles_stereo() {
		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(1,2,-0.5,null);
		Vector3D_F64 T = new Vector3D_F64(0.5,0.7,-0.3);

		DenseMatrix64F E = MultiViewOps.createEssential(R, T);

		assertTrue(NormOps.normF(E)!=0);

		Point3D_F64 e1 = new Point3D_F64();
		Point3D_F64 e2 = new Point3D_F64();

		MultiViewOps.extractEpipoles(E, e1, e2);

		Point3D_F64 temp = new Point3D_F64();

		GeometryMath_F64.mult(E,e1,temp);
		assertEquals(0,temp.norm(),1e-8);

		GeometryMath_F64.multTran(E,e2,temp);
		assertEquals(0,temp.norm(),1e-8);
	}

	@Test
	public void canonicalCamera() {
		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(200, 250, 0, 100, 110);
		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(1,2,-0.5,null);
		Vector3D_F64 T = new Vector3D_F64(0.5,0.7,-0.3);

		DenseMatrix64F E = MultiViewOps.createEssential(R, T);
		DenseMatrix64F F = MultiViewOps.createFundamental(E, K);

		Point3D_F64 e1 = new Point3D_F64();
		Point3D_F64 e2 = new Point3D_F64();

		CommonOps.scale(-2.0/F.get(0,1),F);
		MultiViewOps.extractEpipoles(F, e1, e2);

		DenseMatrix64F P = MultiViewOps.canonicalCamera(F, e2, new Vector3D_F64(1, 1, 1), 2);

		// recompose the fundamental matrix using the special equation for canonical cameras
		DenseMatrix64F foundF = new DenseMatrix64F(3,3);
		DenseMatrix64F crossEpi = new DenseMatrix64F(3,3);

		GeometryMath_F64.crossMatrix(e2, crossEpi);

		DenseMatrix64F M = new DenseMatrix64F(3,3);
		CommonOps.extract(P,0,3,0,3,M,0,0);
		CommonOps.mult(crossEpi,M,foundF);

		// see if they are equal up to a scale factor
		CommonOps.scale(1.0 / foundF.get(0, 1), foundF);
		CommonOps.scale(1.0 / F.get(0, 1), F);

		assertTrue(MatrixFeatures.isIdentical(F,foundF,1e-8));
	}

	@Test
	public void decomposeCameraMatrix() {
		// compute an arbitrary projection matrix from known values
		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(200, 250, 0, 100, 110);
		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(1,2,-0.5,null);
		Vector3D_F64 T = new Vector3D_F64(0.5,0.7,-0.3);

		DenseMatrix64F P = new DenseMatrix64F(3,4);
		DenseMatrix64F KP = new DenseMatrix64F(3,4);
		CommonOps.insert(R,P,0,0);
		P.set(0,3,T.x);
		P.set(1,3,T.y);
		P.set(2,3,T.z);

		CommonOps.mult(K,P,KP);

		// decompose the projection matrix
		DenseMatrix64F foundK = new DenseMatrix64F(3,3);
		Se3_F64 foundPose = new Se3_F64();
		MultiViewOps.decomposeCameraMatrix(KP, foundK, foundPose);

		// recompute the projection matrix found the found results
		DenseMatrix64F foundKP = new DenseMatrix64F(3,4);
		CommonOps.insert(foundPose.getR(),P,0,0);
		P.set(0,3,foundPose.T.x);
		P.set(1,3,foundPose.T.y);
		P.set(2,3,foundPose.T.z);
		CommonOps.mult(foundK,P,foundKP);

		// see if the two projection matrices are the same
		assertTrue(MatrixFeatures.isEquals(foundKP,KP,1e-8));
	}

	@Test
	public void decomposeEssential() {
		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(1,2,-0.5,null);
		Vector3D_F64 T = new Vector3D_F64(0.5,0.7,-0.3);

		DenseMatrix64F E = MultiViewOps.createEssential(R,T);

		List<Se3_F64> found = MultiViewOps.decomposeEssential(E);

		// the scale factor is lost
		T.normalize();

		int numMatched = 0;

		for( Se3_F64 m : found ) {
			DenseMatrix64F A = new DenseMatrix64F(3,3);

			CommonOps.multTransA(R,m.getR(),A);

			if( !MatrixFeatures.isIdentity(A,1e-8) ) {
				continue;
			}

			Vector3D_F64 foundT = m.getT();
			foundT.normalize();

			if( foundT.isIdentical(T,1e-8) )
				numMatched++;
		}

		assertEquals(1,numMatched);
	}
}
