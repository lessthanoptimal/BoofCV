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

package boofcv.abst.geo.bundle;

import boofcv.abst.geo.BundleAdjustmentCalibrated;
import boofcv.alg.geo.bundle.*;
import georegression.struct.se.Se3_F64;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.RegionStepType;
import org.ddogleg.optimization.UnconstrainedLeastSquares;

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
	CalibPoseAndPointRodriguesCodec codec;
	// storage for model parameters
	double param[] = new double[0];

	// minimization algorithm
	UnconstrainedLeastSquares minimizer;
	// computes residuals for least-squares
	CalibPoseAndPointResiduals func = new CalibPoseAndPointResiduals();
	CalibPoseAndPointRodriguesJacobian jacobian = new CalibPoseAndPointRodriguesJacobian();

	int maxIterations;
	double convergenceTol;

	public BundleAdjustmentCalibratedDense(double convergenceTol,
										   int maxIterations ) {
		this.convergenceTol = convergenceTol;
		minimizer = FactoryOptimization.leastSquaresTrustRegion(1, RegionStepType.DOG_LEG_F,false);
		codec = new CalibPoseAndPointRodriguesCodec();
		this.maxIterations = maxIterations;
	}

	@Override
	public boolean process(CalibratedPoseAndPoint initialModel,
						   List<ViewPointObservations> observations) 
	{
		int numViews = initialModel.getNumViews();
		int numPoints = initialModel.getNumPoints();
		int numViewsUnknown = initialModel.getNumUnknownViews();

		codec.configure(numViews,numPoints,numViewsUnknown,initialModel.getKnownArray());
		
		if( param.length < codec.getParamLength() )
			param = new double[ codec.getParamLength() ];

		
		// TODO redesign to minimize memory creation
		boolean known[] = initialModel.getKnownArray();
		Se3_F64 extrinsic[] = new Se3_F64[initialModel.getNumViews()];
		for( int i = 0; i < extrinsic.length; i++ ) {
			if( known[i]) {
				extrinsic[i] = new Se3_F64();
				extrinsic[i].set( initialModel.getWorldToCamera(i));
			}
		}
		
		codec.encode(initialModel,param);
		func.configure(codec,initialModel,observations);
		jacobian.configure(observations,initialModel.getNumPoints(),extrinsic);

		minimizer.setFunction(func,jacobian);
		minimizer.initialize(param, 0, convergenceTol * observations.size());

		for( int i = 0; i < maxIterations; i++ ) {
			if( minimizer.iterate() )
				break;
		}

		codec.decode(minimizer.getParameters(), initialModel);
		
		return true;
	}
}
