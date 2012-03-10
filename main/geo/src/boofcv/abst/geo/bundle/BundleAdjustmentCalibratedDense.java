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

package boofcv.abst.geo.bundle;

import boofcv.abst.geo.BundleAdjustmentCalibrated;
import boofcv.alg.geo.bundle.*;
import boofcv.numerics.optimization.FactoryOptimization;
import boofcv.numerics.optimization.UnconstrainedLeastSquares;

import java.util.List;

/**
 * Performs bundle adjustment using less efficient, but easier to implement dense matrices.  
 * 
 * @author Peter Abeles
 */
public class BundleAdjustmentCalibratedDense 
		implements BundleAdjustmentCalibrated
{
	// converts to and from a parameterized version of the model
	CalibPoseAndPointRodiguesCodec codec;
	// storage for model parameters
	double param[] = new double[0];

	// minimization algorithm
	UnconstrainedLeastSquares minimizer;
	// computes residuals for least-squares
	CalibPoseAndPointResiduals func = new CalibPoseAndPointResiduals();
	CalibPoseAndPointRodiguesJacobian jacobian = new CalibPoseAndPointRodiguesJacobian();

	int maxIterations;

	public BundleAdjustmentCalibratedDense(double convergenceTol,
										   int maxIterations ) {
		minimizer = FactoryOptimization.leastSquareLevenberg(convergenceTol, convergenceTol, 1e-3);
		codec = new CalibPoseAndPointRodiguesCodec();
		this.maxIterations = maxIterations;
	}

	@Override
	public boolean process(CalibratedPoseAndPoint initialModel,
						   List<ViewPointObservations> observations) {


		int numViews = initialModel.getNumViews();
		int numPoints = initialModel.getNumPoints();
		int numViewsUnknown = initialModel.getNumUnknownViews();

		codec.configure(numViews,numPoints,numViewsUnknown);
		
		if( param.length < codec.getParamLength() )
			param = new double[ codec.getParamLength() ];

		codec.encode(initialModel,param);
		func.configure(codec,initialModel,observations);
		jacobian.configure(observations,initialModel.getNumPoints(),initialModel.getKnownArray());

		// use a numerical jacobian
		minimizer.setFunction(func,null);
		minimizer.initialize(param);

		for( int i = 0; i < maxIterations; i++ ) {
			if( minimizer.iterate() )
				break;
		}

		codec.decode(minimizer.getParameters(), initialModel);
		
		return true;
	}
}
