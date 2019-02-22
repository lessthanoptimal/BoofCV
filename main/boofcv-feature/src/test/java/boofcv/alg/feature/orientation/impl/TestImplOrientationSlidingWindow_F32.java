/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.orientation.GenericOrientationGradientTests;
import boofcv.struct.image.GrayF32;
import org.junit.jupiter.api.Nested;


/**
 * @author Peter Abeles
 */
public class TestImplOrientationSlidingWindow_F32 {
	int N = 10;
	double window = Math.PI/3.0;

	class Base extends GenericOrientationGradientTests {
		Base( boolean weighted ) {
			super(2.0*Math.PI/N,r*2+1,GrayF32.class);
			ImplOrientationSlidingWindow_F32 alg = new ImplOrientationSlidingWindow_F32(1.0/2.0,N,window,weighted);
			alg.setObjectRadius(r);
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
