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

package boofcv.geo.simulation.mono;

import boofcv.geo.simulation.CameraControl;
import boofcv.geo.simulation.impl.ForwardCameraMotion;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class StandardMonoScenarios {

	int width = 640;
	int height = 480;
	
	DenseMatrix64F K;
	
	int numSimulationSteps=1000;
	int numTrials;
	
	List<Long> seeds = new ArrayList<Long>();
	
	EvaluateMonoVisualOdometry evaluator;

	public StandardMonoScenarios( EvaluateMonoVisualOdometry evaluator , 
								  int numTrials , long seed ) {
		this.evaluator = evaluator;

		this.numTrials = numTrials;

		double fx = width*2/3;
		double fy = height*2/3;

		K = new DenseMatrix64F(3,3,true,fx,0,width/2,0,fy,height/2,0,0,1);
	
		Random rand = new Random(234);
		for( int i = 0; i < numTrials; i++ )
			seeds.add( rand.nextLong() );
			
	}

	public List<MonoTrialResults> forwardScenario( double pixelSigma ) {
		CameraControl control = new ForwardCameraMotion(0.1);

		List<MonoTrialResults> results = new ArrayList<MonoTrialResults>();
		
		for( int trial = 0; trial < numTrials; trial++ ) {
			System.out.println();
			System.out.println();
			System.out.println("    "+trial+"          TRIAL "+seeds.get(trial));
			System.out.println();
			System.out.println();

			evaluator.setup(width,height,K,pixelSigma,control,seeds.get(trial));

			for( int i = 0; i < numSimulationSteps; i++ ) {
				System.out.println("STEP "+i);
				if( !evaluator.step() ) {
					break;
				}
			}
			results.add( evaluator.computeStatistics() );
		}

		return results;
	}

}
