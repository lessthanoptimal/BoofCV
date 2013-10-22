/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.ImageDimension;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
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
		ImageSInt32 orig = new ImageSInt32(width,height);
		GImageMiscOps.fillUniform(orig, rand, 0, 20);
		ImageSInt32 origCopy = orig.clone();

		int N = 3;
		ImageDimension dimen = UtilWavelet.transformDimension(orig,N);

		ImageSInt32 found = new ImageSInt32(dimen.width,dimen.height);
		ImageSInt32 expected = new ImageSInt32(dimen.width,dimen.height);

 		WaveletDescription<WlCoef_I32> desc = FactoryWaveletDaub.biorthogonal_I32(5, BorderType.REFLECT);

		ImageSInt32 storage = new ImageSInt32(dimen.width,dimen.height);
		WaveletTransformOps.transformN(desc,orig.clone(),expected,storage,N);

		WaveletTransformInt<ImageSInt32> alg = new WaveletTransformInt<ImageSInt32>(desc,N,0,255,ImageSInt32.class);
		alg.transform(orig,found);

		// make sure the original input was not modified like it is in WaveletTransformOps
		BoofTesting.assertEquals(origCopy,orig, 0);
		// see if the two techniques produced the same results
		BoofTesting.assertEquals(expected,found, 0);

		// test inverse transform
		ImageSInt32 reconstructed = new ImageSInt32(width,height);
		alg.invert(found,reconstructed);
		BoofTesting.assertEquals(orig,reconstructed, 0);
		// make sure the input has not been modified
		BoofTesting.assertEquals(expected,found, 0);
	}

	/**
	 * See how well it processes an image which is not an ImageSInt32
	 */
	@Test
	public void checkOtherType() {
		ImageSInt32 orig = new ImageSInt32(width,height);
		GImageMiscOps.fillUniform(orig, rand, 0, 20);
		ImageUInt8 orig8 = ConvertImage.convert(orig,(ImageUInt8)null);

		int N = 3;
		ImageDimension dimen = UtilWavelet.transformDimension(orig,N);

		ImageSInt32 found = new ImageSInt32(dimen.width,dimen.height);
		ImageSInt32 expected = new ImageSInt32(dimen.width,dimen.height);

		WaveletDescription<WlCoef_I32> desc = FactoryWaveletDaub.biorthogonal_I32(5, BorderType.REFLECT);

		ImageSInt32 storage = new ImageSInt32(dimen.width,dimen.height);
		WaveletTransformOps.transformN(desc,orig.clone(),expected,storage,N);

		WaveletTransformInt<ImageUInt8> alg = new WaveletTransformInt<ImageUInt8>(desc,N,0,255,ImageUInt8.class);
		alg.transform(orig8,found);

		// see if the two techniques produced the same results
		BoofTesting.assertEquals(expected,found, 0);

		// see if it can convert it back
		ImageUInt8 reconstructed = new ImageUInt8(width,height);
		alg.invert(found,reconstructed);
		BoofTesting.assertEquals(orig8,reconstructed, 0);
		// make sure the input has not been modified
		BoofTesting.assertEquals(expected,found, 0);
	}
}
