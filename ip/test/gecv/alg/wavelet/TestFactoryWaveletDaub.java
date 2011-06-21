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

import gecv.struct.wavelet.WaveletDesc_F32;
import gecv.struct.wavelet.WaveletDesc_I32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestFactoryWaveletDaub extends CommonFactoryWavelet {


	/**
	 * Sees if the DaubJ transform can reconstruct an image.
	 */
	@Test
	public void transform_standard_F32() {

		for( int i = 4; i <= 4; i += 2 ) {
			WaveletDesc_F32 desc = FactoryWaveletDaub.standard_F32(i);

			checkEncodeDecode_F32(desc,desc);
		}
	}

	/**
	 * Sees if the standard DaubJ wavelets have the expected characteristics
	 */
	@Test
	public void standard_F32() {
		for( int i = 4; i <= 4; i += 2 ) {

			WaveletDesc_F32 desc = FactoryWaveletDaub.standard_F32(i);

			double sumScaling = UtilWavelet.sumCoefficients(desc.scaling);
			double sumWavelet = UtilWavelet.sumCoefficients(desc.wavelet);

			assertEquals(Math.sqrt(2),sumScaling,1e-4);
			assertEquals(0,sumWavelet,1e-4);

			double energyScaling = UtilWavelet.computeEnergy(desc.scaling);
			double energyWavelet = UtilWavelet.computeEnergy(desc.wavelet);

			assertEquals(1,energyScaling,1e-4);
			assertEquals(1,energyWavelet,1e-4);

			int polyOrder = i/2-1;

			checkPolySumToZero(desc.wavelet, polyOrder,0);

			for( int offset = 0; offset <= 4; offset += 2 ) {
				checkBiorthogonal(offset,desc.scaling,desc.offsetScaling,desc.scaling,desc.offsetScaling);
				checkBiorthogonal(offset,desc.wavelet,desc.offsetWavelet,desc.wavelet,desc.offsetWavelet);
			}
		}
	}

	@Test
	public void transform_biorthogonal_F32() {

		for( int i = 5; i <= 5; i += 2 ) {
			WaveletDesc_F32 forward = FactoryWaveletDaub.biorthogonal_F32(i);
			WaveletDesc_F32 inverse = FactoryWaveletDaub.biorthogonalInv_F32(i);

			checkEncodeDecode_F32(forward,inverse);
		}
	}

	@Test
	public void biorthogonal_F32() {

		for( int i = 5; i <= 5; i += 2 ) {

			WaveletDesc_F32 desc = FactoryWaveletDaub.biorthogonal_F32(i);

			double sumScaling = UtilWavelet.sumCoefficients(desc.scaling);
			double sumWavelet = UtilWavelet.sumCoefficients(desc.wavelet);

			assertEquals(1,sumScaling,1e-4);
			assertEquals(0,sumWavelet,1e-4);

			double energyScaling = UtilWavelet.computeEnergy(desc.scaling);
			double energyWavelet = UtilWavelet.computeEnergy(desc.wavelet);

			assertTrue(Math.abs(1-energyScaling) > 1e-4);
			assertTrue(Math.abs(1-energyWavelet) > 1e-4);

			int polyOrder = i/2-1;

			checkPolySumToZero(desc.wavelet, polyOrder,-1);
			checkPolySumToZero(desc.scaling, polyOrder,-2);
		}
	}

	@Test
	public void transform_biorthogonal_I32() {

		for( int i = 5; i <= 5; i += 2 ) {
			WaveletDesc_I32 forward = FactoryWaveletDaub.biorthogonal_I32(i);
			WaveletDesc_I32 inverse = FactoryWaveletDaub.biorthogonalInv_I32(i);

			checkEncodeDecode_I32(forward,inverse);
		}
	}

	@Test
	public void biorthogonal_I32() {

		for( int i = 5; i <= 5; i += 2 ) {

			WaveletDesc_I32 desc = FactoryWaveletDaub.biorthogonal_I32(i);

			int sumScaling = UtilWavelet.sumCoefficients(desc.scaling)/desc.denominatorScaling;
			assertEquals(1,sumScaling);

			int sumWavelet = UtilWavelet.sumCoefficients(desc.wavelet);
			assertEquals(0,sumWavelet);

			double energyScaling = UtilWavelet.computeEnergy(desc.scaling,desc.denominatorScaling);
			double energyWavelet = UtilWavelet.computeEnergy(desc.wavelet,desc.denominatorWavelet);

			assertTrue(Math.abs(1-energyScaling) > 1e-4);
			assertTrue(Math.abs(1-energyWavelet) > 1e-4);

			int polyOrder = i/2-1;

			checkPolySumToZero(desc.wavelet, polyOrder,-1);
			checkPolySumToZero(desc.scaling, polyOrder,-2);
		}
	}

	@Test
	public void biorthogonalInv_F32() {

		for( int i = 5; i <= 5; i += 2 ) {
			WaveletDesc_F32 desc = FactoryWaveletDaub.biorthogonal_F32(i);
			WaveletDesc_F32 descInv = FactoryWaveletDaub.biorthogonalInv_F32(i);

			for( int offset = 0; offset <= 4; offset += 2 ) {
				checkBiorthogonal(offset,desc.scaling,desc.offsetScaling,descInv.scaling,descInv.offsetScaling);
				checkBiorthogonal(offset,desc.wavelet,desc.offsetWavelet,descInv.wavelet,descInv.offsetWavelet);
			}
		}
	}

	@Test
	public void biorthogonalInv_I32() {
		
		for( int i = 5; i <= 5; i += 2 ) {
			WaveletDesc_I32 desc = FactoryWaveletDaub.biorthogonal_I32(i);
			WaveletDesc_I32 descInv = FactoryWaveletDaub.biorthogonalInv_I32(i);

			for( int offset = 0; offset <= 4; offset += 2 ) {
				checkBiorthogonal(offset,desc.scaling,desc.offsetScaling,desc.denominatorScaling,
						descInv.scaling,descInv.offsetScaling,descInv.denominatorScaling, true);
				checkBiorthogonal(offset,desc.wavelet,desc.offsetWavelet,desc.denominatorWavelet,
						descInv.wavelet,descInv.offsetWavelet,descInv.denominatorWavelet, true);
			}
		}
	}


}
