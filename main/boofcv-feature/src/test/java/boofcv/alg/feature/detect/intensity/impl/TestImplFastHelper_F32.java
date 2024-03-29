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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.struct.image.GrayF32;

public class TestImplFastHelper_F32 extends GenericImplFastCornerInterfaceTests<GrayF32> {

	public TestImplFastHelper_F32() {
		super(GrayF32.class, new ImplFastHelper_F32(10){
			@Override
			public int checkPixel(int index) {
				return 0;
			}

			@Override public FastCornerInterface<GrayF32> newInstance() {
				return null;
			}
		}, 10);
	}

}
