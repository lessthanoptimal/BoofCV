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

package boofcv.alg.feature.orientation.impl;

import boofcv.alg.feature.orientation.GenericOrientationIntegralTests;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;


public class TestImplOrientationSlidingWindowIntegral extends BoofStandardJUnit {
	double angleTol = Math.PI/9;
	int r = 3;

	class Base extends GenericOrientationIntegralTests {
		Base( boolean weighted ) {
			super(angleTol,r*2+3, GrayF32.class);

			double weight = weighted ? -1 : 2;

			ImplOrientationSlidingWindowIntegral alg =
					new ImplOrientationSlidingWindowIntegral(0.3, 1, Math.PI/3,r,weight, 4,GrayF32.class);
			setRegionOrientation(alg);
		}
	}

	@Nested
	class Unweighted extends Base {
		Unweighted() {
			super(false);
		}
	}

	@Nested
	class Weighted extends Base {
		Weighted() {
			super(true);
		}
	}
}
