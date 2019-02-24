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

import boofcv.struct.image.GrayF32;

class TestImplSsdCornerWeighted_F32_MT extends CompareCornerIntensity<GrayF32> {
	TestImplSsdCornerWeighted_F32_MT() {
		super(GrayF32.class);

		ImplSsdCornerWeighted_F32    algA = new ImplSsdCornerWeighted_F32(2,new ShiTomasiCorner_F32());
		ImplSsdCornerWeighted_F32_MT algB = new ImplSsdCornerWeighted_F32_MT(2,new ShiTomasiCorner_F32());

		initialize(algA,algB);
	}
}

