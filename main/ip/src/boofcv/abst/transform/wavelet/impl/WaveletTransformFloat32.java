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

package boofcv.abst.transform.wavelet.impl;

import boofcv.abst.transform.wavelet.WaveletTransform;
import boofcv.alg.transform.wavelet.UtilWavelet;
import boofcv.alg.transform.wavelet.WaveletTransformOps;
import boofcv.core.image.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef_F32;


/**
 * Implementation of {@link boofcv.abst.transform.wavelet.WaveletTransform} for {@link GrayF32}.
 *
 * @author Peter Abeles
 */
public class WaveletTransformFloat32 implements WaveletTransform<GrayF32,GrayF32, WlCoef_F32> {

	GrayF32 copy = new GrayF32(1,1);
	GrayF32 temp = new GrayF32(1,1);
	WaveletDescription<WlCoef_F32> desc;
	// number of levels in the transform
	int numLevels;

	// minimum and maximum allowed pixel values
	float minPixelValue;
	float maxPixelValue;

	public WaveletTransformFloat32(WaveletDescription<WlCoef_F32> desc, int numLevels,
								   float minPixelValue , float maxPixelValue ) {
		this.desc = desc;
		this.numLevels = numLevels;
		this.minPixelValue = minPixelValue;
		this.maxPixelValue = maxPixelValue;
	}

	@Override
	public GrayF32 transform(GrayF32 original, GrayF32 transformed) {

		if( transformed == null ) {
			ImageDimension d = UtilWavelet.transformDimension(original,numLevels);
			transformed = new GrayF32(d.width,d.height);
		}
		temp.reshape(transformed.width,transformed.height);
		copy.reshape(original.width,original.height);
		copy.setTo(original);

		WaveletTransformOps.transformN(desc,copy,transformed,temp,numLevels);

		return transformed;
	}

	@Override
	public void invert(GrayF32 transformed, GrayF32 original) {
		temp.reshape(transformed.width,transformed.height);
		copy.reshape(transformed.width,transformed.height);
		copy.setTo(transformed);

		WaveletTransformOps.inverseN(desc,copy,original,temp,numLevels,minPixelValue,maxPixelValue);
	}

	@Override
	public int getLevels() {
		return numLevels;
	}

	@Override
	public BorderType getBorderType() {
		return UtilWavelet.convertToType(desc.getBorder());
	}

	@Override
	public WaveletDescription<WlCoef_F32> getDescription() {
		return desc;
	}

	@Override
	public Class<GrayF32> getOriginalType() {
		return GrayF32.class;
	}
}
