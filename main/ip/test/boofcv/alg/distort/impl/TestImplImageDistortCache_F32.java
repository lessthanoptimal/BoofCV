/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.ImageDistortCache;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.border.ImageBorder;
import boofcv.struct.image.ImageFloat32;

/**
 * @author Peter Abeles
 */
public class TestImplImageDistortCache_F32 extends CommonImageDistortCacheTests<ImageFloat32> {

	public TestImplImageDistortCache_F32() {
		super(ImageFloat32.class);
	}

	@Override
	public ImageDistortCache<ImageFloat32,ImageFloat32> create(InterpolatePixelS<ImageFloat32> interp,
															   ImageBorder<ImageFloat32> border ,
															   Class<ImageFloat32> imageType) {
		return new ImplImageDistortCache_F32(interp,border);
	}
}
