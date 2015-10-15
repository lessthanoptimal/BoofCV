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

package boofcv.alg.geo.calibration;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SingularOps;
import org.ejml.ops.SpecializedOps;

import java.util.List;

/**
 * <p>
 * Estimates camera calibration matrix from a set of homographies using linear algebra.  Based upon the
 * description found in [1], but has been modified to improve stability and flexibility.  Two
 * variants are implemented inside this class.  One variant assumes that the skew is zero and requires two or
 * more homographies and the other variant does not assume the skew is zero and requires three or more
 * homographies. The calibration matrix structure is shown below.
 * </p>
 *
 * <p>
 * Calibration matrix is defined as follows:<br>
 * [ &alpha; c u<sub>0</sub> ]<br>
 * [ 0 &beta; v<sub>0</sub> ]<br>
 * [ 0 0 1 ]<br>
 * where 'c' is the camera's skew.
 * </p>
 *
 * <p>
 * The zero skew variant is a modification of what was described in [1].  Instead of simply adding another row
 * to force the skew to be zero that entire part of the equation has been omitted. The algorithm described in
 * [1] was numerically unstable and did not produce meaningful results.
 * </p>
 *
 * <p>
 * [1] Zhengyou Zhang, "Flexible Camera Calibration By Viewing a Plane From Unknown Orientations,",
 * International Conference on Computer Vision (ICCV'99), Corfu, Greece, pages 666-673, September 1999.
 * </p>
 */
public class Zhang99CalibrationMatrixFromHomographies {

	// system of equations
	private DenseMatrix64F A = new DenseMatrix64F(1,1);
	// computes the SVD of the A matrix
	private SingularValueDecomposition<DenseMatrix64F> svd = DecompositionFactory.svd(0, 0,true,true,false);

	// a vectorized description of the B = A^-T * A^-1 matrix.
	private DenseMatrix64F b;
	// the found calibration matrix
	private DenseMatrix64F K = new DenseMatrix64F(3,3);

	// if it should assume the skew is zero or not
	private boolean assumeZeroSkew;


	/**
	 * Configures calibration estimation.
	 *
	 * @param assumeZeroSkew  Assume that skew matrix is zero or not
	 */
	public Zhang99CalibrationMatrixFromHomographies(boolean assumeZeroSkew) {
		this.assumeZeroSkew = assumeZeroSkew;

		if( assumeZeroSkew )
			b = new DenseMatrix64F(5,1);
		else
			b = new DenseMatrix64F(6,1);

	}

	/**
	 * Given a set of homographies computed from a sequence of images that observe the same
	 * plane it estimates the camera's calibration.
	 *
	 * @param homographies Homographies computed from observations of the calibration grid.
	 */
	public void process( List<DenseMatrix64F> homographies ) {
		if( assumeZeroSkew ) {
			if( homographies.size() < 2 )
				throw new IllegalArgumentException("At least two homographies are required");
		} else if( homographies.size() < 3 ) {
			throw new IllegalArgumentException("At least three homographies are required");
		}

		if( assumeZeroSkew ) {
			setupA_NoSkew(homographies);
			if( !svd.decompose(A) )
				throw new RuntimeException("SVD failed");
			if( homographies.size() == 2 ) {
				DenseMatrix64F V = svd.getV(null,false);
				SpecializedOps.subvector(V, 0, 4, V.numRows, false, 0, b);
			} else {
				SingularOps.nullVector(svd,true,b);
			}
			computeParam_ZeroSkew();
		} else {
			setupA(homographies);
			if( !svd.decompose(A) )
				throw new RuntimeException("SVD failed");
			SingularOps.nullVector(svd,true,b);
			computeParam();
		}
	}

	/**
	 * Sets up the system of equations which are to be solved.  This equation is derived from
	 * constraints (3) and (4) in the paper.   See section 3.1.
	 *
	 * @param homographies set of observed homographies.
	 */
	private void setupA( List<DenseMatrix64F> homographies ) {
		A.reshape(2*homographies.size(),6, false);

		DenseMatrix64F h1 = new DenseMatrix64F(3,1);
		DenseMatrix64F h2 = new DenseMatrix64F(3,1);

		DenseMatrix64F v12 = new DenseMatrix64F(1,6);
		DenseMatrix64F v11 = new DenseMatrix64F(1,6);
		DenseMatrix64F v22 = new DenseMatrix64F(1,6);

		DenseMatrix64F v11m22 = new DenseMatrix64F(1,6);

		for( int i = 0; i < homographies.size(); i++ ) {
			DenseMatrix64F H = homographies.get(i);

			CommonOps.extract(H,0,3,0,1,h1,0,0);
			CommonOps.extract(H,0,3,1,2,h2,0,0);

			// normalize H by the max value to reduce numerical error when computing A
			// several numbers are multiplied against each other and could become quite large/small
			double max1 = CommonOps.elementMaxAbs(h1);
			double max2 = CommonOps.elementMaxAbs(h2);
			double max = Math.max(max1,max2);

			CommonOps.divide(h1,max);
			CommonOps.divide(h2,max);

			// compute elements of A
			computeV(h1, h2, v12);
			computeV(h1, h1, v11);
			computeV(h2, h2, v22);

			CommonOps.subtract(v11, v22, v11m22);

			CommonOps.insert( v12    , A, i*2   , 0);
			CommonOps.insert( v11m22 , A, i*2+1 , 0);
		}
	}

