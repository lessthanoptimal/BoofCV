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

package boofcv.struct.geo;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SpecializedOps;

/**
 * The trifocal tensor describes the projective relationship between three different camera views and is
 * analogous to the Fundamental matrix for two views. The trifocal tensor is composed of three matrices
 * which are 3x3.
 *
 * @author Peter Abeles
 */
public class TrifocalTensor {
	public DenseMatrix64F T1 = new DenseMatrix64F(3,3);
	public DenseMatrix64F T2 = new DenseMatrix64F(3,3);
	public DenseMatrix64F T3 = new DenseMatrix64F(3,3);

	public DenseMatrix64F getT( int index ) {
		switch( index ) {
			case 0:
				return T1;

			case 1:
				return T2;

			case 2:
				return T3;
		}

		throw new IllegalArgumentException("Invalid index");
	}

	public void set( TrifocalTensor a ) {
		T1.set(a.T1);
		T2.set(a.T2);
		T3.set(a.T3);
	}

	/**
	 * <p>
	 * Converts the 27 element vector into a three matrix format:<br>
	 * T_i(j,k) = m.data[ i*9 + j*3 + k ]
	 * </p>
	 *
	 * @param m Input: Trifocal tensor encoded in a vector
	 */
	public void convertFrom( DenseMatrix64F m ) {
		if( m.getNumElements() != 27 )
			throw new IllegalArgumentException("Input matrix/vector must have 27 elements");

		for( int i = 0; i < 9; i++ ) {
			T1.data[i] = m.data[i];
			T2.data[i] = m.data[i+9];
			T3.data[i] = m.data[i+18];
		}
	}

	/**
	 * <p>
	 * Converts this matrix formated trifocal into a 27 element vector:<br>
	 * m.data[ i*9 + j*3 + k ] = T_i(j,k)
	 * </p>
	 *
	 * @param m Output: Trifocal tensor encoded in a vector
	 */
	public void convertTo( DenseMatrix64F m ) {
		if( m.getNumElements() != 27 )
			throw new IllegalArgumentException("Input matrix/vector must have 27 elements");

		for( int i = 0; i < 9; i++ ) {
			m.data[i] = T1.data[i];
			m.data[i+9] = T2.data[i];
			m.data[i+18] = T3.data[i];
		}
	}

	/**
	 * Returns a new copy of the TrifocalTensor
	 *
	 * @return Copy of the trifocal tensor
	 */
	public TrifocalTensor copy() {
		TrifocalTensor ret = new TrifocalTensor();
		ret.T1.set(T1);
		ret.T2.set(T2);
		ret.T3.set(T3);
		return ret;
	}

	/**
	 * The scale of the trifocal tensor is arbitrary.  However there are situations when comparing results that
	 * using a consistent scale is useful.  This function normalizes the sensor such that its Euclidean length
	 * (the f-norm) is equal to one.
	 */
	public void normalizeScale() {
		double sum = 0;

		sum += SpecializedOps.elementSumSq(T1);
		sum += SpecializedOps.elementSumSq(T2);
		sum += SpecializedOps.elementSumSq(T3);

		double n = Math.sqrt(sum);

		CommonOps.scale(1.0/n,T1);
		CommonOps.scale(1.0/n,T2);
		CommonOps.scale(1.0/n,T3);
	}

	@Override
	public String toString() {
		return "TrifocalTensor {\nT1:\n"+T1+"\nT2:\n"+T2+"\nT3\n"+T3+"}";
	}

	public void print() {
		System.out.println(this);
	}
}
