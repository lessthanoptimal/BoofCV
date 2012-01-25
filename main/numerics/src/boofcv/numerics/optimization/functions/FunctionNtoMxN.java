/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.numerics.optimization.functions;

/**
 * Function that takes in a vector of length N and outputs a matrix with dimension M x N.
 *
 * @author Peter Abeles
 */
public interface FunctionNtoMxN {

	/**
	 * Number of input parameters and columns in output matrix.
	 */
	public int getN();

	/**
	 * Number of rows in output matrix.
	 */
	public int getM();

	/**
	 * Processes the input vector to output a 2D a matrix.  The matrix has a dimension of M rows and N columns
	 * and is formatted as a row major 1D-array.  EJML can be used to provide a matrix wrapper around
	 * the output array: DenseMatrix J = DenseMatrix.wrap(m,n,output);
	 *
	 * @param input Vector with input parameters.
	 * @param output Row major array with M rows and N columns.
	 */
	public void process( double input[] , double[] output );
}
