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

import gecv.alg.wavelet.FactoryWaveletDaub;
import gecv.alg.wavelet.UtilWavelet;
import gecv.alg.wavelet.WaveletBorderType;
import gecv.alg.wavelet.WaveletTransformOps;
import gecv.core.image.GeneralizedImageOps;
import gecv.struct.image.ImageDimension;
import gecv.struct.image.ImageFloat32;
import gecv.struct.wavelet.WaveletDescription;
import gecv.struct.wavelet.WlCoef_F32;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class TestWaveletTransformFloat32 {
	Random rand = new Random(3445);
	int width = 30;
	int height = 40;

	@Test
	public void compareToWaveletTransformOps() {
		ImageFloat32 orig = new ImageFloat32(width,height);
		GeneralizedImageOps.randomize(orig,rand,0,20);
		ImageFloat32 origCopy = orig.clone();

		int N = 3;
		ImageDimension dimen = UtilWavelet.transformDimension(orig,N);

		ImageFloat32 found = new ImageFloat32(dimen.width,dimen.height);
		ImageFloat32 expected = new ImageFloat32(dimen.width,dimen.height);

		WaveletDescription<WlCoef_F32> desc = FactoryWaveletDaub.biorthogonal_F32(5, WaveletBorderType.REFLECT);

		ImageFloat32 storage = new ImageFloat32(dimen.width,dimen.height);
		WaveletTransformOps.transformN(desc,orig.clone(),expected,storage,N);

		WaveletTransformFloat32 alg = new WaveletTransformFloat32(desc,N);
		alg.transform(orig,found);

		// make sure the original input was not modified like it is in WaveletTransformOps
		GecvTesting.assertEquals(origCopy,orig, 0, 1e-4);
		// see if the two techniques produced the same results
		GecvTesting.assertEquals(expected,found, 0, 1e-4);

		// test inverse transform
		ImageFloat32 reconstructed = new ImageFloat32(width,height);
		alg.invert(found,reconstructed);
		GecvTesting.assertEquals(orig,reconstructed, 0, 1e-4);
		// make sure the input has not been modified
		GecvTesting.assertEquals(expected,found, 0, 1e-4);
	}
}
