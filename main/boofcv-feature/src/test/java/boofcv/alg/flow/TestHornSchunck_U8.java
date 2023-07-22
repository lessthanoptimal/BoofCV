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

package boofcv.alg.flow;

import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;

public class TestHornSchunck_U8 extends ChecksHornSchunck<GrayU8,GrayS16> {


	public TestHornSchunck_U8() {
		super(GrayU8.class, GrayS16.class);
	}

	@Override
	public HornSchunck<GrayU8, GrayS16> createAlg() {
		return new HornSchunck_U8(0.2f,1);
	}
}
