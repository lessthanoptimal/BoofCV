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

package gecv.alg.denoise.impl;

import gecv.abst.wavelet.FactoryWaveletTransform;
import gecv.abst.wavelet.WaveletTransform;
import gecv.alg.denoise.GenericDenoiseTests;
import gecv.struct.image.ImageBase;
import gecv.struct.wavelet.WaveletDescription;
import gecv.testing.GecvTesting;


/**
 * Handles the forward and inverse wavelet transform
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public abstract class GenericWaveletDenoiseTests<T extends ImageBase> extends GenericDenoiseTests<T> {


	WaveletTransform transform;

	public GenericWaveletDenoiseTests(Class imageType, int noiseSigma,
									  WaveletDescription waveletDesc, int numLevels ) {
		super(imageType, noiseSigma);

		transform = FactoryWaveletTransform.create(waveletDesc,numLevels);
	}

	public abstract void denoiseWavelet( ImageBase transformedImg , int numLevels );

	@Override
	public void denoiseImage(T imageNoisy, T imageDenoised) {

		ImageBase transformedImg = transform.transform(imageNoisy,null);

		// if the input is a subimage make the transform a subimage
		// so this condition is also tested
		if( imageNoisy.isSubimage() ) {
			transformedImg = GecvTesting.createSubImageOf(transformedImg);
		}

		denoiseWavelet(transformedImg,transform.getLevels());

		transform.invert(transformedImg,imageDenoised);
	}
}
