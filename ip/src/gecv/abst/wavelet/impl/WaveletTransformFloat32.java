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

package gecv.abst.wavelet.impl;

import gecv.abst.wavelet.WaveletTransform;
import gecv.alg.wavelet.UtilWavelet;
import gecv.alg.wavelet.WaveletTransformOps;
import gecv.core.image.border.BorderType;
import gecv.struct.image.ImageDimension;
import gecv.struct.image.ImageFloat32;
import gecv.struct.wavelet.WaveletDescription;
import gecv.struct.wavelet.WlCoef_F32;


/**
 * Implementation of {@link gecv.abst.wavelet.WaveletTransform} for {@link ImageFloat32}.
 *
 * @author Peter Abeles
 */
public class WaveletTransformFloat32 implements WaveletTransform<ImageFloat32,ImageFloat32, WlCoef_F32> {

	ImageFloat32 copy = new ImageFloat32(1,1);
	ImageFloat32 temp = new ImageFloat32(1,1);
	WaveletDescription<WlCoef_F32> desc;
	int numLevels;

	public WaveletTransformFloat32(WaveletDescription<WlCoef_F32> desc, int numLevels) {
		this.desc = desc;
		this.numLevels = numLevels;
	}

	@Override
	public ImageFloat32 transform(ImageFloat32 original, ImageFloat32 transformed) {

		if( transformed == null ) {
			ImageDimension d = UtilWavelet.transformDimension(original,numLevels);
			transformed = new ImageFloat32(d.width,d.height);
		}
		temp.reshape(transformed.width,transformed.height);
		copy.reshape(original.width,original.height);
		copy.setTo(original);

		WaveletTransformOps.transformN(desc,copy,transformed,temp,numLevels);

		return transformed;
	}

	@Override
	public void invert(ImageFloat32 transformed, ImageFloat32 original) {
		temp.reshape(transformed.width,transformed.height);
		copy.reshape(transformed.width,transformed.height);
		copy.setTo(transformed);

		WaveletTransformOps.inverseN(desc,copy,original,temp,numLevels);
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
}
