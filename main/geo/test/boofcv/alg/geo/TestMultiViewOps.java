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

package boofcv.alg.geo;

import boofcv.alg.geo.h.CommonHomographyInducedPlane;
import boofcv.struct.geo.PairLineNorm;
import boofcv.struct.geo.TrifocalTensor;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.Tuple2;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixFeatures;
import org.ejml.ops.NormOps;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestMultiViewOps {

	Random rand = new Random(234);

	// camera calibration matrix
	DenseMatrix64F K = new DenseMatrix64F(3,3,true,60,0.01,200,0,80,150,0,0,1);

	// camera locations
	Se3_F64 worldToCam2, worldToCam3;

	// camera matrix for views 2 and 3
	DenseMatrix64F P2,P3;

	// Fundamental matrix for views 2 and 3
	DenseMatrix64F F2,F3;

	// trifocal tensor for these views
	TrifocalTensor tensor;

	// storage for lines in 3 views
	Vector3D_F64 line1 = new Vector3D_F64();
	Vector3D_F64 line2 = new Vector3D_F64();
	Vector3D_F64 line3 = new Vector3D_F64();

	public TestMultiViewOps() {
		worldToCam2 = new Se3_F64();
		worldToCam3 = new Se3_F64();

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.2, 0.001, -0.02, worldToCam2.R);
		worldToCam2.getT().set(0.3, 0, 0.05);

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.8, -0.02, 0.003, worldToCam3.R);
		worldToCam3.getT().set(0.6, 0.2, -0.02);

		P2 = PerspectiveOps.createCameraMatrix(worldToCam2.R, worldToCam2.T, K, null);
		P3 = PerspectiveOps.createCameraMatrix(worldToCam3.R, worldToCam3.T, K, null);
		tensor = MultiViewOps.createTrifocal(P2, P3, null);
		tensor.normalizeScale();

		F2 = MultiViewOps.createEssential(worldToCam2.getR(), worldToCam2.getT());
		F2 = MultiViewOps.createFundamental(F2, K);
		F3 = MultiViewOps.createEssential(worldToCam3.getR(), worldToCam3.getT());
		F3 = MultiViewOps.createFundamental(F3, K);
	}

	/**
	 * Check the trifocal tensor using its definition
	 */
	@Test
	public void createTrifocal_CameraMatrix() {

		SimpleMatrix P1 = SimpleMatrix.wrap(
				PerspectiveOps.createCameraMatrix(worldToCam2.getR(), worldToCam2.getT(), null, null));
		SimpleMatrix P2 = SimpleMatrix.wrap(
				PerspectiveOps.createCameraMatrix(worldToCam3.getR(), worldToCam3.getT(), null, null));

		TrifocalTensor found = MultiViewOps.createTrifocal(worldToCam2,worldToCam3,null);

		for( int i = 0; i < 3; i++ ) {
			SimpleMatrix ai = P1.extractVector(false,i);
			SimpleMatrix b4 = P2.extractVector(false,3);
			SimpleMatrix a4 = P1.extractVector(false,3);
			SimpleMatrix bi = P2.extractVector(false,i);

			SimpleMatrix expected = ai.mult(b4.transpose()).minus(a4.mult(bi.transpose()));

			assertTrue(MatrixFeatures.isIdentical(expected.getMatrix(),found.getT(i),1e-8));
		}
	}

	/**
	 * Check the trifocal tensor using its definition
	 */
	@Test
	public void createTrifocal_SE() {

		TrifocalTensor found = MultiViewOps.createTrifocal(worldToCam2,worldToCam3,null);

		SimpleMatrix R2 = SimpleMatrix.wrap(worldToCam2.getR());
		SimpleMatrix R3 = SimpleMatrix.wrap(worldToCam3.getR());
		SimpleMatrix b4 = new SimpleMatrix(3,1);
		SimpleMatrix a4 = new SimpleMatrix(3,1);

		b4.set(0,worldToCam3.getX());
		b4.set(1,worldToCam3.getY());
		b4.set(2,worldToCam3.getZ());

		a4.set(0,worldToCam2.getX());
		a4.set(1,worldToCam2.getY());
		a4.set(2,worldToCam2.getZ());

		for( int i = 0; i < 3; i++ ) {
			SimpleMatrix ai = R2.extractVector(false, i);
			SimpleMatrix bi = R3.extractVector(false,i);

			SimpleMatrix expected = ai.mult(b4.transpose()).minus(a4.mult(bi.transpose()));

			assertTrue(MatrixFeatures.isIdentical(expected.getMatrix(),found.getT(i),1e-8));
		}
	}

	@Test
	public void constraint_Trifocal_lll() {
		Point3D_F64 X = new Point3D_F64(0.1,-0.05,2);

		computeLines(X, line1, line2, line3);

		Vector3D_F64 found = MultiViewOps.constraint(tensor,line1,line2,line3,null);

		assertEquals(0,found.x,1e-12);
		assertEquals(0,found.y,1e-12);
		assertEquals(0,found.z,1e-12);
	}

	@Test
	public void constraint_Trifocal_pll() {
		Point3D_F64 X = new Point3D_F64(0.1,-0.05,2);

		computeLines(X,line1,line2,line3);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(),null,X);

		double found = MultiViewOps.constraint(tensor,x1,line2,line3);

		assertEquals(0,found,1e-12);
	}

	@Test
	public void constraint_Trifocal_plp() {
		Point3D_F64 X = new Point3D_F64(0.1,-0.05,2);

		computeLines(X,line1,line2,line3);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(),null,X);
		Point2D_F64 x3 = PerspectiveOps.renderPixel(worldToCam3,K,X);

		Vector3D_F64 found = MultiViewOps.constraint(tensor,x1,line2,x3,null);

		assertEquals(0,found.x,1e-12);
		assertEquals(0,found.y,1e-12);
		assertEquals(0, found.z, 1e-12);
	}

	@Test
	public void constraint_Trifocal_ppl() {
		Point3D_F64 X = new Point3D_F64(0.1,-0.05,2);

		computeLines(X,line1,line2,line3);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(),null,X);
		Point2D_F64 x2 = PerspectiveOps.renderPixel(worldToCam2,K,X);

		Vector3D_F64 found = MultiViewOps.constraint(tensor,x1,x2,line3,null);

		assertEquals(0,found.x,1e-12);
		assertEquals(0,found.y,1e-12);
		assertEquals(0, found.z, 1e-12);
	}

	@Test
	public void constraint_Trifocal_ppp() {
		// Point in 3D space being observed
		Point3D_F64 X = new Point3D_F64(0.1,0.5,3);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(),null,X);
		Point2D_F64 p2 = PerspectiveOps.renderPixel(worldToCam2,K,X);
		Point2D_F64 p3 = PerspectiveOps.renderPixel(worldToCam3,K,X);

		// check the constraint
		DenseMatrix64F A = MultiViewOps.constraint(tensor, p1, p2, p3, null);

		for( int i = 0; i < 3; i++ ) {
			for( int j = 0; j < 3; j++ ) {
				assertEquals(0,A.get(i,j),1e-11);
			}
		}
	}

	@Test
	public void constraint_epipolar() {
		Point3D_F64 X = new Point3D_F64(0.1,-0.05,2);

		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(),K,X);
		Point2D_F64 p2 = PerspectiveOps.renderPixel(worldToCam2,K,X);

		DenseMatrix64F E = MultiViewOps.createEssential(worldToCam2.R, worldToCam2.T);
		DenseMatrix64F F = MultiViewOps.createFundamental(E, K);

		assertEquals(0,MultiViewOps.constraint(F,p1,p2),1e-8);
	}

	@Test
	public void constraint_homography() {

		double d = 2;
		Vector3D_F64 N = new Vector3D_F64(0,0,1);
		Point3D_F64 X = new Point3D_F64(0.1,-0.4,d);


		DenseMatrix64F H = MultiViewOps.createHomography(worldToCam2.getR(),worldToCam2.getT(),d,N);

		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(),null,X);
		Point2D_F64 p2 = PerspectiveOps.renderPixel(worldToCam2,null,X);

		Point2D_F64 found = MultiViewOps.constraintHomography(H,p1,null);

		assertEquals(p2.x,found.x,1e-8);
		assertEquals(p2.y,found.y,1e-8);
	}

	@Test
	public void inducedHomography13() {
		Point3D_F64 X = new Point3D_F64(0.1,-0.05,2);

		computeLines(X,line1,line2,line3);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(),null,X);
		Point2D_F64 p3 = PerspectiveOps.renderPixel(worldToCam3,K,X);

		DenseMatrix64F H13 = MultiViewOps.inducedHomography13(tensor,line2,null);

		Point2D_F64 found = new Point2D_F64();

		GeometryMath_F64.mult(H13,p1,found);

		assertEquals(p3.x,found.x,1e-8);
		assertEquals(p3.y,found.y,1e-8);
	}

	@Test
	public void inducedHomography12() {
		Point3D_F64 X = new Point3D_F64(0.1,-0.05,2);

		computeLines(X,line1,line2,line3);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(),null,X);
		Point2D_F64 p2 = PerspectiveOps.renderPixel(worldToCam2,K,X);

		DenseMatrix64F H12 = MultiViewOps.inducedHomography12(tensor, line3, null);

		Point2D_F64 found = new Point2D_F64();

		GeometryMath_F64.mult(H12,p1,found);

		assertEquals(p2.x,found.x,1e-8);
		assertEquals(p2.y,found.y,1e-8);
	}

	@Test
	public void homographyStereo3Pts() {
		CommonHomographyInducedPlane common = new CommonHomographyInducedPlane();

		DenseMatrix64F H = MultiViewOps.homographyStereo3Pts(common.F, common.p1, common.p2, common.p3);

		common.checkHomography(H);
	}

	@Test
	public void homographyStereoLinePt() {
		CommonHomographyInducedPlane common = new CommonHomographyInducedPlane();

		PairLineNorm l1 = CommonHomographyInducedPlane.convert(common.p1,common.p2);

		DenseMatrix64F H = MultiViewOps.homographyStereoLinePt(common.F, l1, common.p3);

		common.checkHomography(H);
	}

	@Test
	public void homographyStereo2Lines() {
		CommonHomographyInducedPlane common = new CommonHomographyInducedPlane();

		PairLineNorm l1 = CommonHomographyInducedPlane.convert(common.p1,common.p2);
		PairLineNorm l2 = CommonHomographyInducedPlane.convert(common.p1,common.p3);

		DenseMatrix64F H = MultiViewOps.homographyStereo2Lines(common.F,l1,l2);

		common.checkHomography(H);
	}

	/**
	 * Compute lines in each view using epipolar geometry that include point X. The first view is
	 * in normalized image coordinates
	 */
	private void computeLines( Point3D_F64 X , Vector3D_F64 line1 , Vector3D_F64 line2, Vector3D_F64 line3 ) {
		Point3D_F64 X2 = X.copy();
		X2.y += 1;

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		line1.set(computeLine(X,X2,new Se3_F64(),null));
		line2.set(computeLine(X,X2,worldToCam2,K));
		line3.set(computeLine(X,X2,worldToCam3,K));
	}

	private Vector3D_F64 computeLine( Point3D_F64 X1, Point3D_F64 X2 , Se3_F64 worldToCam , DenseMatrix64F K ) {
		Point2D_F64 a = PerspectiveOps.renderPixel(worldToCam,K,X1);
		Point2D_F64 b = PerspectiveOps.renderPixel(worldToCam,K,X2);

		Vector3D_F64 v1 = new Vector3D_F64(b.x-a.x,b.y-a.y,0);
		Vector3D_F64 v2 = new Vector3D_F64(a.x,a.y,1);
		Vector3D_F64 norm = new Vector3D_F64();

		GeometryMath_F64.cross(v1,v2,norm);

		norm.normalize();

		double sanity1 = a.x*norm.x + a.y*norm.y + norm.z;
		double sanity2 = b.x*norm.x + b.y*norm.y + norm.z;


		return norm;
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
		Point2D_F64 x2 = PerspectiveOps.renderPixel(worldToCam2, K, X);
		Point2D_F64 x3 = PerspectiveOps.renderPixel(worldToCam3, K, X);

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
		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.05, -0.04, 0.1, null);
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
		DenseMatrix64F E = MultiViewOps.createEssential(worldToCam2.R, worldToCam2.T);
		DenseMatrix64F F = MultiViewOps.createFundamental(E, K);

		Point3D_F64 X = new Point3D_F64(0.1,-0.1,2.5);
		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(),K,X);
		Point2D_F64 p2 = PerspectiveOps.renderPixel(worldToCam2,K,X);

		assertEquals(0,MultiViewOps.constraint(F,p1,p2),1e-8);
	}

	@Test
	public void computeFundamental2() {
		DenseMatrix64F K2 = new DenseMatrix64F(3,3,true,80,0.02,190,0,30,170,0,0,1);

		DenseMatrix64F E = MultiViewOps.createEssential(worldToCam2.R, worldToCam2.T);
		DenseMatrix64F F = MultiViewOps.createFundamental(E, K,K2);

		Point3D_F64 X = new Point3D_F64(0.1,-0.1,2.5);
		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(),K,X);
		Point2D_F64 p2 = PerspectiveOps.renderPixel(worldToCam2,K2,X);

		assertEquals(0,MultiViewOps.constraint(F,p1,p2),1e-8);
	}

	@Test
	public void createHomography_calibrated() {
		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.1,-0.01,0.2, null);
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
		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.1,-0.01,0.2, null);
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
		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,1,2,-0.5,null);
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
		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,1,2,-0.5,null);
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
		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,1,2,-0.5,null);
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
		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,1,2,-0.5,null);
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

	@Test
	public void decomposeHomography() {
		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.2, -0.06, -0.05, null);
		Vector3D_F64 T = new Vector3D_F64(2,1,-3);

		double d = 2.5;
		Vector3D_F64 N = new Vector3D_F64(0.68,0.2,-0.06);
		N.normalize();

		DenseMatrix64F H = MultiViewOps.createHomography(R, T, d, N);

		List<Tuple2<Se3_F64,Vector3D_F64>> found = MultiViewOps.decomposeHomography(H);

		assertEquals(4, found.size());

		List<Se3_F64> solutionsSE = new ArrayList<>();
		List<Vector3D_F64> solutionsN = new ArrayList<>();

		for( Tuple2<Se3_F64,Vector3D_F64> t : found ) {
			solutionsSE.add( t.data0 );
			solutionsN.add( t.data1 );
		}

		TestDecomposeHomography.checkHasOriginal(solutionsSE, solutionsN, R, T, d, N);
	}
}
