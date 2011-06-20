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

package gecv.alg.wavelet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestFactoryWaveletCoiflet extends CommonFactoryWavelet {

	@Test
	public void transform_generate_F32() {

		for( int i = 6; i <= 6; i += 2 ) {
			WaveletDesc_F32 desc = FactoryWaveletCoiflet.generate_F32(i);

			checkEncodeDecode_F32(desc,desc);
		}
	}

	/**
	 * Sees if the standard CoifI wavelets have the expected characteristics
	 */
	@Test
	public void generate_F32() {

		for( int i = 6; i <= 6; i += 2 ) {

			WaveletDesc_F32 desc = FactoryWaveletCoiflet.generate_F32(i);

			double sumScaling = UtilWavelet.sumCoefficients(desc.scaling);
			double sumWavelet = UtilWavelet.sumCoefficients(desc.wavelet);

			assertEquals(Math.sqrt(2),sumScaling,1e-4);
			assertEquals(0,sumWavelet,1e-4);

			double energyScaling = UtilWavelet.computeEnergy(desc.scaling);
			double energyWavelet = UtilWavelet.computeEnergy(desc.wavelet);

			assertEquals(1,energyScaling,1e-4);
			assertEquals(1,energyWavelet,1e-4);

			int polyOrder = i/2-1;

			checkPolySumToZero(desc.scaling, polyOrder,-2);
			checkPolySumToZero(desc.wavelet, polyOrder-1,0);

			for( int offset = 0; offset <= 4; offset += 2 ) {
				checkBiorthogonal(offset,desc.scaling,desc.offsetScaling,desc.scaling,desc.offsetScaling);
				checkBiorthogonal(offset,desc.wavelet,desc.offsetWavelet,desc.wavelet,desc.offsetWavelet);
			}
		}
	}
}
