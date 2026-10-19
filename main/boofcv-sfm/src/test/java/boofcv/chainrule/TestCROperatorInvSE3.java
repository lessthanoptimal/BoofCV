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
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.CROperatorForward;
import org.ddogleg.chainrule.CommonCROperatorBackwardsJac.JacobianCase;
import org.ddogleg.chainrule.CommonCROperatorBackwardsJac;
import org.ddogleg.chainrule.CommonCROperatorForward;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCROperatorInvSE3 extends BoofStandardJUnit {
	@TestFactory Stream<DynamicTest> forward() {
		return new CommonCROperatorForward(this::createVariants, rand).tests();
	}

	RotationNumerics rotate = new Rodrigues3Numerics();

	List<CROperatorForward<double[]>> createVariants() {
		return List.of(new CROperatorInvSE3(false, rotate), new CROperatorSE3(true, rotate));
	}

	@TestFactory Stream<DynamicTest> jacobian() {
		return new CommonCROperatorBackwardsJac<double[]>(this::jacobianVariants, rand).tests();
	}

	List<JacobianCase<double[]>> jacobianVariants() {
		var variants = new ArrayList<JacobianCase<double[]>>();

		variants.add(new JacobianCase<>(new CROperatorInvSE3(false, rotate)).
				in(1, -2, 3).in(5, 4, 3, -0.2, 0.1, 0.4));

		variants.add(new JacobianCase<>(new CROperatorInvSE3(true, rotate)).
				in(1, -2, 3, 1.3).in(5, 4, 3, -0.2, 0.1, 0.4));

		return variants;
	}

	@Test void encode() {
		var alg = new CROperatorInvSE3(true, rotate);
		Se3_F64 expected = SpecialEuclideanOps_F64.eulerXyz(1, -1, 2, 0.1, -0.15, 0.1, null);
		var work = new double[6];

		// test general case
		alg.encode(expected, work, 0);
		alg.setInput(1, new CRArray<>(work, 0));

		assertTrue(SpecialEuclideanOps_F64.isIdentical(expected, alg.srcToDst, 1e-4, 1e-4));

		// test case with no rotation
		CommonOps_DDRM.setIdentity(expected.R);
		alg.encode(expected, work, 0);
		alg.setInput(1, new CRArray<>(work, 0));

		assertTrue(SpecialEuclideanOps_F64.isIdentical(expected, alg.srcToDst, 1e-4, 1e-4));
	}
}
