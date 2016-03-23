/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.impl;

import boofcv.struct.image.GrayU8;

/**
 * @author Peter Abeles
 */
public class TestImplSelectRectStandard_F32_U8 extends ChecksSelectRectStandardBase<float[],GrayU8> {

	public TestImplSelectRectStandard_F32_U8() {
		super(float[].class,GrayU8.class);
	}

	@Override
	public ImplSelectRectStandardBase_F32<GrayU8> createSelector(int maxError, int rightToLeftTolerance, double texture) {
		return new ImplSelectRectStandard_F32_U8(maxError,rightToLeftTolerance,texture);
	}
}
