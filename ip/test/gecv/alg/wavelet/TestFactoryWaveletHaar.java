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

import gecv.struct.wavelet.WaveletCoefficient_F32;
import gecv.struct.wavelet.WaveletCoefficient_I32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestFactoryWaveletHaar extends CommonFactoryWavelet {



	/**
	 * Encode and decode an image using the Haar wavelet
	 */
	@Test
	public void transform_F32() {
		WaveletCoefficient_F32 desc = FactoryWaveletHaar.generate_F32();
		checkEncodeDecode_F32(desc,desc);
	}

	@Test
	public void checkProperties_F32() {
		WaveletCoefficient_F32 desc = FactoryWaveletHaar.generate_F32();

		double energyScaling = UtilWavelet.computeEnergy(desc.scaling);
		double energyWavelet = UtilWavelet.computeEnergy(desc.wavelet);
		assertEquals(1,energyScaling,1e-4);
		assertEquals(1,energyWavelet,1e-4);

		double sumWavelet = UtilWavelet.sumCoefficients(desc.wavelet);
		assertEquals(0,sumWavelet,1e-4);

		for( int offset = 0; offset <= 4; offset += 2 ) {
			checkBiorthogonal(offset,desc.scaling,desc.offsetScaling,desc.scaling,desc.offsetScaling);
			checkBiorthogonal(offset,desc.wavelet,desc.offsetWavelet,desc.wavelet,desc.offsetWavelet);
		}
	}

	@Test
	public void checkProperties_I32() {
		WaveletCoefficient_I32 desc = FactoryWaveletHaar.generate_I32();

		double energyScaling = UtilWavelet.computeEnergy(desc.scaling,desc.denominatorScaling);
		double energyWavelet = UtilWavelet.computeEnergy(desc.wavelet,desc.denominatorWavelet);

		assertEquals(energyWavelet,energyScaling,1e-4);

		double sumWavelet = UtilWavelet.sumCoefficients(desc.wavelet);
		assertEquals(0,sumWavelet,1e-4);

		for( int offset = 0; offset <= 4; offset += 2 ) {
			checkBiorthogonal(offset,desc.scaling,desc.offsetScaling,desc.denominatorScaling,
					desc.scaling,desc.offsetScaling,desc.denominatorScaling, false);
			checkBiorthogonal(offset,desc.wavelet,desc.offsetWavelet,desc.denominatorWavelet,
					desc.wavelet,desc.offsetWavelet,desc.denominatorWavelet, false);
		}
	}
}
