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

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.wavelet.UtilWavelet;
import boofcv.alg.transform.wavelet.WaveletTransformOps;
import boofcv.core.image.ConvertImage;
import boofcv.core.image.border.BorderType;
import boofcv.factory.transform.wavelet.FactoryWaveletDaub;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef_I32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class TestWaveletTransformInt {

	Random rand = new Random(3445);
	int width = 30;
	int height = 40;

	@Test
	public void compareToWaveletTransformOps() {
		GrayS32 orig = new GrayS32(width,height);
		GImageMiscOps.fillUniform(orig, rand, 0, 20);
		GrayS32 origCopy = orig.clone();

		int N = 3;
		ImageDimension dimen = UtilWavelet.transformDimension(orig,N);

		GrayS32 found = new GrayS32(dimen.width,dimen.height);
		GrayS32 expected = new GrayS32(dimen.width,dimen.height);

 		WaveletDescription<WlCoef_I32> desc = FactoryWaveletDaub.biorthogonal_I32(5, BorderType.REFLECT);

		GrayS32 storage = new GrayS32(dimen.width,dimen.height);
		WaveletTransformOps.transformN(desc,orig.clone(),expected,storage,N);

		WaveletTransformInt<GrayS32> alg = new WaveletTransformInt<>(desc, N, 0, 255, GrayS32.class);
		alg.transform(orig,found);

		// make sure the original input was not modified like it is in WaveletTransformOps
		BoofTesting.assertEquals(origCopy,orig, 0);
		// see if the two techniques produced the same results
		BoofTesting.assertEquals(expected,found, 0);

		// test inverse transform
		GrayS32 reconstructed = new GrayS32(width,height);
		alg.invert(found,reconstructed);
		BoofTesting.assertEquals(orig,reconstructed, 0);
		// make sure the input has not been modified
		BoofTesting.assertEquals(expected,found, 0);
	}

	/**
	 * See how well it processes an image which is not an GrayS32
	 */
	@Test
	public void checkOtherType() {
		GrayS32 orig = new GrayS32(width,height);
		GImageMiscOps.fillUniform(orig, rand, 0, 20);
		GrayU8 orig8 = ConvertImage.convert(orig,(GrayU8)null);

		int N = 3;
		ImageDimension dimen = UtilWavelet.transformDimension(orig,N);

		GrayS32 found = new GrayS32(dimen.width,dimen.height);
		GrayS32 expected = new GrayS32(dimen.width,dimen.height);

		WaveletDescription<WlCoef_I32> desc = FactoryWaveletDaub.biorthogonal_I32(5, BorderType.REFLECT);

		GrayS32 storage = new GrayS32(dimen.width,dimen.height);
		WaveletTransformOps.transformN(desc,orig.clone(),expected,storage,N);

		WaveletTransformInt<GrayU8> alg = new WaveletTransformInt<>(desc,N,0,255,GrayU8.class);
		alg.transform(orig8,found);

		// see if the two techniques produced the same results
		BoofTesting.assertEquals(expected,found, 0);

		// see if it can convert it back
		GrayU8 reconstructed = new GrayU8(width,height);
		alg.invert(found,reconstructed);
		BoofTesting.assertEquals(orig8,reconstructed, 0);
		// make sure the input has not been modified
		BoofTesting.assertEquals(expected,found, 0);
	}
}
