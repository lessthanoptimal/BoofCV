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

package boofcv.alg.geo.bundle;

import boofcv.numerics.optimization.JacobianChecker;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static boofcv.abst.geo.bundle.TestBundleAdjustmentCalibratedDense.createModel;
import static boofcv.abst.geo.bundle.TestBundleAdjustmentCalibratedDense.createObservations;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestCalibPoseAndPointRodiguesJacobian {

	Random rand = new Random(48854);
	int numViews = 2;
	int numPoints = 3;
			
	

	CalibPoseAndPointRodiguesCodec codec = new CalibPoseAndPointRodiguesCodec();
	CalibPoseAndPointResiduals func = new CalibPoseAndPointResiduals();
	
	/**
	 * Check Jacobian against numerical.  All views have unknown extrinsic parameters
	 */
	@Test
	public void allUnknown() {
		CalibratedPoseAndPoint model = createModel(numViews,numPoints,rand);
		List<ViewPointObservations> observations = createObservations(model,numViews,numPoints);

		boolean known[]= new boolean[]{false,false};

		int numViewsUnknown = model.getNumUnknownViews();
		codec.configure(numViews,numPoints,numViewsUnknown);
		func.configure(codec,model,observations);
		
		CalibPoseAndPointRodiguesJacobian alg = new CalibPoseAndPointRodiguesJacobian();
		alg.configure(observations,numPoints,known);

		double []param = new double[ codec.getParamLength() ];

		codec.encode(model,param);

		JacobianChecker.jacobianPrint(func, alg, param, 1e-6);
		assertTrue(JacobianChecker.jacobian(func, alg, param, 1e-6));
	}

	/**
	 * Check Jacobian against numerical.  All views have unknown extrinsic parameters
	 */
	@Test
	public void oneKnown() {
		fail("implement");
	}
}
