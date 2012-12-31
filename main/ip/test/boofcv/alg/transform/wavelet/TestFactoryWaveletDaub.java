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

package boofcv.alg.transform.wavelet;

import boofcv.core.image.border.BorderType;
import boofcv.factory.transform.wavelet.FactoryWaveletDaub;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef_F32;
import boofcv.struct.wavelet.WlCoef_I32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestFactoryWaveletDaub extends CommonFactoryWavelet {

	BorderType borderDefault = BorderType.WRAP;

	BorderType[] borderTypes = new BorderType[]{BorderType.WRAP,BorderType.REFLECT};

	/**
	 * Sees if the DaubJ transform can reconstruct an image.
	 */
	@Test
	public void transform_daubJ_F32() {

		for( int i = 4; i <= 4; i += 2 ) {
			WaveletDescription<WlCoef_F32> desc = FactoryWaveletDaub.daubJ_F32(i);

			checkEncodeDecode_F32(desc);
		}
	}

	/**
	 * Sees if the standard DaubJ wavelets have the expected characteristics
	 */
	@Test
	public void daubJ_F32_forward() {
		for( int i = 4; i <= 4; i += 2 ) {

			// test forward coefficients for the expected properties
			WaveletDescription<WlCoef_F32> desc = FactoryWaveletDaub.daubJ_F32(i);
			WlCoef_F32 forwardCoef = desc.forward;

			double sumScaling = UtilWavelet.sumCoefficients(forwardCoef.scaling);
			double sumWavelet = UtilWavelet.sumCoefficients(forwardCoef.wavelet);

			assertEquals(Math.sqrt(2),sumScaling,1e-4);
			assertEquals(0,sumWavelet,1e-4);

			double energyScaling = UtilWavelet.computeEnergy(forwardCoef.scaling);
			double energyWavelet = UtilWavelet.computeEnergy(forwardCoef.wavelet);

			assertEquals(1,energyScaling,1e-4);
			assertEquals(1,energyWavelet,1e-4);

			int polyOrder = i/2-1;

			checkPolySumToZero(forwardCoef.wavelet, polyOrder,0);

			// should coefficients should be orthogonal
			checkBiorthogonal_F32(desc);
		}
	}

	@Test
	public void transform_biorthogonal_F32() {

		for( BorderType type : borderTypes  )
		{
			for( int i = 5; i <= 5; i += 2 ) {
				WaveletDescription<WlCoef_F32> desc = FactoryWaveletDaub.biorthogonal_F32(i,type);

				checkEncodeDecode_F32(desc);
			}
		}
	}

	@Test
	public void biorthogonal_F32_forward() {

		for( int i = 5; i <= 5; i += 2 ) {

			WlCoef_F32 forward = FactoryWaveletDaub.biorthogonal_F32(i,borderDefault).getForward();

			double sumScaling = UtilWavelet.sumCoefficients(forward.scaling);
			double sumWavelet = UtilWavelet.sumCoefficients(forward.wavelet);

			assertEquals(1,sumScaling,1e-4);
			assertEquals(0,sumWavelet,1e-4);

			double energyScaling = UtilWavelet.computeEnergy(forward.scaling);
			double energyWavelet = UtilWavelet.computeEnergy(forward.wavelet);

			assertTrue(Math.abs(1-energyScaling) > 1e-4);
			assertTrue(Math.abs(1-energyWavelet) > 1e-4);

			int polyOrder = i/2-1;

			checkPolySumToZero(forward.wavelet, polyOrder,-1);
			checkPolySumToZero(forward.scaling, polyOrder,-2);
		}
	}

	@Test
	public void transform_biorthogonal_I32() {

		for( BorderType type : borderTypes ) {
			for( int i = 5; i <= 5; i += 2 ) {
				WaveletDescription<WlCoef_I32> desc = FactoryWaveletDaub.biorthogonal_I32(i,type);

				checkEncodeDecode_I32(desc);
			}
		}
	}

	@Test
	public void biorthogonal_I32_forward() {

		for( int i = 5; i <= 5; i += 2 ) {

			WlCoef_I32 forward = FactoryWaveletDaub.biorthogonal_I32(i,borderDefault).getForward();

			int sumScaling = UtilWavelet.sumCoefficients(forward.scaling)/forward.denominatorScaling;
			assertEquals(1,sumScaling);

			int sumWavelet = UtilWavelet.sumCoefficients(forward.wavelet);
			assertEquals(0,sumWavelet);

			double energyScaling = UtilWavelet.computeEnergy(forward.scaling,forward.denominatorScaling);
			double energyWavelet = UtilWavelet.computeEnergy(forward.wavelet,forward.denominatorWavelet);

			assertTrue(Math.abs(1-energyScaling) > 1e-4);
			assertTrue(Math.abs(1-energyWavelet) > 1e-4);

			int polyOrder = i/2-1;

			checkPolySumToZero(forward.wavelet, polyOrder,-1);
			checkPolySumToZero(forward.scaling, polyOrder,-2);
		}
	}

	@Test
	public void biorthogonal_F32_inverse() {

		for( BorderType type : borderTypes ) {
			for( int i = 5; i <= 5; i += 2 ) {
				WaveletDescription<WlCoef_F32> desc = FactoryWaveletDaub.biorthogonal_F32(i,type );

				checkBiorthogonal_F32(desc);
			}
		}
	}

	@Test
	public void biorthogonal_I32_inverse() {

		for( BorderType type : borderTypes  ) {
			for( int i = 5; i <= 5; i += 2 ) {
				WaveletDescription<WlCoef_I32> desc = FactoryWaveletDaub.biorthogonal_I32(i,type);

				checkBiorthogonal_I32(desc);
			}
		}
	}


}
