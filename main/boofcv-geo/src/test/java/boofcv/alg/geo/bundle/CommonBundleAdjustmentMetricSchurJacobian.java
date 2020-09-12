/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import boofcv.testing.BoofTesting;
import org.ddogleg.optimization.DerivativeChecker;
import org.ddogleg.optimization.wrap.SchurJacobian_to_NtoMxN;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrix;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static boofcv.alg.geo.bundle.TestBundleAdjustmentMetricResidualFunction.createObservations;
import static boofcv.alg.geo.bundle.TestCodecSceneStructureMetric.createScene;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class CommonBundleAdjustmentMetricSchurJacobian<M extends DMatrix> {
	private final Random rand = BoofTesting.createRandom(2345);

	protected abstract BundleAdjustmentMetricSchurJacobian<M> createAlg();

	protected abstract SchurJacobian_to_NtoMxN<M> createJacobian( BundleAdjustmentMetricSchurJacobian<M> alg );

	@Test
	public void compareToNumerical() {
		compareToNumerical(true, false, false);
		compareToNumerical(false, false, false);

		// Test with rigid points
		compareToNumerical(true, true, false);
		compareToNumerical(false, true, false);

		// Rigid and Relative. More careful check of relative views is later on
		compareToNumerical(false, true, true);
	}

	public void compareToNumerical( boolean homogenous, boolean hasRigid, boolean hasRelative ) {
		SceneStructureMetric structure = createScene(rand, homogenous, hasRigid, hasRelative);
		SceneObservations observations = createObservations(rand, structure);

		var param = new double[structure.getParameterCount()];
		new CodecSceneStructureMetric().encode(structure, param);

		var alg = createAlg();

		var jac = createJacobian(alg);
		var func = new BundleAdjustmentMetricResidualFunction();

		alg.configure(structure, observations);
		func.configure(structure, observations);

		// TODO I think the Rodrigues jacobian is computed in a numerically unstable way.
		//      multiplying tolerance by 100 is ridiculous

//		DerivativeChecker.jacobianPrint(func, jac, param, 100*UtilEjml.TEST_F64_SQ );
		assertTrue(DerivativeChecker.jacobian(func, jac, param, 100*UtilEjml.TEST_F64_SQ));
	}

	/**
	 * Have a chain of relative views and test with different ones being fixed or not
	 */
	@Test
	public void compareToNumerical_relative() {
		compareToNumerical_relative(false);
		compareToNumerical_relative(true);
	}

	public void compareToNumerical_relative( boolean homogenous ) {
		SceneStructureMetric structure = createScene(rand, homogenous, false, true);
		SceneObservations observations = createObservations(rand, structure);

		// Try different patterns of relative views and attempt to cover all the edge cases
		compareToNumerical_relative(new boolean[]{true, true, true, false}, structure, observations);
		compareToNumerical_relative(new boolean[]{true, true, false, true}, structure, observations);
		compareToNumerical_relative(new boolean[]{true, true, false, false}, structure, observations);
		compareToNumerical_relative(new boolean[]{true, false, false, false}, structure, observations);
		compareToNumerical_relative(new boolean[]{true, false, true, false}, structure, observations);
		compareToNumerical_relative(new boolean[]{false, true, true, true}, structure, observations);
	}

	public void compareToNumerical_relative( boolean[] knownChain,
											 SceneStructureMetric structure,
											 SceneObservations observations ) {
		structure.views.forIdx(( i, v ) -> v.known = knownChain[i]);

		var param = new double[structure.getParameterCount()];
		new CodecSceneStructureMetric().encode(structure, param);

		var alg = createAlg();

		var jac = createJacobian(alg);
		var func = new BundleAdjustmentMetricResidualFunction();

		alg.configure(structure, observations);
		func.configure(structure, observations);

//		DerivativeChecker.jacobianPrint(func, jac, param, 100*UtilEjml.TEST_F64_SQ );
		assertTrue(DerivativeChecker.jacobian(func, jac, param, 100*UtilEjml.TEST_F64_SQ));
	}
}
