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

package boofcv.alg.distort.impl;

import boofcv.alg.distort.AssignPixelValue_MB;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.ImageDistortBasic_IL;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedF32;
import georegression.struct.point.Point2D_F32;

public class TestImplImageDistort_IL_F32 extends GeneralImageDistortTests<InterleavedF32>{

	public TestImplImageDistort_IL_F32() {
		super(ImageType.il(2,InterleavedF32.class));
	}

	@Override
	public ImageDistort<InterleavedF32, InterleavedF32>
	createDistort(PixelTransform<Point2D_F32> dstToSrc, InterpolatePixel<InterleavedF32> interp) {
		ImageDistort<InterleavedF32,InterleavedF32> ret =
				new ImageDistortBasic_IL<>(new AssignPixelValue_MB.F32(),(InterpolatePixelMB)interp);
		ret.setModel(dstToSrc);
		return ret;
	}
}
