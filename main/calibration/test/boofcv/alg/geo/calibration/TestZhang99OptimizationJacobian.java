/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.optimization.JacobianChecker;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static boofcv.alg.geo.calibration.TestZhang99OptimizationFunction.estimate;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestZhang99OptimizationJacobian {

	Random rand = new Random(234);

	@Test
	public void compareToNumeric() {
		compareToNumerical(false);
		compareToNumerical(true);
	}

	private void compareToNumerical(boolean assumeZeroSkew) {
		PlanarCalibrationTarget config = FactoryPlanarCalibrationTarget.gridSquare(1, 1, 30, 30);
		Zhang99Parameters param = GenericCalibrationGrid.createStandardParam(assumeZeroSkew, 2, 3, rand);
		param.distortion[0] = 0.1;
		param.distortion[1] = -0.2;
		param.distortion = new double[]{0.1};

		List<Point2D_F64> gridPts = config.points;

		List<List<Point2D_F64>> observations = new ArrayList<List<Point2D_F64>>();

		for( int i = 0; i < param.views.length; i++ ) {
			observations.add( estimate(param,param.views[i],gridPts));
		}

		double dataParam[] = new double[ param.size() ];
		param.convertToParam(dataParam);

		Zhang99OptimizationFunction func =
				new Zhang99OptimizationFunction( param.copy(),gridPts,observations );

		Zhang99OptimizationJacobian alg = new Zhang99OptimizationJacobian(
				assumeZeroSkew,param.distortion.length,observations.size(),gridPts);

		// Why does the tolerance need to be so crude?  Is there a fundamental reason for this?
//		JacobianChecker.jacobianPrint(func, alg, dataParam, 1e-3);
		assertTrue(JacobianChecker.jacobian(func, alg, dataParam, 1e-3));
	}
}
