/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.geo.bundle.SceneStructureMetric;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static boofcv.alg.geo.bundle.TestCodecSceneStructureMetric.createScene;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Peter Abeles
 */
public class TestBundleAdjustmentMetricResidualFunction {
	private Random rand = new Random(234);

	/**
	 * Makes sure that when given the same input it produces the same output
	 */
	@Test
	public void multipleCalls() {
		multipleCalls(true, false);
		multipleCalls(false, false);
		multipleCalls(true, true);
		multipleCalls(false, true);
	}

	public void multipleCalls(boolean homogenous, boolean hasRigid) {
		SceneStructureMetric structure = createScene(rand, homogenous, hasRigid);
		SceneObservations obs = createObservations(rand, structure);

		double param[] = new double[structure.getParameterCount()];

		new CodecSceneStructureMetric().encode(structure, param);

		BundleAdjustmentMetricResidualFunction alg = new BundleAdjustmentMetricResidualFunction();
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
	public void changeInParamChangesOutput() {
		changeInParamChangesOutput(true);
		changeInParamChangesOutput(false);
	}

	public void changeInParamChangesOutput(boolean homogenous) {
		SceneStructureMetric structure = createScene(rand, homogenous, false);
		double param[] = new double[structure.getParameterCount()];

		new CodecSceneStructureMetric().encode(structure, param);

		// Create random observations
		SceneObservations obs = createObservations(rand, structure);

		BundleAdjustmentMetricResidualFunction alg = new BundleAdjustmentMetricResidualFunction();
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

	public static SceneObservations createObservations(Random rand, SceneStructureMetric structure) {
		SceneObservations obs = new SceneObservations(structure.views.length, structure.hasRigid());

		for (int j = 0; j < structure.points.length; j++) {
			SceneStructureMetric.Point p = structure.points[j];

			for (int i = 0; i < p.views.size; i++) {
				SceneObservations.View v = obs.getView(p.views.get(i));
				v.point.add(j);
				v.observations.add(rand.nextInt(300) + 20);
				v.observations.add(rand.nextInt(300) + 20);
			}
		}

		if (structure.hasRigid()) {
			for (int indexRigid = 0; indexRigid < structure.rigids.length; indexRigid++) {
				SceneStructureMetric.Rigid r = structure.rigids[indexRigid];
				for (int i = 0; i < r.points.length; i++) {
					SceneStructureMetric.Point p = r.points[i];
					int indexPoint = r.indexFirst + i;

					for (int j = 0; j < p.views.size; j++) {
						SceneObservations.View v = obs.getViewRigid(p.views.get(j));
						v.point.add(indexPoint);
						v.observations.add(rand.nextInt(300) + 20);
						v.observations.add(rand.nextInt(300) + 20);
					}
				}
			}
		}

		return obs;
	}
}