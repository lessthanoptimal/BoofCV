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
import org.ddogleg.optimization.DerivativeChecker;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.ddogleg.optimization.wrap.SchurJacobian_to_NtoMxN;
import org.ejml.data.DMatrixSparseCSC;
import org.junit.Test;

import java.util.Random;

import static boofcv.alg.geo.bundle.TestBundleAdjustmentResidualFunction.createObservations;
import static boofcv.alg.geo.bundle.TestCodecBundleAdjustmentSceneStructure.createScene;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestBundleAdjustmentShurJacobian_DSCC {
	Random rand = new Random(48854);

	@Test
	public void compareToNumerical() {
		BundleAdjustmentSceneStructure structure = createScene(rand);
		BundleAdjustmentObservations observations = createObservations(rand,structure);

		double param[] = new double[structure.getParameterCount()];
		new CodecBundleAdjustmentSceneStructure().encode(structure,param);

		BundleAdjustmentShurJacobian_DSCC alg = new BundleAdjustmentShurJacobian_DSCC();

		FunctionNtoMxN<DMatrixSparseCSC> jac = new SchurJacobian_to_NtoMxN(alg);
		BundleAdjustmentResidualFunction func = new BundleAdjustmentResidualFunction();

		alg.configure(structure,observations);
		func.configure(structure,observations);

//		DerivativeChecker.jacobianPrintR(func, jac, param, 1e-3);
		assertTrue(DerivativeChecker.jacobianR(func, jac, param, 1e-3));
	}
}
