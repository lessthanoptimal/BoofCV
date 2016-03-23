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
import boofcv.core.image.border.BorderType;
import boofcv.factory.transform.wavelet.FactoryWaveletDaub;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef_F32;
import boofcv.testing.BoofTesting;
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
		GrayF32 orig = new GrayF32(width,height);
		GImageMiscOps.fillUniform(orig, rand, 0, 20);
		GrayF32 origCopy = orig.clone();

		int N = 3;
		ImageDimension dimen = UtilWavelet.transformDimension(orig,N);

		GrayF32 found = new GrayF32(dimen.width,dimen.height);
		GrayF32 expected = new GrayF32(dimen.width,dimen.height);

		WaveletDescription<WlCoef_F32> desc = FactoryWaveletDaub.biorthogonal_F32(5, BorderType.REFLECT);

		GrayF32 storage = new GrayF32(dimen.width,dimen.height);
		WaveletTransformOps.transformN(desc,orig.clone(),expected,storage,N);

		WaveletTransformFloat32 alg = new WaveletTransformFloat32(desc,N,0,255);
		alg.transform(orig,found);

		// make sure the original input was not modified like it is in WaveletTransformOps
		BoofTesting.assertEquals(origCopy,orig, 1e-4);
		// see if the two techniques produced the same results
		BoofTesting.assertEquals(expected,found, 1e-4);

		// test inverse transform
		GrayF32 reconstructed = new GrayF32(width,height);
		alg.invert(found,reconstructed);
		BoofTesting.assertEquals(orig,reconstructed, 1e-4);
		// make sure the input has not been modified
		BoofTesting.assertEquals(expected,found, 1e-4);
	}
}
