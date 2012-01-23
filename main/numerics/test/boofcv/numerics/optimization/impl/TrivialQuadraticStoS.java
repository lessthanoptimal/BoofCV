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

package boofcv.numerics.optimization.impl;

import boofcv.numerics.optimization.FunctionStoS;

/**
 * Function used for testing optimization functions.  Slightly perturbed from a quadratic.
 *
 * @author Peter Abeles
 */
public class TrivialQuadraticStoS implements FunctionStoS {

	public static final double PERTURBATION = 0.00001;
	double center;

	public TrivialQuadraticStoS(double center) {
		this.center = center;
	}

	@Override
	public double process(double input) {

		double v = input-center;

		return v*v+PERTURBATION*v*v*v*v;
	}
}
