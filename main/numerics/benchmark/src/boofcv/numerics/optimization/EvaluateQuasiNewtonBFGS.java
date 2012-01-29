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
public class EvaluateQuasiNewtonBFGS extends UnconstrainedMinimizationEvaluator{

	public EvaluateQuasiNewtonBFGS(boolean verbose) {
		super(verbose,true);
	}

	@Override
	protected UnconstrainedMinimization createSearch(double minimumValue) {
		return FactoryOptimization.unconstrained(1e-5,1e-8,minimumValue);
	}
	
	public static void main( String args[] ) {
		EvaluateQuasiNewtonBFGS eval = new EvaluateQuasiNewtonBFGS(false);

		System.out.println("Powell              ----------------");
		eval.powell();
		System.out.println("Helical Valley      ----------------");
		eval.helicalValley();
		System.out.println("Rosenbrock          ----------------");
		eval.rosenbrock();
//		System.out.println("dodcfg              ----------------");
//		eval.dodcfg();
		System.out.println("variably            ----------------");
		eval.variably();
		System.out.println("trigonometric       ----------------");
		eval.trigonometric();
		System.out.println("Bady Scaled Brown   ----------------");
		eval.badlyScaledBrown();
	}
}
