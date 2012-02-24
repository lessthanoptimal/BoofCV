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
import boofcv.alg.geo.d3.epipolar.f.ParamFundamentalEpipolar;
import boofcv.alg.geo.d3.epipolar.f.ResidualsFundamentalSampson;
import boofcv.alg.geo.d3.epipolar.f.ResidualsFundamentalSimple;
import boofcv.numerics.fitting.modelset.ModelCodec;
import boofcv.numerics.optimization.FactoryOptimization;
import boofcv.numerics.optimization.UnconstrainedLeastSquares;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Improves upon the initial estimate of the Fundamental matrix by minimizing the error.
 *
 * @author Peter Abeles
 */
public class LeastSquaresFundamental implements RefineEpipolarMatrix {
	ModelCodec<DenseMatrix64F> paramModel;
	EpipolarResiduals func;
	double param[];

	UnconstrainedLeastSquares minimizer;

	DenseMatrix64F found = new DenseMatrix64F(3,3);
	int maxIterations;

	public LeastSquaresFundamental(double convergenceTol,
								   int maxIterations,
								   boolean useSampson) {
		this( new ParamFundamentalEpipolar() , convergenceTol, maxIterations,useSampson);
	}

	public LeastSquaresFundamental(ModelCodec<DenseMatrix64F> paramModel,
								   double convergenceTol,
								   int maxIterations,
								   boolean useSampson) {
		this.paramModel = paramModel;
		this.maxIterations = maxIterations;

		param = new double[paramModel.getParamLength()];
		if( useSampson )
			func = new ResidualsFundamentalSampson(paramModel);
		else
			func = new ResidualsFundamentalSimple(paramModel);

		minimizer = FactoryOptimization.leastSquareLevenberg(convergenceTol, convergenceTol, 1e-3);
	}

	@Override
	public boolean process(DenseMatrix64F F, List<AssociatedPair> obs) {
		func.setObservations(obs);
		
		paramModel.encode(F, param);

		minimizer.setFunction(func,null);

		minimizer.initialize(param);

		for( int i = 0; i < maxIterations; i++ ) {
			if( minimizer.iterate() )
				break;
		}

		paramModel.decode(minimizer.getParameters(), found);

		return true;
	}

	@Override
	public DenseMatrix64F getRefinement() {
		return found;
	}
}
