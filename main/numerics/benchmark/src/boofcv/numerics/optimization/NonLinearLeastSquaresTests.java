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

import java.util.ArrayList;
import java.util.List;

/**
 * Sees how well it can converge given standard test functions
 *
 * @author Peter Abeles
 */
public abstract class NonLinearLeastSquaresTests {

	public void performTest( OptimizationFunction<double[]> f , double []x0 , double []optimal) {

		double obs[] = new double[ f.getNumberOfFunctions() ];
		f.setModel(optimal);
		f.estimate(null,obs);
		
		LevenbergMarquardt<double[],Object> alg = createAlg(f);

		List<double[]> listOfObs = new ArrayList<double[]>(1);
		listOfObs.add(obs);
		
		alg.setMaxIterations(2000);
		boolean worked = alg.process(x0,listOfObs,null);
		
		// todo print number of iterations
		// todo print number of jacobian calculations
		
		double found[] = alg.getModelParameters();
		
		System.out.println("worked = "+worked);
		for( int i = 0; i < found.length; i++ ) {
			System.out.println("error["+i+"] = "+(found[i]-optimal[i]));
		}
	}

	// todo change to no constructor, setFunction()
	public abstract LevenbergMarquardt<double[],Object> createAlg( OptimizationFunction f );

	public static void main( String args[] ) {

		OptimizationFunction f = new FunctionRosenbrock();
		
		new NonLinearLeastSquaresTests() {

			@Override
			public LevenbergMarquardt<double[], Object> createAlg(OptimizationFunction f) {
				return new LevenbergMarquardt<double[], Object>(f.getModelSize(),new WrapFunctionResidual<Object>(f));
			}
		}.performTest(f,new double[]{-1.2,1},new double[]{1,1});
	}
}
