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

package gecv.alg.wavelet.impl;

import gecv.alg.misc.ImageTestingOps;
import gecv.alg.wavelet.FactoryWaveletHaar;
import gecv.alg.wavelet.WaveletDesc_F32;
import gecv.core.image.border.ImageBorderReflect;
import gecv.core.image.border.ImageBorder_F32;
import gecv.struct.image.ImageFloat32;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class TestWaveletTransformNaive_F32_F32 {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;

	ImageBorder_F32 border = ImageBorderReflect.wrap((ImageFloat32)null);
	WaveletDesc_F32 forward = FactoryWaveletHaar.generate_F32();
	WaveletDesc_F32 reverse = FactoryWaveletHaar.generate_F32();

	/**
	 * See if it handles an image with an odd number of pixels
	 */
	@Test
	public void oddImage() {
		testEncodeDecode(width-1,height-1);
	}

	/**
	 * See if it handles an image with an even number of pixels
	 */
	@Test
	public void evenImage() {
		testEncodeDecode(width,height);
	}

	private void testEncodeDecode( int widthOrig , int heightOrig ) {
		ImageFloat32 orig = new ImageFloat32(widthOrig, heightOrig);
		ImageTestingOps.randomize(orig,rand,0,30);

		ImageFloat32 transformed = new ImageFloat32(width,height);
		ImageFloat32 reconstructed = new ImageFloat32(widthOrig, heightOrig);

		GecvTesting.checkSubImage(this,"checkTransforms",true,
				orig, transformed, reconstructed );
	}

	public void checkTransforms(ImageFloat32 orig,
								 ImageFloat32 transformed,
								 ImageFloat32 reconstructed ) {
		// Test horizontal transformation
		LevelWaveletTransformNaive.horizontal(border,forward,orig,transformed);
		LevelWaveletTransformNaive.horizontalInverse(reverse,transformed,reconstructed);
		GecvTesting.assertEquals(orig,reconstructed,0,1e-2f);

		// Test vertical transformation
		LevelWaveletTransformNaive.vertical(border,forward,orig,transformed);
		LevelWaveletTransformNaive.verticalInverse(reverse,transformed,reconstructed);
		GecvTesting.assertEquals(orig,reconstructed,0,1e-2f);
	}
}
