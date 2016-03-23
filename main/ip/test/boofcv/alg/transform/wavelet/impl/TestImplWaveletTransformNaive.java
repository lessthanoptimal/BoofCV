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

package boofcv.alg.transform.wavelet.impl;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.transform.wavelet.FactoryWaveletDaub;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef_F32;
import boofcv.struct.wavelet.WlCoef_I32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class TestImplWaveletTransformNaive {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;

	WaveletDescription<WlCoef_F32> desc_F32 = FactoryWaveletDaub.daubJ_F32(4);

	WaveletDescription<WlCoef_I32> desc_I32 = FactoryWaveletDaub.biorthogonal_I32(5, BorderType.WRAP);

	/**
	 * See if it can handle odd image sizes and output with extra padding
	 */
	@Test
	public void encodeDecode_F32() {
		testEncodeDecode_F32(20,30,20,30);
		testEncodeDecode_F32(19,29,20,30);
		testEncodeDecode_F32(19,29,22,32);
		testEncodeDecode_F32(19,29,24,34);
		testEncodeDecode_F32(20,30,24,34);
	}


	/**
	 * See if it can handle odd image sizes and output with extra padding
	 */
	@Test
	public void encodeDecode_I32() {
		testEncodeDecode_I32(20,30,20,30);
		testEncodeDecode_I32(19,29,20,30);
		testEncodeDecode_I32(19,29,22,32);
		testEncodeDecode_I32(19,29,24,34);
		testEncodeDecode_I32(20,30,24,34);
	}


	private void testEncodeDecode_F32( int widthOrig , int heightOrig ,
									   int widthOut , int heightOut ) {
		GrayF32 orig = new GrayF32(widthOrig, heightOrig);
		ImageMiscOps.fillUniform(orig,rand,0,30);

		GrayF32 transformed = new GrayF32(widthOut,heightOut);
		GrayF32 reconstructed = new GrayF32(widthOrig, heightOrig);

		BoofTesting.checkSubImage(this,"checkTransforms_F32",true,
				orig, transformed, reconstructed );
	}

	public void checkTransforms_F32(GrayF32 orig,
									GrayF32 transformed,
									GrayF32 reconstructed ) {
		// Test horizontal transformation
		ImplWaveletTransformNaive.horizontal(desc_F32.getBorder(),desc_F32.getForward(),orig,transformed);
		ImplWaveletTransformNaive.horizontalInverse(desc_F32.getBorder(),desc_F32.getInverse(),transformed,reconstructed);
		BoofTesting.assertEquals(orig,reconstructed,1e-4);

		// Test vertical transformation
		ImplWaveletTransformNaive.vertical(desc_F32.getBorder(),desc_F32.getForward(),orig,transformed);
		ImplWaveletTransformNaive.verticalInverse(desc_F32.getBorder(),desc_F32.getInverse(),transformed,reconstructed);
		BoofTesting.assertEquals(orig,reconstructed,1e-4);
	}

	private void testEncodeDecode_I32( int widthOrig , int heightOrig ,
									   int widthOut , int heightOut ) {
		GrayU8 orig = new GrayU8(widthOrig, heightOrig);
		ImageMiscOps.fillUniform(orig,rand,0,10);

		GrayS32 transformed = new GrayS32(widthOut,heightOut);
		GrayU8 reconstructed = new GrayU8(widthOrig, heightOrig);

		BoofTesting.checkSubImage(this,"checkTransforms_I",true,
				orig, transformed, reconstructed );
	}

	public void checkTransforms_I(GrayU8 orig,
								  GrayS32 transformed,
								  GrayU8 reconstructed ) {
		// Test horizontal transformation
		ImplWaveletTransformNaive.horizontal(desc_I32.getBorder(),desc_I32.getForward(),orig,transformed);
		ImplWaveletTransformNaive.horizontalInverse(desc_I32.getBorder(),desc_I32.getInverse(),transformed,reconstructed);

//		BoofTesting.printDiff(orig,reconstructed);
		BoofTesting.assertEquals(orig,reconstructed,1);

		// Test vertical transformation
		ImplWaveletTransformNaive.vertical(desc_I32.getBorder(),desc_I32.getForward(),orig,transformed);
		ImplWaveletTransformNaive.verticalInverse(desc_I32.getBorder(),desc_I32.getInverse(),transformed,reconstructed);
		BoofTesting.assertEquals(orig,reconstructed,0);
	}
}
