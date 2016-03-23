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

import boofcv.abst.filter.FilterImageInterface;
import boofcv.abst.transform.wavelet.WaveletTransform;
import boofcv.alg.denoise.DenoiseWavelet;
import boofcv.alg.transform.wavelet.UtilWavelet;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;


/**
 * Simplifies removing image noise using a wavelet transform.
 *
 * @param <T> Type of image being processed.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class WaveletDenoiseFilter<T extends ImageGray> implements FilterImageInterface<T, T> {

	// performs the wavelet transform
	private WaveletTransform<T,ImageGray,?> wavelet;

	// algorithm used to denoise the image
	private DenoiseWavelet alg;

	// where the wavelet transform is stored
	private ImageGray transform;

	/**
	 * Specifies which wavelet and algorithm is used for removing image noise.
	 *
	 * @param wavelet Description of the wavelet transform.
	 * @param denoiser Algorithm used to remove noise
	 */
	public WaveletDenoiseFilter(WaveletTransform<T, ?, ?> wavelet,
								DenoiseWavelet denoiser) {
		this.wavelet = (WaveletTransform<T,ImageGray,?>)wavelet;
		this.alg = denoiser;
	}

	@Override
	public void process(T original, T denoised ) {

		// compute the wavelet transform
		if( transform != null ) {
			ImageDimension d = UtilWavelet.transformDimension(original,wavelet.getLevels());
			transform.reshape(d.width,d.height);
		}
		transform = wavelet.transform(original,transform);

		// remove noise from the transformed image
		alg.denoise(transform,wavelet.getLevels());

		// reverse the transform
		wavelet.invert(transform,denoised);
	}

	@Override
	public int getHorizontalBorder() {
		return 0;
	}

	@Override
	public int getVerticalBorder() {
		return 0;
	}

	@Override
	public ImageType<T> getInputType() {
		return ImageType.single(wavelet.getOriginalType());
	}

	@Override
	public ImageType<T> getOutputType() {
		return ImageType.single(wavelet.getOriginalType());
	}
}
