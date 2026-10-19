/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.chainrule;

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSE3State extends BoofStandardJUnit {
	@Test void decode_encode() {
		var src = new SE3State(2);
		var dst = new SE3State(2);

		src.loc.setTo(1, 2, 3);
		src.ori[0] = -1;
		src.ori[1] = -2;

		var params = new double[src.length() + 2];
		src.encode(params, 1);
		dst.decode(params, 1);

		assertEquals(0.0, src.loc.distance(dst.loc));
		assertArrayEquals(src.ori, dst.ori);
	}

	@Test void length() {
		var alg = new SE3State(2);
		assertEquals(5, alg.length());
	}

	@Test void toSe3() {
		var rotate = new QuaternionNumericsHam();
		var state = new SE3State(rotate.length());
		Se3_F64 expected = SpecialEuclideanOps_F64.eulerXyz(0.5, 0.9, 1.1, -0.2, 1.0, 0.5, null);

		state.loc.setTo(expected.T);
		rotate.matrixToParam(expected.R, state.ori, 0);

		Se3_F64 found = state.toSe3(rotate, null);

		assertTrue(expected.T.isIdentical(found.T, 1e-8));
		assertTrue(MatrixFeatures_DDRM.isIdentical(expected.R, found.R, 1e-8));
	}

	@Test void toString_notBlank() {
		var alg = new SE3State(3);
		alg.loc.setTo(1, 2, 3);
		alg.ori[0] = 0.5;
		alg.ori[1] = -0.2;
		alg.ori[2] = 1.0;
		assertFalse(alg.toString().isBlank());
	}

	@Test void fill() {
		var alg = new SE3State(2);
		alg.fill(1.5);

		assertEquals(1.5, alg.loc.x);
		assertEquals(1.5, alg.loc.y);
		assertEquals(1.5, alg.loc.z);
		assertArrayEquals(new double[]{1.5, 1.5}, alg.ori);
	}

	@Test void zero() {
		var alg = new SE3State(2);
		alg.fill(9); // dirty it first so zero() has to overwrite

		alg.zero();

		assertEquals(0.0, alg.loc.x);
		assertEquals(0.0, alg.loc.y);
		assertEquals(0.0, alg.loc.z);
		assertArrayEquals(new double[]{0, 0}, alg.ori);
	}

	@Test void fillNaN() {
		var alg = new SE3State(2);
		alg.fillNaN();

		assertTrue(Double.isNaN(alg.loc.x));
		assertTrue(Double.isNaN(alg.loc.y));
		assertTrue(Double.isNaN(alg.loc.z));
		assertTrue(Double.isNaN(alg.ori[0]));
		assertTrue(Double.isNaN(alg.ori[1]));
	}

	@Test void normalize() {
		var rotate = new QuaternionNumericsHam();
		var alg = new SE3State(rotate.length());
		alg.ori[0] = 1;
		alg.ori[1] = 1; // non-unit quaternion (norm sqrt 2)

		alg.normalize(rotate);

		double norm = 0;
		for (double o : alg.ori)
			norm += o*o;
		assertEquals(1.0, Math.sqrt(norm), 1e-8);
	}

	@Test void encode_decode_se3() {
		var rotate = new QuaternionNumericsHam();
		Se3_F64 expected = SpecialEuclideanOps_F64.eulerXyz(0.5, 0.9, 1.1, -0.2, 1.0, 0.5, null);

		var params = new double[1 + 3 + rotate.length() + 1]; // padded to exercise the offset
		int endEncode = SE3State.encode(expected, params, 1, rotate);
		assertEquals(1 + 3 + rotate.length(), endEncode);

		var found = new Se3_F64();
		int endDecode = SE3State.decode(params, 1, found, rotate);
		assertEquals(1 + 3 + rotate.length(), endDecode);

		assertTrue(expected.T.isIdentical(found.T, 1e-8));
		assertTrue(MatrixFeatures_DDRM.isIdentical(expected.R, found.R, 1e-8));
	}
}
