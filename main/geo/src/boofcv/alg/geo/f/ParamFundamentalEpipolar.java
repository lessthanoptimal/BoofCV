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

import boofcv.alg.geo.MultiViewOps;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.fitting.modelset.ModelCodec;
import org.ejml.UtilEjml;
import org.ejml.data.DenseMatrix64F;
import org.ejml.simple.SimpleMatrix;

/**
 * <p>
 * Parameterizes F by specifying the first two columns and the third being a linear combination of
 * the first two.  By setting one of the elements in f1 or f2 to be one, it can achieve the minimum
 * possible parameter size of 7.  Care is taken to avoid the degenerate case when f1 and f2
 * are linearly dependent.
 * </p>
 * <p>
 * F=[f1 , f2 , &alpha;f1 + &beta; f2]
 * </p>
 * 
 * <p>
 * Page 286 in: R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </p>
 * 
 * @author Peter Abeles
 */
public class ParamFundamentalEpipolar implements ModelCodec<DenseMatrix64F> {

	// order of columns
	int col0,col1,col2;
	// map from index in parameter to index in F
	// last index is the one index
	int indexes[] = new int[6];
	
	@Override
	public int getParamLength() {
		return 7;
	}

	/**
	 * Examines the matrix structure to determine how to parameterize F.  
	 */
	@Override
	public void encode(DenseMatrix64F F, double[] param) {
		// see if which columns are to be used
		selectColumns(F);
		
		// set the largest element in the first two columns and normalize
		// using that value
		double v[] = new double[]{F.get(0,col0),F.get(1,col0),F.get(2,col0),
				F.get(0,col1),F.get(1,col1),F.get(2,col1)};
		double divisor = selectDivisor(v,param);
		
		// solve for alpha and beta and put into param
		SimpleMatrix A = new SimpleMatrix(3,2);
		SimpleMatrix y = new SimpleMatrix(3,1);
		for( int i = 0; i < 3; i++ ) {
			A.set(i,0,v[i]);
			A.set(i,1,v[i+3]);
			y.set(i,0,F.get(i,col2)/divisor);
		}

		SimpleMatrix x = A.solve(y);
		
		param[5] = x.get(0);
		param[6] = x.get(1);
	}

	/**
	 * The divisor is the element in the first two columns that has the largest absolute value.
	 * Finds this element, sets col0 to be the row which contains it, and specifies which elements
	 * in that column are to be used.
	 * 
	 * @return Value of the divisor
	 */
	private double selectDivisor( double v[] , double param[] ) {

		double maxValue = 0;
		int maxIndex = 0;
		for( int i = 0; i < v.length; i++ ) {
			if( Math.abs(v[i]) > maxValue ) {
				maxValue = Math.abs(v[i]);
				maxIndex = i;
			}
		}

		double divisor = v[maxIndex];

		int index = 0;
		for( int i = 0; i < v.length; i++ ) {
			v[i] /= divisor;

			if( i != maxIndex ) {
				// save first 5 parameters
				param[index] = v[i];
				// save indexes in the matrix
				int col = i < 3 ? col0 : col1;
				indexes[index++] = 3*(i%3)+ col;
			}
		}

		// index of 1
		int col = maxIndex >= 3 ? col1 : col0;
		indexes[5] = 3*(maxIndex % 3) + col;

		return divisor;
	}

	private void selectColumns(DenseMatrix64F F) {
		Point3D_F64 e1 = new Point3D_F64();
		Point3D_F64 e2 = new Point3D_F64();

		MultiViewOps.extractEpipoles(F, e1, e2);

		// if the right epipole lies at infinity (z=0) then don't use the first two columns
		if( Math.abs(e2.z) <= UtilEjml.EPS ) {
			col0 = 1; col1 = 2; col2 = 0;
		} else {
			col0 = 0; col1 = 1; col2 = 2;
		}
	}

	@Override
	public void decode(double[] input, DenseMatrix64F F) {
		F.data[indexes[0]] = input[0];
		F.data[indexes[1]] = input[1];
		F.data[indexes[2]] = input[2];
		F.data[indexes[3]] = input[3];
		F.data[indexes[4]] = input[4];
		F.data[indexes[5]] = 1;
		
		double alpha = input[5];
		double beta = input[6];

		F.data[col2] = alpha*F.data[col0] + beta*F.data[col1];
		F.data[col2+3] = alpha*F.data[col0+3] + beta*F.data[col1+3];
		F.data[col2+6] = alpha*F.data[col0+6] + beta*F.data[col1+6];
	}
}
