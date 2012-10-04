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

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.sfm.MonocularVisualOdometry;
import boofcv.struct.distort.PointTransform_F64;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class EvaluateMonoSimple extends EvaluateMonoVisualOdometry {


	protected EvaluateMonoSimple(boolean visualize) {
		super(visualize);
	}

	@Override
	public MonocularVisualOdometry<?> createAlg(ImagePointTracker<?> tracker, 
												PointTransform_F64 pixelToNormalized) 
	{
		int minTracks = targetTracks/3;

		return null;//FactoryVisualOdometry.monoSimple(minTracks,3,4,tracker,pixelToNormalized);
	}

	public static void main( String args[] ) {
		EvaluateMonoSimple target = new EvaluateMonoSimple(false);
		StandardMonoScenarios scenarios = new StandardMonoScenarios(target,10,3423);

		List<MonoTrialResults> results = scenarios.forwardScenario(1);

		// todo add final position error
		MonoMonteCarloStatistics mc = new MonoMonteCarloStatistics(results);
		
		System.out.printf("drift50     %10.6f  drift95    %10.6f\n",mc.drift50,mc.drift95);
		System.out.printf("location50  %10.6f  location95 %10.6f\n", mc.location50, mc.location95);
		System.out.printf("rotation50  %10.6f  rotation95 %10.6f\n",mc.rotation50,mc.rotation95);
		System.out.printf("average fatal    %5.2f\n",mc.aveFatal);
		System.out.printf("Total Exceptions %d\n",mc.numException);
		System.out.printf("FPS50            %6.2f\n",mc.fps50);

		System.out.println("Done");
	}
}
