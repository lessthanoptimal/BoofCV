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

package boofcv.abst.geo.epipolar;

import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.d3.epipolar.EpipolarResiduals;
import boofcv.numerics.optimization.FactoryOptimization;
import boofcv.numerics.optimization.UnconstrainedLeastSquares;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Improves upon the initial estimate of the Fundamental matrix by minimizing the sampson error.
 *
 * @author Peter Abeles
 */
public class LeastSquaresHomography implements RefineEpipolarMatrix {
	EpipolarResiduals func;

	UnconstrainedLeastSquares minimizer;

	DenseMatrix64F found = new DenseMatrix64F(3,3);
	int maxIterations;

	public LeastSquaresHomography(double convergenceTol,
								  int maxIterations,
								  EpipolarResiduals func) {
		this.maxIterations = maxIterations;
		this.func = func;

		minimizer = FactoryOptimization.leastSquareLevenberg(convergenceTol, convergenceTol, 1e-3);
	}

	@Override
	public boolean process(DenseMatrix64F F, List<AssociatedPair> obs) {

		func.setObservations(obs);
		minimizer.setFunction(func,null);

		minimizer.initialize(F.data);

		for( int i = 0; i < maxIterations; i++ ) {
			if( minimizer.iterate() )
				break;
		}
		
		found.data = minimizer.getParameters();

		return true;
	}

	@Override
	public DenseMatrix64F getRefinement() {
		return found;
	}
}
