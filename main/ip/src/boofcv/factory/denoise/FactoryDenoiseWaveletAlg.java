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

package boofcv.factory.denoise;

import boofcv.alg.denoise.DenoiseWavelet;
import boofcv.alg.denoise.ShrinkThresholdRule;
import boofcv.alg.denoise.wavelet.DenoiseBayesShrink_F32;
import boofcv.alg.denoise.wavelet.DenoiseSureShrink_F32;
import boofcv.alg.denoise.wavelet.DenoiseVisuShrink_F32;
import boofcv.alg.denoise.wavelet.ShrinkThresholdSoft_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;


/**
 * Factory for creating wavelet based image denoising classes.
 *
 *  @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryDenoiseWaveletAlg {

	/**
	 * Returns {@link DenoiseBayesShrink_F32 Bayes shrink} wavelet based image denoiser.
	 *
	 * @param rule Shrinkage rule. If null then a {@link ShrinkThresholdSoft_F32 soft threshold} rule will be used.
	 * @param imageType Type of image it will process.
	 * @return Bayes Shrink
	 */
	public static <T extends ImageGray> DenoiseWavelet<T> bayes(ShrinkThresholdRule<T> rule , Class<T> imageType )
	{
		if( rule == null ) {
			rule = (ShrinkThresholdRule<T>)new ShrinkThresholdSoft_F32();
		}

		if( imageType == GrayF32.class ) {
			return (DenoiseWavelet<T>)new DenoiseBayesShrink_F32((ShrinkThresholdRule<GrayF32>)rule);
		} else {
			throw new IllegalArgumentException("Unsupported image type "+imageType);
		}
	}

	/**
	 * Returns {@link DenoiseSureShrink_F32 sure shrink} wavelet based image denoiser.
	 *
	 * @param imageType Type of image it will process.
	 * @return Bayes Shrink
	 */
	public static <T extends ImageGray> DenoiseWavelet<T> sure(Class<T> imageType )
	{
		if( imageType == GrayF32.class ) {
			return (DenoiseWavelet<T>)new DenoiseSureShrink_F32();
		} else {
			throw new IllegalArgumentException("Unsupported image type "+imageType);
		}
	}

	/**
	 * Returns {@link DenoiseVisuShrink_F32 visu shrink} wavelet based image denoiser.
	 *
	 * @param imageType Type of image it will process.
	 * @return Bayes Shrink
	 */
	public static <T extends ImageGray> DenoiseWavelet<T> visu(Class<T> imageType )
	{
		if( imageType == GrayF32.class ) {
			return (DenoiseWavelet<T>)new DenoiseVisuShrink_F32();
		} else {
			throw new IllegalArgumentException("Unsupported image type "+imageType);
		}
	}
}
