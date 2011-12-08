/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.denoise;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.testing.BoofTesting;

import java.util.Random;

import static org.junit.Assert.assertTrue;


/**
 * Test which check to see if images actually have the noise reduced.
 * Creates a simple image, adds noise, and sees if the error is reduced.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public abstract class GenericDenoiseTests<T extends ImageBase> {

	Random rand = new Random(10);
	int width = 20;
	int height = 30;

	Class<T> imageType;
	int noiseSigma;

	T image;
	T imageNoisy;
	T imageDenoised;

	protected GenericDenoiseTests(Class<T> imageType, int noiseSigma) {
		this.imageType = imageType;
		this.noiseSigma = noiseSigma;
	}

	public abstract void denoiseImage( T imageNoisy , T imageDenoised );

	public void performTest() {
		declareImages();

		BoofTesting.checkSubImage(this,"performTest",false,imageNoisy,imageDenoised);
	}

	public void performTest( T imageNoisy , T imageDenoised ) {
		denoiseImage(imageNoisy,imageDenoised);

		double noisyMSE = GeneralizedImageOps.computeMeanSquaredError(image,imageNoisy);
		double denoisedMSE = GeneralizedImageOps.computeMeanSquaredError(image,imageDenoised);

		assertTrue( denoisedMSE < noisyMSE );
	}

	private void declareImages() {
		image = GeneralizedImageOps.createImage(imageType,width,height);
		imageDenoised = GeneralizedImageOps.createImage(imageType,width,height);

		// render a simple scene
		GeneralizedImageOps.fill(image,20);
		GeneralizedImageOps.fillRectangle(image,10,5,5,10,10);
		GeneralizedImageOps.fillRectangle(image,10,15,15,20,20);

		// create the noisy image
		imageNoisy = (T)image.clone();
		GeneralizedImageOps.addGaussian(imageNoisy,rand,noiseSigma,0,255);
	}
}
