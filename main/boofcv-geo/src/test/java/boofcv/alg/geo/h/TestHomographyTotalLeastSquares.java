/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.h;

import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.equation.Equation;
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
public class TestHomographyTotalLeastSquares extends CommonHomographyChecks{
	private Random rand = new Random(234);

	@Test
	public void perfect() {
		HomographyTotalLeastSquares alg = new HomographyTotalLeastSquares();

		checkHomography(4, alg );
		checkHomography(10, alg );
		checkHomography(500, alg );
	}

	/**
	 * Create a set of points perfectly on a plane and provide perfect observations of them
	 *
	 * @param N Number of observed points.
	 * @param alg Algorithm being evaluated
	 */
	private void checkHomography(int N, HomographyTotalLeastSquares alg) {
		createScene(N,true);

		// compute essential
		assertTrue(alg.process(pairs,solution));

		// validate by testing essential properties

		// sanity check, F is not zero
		assertTrue(NormOps_DDRM.normF(solution) > 0.001 );

		// see if it follows the epipolar constraint
		for( AssociatedPair p : pairs ) {
			Point2D_F64 a = GeometryMath_F64.mult(solution,p.p1,new Point2D_F64());

			double diff = a.distance(p.p2);
			assertEquals(0,diff,1e-8);
		}
	}

	@Test
	public void checkAgainstKnownH() {
		DMatrixRMaj H = new DMatrixRMaj(new double[][]{{1.5,0,0.5},{0.2,2,0.2},{0.05,0,1.5}});
//		DMatrixRMaj H = new DMatrixRMaj(new double[][]{{1,0,0},{0,1,0},{0,0,1}});
//		DMatrixRMaj H = new DMatrixRMaj(new double[][]{{1,0,0},{0,1,0},{0,0,2.0}});

		int N = 10000;
		List<AssociatedPair> list = new ArrayList<>();

		for (int i = 0; i < N; i++) {
			AssociatedPair p = new AssociatedPair();
			p.p1.x = rand.nextGaussian();
			p.p1.y = rand.nextGaussian();
			GeometryMath_F64.mult(H,p.p1,p.p2);
			list.add(p);
		}

		CommonOps_DDRM.scale(1.0/H.get(2,2),H);
//		H.print();
		DMatrixRMaj found = new DMatrixRMaj(3,3);
		HomographyTotalLeastSquares alg = new HomographyTotalLeastSquares();
		alg.process(list,found);

//		System.out.println("\n\nFound");
//		found.print();
		assertTrue(MatrixFeatures_DDRM.isIdentical(found,H,UtilEjml.TEST_F64));
	}

	@Test
	public void constructA678() {
		int N = 10;
		SimpleMatrix P = SimpleMatrix.random_DDRM(N,2,-1,1,rand);
		SimpleMatrix X = SimpleMatrix.random_DDRM(N,2,-1,1,rand);

		Equation eq = new Equation();
		eq.alias(P.copy(),"P",X.copy(),"X",N,"N");
		eq.process("X=-X");
		eq.process("Xd = diag(X(:,0))");
		eq.process("Yd = diag(X(:,1))");
		eq.process("One=ones(N,1)");
		eq.process("Pp=inv(P'*P)*P'");
		eq.process("XP=(X'*P)/N");
		eq.process("top    = [Xd*P-One*XP(0,:)-P*Pp*Xd*P , X(:,0)-P*Pp*X(:,0)]");
		eq.process("bottom = [Yd*P-One*XP(1,:)-P*Pp*Yd*P , X(:,1)-P*Pp*X(:,1)]");
		eq.process("A=[top;bottom]");
		System.out.println();
		HomographyTotalLeastSquares alg = new HomographyTotalLeastSquares();
		alg.X1.set(P.getDDRM());
		alg.X2.set(X.getDDRM());
		alg.constructA678();
		assertTrue(MatrixFeatures_DDRM.isIdentical(eq.lookupDDRM("A"),alg.A,UtilEjml.TEST_F64));
	}

