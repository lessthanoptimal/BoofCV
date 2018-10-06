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

import org.ddogleg.optimization.UnconstrainedLeastSquaresSchur;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ddogleg.optimization.functions.SchurJacobian;
import org.ejml.data.DMatrixSparseCSC;

import javax.annotation.Nullable;
import java.io.PrintStream;

/**
 * Implementation of bundle adjustment using Shur Complement and generic sparse matrices.
 *
 * @author Peter Abeles
 */
public class BundleAdjustmentSchur_DSCC<Structure extends SceneStructure>
		implements BundleAdjustment<Structure>
{
	// minimization algorithm
	private UnconstrainedLeastSquaresSchur<DMatrixSparseCSC> minimizer;

	private FunctionResiduals<Structure> function;
	private Jacobian<Structure> jacobian;

	private int maxIterations;
	private double parameters[]=new double[0];

	private volatile boolean stopRequested = false;

	private double ftol,gtol;

	private PrintStream verbose;

	private Codec<Structure> codec;

	public BundleAdjustmentSchur_DSCC(UnconstrainedLeastSquaresSchur<DMatrixSparseCSC> minimizer,
									  FunctionResiduals<Structure> function,
									  Jacobian<Structure> jacobian,
									  Codec<Structure> codec) {
		this.minimizer = minimizer;
		this.function = function;
		this.jacobian = jacobian;
		this.codec = codec;
	}

	@Override
	public void configure(double ftol, double gtol, int maxIterations) {
		this.ftol = ftol;
		this.gtol = gtol;
		this.maxIterations = maxIterations;
	}

	@Override
	public void setParameters(Structure structure, SceneObservations observations) {
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
	public boolean optimize( Structure output) {
		stopRequested = false;

		double before = minimizer.getFunctionValue();
		for( int i = 0; i < maxIterations && !stopRequested; i++ ) {
			if( minimizer.iterate() )
				break;
		}

		codec.decode(minimizer.getParameters(), output);
		return minimizer.getFunctionValue() < before;
	}

	@Override
	public double getFitScore() {
		return minimizer.getFunctionValue();
	}

	@Override
	public void setVerbose(@Nullable PrintStream out, int level) {
		this.verbose = out;
		this.minimizer.setVerbose(out,level);
	}

	@Override
	public void requestStop() {
		stopRequested = true;
	}

	@Override
	public boolean isStopRequested() {
		return stopRequested;
	}

	public interface Codec<Structure extends SceneStructure>
	{
		void decode(double[] input , Structure structure );

		void encode(Structure structure , double[] output );
	}

	public interface FunctionResiduals<Structure extends SceneStructure> extends FunctionNtoM {
		void configure(Structure structure ,
					   SceneObservations observations );
	}

	public interface Jacobian<Structure extends SceneStructure> extends SchurJacobian<DMatrixSparseCSC>  {
		void configure(Structure structure ,
					   SceneObservations observations );
	}
}