	/**
	 * Similar to {@link #setupA(java.util.List)} but all references to B12 have been removed
	 * since it will always be zero when the skew is zero
	 *
	 * @param homographies set of observed homographies.
	 */
	private void setupA_NoSkew( List<DenseMatrix64F> homographies ) {
		A.reshape(2*homographies.size(),5, false);

		DenseMatrix64F h1 = new DenseMatrix64F(3,1);
		DenseMatrix64F h2 = new DenseMatrix64F(3,1);

		DenseMatrix64F v12 = new DenseMatrix64F(1,5);
		DenseMatrix64F v11 = new DenseMatrix64F(1,5);
		DenseMatrix64F v22 = new DenseMatrix64F(1,5);

		DenseMatrix64F v11m22 = new DenseMatrix64F(1,5);

		for( int i = 0; i < homographies.size(); i++ ) {
			DenseMatrix64F H = homographies.get(i);

			CommonOps.extract(H,0,3,0,1,h1,0,0);
			CommonOps.extract(H,0,3,1,2,h2,0,0);

			// normalize H by the max value to reduce numerical error when computing A
			// several numbers are multiplied against each other and could become quite large/small
			double max1 = CommonOps.elementMaxAbs(h1);
			double max2 = CommonOps.elementMaxAbs(h2);
			double max = Math.max(max1,max2);

			CommonOps.divide(h1,max);
			CommonOps.divide(h2,max);

			// compute elements of A
			computeV_NoSkew(h1, h2, v12);
			computeV_NoSkew(h1, h1, v11);
			computeV_NoSkew(h2, h2, v22);

			CommonOps.subtract(v11, v22, v11m22);

			CommonOps.insert( v12    , A, i*2   , 0);
			CommonOps.insert( v11m22 , A, i*2+1 , 0);
		}
	}

	/**
	 * This computes the v_ij vector found in the paper.
	 */
	private void computeV( DenseMatrix64F h1 ,DenseMatrix64F h2 , DenseMatrix64F v )
	{
		double h1x = h1.get(0,0);
		double h1y = h1.get(1,0);
		double h1z = h1.get(2,0);

		double h2x = h2.get(0,0);
		double h2y = h2.get(1,0);
		double h2z = h2.get(2,0);

		v.set(0,0,h1x*h2x);
		v.set(0,1,h1x*h2y+h1y*h2x);
		v.set(0,2,h1y*h2y);
		v.set(0,3,h1z*h2x+h1x*h2z);
		v.set(0,4,h1z*h2y+h1y*h2z);
		v.set(0,5,h1z*h2z);
	}

	/**
	 * This computes the v_ij vector found in the paper.  Leaving out components that would
	 * interact with B12, since that is known to be zero.
	 */
	private void computeV_NoSkew( DenseMatrix64F h1 ,DenseMatrix64F h2 , DenseMatrix64F v )
	{
		double h1x = h1.get(0,0);
		double h1y = h1.get(1,0);
		double h1z = h1.get(2,0);

		double h2x = h2.get(0,0);
		double h2y = h2.get(1,0);
		double h2z = h2.get(2,0);

		v.set(0,0,h1x*h2x);
		v.set(0,1,h1y*h2y);
		v.set(0,2,h1z*h2x+h1x*h2z);
		v.set(0,3,h1z*h2y+h1y*h2z);
		v.set(0,4,h1z*h2z);
	}

	/**
	 * Compute the calibration parameters from the b matrix.
	 */
	private void computeParam() {
		// reduce overflow/underflow
		CommonOps.divide(b,CommonOps.elementMaxAbs(b));

		double B11 = b.get(0,0);
		double B12 = b.get(1,0);
		double B22 = b.get(2,0);
		double B13 = b.get(3,0);
		double B23 = b.get(4,0);
		double B33 = b.get(5,0);

		double temp0 = B12*B13 - B11*B23;
		double temp1 = B11*B22 - B12*B12;

		double v0 = temp0/temp1;
		double lambda = B33-(B13*B13 + v0*temp0)/B11;
		// Using abs() inside is an adhoc modification to make it more stable
		// If there is any good theoretical reason for it, that's a pure accident.  Seems
		// to work well in practice
		double a = Math.sqrt(Math.abs(lambda / B11));
		double b = Math.sqrt(Math.abs(lambda * B11 / temp1));
		double c = -B12*b/B11;
		double u0 = c*v0/a - B13/B11;

		K.set(0,0,a);
		K.set(0,1,c);
		K.set(0,2,u0);
		K.set(1,1,b);
		K.set(1,2,v0);
		K.set(2,2,1);
	}

	/**
	 * Compute the calibration parameters from the b matrix when the skew is assumed to be zero
	 */
	private void computeParam_ZeroSkew() {
		// reduce overflow/underflow
		CommonOps.divide(b,CommonOps.elementMaxAbs(b));

		double B11 = b.get(0,0);
		double B22 = b.get(1,0);
		double B13 = b.get(2,0);
		double B23 = b.get(3,0);
		double B33 = b.get(4,0);

		double temp0 = -B11*B23;
		double temp1 = B11*B22;

		double v0 = temp0/temp1;
		double lambda = B33-(B13*B13 + v0*temp0)/B11;
		// Using abs() inside is an adhoc modification to make it more stable
		// If there is any good theoretical reason for it, that's a pure accident.  Seems
		// to work well in practice
		double a = Math.sqrt(Math.abs(lambda / B11));
		double b = Math.sqrt(Math.abs(lambda*B11/temp1));
		double u0 = - B13/B11;

		K.set(0,0,a);
		K.set(0,1,0);
		K.set(0,2,u0);
		K.set(1,1,b);
		K.set(1,2,v0);
		K.set(2,2,1);
	}


	/**
	 * Returns the computed calibration matrix.
	 *
	 * @return Calibration matrix.
	 */
	public DenseMatrix64F getCalibrationMatrix() {
		return K;
	}

	public SingularValueDecomposition<DenseMatrix64F> getSvd() {
		return svd;
	}
}
