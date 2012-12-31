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

import boofcv.factory.transform.wavelet.FactoryWaveletHaar;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef_F32;
import boofcv.struct.wavelet.WlCoef_I32;
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
		WaveletDescription<WlCoef_F32> desc = FactoryWaveletHaar.generate(false,32);
		checkEncodeDecode_F32(desc);
	}

	@Test
	public void checkProperties_F32() {
		WaveletDescription<WlCoef_F32> desc = FactoryWaveletHaar.generate(false,32);
		WlCoef_F32 coef = desc.getForward();

		double energyScaling = UtilWavelet.computeEnergy(coef.scaling);
		double energyWavelet = UtilWavelet.computeEnergy(coef.wavelet);
		assertEquals(1,energyScaling,1e-4);
		assertEquals(1,energyWavelet,1e-4);

		double sumWavelet = UtilWavelet.sumCoefficients(coef.wavelet);
		assertEquals(0,sumWavelet,1e-4);

		checkBiorthogonal_F32(desc);
	}

	@Test
	public void checkProperties_I32() {
		WaveletDescription<WlCoef_I32> desc = FactoryWaveletHaar.generate(true,32);
		WlCoef_I32 coef = desc.getForward();

		double energyScaling = UtilWavelet.computeEnergy(coef.scaling,coef.denominatorScaling);
		double energyWavelet = UtilWavelet.computeEnergy(coef.wavelet,coef.denominatorWavelet);

		assertEquals(energyWavelet,energyScaling,1e-4);

		double sumWavelet = UtilWavelet.sumCoefficients(coef.wavelet);
		assertEquals(0,sumWavelet,1e-4);

		checkBiorthogonal_I32(desc);
	}
}
