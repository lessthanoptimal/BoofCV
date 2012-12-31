/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.f;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.RandomMatrices;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestHelperNister5 {

	Random rand = new Random(234);

	double X[] = new double[9];
	double Y[] = new double[9];
	double Z[] = new double[9];
	double W[] = new double[9];

	public TestHelperNister5() {
		for( int i = 0; i < 9; i++ ) {
			X[i] = rand.nextGaussian();
			Y[i] = rand.nextGaussian();
			Z[i] = rand.nextGaussian();
			W[i] = rand.nextGaussian();
		}
	}

	/**
	 * Validates A and B by computing solutions to constraint equations for a specific instance of x,y,z
	 */
	@Test
	public void setupA1_setupA2() {
		double x = 0.5, y = 2, z = -0.2, w = 1;

		SimpleMatrix E = SimpleMatrix.wrap(constructE(x,y,z,w));

		DenseMatrix64F A = new DenseMatrix64F(10,10);
		DenseMatrix64F B = new DenseMatrix64F(10,10);

		HelperNister5 alg = new HelperNister5();
		alg.setNullSpace(X,Y,Z,W);
		alg.setupA1(A);
		alg.setupA2(B);

		DenseMatrix64F Y1 = new DenseMatrix64F(10,1);
		DenseMatrix64F Y2 = new DenseMatrix64F(10,1);

		CommonOps.mult(A,createCoefsA(x,y,z),Y1);
		CommonOps.mult(B,createCoefsB(x, y, z),Y2);

		DenseMatrix64F Y = new DenseMatrix64F(10,1);

		CommonOps.add(Y1,Y2,Y);

		// compute the constraints equations
		SimpleMatrix EEt = E.mult(E.transpose());
		SimpleMatrix EEtE = EEt.mult(E);
		SimpleMatrix aE = E.scale(-0.5*EEt.trace());
		DenseMatrix64F eq2 = EEtE.plus(aE).getMatrix();

		// check the solution
		assertEquals(E.determinant(),Y.data[0],1e-8);
		assertEquals(eq2.data[0],Y.data[1],1e-8);
		assertEquals(eq2.data[1],Y.data[2],1e-8);
		assertEquals(eq2.data[2],Y.data[3],1e-8);
		assertEquals(eq2.data[3],Y.data[4],1e-8);
		assertEquals(eq2.data[4],Y.data[5],1e-8);
		assertEquals(eq2.data[5],Y.data[6],1e-8);
		assertEquals(eq2.data[6],Y.data[7],1e-8);
		assertEquals(eq2.data[7],Y.data[8],1e-8);
		assertEquals(eq2.data[8],Y.data[9],1e-8);
	}

	@Test
	public void setDeterminantVectors() {
		DenseMatrix64F A = RandomMatrices.createRandom(10,10,-1,1,rand);

		HelperNister5 alg = new HelperNister5();
		alg.setDeterminantVectors(A);

		assertEquals(          -A.get(5,0),alg.K00,1e-8);
		assertEquals(A.get(4,0)-A.get(5,1),alg.K01,1e-8);
		assertEquals(A.get(4,1)-A.get(5,2),alg.K02,1e-8);
		assertEquals(A.get(4,2)           ,alg.K03,1e-8);
		assertEquals(          -A.get(5,3),alg.K04,1e-8);
		assertEquals(A.get(4,3)-A.get(5,4),alg.K05,1e-8);
		assertEquals(A.get(4,4)-A.get(5,5),alg.K06,1e-8);
		assertEquals(A.get(4,5)           ,alg.K07,1e-8);
		assertEquals(          -A.get(5,6),alg.K08,1e-8);
		assertEquals(A.get(4,6)-A.get(5,7),alg.K09,1e-8);
		assertEquals(A.get(4,7)-A.get(5,8),alg.K10,1e-8);
		assertEquals(A.get(4,8)-A.get(5,9),alg.K11,1e-8);
		assertEquals(A.get(4,9)           ,alg.K12,1e-8);

		assertEquals(          -A.get(7,0),alg.L00,1e-8);
		assertEquals(A.get(6,0)-A.get(7,1),alg.L01,1e-8);
		assertEquals(A.get(6,1)-A.get(7,2),alg.L02,1e-8);
		assertEquals(A.get(6,2)           ,alg.L03,1e-8);
		assertEquals(          -A.get(7,3),alg.L04,1e-8);
		assertEquals(A.get(6,3)-A.get(7,4),alg.L05,1e-8);
		assertEquals(A.get(6,4)-A.get(7,5),alg.L06,1e-8);
		assertEquals(A.get(6,5)           ,alg.L07,1e-8);
		assertEquals(          -A.get(7,6),alg.L08,1e-8);
		assertEquals(A.get(6,6)-A.get(7,7),alg.L09,1e-8);
		assertEquals(A.get(6,7)-A.get(7,8),alg.L10,1e-8);
		assertEquals(A.get(6,8)-A.get(7,9),alg.L11,1e-8);
		assertEquals(A.get(6,9)           ,alg.L12,1e-8);

		assertEquals(          -A.get(9,0),alg.M00,1e-8);
		assertEquals(A.get(8,0)-A.get(9,1),alg.M01,1e-8);
		assertEquals(A.get(8,1)-A.get(9,2),alg.M02,1e-8);
		assertEquals(A.get(8,2)           ,alg.M03,1e-8);
		assertEquals(          -A.get(9,3),alg.M04,1e-8);
		assertEquals(A.get(8,3)-A.get(9,4),alg.M05,1e-8);
		assertEquals(A.get(8,4)-A.get(9,5),alg.M06,1e-8);
		assertEquals(A.get(8,5)           ,alg.M07,1e-8);
		assertEquals(          -A.get(9,6),alg.M08,1e-8);
		assertEquals(A.get(8,6)-A.get(9,7),alg.M09,1e-8);
		assertEquals(A.get(8,7)-A.get(9,8),alg.M10,1e-8);
		assertEquals(A.get(8,8)-A.get(9,9),alg.M11,1e-8);
		assertEquals(A.get(8,9)           ,alg.M12,1e-8);
	}

	@Test
	public void extractPolynomial() {
		DenseMatrix64F A = RandomMatrices.createRandom(10,10,-1,1,rand);

		HelperNister5 alg = new HelperNister5();
		alg.setDeterminantVectors(A);

		double z = 2.3;

		DenseMatrix64F B = new DenseMatrix64F(3,3);

		B.data[0] = alg.K00*z*z*z + alg.K01*z*z + alg.K02*z + alg.K03;
		B.data[1] = alg.K04*z*z*z + alg.K05*z*z + alg.K06*z + alg.K07;
		B.data[2] = alg.K08*z*z*z*z + alg.K09*z*z*z + alg.K10*z*z + alg.K11*z + alg.K12;

		B.data[3] = alg.L00*z*z*z + alg.L01*z*z + alg.L02*z + alg.L03;
		B.data[4] = alg.L04*z*z*z + alg.L05*z*z + alg.L06*z + alg.L07;
		B.data[5] = alg.L08*z*z*z*z + alg.L09*z*z*z + alg.L10*z*z + alg.L11*z + alg.L12;

		B.data[6] = alg.M00*z*z*z + alg.M01*z*z + alg.M02*z + alg.M03;
		B.data[7] = alg.M04*z*z*z + alg.M05*z*z + alg.M06*z + alg.M07;
		B.data[8] = alg.M08*z*z*z*z + alg.M09*z*z*z + alg.M10*z*z + alg.M11*z + alg.M12;

		double expected = CommonOps.det(B);

		double coefs[] = new double[11];

		alg.extractPolynomial(coefs);

		double found = 0;
		for( int i = 0; i < coefs.length; i++ ) {
			found += coefs[i]*Math.pow(z,i);
		}

		assertEquals(expected,found,1e-8);
	}


	public DenseMatrix64F constructE( double x , double y , double z , double w ) {
		DenseMatrix64F E = new DenseMatrix64F(3,3);

		for( int i = 0; i < 9; i++)  {
			E.data[i] = x*X[i] + y*Y[i] + z*Z[i] + w*W[i];
		}

		return E;
	}

	public DenseMatrix64F createCoefsA( double x , double y , double z ) {

		DenseMatrix64F X = new DenseMatrix64F(10,1);

		X.data[0] = x*x*x;
		X.data[1] = y*y*y;
		X.data[2] = x*x*y;
		X.data[3] = x*y*y;
		X.data[4] = x*x*z;
		X.data[5] = x*x;
		X.data[6] = y*y*z;
		X.data[7] = y*y;
		X.data[8] = x*y*z;
		X.data[9] = x*y;

		return X;
	}

	public DenseMatrix64F createCoefsB( double x , double y , double z ) {

		DenseMatrix64F X = new DenseMatrix64F(10,1);

//		'x*z^2','x*z','x','y*z^2','y*z','y','z^3','z^2','z',''

		X.data[0] = x*z*z;
		X.data[1] = x*z;
		X.data[2] = x;
		X.data[3] = y*z*z;
		X.data[4] = y*z;
		X.data[5] = y;
		X.data[6] = z*z*z;
		X.data[7] = z*z;
		X.data[8] = z;
		X.data[9] = 1;

		return X;
	}

	public double sumRow( int row , DenseMatrix64F A , DenseMatrix64F B ) {
		double total = 0;

		for( int i = 0; i < 10; i++ ) {
			total += A.get(row,i);
			total += B.get(row,i);
		}

		return total;
	}
}