	@Test
	public void backsubstitution0134() {
		int N = 10;
		SimpleMatrix Pp = SimpleMatrix.random_DDRM(2,N,-1,1,rand);
		SimpleMatrix P = SimpleMatrix.random_DDRM(N,2,-1,1,rand);
		SimpleMatrix X = SimpleMatrix.random_DDRM(N,2,-1,1,rand);

		double H[] = new double[9];
		H[6] = 0.4;
		H[7] = 0.8;
		H[8] = -0.3;

		Equation eq = new Equation();
		eq.alias(P.copy(),"P",Pp.copy(),"Pp",X.copy(),"X",N,"N");

		eq.process("H=[0.4;0.8;-0.3]");
		eq.process("Ax = -Pp*diag(-X(:,0))*[P,ones(N,1)]*H");
		eq.process("Ay = -Pp*diag(-X(:,1))*[P,ones(N,1)]*H");

		HomographyTotalLeastSquares.backsubstitution0134(Pp.getMatrix(),P.getMatrix(),X.getMatrix(),H);

		DMatrixRMaj Ax = eq.lookupDDRM("Ax");
		DMatrixRMaj Ay = eq.lookupDDRM("Ay");

		assertEquals(Ax.data[0],H[0], UtilEjml.TEST_F64);
		assertEquals(Ax.data[1],H[1], UtilEjml.TEST_F64);
		assertEquals(Ay.data[0],H[3], UtilEjml.TEST_F64);
		assertEquals(Ay.data[1],H[4], UtilEjml.TEST_F64);

	}

	@Test
	public void computeEq20() {
		SimpleMatrix A = SimpleMatrix.random_DDRM(10,2,-1,1,rand);
		SimpleMatrix B = SimpleMatrix.random_DDRM(10,2,-1,1,rand);

		SimpleMatrix expected = A.transpose().mult(B).scale(-1.0/10.0);
		double[] found = new double[4];

		HomographyTotalLeastSquares.computeEq20(A.getDDRM(),B.getDDRM(),found);
		assertEquals(expected.get(0,0),found[0],UtilEjml.TEST_F64);
		assertEquals(expected.get(0,1),found[1],UtilEjml.TEST_F64);
		assertEquals(expected.get(1,0),found[2],UtilEjml.TEST_F64);
		assertEquals(expected.get(1,1),found[3],UtilEjml.TEST_F64);
	}

	@Test
	public void computePseudo() {
		SimpleMatrix P = SimpleMatrix.random_DDRM(10,2,-1,1,rand);
		SimpleMatrix found = new SimpleMatrix(1,1);

		HomographyTotalLeastSquares.computePseudo(P.getDDRM(),found.getDDRM());

		SimpleMatrix expected = P.transpose().mult(P).invert().mult(P.transpose());

		assertTrue(expected.isIdentical(found, UtilEjml.TEST_F64));
	}

	@Test
	public void computePPXP() {
		SimpleMatrix P = SimpleMatrix.random_DDRM(10,2,-1,1,rand);
		SimpleMatrix P_plus = SimpleMatrix.random_DDRM(2,10,-1,1,rand);
		SimpleMatrix X = SimpleMatrix.random_DDRM(10,2,-1,1,rand);

		SimpleMatrix found = new SimpleMatrix(1,1);

		for (int i = 0; i < 2; i++) {
			HomographyTotalLeastSquares.computePPXP(P.getDDRM(), P_plus.getDDRM(), X.getDDRM(), i, found.getDDRM());

			SimpleMatrix Xx = X.extractVector(false, i);
			SimpleMatrix expected = P.mult(P_plus).mult(Xx.negative().diag()).mult(P);

			assertTrue(expected.isIdentical(found, UtilEjml.TEST_F64));
		}
	}

	@Test
	public void computePPpX() {
		SimpleMatrix P = SimpleMatrix.random_DDRM(10,2,-1,1,rand);
		SimpleMatrix P_plus = SimpleMatrix.random_DDRM(2,10,-1,1,rand);
		SimpleMatrix X = SimpleMatrix.random_DDRM(10,2,-1,1,rand);

		SimpleMatrix found = new SimpleMatrix(1,1);

		for (int i = 0; i < 2; i++) {
			HomographyTotalLeastSquares.computePPpX(P.getDDRM(),P_plus.getDDRM(),X.getDDRM(),i,found.getDDRM());
			SimpleMatrix Xx = X.extractVector(false,i);
			SimpleMatrix expected = P.mult(P_plus).mult(Xx.negative());
			assertTrue(expected.isIdentical(found, UtilEjml.TEST_F64));
		}
	}
}