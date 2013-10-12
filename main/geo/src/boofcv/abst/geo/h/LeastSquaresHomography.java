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

package boofcv.abst.geo.h;

import boofcv.abst.geo.RefineEpipolar;
import boofcv.abst.geo.optimization.ResidualsEpipolarMatrixN;
import boofcv.alg.geo.ModelObservationResidualN;
import boofcv.struct.geo.AssociatedPair;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Improves upon the initial estimate of the Homography matrix by minimizing residuals.
 *
 * @author Peter Abeles
 */
public class LeastSquaresHomography implements RefineEpipolar {
	ResidualsEpipolarMatrixN func;

	UnconstrainedLeastSquares minimizer;

	int maxIterations;
	double convergenceTol;

	public LeastSquaresHomography(double convergenceTol,
								  int maxIterations,
								  ModelObservationResidualN residuals ) {
		this.maxIterations = maxIterations;
		this.convergenceTol = convergenceTol;
		this.func = new ResidualsEpipolarMatrixN(null,residuals);

		minimizer = FactoryOptimization.leastSquareLevenberg( 1e-3);
	}

	@Override
	public boolean fitModel(List<AssociatedPair> obs, DenseMatrix64F F, DenseMatrix64F refinedF) {

		func.setObservations(obs);
		minimizer.setFunction(func,null);

		minimizer.initialize(F.data,0,convergenceTol*obs.size());

		for( int i = 0; i < maxIterations; i++ ) {
			if( minimizer.iterate() )
				break;
		}

		System.arraycopy(minimizer.getParameters(),0,refinedF.data,0,9);

		return true;
	}
}
