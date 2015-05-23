/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import georegression.struct.se.Se3_F64;
import org.ddogleg.optimization.DerivativeChecker;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static boofcv.abst.geo.bundle.TestBundleAdjustmentCalibratedDense.createModel;
import static boofcv.abst.geo.bundle.TestBundleAdjustmentCalibratedDense.createObservations;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCalibPoseAndPointRodiguesJacobian {

	Random rand = new Random(48854);
	int numViews = 2;
	int numPoints = 3;

	CalibPoseAndPointRodriguesCodec codec = new CalibPoseAndPointRodriguesCodec();
	CalibPoseAndPointResiduals func = new CalibPoseAndPointResiduals();
	
	/**
	 * Check Jacobian against numerical.  All views have unknown extrinsic parameters
	 */
	@Test
	public void allUnknown() {
		check(false,false);
	}

	/**
	 * Check Jacobian against numerical.  All views have unknown extrinsic parameters
	 */
	@Test
	public void allKnown() {
		check(true, true);
	}
	
	private void check( boolean ...known ) {
		CalibratedPoseAndPoint model = createModel(numViews,numPoints,rand);
		List<ViewPointObservations> observations = createObservations(model,numViews,numPoints);

		Se3_F64 extrinsic[] = new Se3_F64[known.length];
		
		for( int i = 0; i < known.length; i++ ) {
			model.setViewKnown(i,known[i]);
			if( known[i] ) {
				Se3_F64 e = new Se3_F64();
				e.set(model.getWorldToCamera(i));
				extrinsic[i] = e;
			}
		}

		int numViewsUnknown = model.getNumUnknownViews();
		codec.configure(numViews,numPoints,numViewsUnknown,known);
		func.configure(codec,model,observations);

		CalibPoseAndPointRodriguesJacobian alg = new CalibPoseAndPointRodriguesJacobian();
		alg.configure(observations,numPoints,extrinsic);

		double []param = new double[ codec.getParamLength() ];

		codec.encode(model,param);

//		DerivativeChecker.jacobianPrint(func, alg, param, 1e-6);
		assertTrue(DerivativeChecker.jacobian(func, alg, param, 1e-4));
	}
}
