/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.feature.orientation.OrientationIntegralBase;
import boofcv.struct.image.GrayF32;
import boofcv.struct.sparse.GradientValue_F32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;


/**
 * @author Peter Abeles
 */
public class TestImplOrientationImageAverageIntegral extends BoofStandardJUnit {
	double angleTol = 0.01;
	int r = 4;

	double radiusToScale = 0.25;
	double period = 1.0;
	double scale = 1.0/radiusToScale;

	class Base extends GenericOrientationIntegralTests {
		Base( boolean weighted ) {
			super(angleTol,(int)Math.round(scale*period+r), GrayF32.class);

			double weight = weighted ? -1 : 0;

			OrientationIntegralBase<GrayF32,GradientValue_F32> alg =
					new ImplOrientationImageAverageIntegral(radiusToScale,r,period,2,weight,GrayF32.class);
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
