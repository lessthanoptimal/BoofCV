/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.numerics.optimization;

/**
 * @author Peter Abeles
 */
public class WrapFunctionResidual<State> implements OptimizationResidual<double[],State>
{
	OptimizationFunction<Object> f;

	public WrapFunctionResidual(OptimizationFunction<Object> f) {
		this.f = f;
	}

	@Override
	public boolean computeResiduals(double[] obs, State o, double[] residuals) {

		f.estimate(o,residuals);
		for( int i = 0; i < residuals.length; i++ ) {
			residuals[i] = obs[i] - residuals[i];
		}

		return true;
	}

	@Override
	public void setModel(double[] model) {
		f.setModel(model);
	}

	@Override
	public int getNumberOfFunctions() {
		return f.getNumberOfFunctions();
	}

	@Override
	public int getModelSize() {
		return f.getModelSize();
	}

	@Override
	public boolean estimate(Object o, double[] estimated) {
		return f.estimate(o,estimated);
	}
}
