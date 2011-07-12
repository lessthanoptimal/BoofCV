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

package gecv.abst.denoise;

import gecv.abst.wavelet.FactoryWaveletTransform;
import gecv.abst.wavelet.WaveletTransform;
import gecv.alg.denoise.DenoiseWavelet;
import gecv.alg.denoise.FactoryDenoiseWavelet;
import gecv.alg.wavelet.FactoryWaveletDaub;
import gecv.core.image.border.BorderType;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageTypeInfo;
import gecv.struct.wavelet.WaveletDescription;
import gecv.struct.wavelet.WlCoef_F32;
import gecv.struct.wavelet.WlCoef_I32;


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
	 * @return filter for image noise removal.
	 */
	public static <T extends ImageBase> WaveletDenoiseFilter<T>
	waveletVisu( Class<?> imageType , int numLevels )
	{
		ImageTypeInfo info = ImageTypeInfo.classToType(imageType);
		WaveletTransform descTran = createDefaultShrinkTransform(info, numLevels);
		DenoiseWavelet denoiser;

		if( info.getSumType() == float.class )
			denoiser = FactoryDenoiseWavelet.visu_F32();
		else
			throw new IllegalArgumentException("Unsupported");

		return new WaveletDenoiseFilter<T>(descTran,denoiser);
	}

	/**
	 * Denoises an image using BayesShrink wavelet denoiser.
	 *
	 * @param imageType The type of image being transform.
	 * @param numLevels Number of levels in the wavelet transform.  If not sure, try using 3.
	 * @return filter for image noise removal.
	 */
	public static <T extends ImageBase> WaveletDenoiseFilter<T>
	waveletBayes( Class<?> imageType , int numLevels )
	{
		ImageTypeInfo info = ImageTypeInfo.classToType(imageType);
		WaveletTransform descTran = createDefaultShrinkTransform(info, numLevels);
		DenoiseWavelet denoiser;

		if( info.getSumType() == float.class )
			denoiser = FactoryDenoiseWavelet.bayes_F32();
		else
			throw new IllegalArgumentException("Unsupported");

		return new WaveletDenoiseFilter<T>(descTran,denoiser);
	}

	/**
	 * Denoises an image using SureShrink wavelet denoiser.
	 *
	 * @param imageType The type of image being transform.
	 * @param numLevels Number of levels in the wavelet transform.  If not sure, try using 3.
	 * @return filter for image noise removal.
	 */
	public static <T extends ImageBase> WaveletDenoiseFilter<T>
	waveletSure( Class<?> imageType , int numLevels )
	{
		ImageTypeInfo info = ImageTypeInfo.classToType(imageType);
		WaveletTransform descTran = createDefaultShrinkTransform(info, numLevels);
		DenoiseWavelet denoiser;

		if( info.getSumType() == float.class )
			denoiser = FactoryDenoiseWavelet.sure_F32();
		else
			throw new IllegalArgumentException("Unsupported");

		return new WaveletDenoiseFilter<T>(descTran,denoiser);
	}

	/**
	 * Default wavelet transform used for denoising images.
	 */
	private static WaveletTransform createDefaultShrinkTransform(ImageTypeInfo imageType, int numLevels) {

		WaveletTransform descTran;

		if( !imageType.isInteger()) {
			WaveletDescription<WlCoef_F32> waveletDesc_F32 = FactoryWaveletDaub.daubJ_F32(4);
			descTran = FactoryWaveletTransform.create_F32(waveletDesc_F32,numLevels);
		} else {
			WaveletDescription<WlCoef_I32> waveletDesc_I32 = FactoryWaveletDaub.biorthogonal_I32(5, BorderType.REFLECT);
			descTran = FactoryWaveletTransform.create_I(waveletDesc_I32,numLevels);
		}
		return descTran;
	}

}
