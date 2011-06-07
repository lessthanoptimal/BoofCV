/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.misc.impl;

import gecv.alg.interpolate.InterpolatePixel;
import gecv.alg.misc.ImageDistort;
import gecv.struct.distort.PixelDistort;
import gecv.struct.image.ImageInt8;
import gecv.struct.image.ImageUInt8;


/**
 * @author Peter Abeles
 */
public class TestImageDistort_I8 extends GeneralImageDistortTests<ImageInt8>{

	public TestImageDistort_I8() {
		super(ImageUInt8.class);
	}

	@Override
	public ImageDistort<ImageInt8> createDistort(PixelDistort dstToSrc, InterpolatePixel<ImageInt8> interp) {
		return new ImageDistort_I8(dstToSrc,interp);
	}
}
