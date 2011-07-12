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
import gecv.alg.transform.wavelet.UtilWavelet;
import gecv.alg.transform.wavelet.WaveletTransformOps;
import gecv.core.image.GeneralizedImageOps;
import gecv.core.image.border.BorderType;
import gecv.struct.image.ImageDimension;
import gecv.struct.image.ImageInteger;
import gecv.struct.image.ImageSInt32;
import gecv.struct.wavelet.WaveletDescription;
import gecv.struct.wavelet.WlCoef_I32;


/**
 * <p>
 * Implementation of {@link gecv.abst.wavelet.WaveletTransform} for {@link gecv.struct.image.ImageInteger}.
 * </p>
 *
 * <p>
 * Wavelet transforms are only provided for 32-bit integer images, so if the input image is not 32-bit
 * then it is first copied into a 32-bit image and then transformed.  This is all done internally
 * and is transparent to the user.
 * </p>
 * @author Peter Abeles
 */
public class WaveletTransformInt<T extends ImageInteger> implements WaveletTransform<T,ImageSInt32, WlCoef_I32> {

	// is either a copy of the transformed image or the input image
	ImageSInt32 copyInput = new ImageSInt32(1,1);
	ImageSInt32 copyOutput = new ImageSInt32(1,1);
	// temporary storage of intermediate results
	ImageSInt32 temp = new ImageSInt32(1,1);
	// description of the wavelet
	WaveletDescription<WlCoef_I32> desc;
	// number of levels in the transform
	int numLevels;

	public WaveletTransformInt(WaveletDescription<WlCoef_I32> desc, int numLevels) {
		this.desc = desc;
		this.numLevels = numLevels;
	}

	@Override
	public ImageSInt32 transform(T original, ImageSInt32 transformed) {

		if( transformed == null ) {
			ImageDimension d = UtilWavelet.transformDimension(original,numLevels);
			transformed = new ImageSInt32(d.width,d.height);
		}
		temp.reshape(transformed.width,transformed.height);

		copyInput.reshape(original.width,original.height);
		if( original.getTypeInfo().getDataType() == int.class ) {
			copyInput.setTo((ImageSInt32)original);
		} else {
			GeneralizedImageOps.convert(original, copyInput);
		}
		WaveletTransformOps.transformN(desc, copyInput,transformed,temp,numLevels);

		return transformed;
	}

	@Override
	public void invert(ImageSInt32 transformed, T original) {
		copyInput.reshape(transformed.width,transformed.height);
		temp.reshape(transformed.width,transformed.height);
		copyInput.setTo(transformed);

		if( original.getTypeInfo().getDataType() == int.class ) {
			WaveletTransformOps.inverseN(desc, copyInput,(ImageSInt32)original,temp,numLevels);
		} else {
			copyOutput.reshape(original.width,original.height);
			WaveletTransformOps.inverseN(desc, copyInput, copyOutput,temp,numLevels);
			GeneralizedImageOps.convert(copyOutput,original);
		}
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
	public WaveletDescription<WlCoef_I32> getDescription() {
		return desc;
	}
}
