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

package boofcv.alg.geo.bundle;

import boofcv.abst.geo.bundle.BundleAdjustmentObservations;
import boofcv.abst.geo.bundle.BundleAdjustmentSceneStructure;
import org.ejml.UtilEjml;
import org.junit.Test;

import java.util.Random;

import static boofcv.alg.geo.bundle.TestCodecBundleAdjustmentSceneStructure.createScene;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Peter Abeles
 */
public class TestBundleAdjustmentResidualFunction {
	Random rand = new Random(234);

	/**
	 * Makes sure that when given the same input it produces the same output
	 */
	@Test
	public void multipleCalls() {
		BundleAdjustmentSceneStructure structure = createScene(rand);
		BundleAdjustmentObservations obs = createObservations(rand,structure);

		double param[] = new double[structure.getParameterCount()];

		new CodecBundleAdjustmentSceneStructure().encode(structure,param);

		BundleAdjustmentResidualFunction alg = new BundleAdjustmentResidualFunction();
		alg.configure(structure,obs);

		double []expected = new double[alg.getNumOfOutputsM()];
		double []found = new double[alg.getNumOfOutputsM()];

		alg.process(param,expected);
		alg.process(param,found);

		assertArrayEquals(expected,found,UtilEjml.TEST_F64);
	}

	/**
	 * Change each parameter and see if it changes the output
	 */
	@Test
	public void changeInParamChangesOutput() {
		BundleAdjustmentSceneStructure structure = createScene(rand);
		double param[] = new double[structure.getParameterCount()];

		new CodecBundleAdjustmentSceneStructure().encode(structure,param);

		// Create random observations
		BundleAdjustmentObservations obs = createObservations(rand,structure);

		BundleAdjustmentResidualFunction alg = new BundleAdjustmentResidualFunction();
		alg.configure(structure,obs);

		double []original = new double[alg.getNumOfOutputsM()];
		double []found = new double[alg.getNumOfOutputsM()];
		alg.process(param,original);

		for (int paramIndex = 0; paramIndex < original.length; paramIndex++) {
			double v = param[paramIndex];
			param[paramIndex] += 0.001;
			alg.process(param,found);

			boolean identical = true;
			for (int i = 0; i < found.length; i++) {
				if( Math.abs(original[i]-found[i]) > UtilEjml.TEST_F64 ) {
					identical = false;
					break;
				}
			}
			assertFalse(identical);
			param[paramIndex] = v;
		}
	}

	public static BundleAdjustmentObservations createObservations( Random rand , BundleAdjustmentSceneStructure structure) {
		BundleAdjustmentObservations obs = new BundleAdjustmentObservations(structure.views.length);

		for (int j = 0; j < structure.points.length; j++) {
			BundleAdjustmentSceneStructure.Point p = structure.points[j];

			for (int i = 0; i < p.views.size; i++) {
				BundleAdjustmentObservations.View v = obs.getView(p.views.get(i));
				v.feature.add( j );
				v.observations.add( rand.nextInt(300)+20);
				v.observations.add( rand.nextInt(300)+20);
			}
		}
		return obs;
	}
}