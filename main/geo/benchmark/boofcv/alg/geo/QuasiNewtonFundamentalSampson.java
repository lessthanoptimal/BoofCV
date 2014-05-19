/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo;

import boofcv.abst.geo.RefineEpipolar;
import boofcv.alg.geo.f.ParamFundamentalEpipolar;
import boofcv.struct.geo.AssociatedPair;
import org.ddogleg.fitting.modelset.ModelCodec;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedMinimization;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Improves upon the initial estimate of the Fundamental matrix by minimizing the sampson error.
 *
 * Found to be much slower and doesn't produce as good of an answer as least squares.
 *
 * @author Peter Abeles
 */
public class QuasiNewtonFundamentalSampson implements RefineEpipolar {
	ModelCodec<DenseMatrix64F> paramModel;
	FunctionSampsonFundamental func = new FunctionSampsonFundamental();
	double param[];

	UnconstrainedMinimization minimizer;

	int maxIterations;
	double convergenceTol;

	public QuasiNewtonFundamentalSampson(double convergenceTol, int maxIterations) {
		this( new ParamFundamentalEpipolar() , convergenceTol, maxIterations);
	}

	public QuasiNewtonFundamentalSampson(ModelCodec<DenseMatrix64F> paramModel,
										 double convergenceTol, int maxIterations) {
		this.paramModel = paramModel;
		this.maxIterations = maxIterations;
		this.convergenceTol = convergenceTol;

		param = new double[paramModel.getParamLength()];

		minimizer = FactoryOptimization.unconstrained();
	}

	@Override
	public boolean fitModel(List<AssociatedPair> obs, DenseMatrix64F F, DenseMatrix64F refinedF) {
		func.set(paramModel, obs);
		
		paramModel.encode(F, param);

		minimizer.setFunction(func,null,0);

		minimizer.initialize(param,0,convergenceTol*obs.size());

		for( int i = 0; i < maxIterations; i++ ) {
			if( minimizer.iterate() )
				break;
		}

		paramModel.decode(minimizer.getParameters(), refinedF);

		return true;
	}
}
