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
import boofcv.core.image.GConvertImage;
import boofcv.core.image.border.BorderType;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef_I32;


/**
 * <p>
 * Implementation of {@link boofcv.abst.transform.wavelet.WaveletTransform} for {@link GrayI}.
 * </p>
 *
 * <p>
 * Wavelet transforms are only provided for 32-bit integer images, so if the input image is not 32-bit
 * then it is first copied into a 32-bit image and then transformed.  This is all done internally
 * and is transparent to the user.
 * </p>
 * @author Peter Abeles
 */
public class WaveletTransformInt<T extends GrayI> implements WaveletTransform<T,GrayS32, WlCoef_I32> {

	// is either a copy of the transformed image or the input image
	GrayS32 copyInput = new GrayS32(1,1);
	GrayS32 copyOutput = new GrayS32(1,1);
	// temporary storage of intermediate results
	GrayS32 temp = new GrayS32(1,1);
	// description of the wavelet
	WaveletDescription<WlCoef_I32> desc;
	// number of levels in the transform
	int numLevels;
	//  the class can really take any integer image as input, but this adds strong typeing
	Class<T> inputType;

	// minimum and maximum allowed pixel values
	int minPixelValue;
	int maxPixelValue;

	public WaveletTransformInt(WaveletDescription<WlCoef_I32> desc, int numLevels,
							   int minPixelValue , int maxPixelValue, Class<T> inputType  ) {
		this.desc = desc;
		this.numLevels = numLevels;
		this.inputType = inputType;
		this.minPixelValue = minPixelValue;
		this.maxPixelValue = maxPixelValue;
	}

	@Override
	public GrayS32 transform(T original, GrayS32 transformed) {

		if( transformed == null ) {
			ImageDimension d = UtilWavelet.transformDimension(original,numLevels);
			transformed = new GrayS32(d.width,d.height);
		}
		temp.reshape(transformed.width,transformed.height);

		copyInput.reshape(original.width,original.height);
		if( original.getDataType().getDataType() == int.class ) {
			copyInput.setTo((GrayS32)original);
		} else {
			GConvertImage.convert(original, copyInput);
		}
		WaveletTransformOps.transformN(desc, copyInput,transformed,temp,numLevels);

		return transformed;
	}

	@Override
	public void invert(GrayS32 transformed, T original) {
		copyInput.reshape(transformed.width,transformed.height);
		temp.reshape(transformed.width,transformed.height);
		copyInput.setTo(transformed);

		if( original.getDataType().getDataType() == int.class ) {
			WaveletTransformOps.
					inverseN(desc, copyInput, (GrayS32) original, temp, numLevels, minPixelValue, maxPixelValue);
		} else {
			copyOutput.reshape(original.width,original.height);
			WaveletTransformOps.inverseN(desc, copyInput, copyOutput,temp,numLevels,minPixelValue,maxPixelValue);
			GConvertImage.convert(copyOutput,original);
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

	@Override
	public Class<T> getOriginalType() {
		return inputType;
	}
}
