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

package boofcv.abst.geo.pose;

import boofcv.abst.geo.RefinePerspectiveNPoint;
import boofcv.abst.geo.optimization.ResidualsPoseMatrix;
import boofcv.alg.geo.pose.PoseResidualsSimple;
import boofcv.numerics.fitting.modelset.ModelCodec;
import boofcv.numerics.optimization.FactoryOptimization;
import boofcv.numerics.optimization.UnconstrainedLeastSquares;
import boofcv.struct.geo.PointPositionPair;
import georegression.struct.se.Se3_F64;

import java.util.List;

/**
 * Minimizes the projection residual error in a calibrated camera for a pose estimate.
 *
 * @author Peter Abeles
 */
public class LeastSquaresPose implements RefinePerspectiveNPoint {

	ModelCodec<Se3_F64> paramModel;
	ResidualsPoseMatrix func;
	double param[];

	UnconstrainedLeastSquares minimizer;
	int maxIterations;
	double convergenceTol;

	Se3_F64 found = new Se3_F64();

	public LeastSquaresPose( double convergenceTol , int maxIterations ,
							 ModelCodec<Se3_F64> paramModel )
	{
		this.maxIterations = maxIterations;
		this.paramModel = paramModel;
		this.convergenceTol = convergenceTol;
		this.minimizer = FactoryOptimization.leastSquareLevenberg(1e-3);

		func = new ResidualsPoseMatrix(paramModel,new PoseResidualsSimple());

		param = new double[paramModel.getParamLength()];
	}

	@Override
	public boolean process(Se3_F64 pose, List<PointPositionPair> obs) {

		paramModel.encode(pose, param);

		func.setObservations(obs);

		minimizer.setFunction(func,null);

		minimizer.initialize(param,0,convergenceTol*obs.size());

		System.out.println("  error before "+minimizer.getFunctionValue());
		for( int i = 0; i < maxIterations; i++ ) {
			if( minimizer.iterate() )
				break;
		}
		System.out.println("  error after  "+minimizer.getFunctionValue());

		paramModel.decode(minimizer.getParameters(), found);

		return true;
	}

	@Override
	public Se3_F64 getRefinement() {
		return found;
	}
}
