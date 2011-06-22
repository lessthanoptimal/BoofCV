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

import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.wavelet.WaveletCoefficient;
import gecv.struct.wavelet.WaveletCoefficient_F32;
import gecv.testing.GecvTesting;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestImplWaveletTransformBorder  {

	Class<?> typeInput = ImageFloat32.class;
	Class<?> typeOutput  = ImageFloat32.class;
	
	@Test
	public void horizontal() {
		PermuteWaveletCompare test = new PermuteWaveletCompare(typeInput,typeOutput) {

			@Override
			public void applyValidation(WaveletCoefficient desc, ImageBase input, ImageBase output) {
				ImplWaveletTransformNaive.horizontal((WaveletCoefficient_F32)desc,(ImageFloat32)input,(ImageFloat32)output);
			}

			@Override
			public void applyTransform(WaveletCoefficient desc, ImageBase input, ImageBase output) {
				ImplWaveletTransformInner.horizontal((WaveletCoefficient_F32)desc,(ImageFloat32)input,(ImageFloat32)output);
				ImplWaveletTransformBorder.horizontal((WaveletCoefficient_F32)desc,(ImageFloat32)input,(ImageFloat32)output);
			}

			@Override
			public void compareResults(WaveletCoefficient desc, ImageBase input , ImageBase expected, ImageBase found ) {
				GecvTesting.assertEqualsGeneric(expected,found,0,1e-4f);
			}
		};

		test.runTests(false);
	}

	@Test
	public void vertical() {
		PermuteWaveletCompare test = new PermuteWaveletCompare(typeInput,typeOutput) {

			@Override
			public void applyValidation(WaveletCoefficient desc, ImageBase input, ImageBase output) {
				ImplWaveletTransformNaive.vertical((WaveletCoefficient_F32)desc,(ImageFloat32)input,(ImageFloat32)output);
			}

			@Override
			public void applyTransform(WaveletCoefficient desc, ImageBase input, ImageBase output) {
				ImplWaveletTransformInner.vertical((WaveletCoefficient_F32)desc,(ImageFloat32)input,(ImageFloat32)output);
				ImplWaveletTransformBorder.vertical((WaveletCoefficient_F32)desc,(ImageFloat32)input,(ImageFloat32)output);
			}

			@Override
			public void compareResults(WaveletCoefficient desc, ImageBase input , ImageBase expected, ImageBase found ) {
				GecvTesting.assertEqualsGeneric(expected,found,0,1e-4f);
			}
		};

		test.runTests(false);
	}

	@Test
	public void horizontalInverse() {
		PermuteWaveletCompare test = new PermuteWaveletCompare(typeInput,typeOutput) {

			@Override
			public void applyValidation(WaveletCoefficient desc, ImageBase input, ImageBase output) {
				ImplWaveletTransformNaive.horizontalInverse((WaveletCoefficient_F32)desc,(ImageFloat32)input,(ImageFloat32)output);
			}

			@Override
			public void applyTransform(WaveletCoefficient desc, ImageBase input, ImageBase output) {
				ImplWaveletTransformInner.horizontalInverse((WaveletCoefficient_F32)desc,(ImageFloat32)input,(ImageFloat32)output);
				ImplWaveletTransformBorder.horizontalInverse((WaveletCoefficient_F32)desc,(ImageFloat32)input,(ImageFloat32)output);
			}

			@Override
			public void compareResults(WaveletCoefficient desc, ImageBase input , ImageBase expected, ImageBase found ) {
				GecvTesting.assertEqualsGeneric(expected,found,0,1e-4f);
			}
		};

		test.runTests(true);
	}

	@Test
	public void verticalInverse() {
		PermuteWaveletCompare test = new PermuteWaveletCompare(typeInput,typeOutput) {

			@Override
			public void applyValidation(WaveletCoefficient desc, ImageBase input, ImageBase output) {
				ImplWaveletTransformNaive.verticalInverse((WaveletCoefficient_F32)desc,(ImageFloat32)input,(ImageFloat32)output);
			}

			@Override
			public void applyTransform(WaveletCoefficient desc, ImageBase input, ImageBase output) {
				ImplWaveletTransformInner.verticalInverse((WaveletCoefficient_F32)desc,(ImageFloat32)input,(ImageFloat32)output);
				ImplWaveletTransformBorder.verticalInverse((WaveletCoefficient_F32)desc,(ImageFloat32)input,(ImageFloat32)output);
			}

			@Override
			public void compareResults(WaveletCoefficient desc, ImageBase input , ImageBase expected, ImageBase found ) {
				GecvTesting.assertEqualsGeneric(expected,found,0,1e-4f);
			}
		};

		test.runTests(true);
	}
}
