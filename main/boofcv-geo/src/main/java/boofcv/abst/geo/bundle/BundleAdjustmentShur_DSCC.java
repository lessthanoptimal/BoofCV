/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.bundle.BundleAdjustmentResidualFunction;
import boofcv.alg.geo.bundle.BundleAdjustmentShurJacobian_DSCC;
import boofcv.alg.geo.bundle.CodecBundleAdjustmentSceneStructure;
import org.ddogleg.optimization.impl.LevenbergMarquardtSchur_DSCC;

/**
 * Implementation of bundle adjustment using Shur Complement and generic sparse matrices.
 *
 * @author Peter Abeles
 */
public class BundleAdjustmentShur_DSCC
		implements BundleAdjustment
{
	// minimization algorithm
	LevenbergMarquardtSchur_DSCC minimizer;

	BundleAdjustmentResidualFunction function = new BundleAdjustmentResidualFunction();
	BundleAdjustmentShurJacobian_DSCC jacobian = new BundleAdjustmentShurJacobian_DSCC();

	int maxIterations;
	double parameters[]=new double[0];

	volatile boolean stopRequested = false;

	double errorBefore,errorAfter;

	CodecBundleAdjustmentSceneStructure codec = new CodecBundleAdjustmentSceneStructure();

	public BundleAdjustmentShur_DSCC( double initialDampParam) {
		this.minimizer = new LevenbergMarquardtSchur_DSCC(initialDampParam);
	}

	@Override
	public void configure(double ftol, double gtol, int maxIterations) {
		this.minimizer.setConvergence(ftol,gtol);
		this.maxIterations = maxIterations;
	}

	@Override
	public boolean optimize(BundleAdjustmentSceneStructure structure, BundleAdjustmentObservations observations) {
		stopRequested = false;

		this.function.configure(structure, observations);
		this.jacobian.configure(structure, observations);
		this.minimizer.setFunction(function,jacobian);

		int N = structure.getParameterCount();
		if( parameters.length < N) {
			parameters = new double[N];
		}
		codec.encode(structure,parameters);
		minimizer.initialize(parameters);

		errorBefore = minimizer.getFnorm();
		System.out.println("Error Before: "+errorBefore);

		for( int i = 0; i < maxIterations && !stopRequested; i++ ) {
			if( minimizer.iterate() )
				break;
		}

		errorAfter = minimizer.getFnorm();

		System.out.println("Error Before: "+errorBefore+" After: "+errorAfter);

		codec.decode(minimizer.getParameters(), structure);
		return errorAfter < errorBefore;
	}

	public double getErrorBefore() {
		return errorBefore;
	}

	public double getErrorAfter() {
		return errorAfter;
	}

	@Override
	public void requestStop() {
		stopRequested = true;
	}

	@Override
	public boolean isStopRequested() {
		return stopRequested;
	}
}
