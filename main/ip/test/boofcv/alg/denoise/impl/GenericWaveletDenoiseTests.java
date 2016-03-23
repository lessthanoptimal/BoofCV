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

package boofcv.alg.denoise.impl;

import boofcv.abst.transform.wavelet.WaveletTransform;
import boofcv.alg.denoise.GenericDenoiseTests;
import boofcv.factory.transform.wavelet.FactoryWaveletTransform;
import boofcv.struct.image.ImageGray;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.testing.BoofTesting;


/**
 * Handles the forward and inverse wavelet transform
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public abstract class GenericWaveletDenoiseTests<T extends ImageGray> extends GenericDenoiseTests<T> {


	WaveletTransform transform;

	public GenericWaveletDenoiseTests(Class imageType, int noiseSigma,
									  WaveletDescription waveletDesc, int numLevels ) {
		super(imageType, noiseSigma);

		transform = FactoryWaveletTransform.create(imageType,waveletDesc,numLevels,0,255);
	}

	public abstract void denoiseWavelet(ImageGray transformedImg , int numLevels );

	@Override
	public void denoiseImage(T imageNoisy, T imageDenoised) {

		ImageGray transformedImg = transform.transform(imageNoisy,null);

		// if the input is a subimage make the transform a subimage
		// so this condition is also tested
		if( imageNoisy.isSubimage() ) {
			transformedImg = BoofTesting.createSubImageOf(transformedImg);
		}

		denoiseWavelet(transformedImg,transform.getLevels());

		transform.invert(transformedImg,imageDenoised);
	}
}
