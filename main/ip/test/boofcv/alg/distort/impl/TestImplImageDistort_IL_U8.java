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

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedU8;

/**
 * @author Peter Abeles
 */
public class TestImplImageDistort_IL_U8 extends GeneralImageDistortTests<InterleavedU8>{

	public TestImplImageDistort_IL_U8() {
		super(ImageType.il(2,InterleavedU8.class));
	}

	@Override
	public ImageDistort<InterleavedU8, InterleavedU8>
	createDistort(PixelTransform2_F32 dstToSrc, InterpolatePixel<InterleavedU8> interp) {
		ImageDistort<InterleavedU8,InterleavedU8> ret = new ImplImageDistort_IL_U8((InterpolatePixelMB)interp);
		ret.setModel(dstToSrc);
		return ret;
	}
}
