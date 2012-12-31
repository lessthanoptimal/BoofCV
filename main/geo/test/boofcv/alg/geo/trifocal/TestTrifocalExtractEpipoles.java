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

package boofcv.alg.geo.trifocal;

import boofcv.struct.geo.TrifocalTensor;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTrifocalExtractEpipoles extends CommonTrifocalChecks {

	/**
	 * Randomly general several scenarios and see if it produces the correct solution
	 */
	@Test
	public void basicCheck() {

		TrifocalExtractEpipoles alg = new TrifocalExtractEpipoles();

		for( int i = 0; i < 5; i++ ) {
			createRandomScenario();

			Point3D_F64 found2 = new Point3D_F64();
			Point3D_F64 found3 = new Point3D_F64();

			TrifocalTensor input = tensor.copy();

			alg.process(input, found2, found3);

			// make sure the input was not modified
			for( int j = 0; j < 3; j++ )
				assertTrue(MatrixFeatures.isIdentical(tensor.getT(j), input.getT(j), 1e-8));

			Point3D_F64 space = new Point3D_F64();

			// check to see if it is the left-null space of their respective Fundamental matrices
			GeometryMath_F64.multTran(F2, found2, space);
			assertEquals(0,space.norm(),1e-8);

			GeometryMath_F64.multTran(F3, found3, space);
			assertEquals(0,space.norm(),1e-8);
		}
	}
}
