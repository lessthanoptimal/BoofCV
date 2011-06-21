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

import gecv.struct.image.ImageFloat32;
import gecv.struct.wavelet.WaveletDesc_F32;
import gecv.testing.GecvTesting;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestImplWaveletTransformBorder  {

	@Test
	public void horizontal() {
		PermuteWaveletCompareToNaive test = new PermuteWaveletCompareToNaive() {

			@Override
			public void applyValidation(WaveletDesc_F32 desc, ImageFloat32 input, ImageFloat32 output) {
				ImplWaveletTransformNaive.horizontal(desc,input,output);
			}

			@Override
			public void applyTransform(WaveletDesc_F32 desc, ImageFloat32 input, ImageFloat32 output) {
				ImplWaveletTransformInner.horizontal(desc,input,output);
				ImplWaveletTransformBorder.horizontal(desc,input,output);
			}

			@Override
			public void compareResults(WaveletDesc_F32 desc, ImageFloat32 expected, ImageFloat32 found, boolean shrunk) {
				GecvTesting.assertEquals(expected,found,0,1e-4f);
			}
		};

		test.runTests(true);
	}

	@Test
	public void vertical() {
		PermuteWaveletCompareToNaive test = new PermuteWaveletCompareToNaive() {

			@Override
			public void applyValidation(WaveletDesc_F32 desc, ImageFloat32 input, ImageFloat32 output) {
				ImplWaveletTransformNaive.vertical(desc,input,output);
			}

			@Override
			public void applyTransform(WaveletDesc_F32 desc, ImageFloat32 input, ImageFloat32 output) {
				ImplWaveletTransformInner.vertical(desc,input,output);
				ImplWaveletTransformBorder.vertical(desc,input,output);
			}

			@Override
			public void compareResults(WaveletDesc_F32 desc, ImageFloat32 expected, ImageFloat32 found, boolean shrunk) {
				GecvTesting.assertEquals(expected,found,0,1e-4f);
			}
		};

		test.runTests(true);
	}

	@Test
	public void horizontalInverse() {
		PermuteWaveletCompareToNaive test = new PermuteWaveletCompareToNaive() {

			@Override
			public void applyValidation(WaveletDesc_F32 desc, ImageFloat32 input, ImageFloat32 output) {
				ImplWaveletTransformNaive.horizontalInverse(desc,input,output);
			}

			@Override
			public void applyTransform(WaveletDesc_F32 desc, ImageFloat32 input, ImageFloat32 output) {
				ImplWaveletTransformInner.horizontalInverse(desc,input,output);
				ImplWaveletTransformBorder.horizontalInverse(desc,input,output);
			}

			@Override
			public void compareResults(WaveletDesc_F32 desc, ImageFloat32 expected, ImageFloat32 found, boolean shrunk) {
				GecvTesting.assertEquals(expected,found,0,1e-4f);
			}
		};

		test.runTests(false);
	}

	@Test
	public void verticalInverse() {
		PermuteWaveletCompareToNaive test = new PermuteWaveletCompareToNaive() {

			@Override
			public void applyValidation(WaveletDesc_F32 desc, ImageFloat32 input, ImageFloat32 output) {
				ImplWaveletTransformNaive.verticalInverse(desc,input,output);
			}

			@Override
			public void applyTransform(WaveletDesc_F32 desc, ImageFloat32 input, ImageFloat32 output) {
				ImplWaveletTransformInner.verticalInverse(desc,input,output);
				ImplWaveletTransformBorder.verticalInverse(desc,input,output);
			}

			@Override
			public void compareResults(WaveletDesc_F32 desc, ImageFloat32 expected, ImageFloat32 found, boolean shrunk) {
				GecvTesting.assertEquals(expected,found,0,1e-4f);
			}
		};

		test.runTests(false);
	}
}
