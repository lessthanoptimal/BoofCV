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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.struct.image.GrayS16;

class TestImplSsdCorner_S16_MT extends CompareCornerIntensity<GrayS16> {
	TestImplSsdCorner_S16_MT() {
		super(GrayS16.class);

		ImplSsdCorner_S16    algA = new ImplSsdCorner_S16(2,new ShiTomasiCorner_S32());
		ImplSsdCorner_S16_MT algB = new ImplSsdCorner_S16_MT(2,new ShiTomasiCorner_S32());

		initialize(algA,algB);
	}
}

