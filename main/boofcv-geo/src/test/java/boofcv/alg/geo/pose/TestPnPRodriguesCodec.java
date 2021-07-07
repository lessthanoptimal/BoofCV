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

package boofcv.alg.geo.pose;

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import org.ejml.UtilEjml;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPnPRodriguesCodec extends BoofStandardJUnit {

	@Test void decode_encode() {
		double[] param = new double[]{0.1,-0.3,4,1,2,3};

		PnPRodriguesCodec alg = new PnPRodriguesCodec();

		double[] found = new double[6];
		Se3_F64 storage = new Se3_F64();
		Se3_F64 storage2 = new Se3_F64();

		alg.decode(param, storage);
		alg.encode(storage,found);
		alg.decode(found,storage2);

		// multiple parameterization can represent the same model, so test using the model
		assertTrue(storage.T.isIdentical(storage2.T,1e-8));
		assertTrue(MatrixFeatures_DDRM.isIdentical(storage.R,storage2.R,1e-8));
	}

	@Test void testCase0() {
		Se3_F64 a = new Se3_F64();

		a.R = UtilEjml.parse_DDRM(
						"1.000000e+00        -5.423439e-14        -3.165003e-13 \n" +
						"5.420664e-14         1.000000e+00         2.461642e-13 \n" +
						"3.162678e-13        -2.464418e-13         1.000000e+00",3);

		PnPRodriguesCodec alg = new PnPRodriguesCodec();

		double[] param = new double[6];
		alg.encode(a,param);

		Se3_F64 found = new Se3_F64();
		alg.decode(param,found);

		assertTrue(a.T.isIdentical(found.T,1e-8));
		assertTrue(MatrixFeatures_DDRM.isIdentical(a.R,found.R,1e-8));
	}

	/**
	 * Pathological place which caused an issue in the past
	 */
	@Test void testCase1() {
		Se3_F64 input = new Se3_F64();
		CommonOps_DDRM.diag(input.getR(),3,1,-1,-1);

		Se3_F64 output = new Se3_F64();
		PnPRodriguesCodec alg = new PnPRodriguesCodec();

		double[] param = new double[6];
		alg.encode(input,param);
		alg.decode(param, output);

//		output.print();

		assertTrue(input.T.isIdentical(output.T, 1e-8));
		assertTrue(MatrixFeatures_DDRM.isIdentical(input.R,output.R,1e-8));
	}
}
