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
import boofcv.alg.geo.bundle.BundleAdjustmentSchurJacobian_DSCC;
import boofcv.alg.geo.bundle.CodecBundleAdjustmentSceneStructure;
import org.ddogleg.optimization.FactoryOptimizationSparse;
import org.ddogleg.optimization.UnconstrainedLeastSquaresSchur;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
import org.ddogleg.optimization.trustregion.ConfigTrustRegion;
import org.ejml.data.DMatrixSparseCSC;

import javax.annotation.Nullable;

/**
 * Implementation of bundle adjustment using Shur Complement and generic sparse matrices.
 *
 * @author Peter Abeles
 */
public class BundleAdjustmentSchur_DSCC
		implements BundleAdjustment
{
	// minimization algorithm
	private UnconstrainedLeastSquaresSchur<DMatrixSparseCSC> minimizer;

	private BundleAdjustmentResidualFunction function = new BundleAdjustmentResidualFunction();
	private BundleAdjustmentSchurJacobian_DSCC jacobian = new BundleAdjustmentSchurJacobian_DSCC();

	private int maxIterations;
	private double parameters[]=new double[0];

	private volatile boolean stopRequested = false;

	private double ftol,gtol;

	private boolean verbose = false;

	/**
	 * Fit error before and after optimization
	 */
	private double errorBefore,errorAfter;

	private CodecBundleAdjustmentSceneStructure codec = new CodecBundleAdjustmentSceneStructure();

	public BundleAdjustmentSchur_DSCC(@Nullable ConfigTrustRegion config) {
		this.minimizer = FactoryOptimizationSparse.doglegSchur(config);
	}

	public BundleAdjustmentSchur_DSCC(@Nullable ConfigLevenbergMarquardt config) {
		this.minimizer = FactoryOptimizationSparse.levenbergMarquardtSchur(config);
	}

	@Override
	public void configure(double ftol, double gtol, int maxIterations) {
		this.ftol = ftol;
		this.gtol = gtol;
		this.maxIterations = maxIterations;
	}

	@Override
	public void setParameters(BundleAdjustmentSceneStructure structure, BundleAdjustmentObservations observations) {
		this.function.configure(structure, observations);
		this.jacobian.configure(structure, observations);
		this.minimizer.setFunction(function,jacobian);

		int N = structure.getParameterCount();
		if( parameters.length < N) {
			parameters = new double[N];
		}
		codec.encode(structure,parameters);
		this.minimizer.initialize(parameters,ftol,gtol);

	}

	@Override
	public boolean optimize( BundleAdjustmentSceneStructure output) {
		stopRequested = false;

		errorBefore = minimizer.getFunctionValue();

		for( int i = 0; i < maxIterations && !stopRequested; i++ ) {
			if( minimizer.iterate() )
				break;
		}

		errorAfter = minimizer.getFunctionValue();

		if( verbose )
			System.out.printf("Error Before: %9.2E After: %9.2E  ratio=%.5f\n",errorBefore,errorAfter,errorAfter/errorBefore);

		codec.decode(minimizer.getParameters(), output);
		return errorAfter < errorBefore;
	}

	@Override
	public double getFitScore() {
		return minimizer.getFunctionValue();
	}

	public double getErrorBefore() {
		return errorBefore;
	}

	public double getErrorAfter() {
		return errorAfter;
	}

	@Override
	public void setVerbose( boolean verbose ) {
		this.verbose = verbose;
		this.minimizer.setVerbose(verbose);
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
