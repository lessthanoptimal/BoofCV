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

package boofcv.alg.feature.detect.interest;

import boofcv.BoofTesting;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestUnrollSiftScaleSpaceGradient extends BoofStandardJUnit {

	/**
	 * Manually compute the precomputed set of scaled derivatives and see if they are the same
	 */
	@Test void setImage() {
		GrayF32 image = new GrayF32(640, 480);
		GImageMiscOps.fillUniform(image, rand, 0, 200);
		
		UnrollSiftScaleSpaceGradient alg = new UnrollSiftScaleSpaceGradient(new SiftScaleSpace(-1, 3, 3, 2));
		alg.setImage(image);

		SiftScaleSpace ss = new SiftScaleSpace(-1, 3, 3, 2);
		ss.initialize(image);

		GrayF32 derivX = new GrayF32(640, 480);
		GrayF32 derivY = new GrayF32(640, 480);

		int total = 0;
		do {
			for (int i = 0; i < ss.getNumScales(); i++, total++) {
				GrayF32 scaleImage = ss.getImageScale(i);

				derivX.reshape(scaleImage.width, scaleImage.height);
				derivY.reshape(scaleImage.width, scaleImage.height);

				GImageDerivativeOps.gradient(DerivativeType.THREE, scaleImage, derivX, derivY, BorderType.EXTENDED);

				UnrollSiftScaleSpaceGradient.ImageScale found = alg.usedScales.get(total);

				BoofTesting.assertEquals(derivX, found.derivX, 1e-4);
				BoofTesting.assertEquals(derivY, found.derivY, 1e-4);
				assertEquals(ss.computeSigmaScale(i), found.sigma, 1e-4);
				assertEquals(image.width/(double)scaleImage.width, found.imageToInput, 1e-4);
			}
		} while (ss.computeNextOctave());
	}

	@Test void lookup() {
		SiftScaleSpace ss = new SiftScaleSpace(-1, 3, 3, 2);

		UnrollSiftScaleSpaceGradient alg = new UnrollSiftScaleSpaceGradient(ss);

		alg.setImage(new GrayF32(640, 480));

		assertEquals(ss.computeSigmaScale(-1, 0), alg.lookup(0).sigma, 1e-8);
		assertEquals(ss.computeSigmaScale(0, 0), alg.lookup(2).sigma, 1e-8);
		assertEquals(ss.computeSigmaScale(0, 2), alg.lookup(3).sigma, 1e-8);
		assertEquals(ss.computeSigmaScale(3, 2), alg.lookup(200).sigma, 1e-8);
	}
}
