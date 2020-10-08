/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.optimization.DerivativeChecker;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.ddogleg.optimization.wrap.SchurJacobian_to_NtoMxN;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import static boofcv.alg.geo.bundle.TestBundleAdjustmentProjectiveResidualFunction.createObservations;
import static boofcv.alg.geo.bundle.TestCodecSceneStructureProjective.createScene3D;
import static boofcv.alg.geo.bundle.TestCodecSceneStructureProjective.createSceneH;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestBundleAdjustmentProjectiveSchurJacobian_DDRM extends BoofStandardJUnit {
	@Test
	void compareToNumerical_3D() {
		SceneStructureProjective structure = createScene3D(rand);
		SceneObservations observations = createObservations(rand, structure);

		double[] param = new double[structure.getParameterCount()];
		new CodecSceneStructureProjective().encode(structure, param);

		BundleAdjustmentProjectiveSchurJacobian_DDRM alg = new BundleAdjustmentProjectiveSchurJacobian_DDRM();

		FunctionNtoMxN<DMatrixRMaj> jac = new SchurJacobian_to_NtoMxN.DDRM(alg);
		BundleAdjustmentProjectiveResidualFunction func = new BundleAdjustmentProjectiveResidualFunction();

		alg.configure(structure, observations);
		func.configure(structure, observations);

//		DerivativeChecker.jacobianPrint(func, jac, param, 0.1 );
		assertTrue(DerivativeChecker.jacobian(func, jac, param, 0.1));
	}

	@Test
	void compareToNumerical_Homogenous() {
		SceneStructureProjective structure = createSceneH(rand);
		SceneObservations observations = createObservations(rand, structure);

		double[] param = new double[structure.getParameterCount()];
		new CodecSceneStructureProjective().encode(structure, param);

		BundleAdjustmentProjectiveSchurJacobian_DDRM alg = new BundleAdjustmentProjectiveSchurJacobian_DDRM();

		FunctionNtoMxN<DMatrixRMaj> jac = new SchurJacobian_to_NtoMxN.DDRM(alg);
		BundleAdjustmentProjectiveResidualFunction func = new BundleAdjustmentProjectiveResidualFunction();

		alg.configure(structure, observations);
		func.configure(structure, observations);

//		DerivativeChecker.jacobianPrint(func, jac, param, 0.1 );
		assertTrue(DerivativeChecker.jacobian(func, jac, param, 0.1));
	}
}
