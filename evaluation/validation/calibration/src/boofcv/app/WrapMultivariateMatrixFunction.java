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

package boofcv.app;

import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.ejml.data.DenseMatrix64F;

/**
 * @author Peter Abeles
 */
public class WrapMultivariateMatrixFunction implements MultivariateMatrixFunction {

	FunctionNtoMxN jacobian;

	DenseMatrix64F mat;
	double[][] output;

	public WrapMultivariateMatrixFunction(FunctionNtoMxN jacobian) {
		this.jacobian = jacobian;

		mat = new DenseMatrix64F(jacobian.getM(),jacobian.getN());

		output = new double[jacobian.getM()][jacobian.getN()];
	}

	@Override
	public double[][] value(double[] point) throws IllegalArgumentException {

		jacobian.process(point,mat.data);

		for( int i = 0; i < jacobian.getM(); i++ ) {
			for( int j = 0; j < jacobian.getN(); j++ ) {
				output[i][j] = mat.unsafe_get(i,j);
			}
		}

		return output;
	}
}
