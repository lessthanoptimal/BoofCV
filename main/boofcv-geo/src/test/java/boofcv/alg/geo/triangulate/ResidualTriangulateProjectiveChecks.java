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

package boofcv.alg.geo.triangulate;

import georegression.struct.point.Point2D_F64;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class ResidualTriangulateProjectiveChecks extends CommonTriangulationChecks {

	public abstract FunctionNtoM createAlg( List<Point2D_F64> observations , List<DMatrixRMaj> cameraMatrices );

	/**
	 * Give it perfect parameters and no noise in observations then try introducing some errors
	 */
	@Test void perfect() {
		createScene();
		FunctionNtoM alg = createAlg(obsPixels, cameraMatrices);

		double input[] = new double[]{worldPoint.x, worldPoint.y, worldPoint.z, 1};
		double output[] = new double[alg.getNumOfOutputsM()];

		alg.process(input, output);

		// there should be no errors
		double error = computeCost(output);
		assertEquals(0, error, 1e-8);

		// corrupt the parameter, which should cause errors in the residuals
		input[0] += 1;
		alg.process(input,output);

		error = computeCost(output);
		assertTrue( error > 0.1);
	}

	protected double computeCost(double[] output) {
		double cost = 0;
		for( double d : output )
			cost += d*d;
		return cost;
	}
}
