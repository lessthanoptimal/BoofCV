/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.f;

import boofcv.struct.geo.AssociatedPair;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestFundamentalLinear8 extends BoofStandardJUnit {

	@Test void perfectFundamental() {
		CommonFundamentalChecks checks = createCommonChecks(true);

		checks.checkEpipolarMatrix(8,true);
		checks.checkEpipolarMatrix(15,true);
	}

	@Test void perfectEssential() {
		CommonFundamentalChecks checks = createCommonChecks(true);

		checks.checkEpipolarMatrix(8,false);
		checks.checkEpipolarMatrix(15,false);
	}

	private CommonFundamentalChecks createCommonChecks( final boolean isFundamental ) {
		return new CommonFundamentalChecks() {
			FundamentalLinear8 alg = new FundamentalLinear8(isFundamental);

			@Override
			public void computeFundamental(List<AssociatedPair> pairs,DogArray<DMatrixRMaj> solutions) {
				assertTrue(alg.process(pairs,solutions.grow()));
			}
		};
	}
}
