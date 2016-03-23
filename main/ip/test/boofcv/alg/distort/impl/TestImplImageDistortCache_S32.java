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

package boofcv.alg.distort.impl;

import boofcv.alg.distort.ImageDistortCache_SB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.struct.image.GrayS32;

/**
 * @author Peter Abeles
 */
public class TestImplImageDistortCache_S32 extends CommonImageDistortCacheTests<GrayS32> {

	public TestImplImageDistortCache_S32() {
		super(GrayS32.class);
	}

	@Override
	public ImageDistortCache_SB<GrayS32,GrayS32> create(InterpolatePixelS<GrayS32> interp,
														Class<GrayS32> imageType) {
		return new ImplImageDistortCache_S32(interp);
	}
}
