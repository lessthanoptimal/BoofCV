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
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.optimization.DerivativeChecker;
import org.ddogleg.optimization.wrap.SchurJacobian_to_NtoMxN;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrix;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static boofcv.alg.geo.bundle.TestBundleAdjustmentMetricResidualFunction.createObservations;
import static boofcv.alg.geo.bundle.TestCodecSceneStructureMetric.createScene;
import static boofcv.alg.geo.bundle.TestCodecSceneStructureMetric.createSceneStereo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class CommonBundleAdjustmentMetricSchurJacobian<M extends DMatrix>
		extends BoofStandardJUnit {

	protected abstract BundleAdjustmentMetricSchurJacobian<M> createAlg();

	protected abstract SchurJacobian_to_NtoMxN<M> createJacobian( BundleAdjustmentMetricSchurJacobian<M> alg );

	@Test void compareToNumerical() {
		// this will also test multiple calls and reinitializing
		var alg = createAlg();

		compareToNumerical(alg, true, false, false);
		compareToNumerical(alg, false, false, false);

		// Test with rigid points
		compareToNumerical(alg, true, true, false);
		compareToNumerical(alg, false, true, false);

		// Rigid and Relative. More careful check of relative views is later on
		compareToNumerical(alg, false, true, true);
	}

	public void compareToNumerical( BundleAdjustmentMetricSchurJacobian<M> alg,
									boolean homogenous, boolean hasRigid, boolean hasRelative ) {
		SceneStructureMetric structure = createScene(rand, homogenous, hasRigid, hasRelative);
		SceneObservations observations = createObservations(rand, structure);

		var param = new double[structure.getParameterCount()];
		new CodecSceneStructureMetric().encode(structure, param);

		var jac = createJacobian(alg);
		var func = new BundleAdjustmentMetricResidualFunction();

		alg.configure(structure, observations);
		func.configure(structure, observations);

		// TODO I think the Rodrigues jacobian is computed in a numerically unstable way.
		//      multiplying tolerance by 100 is ridiculous

//		DerivativeChecker.jacobianPrint(func, jac, param, 100*UtilEjml.TEST_F64_SQ );
		assertTrue(DerivativeChecker.jacobian(func, jac, param, 110*UtilEjml.TEST_F64_SQ));
	}

	/**
	 * Have a chain of relative views and test with different ones being fixed or not
	 */
	@Test void compareToNumerical_relative() {
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
		structure.motions.forIdx(( i, m ) -> m.known = knownChain[i]);

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

	/**
	 * Simulate a stereo camera. The baseline between the left and right cameras have a common Motion. Only the left
	 * camera moves relative to the world frame the right camera is configure to be relative to the left camera.
	 */
	@Test void movingStereo() {
		twoViewsOneMotion(true, false);
		twoViewsOneMotion(false, false);
		twoViewsOneMotion(true, true);
		twoViewsOneMotion(false, true);
	}

	public void twoViewsOneMotion( boolean homogenous, boolean knownMotion ) {
		SceneStructureMetric structure = createSceneStereo(rand, homogenous);
		SceneObservations observations = createObservations(rand, structure);

		structure.motions.forIdx(( i, m ) -> m.known = knownMotion);

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

	/**
	 * Multiple views that are relative to each other have the same motion
	 */
	@Test void sameMotionInChain() {
		sameMotionInChain(false);
		sameMotionInChain(true);
	}

	public void sameMotionInChain( boolean homogenous ) {
		SceneStructureMetric structure = createSceneChainSameMotion(rand, homogenous);
		SceneObservations observations = createObservations(rand, structure);

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

	/**
	 * Create a scene where a "stereo" camera is created that moves. The right to left transform is fixed and common
	 * across all views
	 */
	static SceneStructureMetric createSceneChainSameMotion( Random rand, boolean homogenous ) {
		SceneStructureMetric out = new SceneStructureMetric(homogenous);

		int numSteps = 3;
		out.initialize(1, numSteps, 5);

		out.setCamera(0, true, new CameraPinhole(200, 300,
				0.1, 400, 500, 1, 1));

		// Create a fixed transform for left to right camera
		int motionIdx = out.addMotion(false, SpecialEuclideanOps_F64.eulerXyz(0.25, 0.01, -0.05, 0.01, 0.02, -0.1, null));

		if (homogenous) {
			for (int i = 0; i < out.points.size; i++) {
				double w = rand.nextDouble()*0.5 + 0.5;
				out.setPoint(i, w*(i + 1), w*(i + 2*rand.nextGaussian()), w*(2*i - 3*rand.nextGaussian()), w);
			}
		} else {
			for (int i = 0; i < out.points.size; i++) {
				out.setPoint(i, i + 1, i + 2*rand.nextGaussian(), 2*i - 3*rand.nextGaussian());
			}
		}

		for (int step = 0; step < numSteps; step++) {
			out.setView(step, -1, motionIdx, step - 1);
			out.connectViewToCamera(step, 0);
		}

		// Assign first point to all views then the other points to just one view
		for (int i = 0; i < out.views.size; i++) {
			out.points.data[0].views.add(i);
		}
		for (int i = 1; i < out.points.size; i++) {
			out.points.data[i].views.add((i - 1)%out.views.size);
		}

		// Sanity check
		assertEquals(1, out.motions.size);
		assertEquals(numSteps, out.views.size);
		return out;
	}
}
