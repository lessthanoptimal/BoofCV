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

import boofcv.factory.transform.wavelet.FactoryWaveletCoiflet;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef_F32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestFactoryWaveletCoiflet extends CommonFactoryWavelet {

	@Test
	public void transform_generate_F32() {

		for( int i = 6; i <= 6; i += 2 ) {
			WaveletDescription<WlCoef_F32> desc = FactoryWaveletCoiflet.generate_F32(i);

			checkEncodeDecode_F32(desc);
		}
	}

	/**
	 * Sees if the standard CoifI wavelets have the expected characteristics
	 */
	@Test
	public void generate_F32() {

		for( int i = 6; i <= 6; i += 2 ) {

			WaveletDescription<WlCoef_F32> desc = FactoryWaveletCoiflet.generate_F32(i);
			WlCoef_F32 coef = desc.forward;

			double sumScaling = UtilWavelet.sumCoefficients(coef.scaling);
			double sumWavelet = UtilWavelet.sumCoefficients(coef.wavelet);

			assertEquals(Math.sqrt(2),sumScaling,1e-4);
			assertEquals(0,sumWavelet,1e-4);

			double energyScaling = UtilWavelet.computeEnergy(coef.scaling);
			double energyWavelet = UtilWavelet.computeEnergy(coef.wavelet);

			assertEquals(1,energyScaling,1e-4);
			assertEquals(1,energyWavelet,1e-4);

			int polyOrder = i/2-1;

			checkPolySumToZero(coef.scaling, polyOrder,-2);
			checkPolySumToZero(coef.wavelet, polyOrder-1,0);

			checkBiorthogonal_F32(desc);
		}
	}
}
