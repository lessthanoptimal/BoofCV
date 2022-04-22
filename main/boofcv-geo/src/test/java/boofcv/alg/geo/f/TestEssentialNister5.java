/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.geo.AssociatedPair3D;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestEssentialNister5 extends EpipolarTestSimulation {

	@Test void perfectEssential() {
		createCommonChecks().checkEpipolarMatrix(5, false);
	}

	@Test void perfectEssentialPointing() {
		createCommonChecksPointing().checkEpipolarMatrix(5);
	}

	private CommonFundamentalChecks createCommonChecks() {
		return new CommonFundamentalChecks() {
			final EssentialNister5 alg = new EssentialNister5();

			{
				// use a more relaxed tolerance
				// in practice the bad hypotheses seem to get thrown out. The robustness benchmark
				// also provides a better idea of what's going on and seems to be similar to what
				// papers show
				zeroTol = 0.0001;
			}

			@Override public void computeFundamental( List<AssociatedPair> pairs, DogArray<DMatrixRMaj> solutions ) {
				assertTrue(alg.processNormalized(pairs, solutions));
			}
		};
	}

	private CommonEssentialPointingChecks createCommonChecksPointing() {
		return new CommonEssentialPointingChecks() {
			final EssentialNister5 alg = new EssentialNister5();

			{
				// use a more relaxed tolerance
				// in practice the bad hypotheses seem to get thrown out. The robustness benchmark
				// also provides a better idea of what's going on and seems to be similar to what
				// papers show
				zeroTol = 0.0001;
			}

			@Override public void computeEssential( List<AssociatedPair3D> pairs, DogArray<DMatrixRMaj> solutions ) {
				assertTrue(alg.processPointing(pairs, solutions));
			}
		};
	}
}
