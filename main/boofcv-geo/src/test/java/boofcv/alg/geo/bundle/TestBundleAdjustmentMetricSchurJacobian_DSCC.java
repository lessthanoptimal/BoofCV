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
import org.ddogleg.optimization.DerivativeChecker;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.ddogleg.optimization.wrap.SchurJacobian_to_NtoMxN;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixSparseCSC;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static boofcv.alg.geo.bundle.TestBundleAdjustmentMetricResidualFunction.createObservations;
import static boofcv.alg.geo.bundle.TestCodecSceneStructureMetric.createScene;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestBundleAdjustmentMetricSchurJacobian_DSCC {
	Random rand = new Random(48854);

	@Test
	public void compareToNumerical() {
		compareToNumerical(true,false);
		compareToNumerical(false,false);
		compareToNumerical(true,true);
		compareToNumerical(false,true);
	}
	public void compareToNumerical(boolean homogenous , boolean hasRigid) {
		SceneStructureMetric structure = createScene(rand,homogenous, hasRigid);
		SceneObservations observations = createObservations(rand,structure);

		double param[] = new double[structure.getParameterCount()];
		new CodecSceneStructureMetric().encode(structure,param);

		BundleAdjustmentMetricSchurJacobian_DSCC alg = new BundleAdjustmentMetricSchurJacobian_DSCC();

		FunctionNtoMxN<DMatrixSparseCSC> jac = new SchurJacobian_to_NtoMxN.DSCC(alg);
		BundleAdjustmentMetricResidualFunction func = new BundleAdjustmentMetricResidualFunction();

		alg.configure(structure,observations);
		func.configure(structure,observations);

		// TODO I think the Rodrigues jacobian is computed in a numerically unstable way.
		//      multiplying tolerance by 100 is ridiculous

//		DerivativeChecker.jacobianPrint(func, jac, param, 100*UtilEjml.TEST_F64_SQ );
		assertTrue(DerivativeChecker.jacobian(func, jac, param, 100*UtilEjml.TEST_F64_SQ ));
	}
}
