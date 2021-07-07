/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.denoise;

import boofcv.alg.denoise.wavelet.UtilDenoiseWavelet;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestUtilDenoiseWavelet extends BoofStandardJUnit {

	int width = 20;
	int height = 30;

	@Test void estimateNoiseStdDev() {
		GrayF32 image = new GrayF32(width,height);

		double sigma = 12;
		ImageMiscOps.addGaussian(image,rand,sigma,-10000,10000);

		double found = UtilDenoiseWavelet.estimateNoiseStdDev(image,null);

		assertEquals(sigma,found,1);
	}

	@Test void universalThreshold() {

		GrayF32 image = new GrayF32(width,height);

		double sigma = 12;

		double found = UtilDenoiseWavelet.universalThreshold(image,sigma);

		double expected = sigma*Math.sqrt(2.0*Math.log(height));

		assertEquals(expected,found,1e-4);
	}
}
