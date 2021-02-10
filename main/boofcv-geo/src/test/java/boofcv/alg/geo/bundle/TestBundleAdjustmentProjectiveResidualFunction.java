/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static boofcv.alg.geo.bundle.TestCodecSceneStructureProjective.createScene3D;
import static boofcv.alg.geo.bundle.TestCodecSceneStructureProjective.createSceneH;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Peter Abeles
 */
class TestBundleAdjustmentProjectiveResidualFunction extends BoofStandardJUnit {

	/**
	 * Makes sure that when given the same input it produces the same output
	 */
	@Test
	void multipleCalls() {
		multipleCalls(false);
		multipleCalls(true);
	}

	void multipleCalls( boolean homogenous ) {
		SceneStructureProjective structure = homogenous ? createSceneH(rand) : createScene3D(rand);
		SceneObservations obs = createObservations(rand, structure);

		double[] param = new double[structure.getParameterCount()];

		new CodecSceneStructureProjective().encode(structure, param);

		BundleAdjustmentProjectiveResidualFunction alg = new BundleAdjustmentProjectiveResidualFunction();
		alg.configure(structure, obs);

		double[] expected = new double[alg.getNumOfOutputsM()];
		double[] found = new double[alg.getNumOfOutputsM()];

		alg.process(param, expected);
		alg.process(param, found);

		assertArrayEquals(expected, found, UtilEjml.TEST_F64);
	}

	/**
	 * Change each parameter and see if it changes the output
	 */
	@Test
	void changeInParamChangesOutput() {
		changeInParamChangesOutput(false);
		changeInParamChangesOutput(true);
	}

	void changeInParamChangesOutput( boolean homogenous ) {
		SceneStructureProjective structure = homogenous ? createSceneH(rand) : createScene3D(rand);
		double[] param = new double[structure.getParameterCount()];

		new CodecSceneStructureProjective().encode(structure, param);

		// Create random observations
		SceneObservations obs = createObservations(rand, structure);

		BundleAdjustmentProjectiveResidualFunction alg = new BundleAdjustmentProjectiveResidualFunction();
		alg.configure(structure, obs);

		double[] original = new double[alg.getNumOfOutputsM()];
		double[] found = new double[alg.getNumOfOutputsM()];
		alg.process(param, original);

		for (int paramIndex = 0; paramIndex < original.length; paramIndex++) {
			double v = param[paramIndex];
			param[paramIndex] += 0.001;
			alg.process(param, found);

			boolean identical = true;
			for (int i = 0; i < found.length; i++) {
				if (Math.abs(original[i] - found[i]) > UtilEjml.TEST_F64) {
					identical = false;
					break;
				}
			}

			assertFalse(identical);
			param[paramIndex] = v;
		}
	}

	static SceneObservations createObservations( Random rand, SceneStructureProjective structure ) {
		SceneObservations obs = new SceneObservations();
		obs.initialize(structure.views.size);

		for (int j = 0; j < structure.points.size; j++) {
			SceneStructureCommon.Point p = structure.points.data[j];

			for (int i = 0; i < p.views.size; i++) {
				SceneObservations.View v = obs.getView(p.views.get(i));
				v.point.add(j);
				v.observations.add(rand.nextInt(300) + 20);
				v.observations.add(rand.nextInt(300) + 20);
			}
		}
		return obs;
	}
}
