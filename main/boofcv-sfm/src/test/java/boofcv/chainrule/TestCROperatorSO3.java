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
import org.ddogleg.chainrule.CROperatorForward;
import org.ddogleg.chainrule.CommonCROperatorBackwardsJac.JacobianCase;
import org.ddogleg.chainrule.CommonCROperatorBackwardsJac;
import org.ddogleg.chainrule.CommonCROperatorForward;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class TestCROperatorSO3 extends BoofStandardJUnit {
	@TestFactory Stream<DynamicTest> forward() {
		return new CommonCROperatorForward(this::createVariants, rand).tests();
	}

	RotationNumerics rotate = new QuaternionNumericsHam();

	List<CROperatorForward<double[]>> createVariants() {
		return List.of(new CROperatorSO3(false, false, rotate), new CROperatorSO3(true, false, rotate),
				new CROperatorSO3(false, true, rotate), new CROperatorSO3(true, true, rotate));
	}

	@TestFactory Stream<DynamicTest> jacobian() {
		return new CommonCROperatorBackwardsJac<double[]>(this::jacobianVariants, rand).tests();
	}

	List<JacobianCase<double[]>> jacobianVariants() {
		var ori = new double[rotate.length()];
		for (int i = 0; i < ori.length; i++) {
			ori[i] = 2.0*(rand.nextDouble() - 0.5);
		}
		rotate.normalizeParams(ori, 0);

		var variants = new ArrayList<JacobianCase<double[]>>();

		variants.add(new JacobianCase<>(new CROperatorSO3(false, false, rotate)).in(1, -2, 3).in(ori));
		variants.add(new JacobianCase<>(new CROperatorSO3(true, false, rotate)).in(1, -2, 3, 1.3).in(ori));
		variants.add(new JacobianCase<>(new CROperatorSO3(false, true, rotate)).in(1, -2, 3).in(ori));
		variants.add(new JacobianCase<>(new CROperatorSO3(true, true, rotate)).in(1, -2, 3, 1.3).in(ori));

		return variants;
	}
}
