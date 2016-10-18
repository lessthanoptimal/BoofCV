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

package boofcv.abst.denoise;

import boofcv.abst.transform.wavelet.WaveletTransform;
import boofcv.alg.denoise.DenoiseWavelet;
import boofcv.core.image.border.BorderType;
import boofcv.factory.denoise.FactoryDenoiseWaveletAlg;
import boofcv.factory.transform.wavelet.FactoryWaveletDaub;
import boofcv.factory.transform.wavelet.FactoryWaveletTransform;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef_F32;
import boofcv.struct.wavelet.WlCoef_I32;


/**
 * <p>
 * Provides and easy to use interface for removing noise from images.  In some cases
 * more advanced option are hidden for sake of ease of use.
 * </p>
 * 
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryImageDenoise {

	/**
	 * Denoises an image using VISU Shrink wavelet denoiser.
	 *
	 * @param imageType The type of image being transform.
	 * @param numLevels Number of levels in the wavelet transform.  If not sure, try using 3.
	 * @param minPixelValue Minimum allowed pixel intensity value
	 * @param maxPixelValue Maximum allowed pixel intensity value
	 * @return filter for image noise removal.
	 */
	public static <T extends ImageGray> WaveletDenoiseFilter<T>
	waveletVisu( Class<T> imageType , int numLevels , double minPixelValue , double maxPixelValue )
	{
		ImageDataType info = ImageDataType.classToType(imageType);
		WaveletTransform descTran = createDefaultShrinkTransform(info, numLevels,minPixelValue,maxPixelValue);
		DenoiseWavelet denoiser  = FactoryDenoiseWaveletAlg.visu(imageType);

		return new WaveletDenoiseFilter<>(descTran, denoiser);
	}

	/**
	 * Denoises an image using BayesShrink wavelet denoiser.
	 *
	 * @param imageType The type of image being transform.
	 * @param numLevels Number of levels in the wavelet transform.  If not sure, try using 3.
	 * @param minPixelValue Minimum allowed pixel intensity value
	 * @param maxPixelValue Maximum allowed pixel intensity value
	 * @return filter for image noise removal.
	 */
	public static <T extends ImageGray> WaveletDenoiseFilter<T>
	waveletBayes( Class<T> imageType , int numLevels , double minPixelValue , double maxPixelValue )
	{
		ImageDataType info = ImageDataType.classToType(imageType);
		WaveletTransform descTran = createDefaultShrinkTransform(info, numLevels,minPixelValue,maxPixelValue);
		DenoiseWavelet denoiser = FactoryDenoiseWaveletAlg.bayes(null, imageType);

		return new WaveletDenoiseFilter<>(descTran, denoiser);
	}

	/**
	 * Denoises an image using SureShrink wavelet denoiser.
	 *
	 * @param imageType The type of image being transform.
	 * @param numLevels Number of levels in the wavelet transform.  If not sure, try using 3.
	 * @param minPixelValue Minimum allowed pixel intensity value
	 * @param maxPixelValue Maximum allowed pixel intensity value
	 * @return filter for image noise removal.
	 */
	public static <T extends ImageGray> WaveletDenoiseFilter<T>
	waveletSure( Class<T> imageType , int numLevels , double minPixelValue , double maxPixelValue )
	{
		ImageDataType info = ImageDataType.classToType(imageType);
		WaveletTransform descTran = createDefaultShrinkTransform(info, numLevels,minPixelValue,maxPixelValue);
		DenoiseWavelet denoiser = FactoryDenoiseWaveletAlg.sure(imageType);

		return new WaveletDenoiseFilter<>(descTran, denoiser);
	}

	/**
	 * Default wavelet transform used for denoising images.
	 */
	private static WaveletTransform createDefaultShrinkTransform(ImageDataType imageType, int numLevels,
																 double minPixelValue , double maxPixelValue ) {

		WaveletTransform descTran;

		if( !imageType.isInteger()) {
			WaveletDescription<WlCoef_F32> waveletDesc_F32 = FactoryWaveletDaub.daubJ_F32(4);
			descTran = FactoryWaveletTransform.create_F32(waveletDesc_F32,numLevels,
					(float)minPixelValue,(float)maxPixelValue);
		} else {
			WaveletDescription<WlCoef_I32> waveletDesc_I32 = FactoryWaveletDaub.biorthogonal_I32(5, BorderType.REFLECT);
			descTran = FactoryWaveletTransform.create_I(waveletDesc_I32,numLevels,
					(int)minPixelValue,(int)maxPixelValue,
					ImageType.getImageClass(ImageType.Family.GRAY, imageType));
		}
		return descTran;
	}

}
