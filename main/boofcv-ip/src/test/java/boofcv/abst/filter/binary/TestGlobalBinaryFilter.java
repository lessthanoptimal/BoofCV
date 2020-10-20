/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.binary;

import boofcv.BoofTesting;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */
class TestGlobalBinaryFilter extends BoofStandardJUnit {
	Class[] imageTypes = new Class[]{GrayU8.class, GrayF32.class};

	@Nested
	class Entropy {
		@Test
		void compare() {
			for( Class type : imageTypes ) {

				ImageGray input = GeneralizedImageOps.createSingleBand(type, 30, 40);
				GrayU8 found = new GrayU8(30,40);
				GrayU8 expected = new GrayU8(30,40);

				GImageMiscOps.fillUniform(input, rand, 0, 200);

				GlobalBinaryFilter.Entropy alg =
						new GlobalBinaryFilter.Entropy<>(0,255,1.0,true, ImageType.single(type));

				alg.process(input,found);
				double threshold = GThresholdImageOps.computeEntropy(input, 0, 255);
				GThresholdImageOps.threshold(input,expected,threshold,true);

				BoofTesting.assertEquals(found, expected, 0);
			}
		}
	}

	@Nested
	class Huang {
		@Test
		void compare() {
			for( Class type : imageTypes ) {

				ImageGray input = GeneralizedImageOps.createSingleBand(type, 30, 40);
				GrayU8 found = new GrayU8(30,40);
				GrayU8 expected = new GrayU8(30,40);

				GImageMiscOps.fillUniform(input, rand, 0, 200);

				GlobalBinaryFilter.Huang alg =
						new GlobalBinaryFilter.Huang<>(0,255,1.0,true, ImageType.single(type));

				alg.process(input,found);
				double threshold = GThresholdImageOps.computeHuang(input, 0, 255);
				GThresholdImageOps.threshold(input,expected,threshold,true);

				BoofTesting.assertEquals(found, expected, 0);
			}
		}
	}

	@Nested
	class Li {
		@Test
		void compare() {
			for( Class type : imageTypes ) {

				ImageGray input = GeneralizedImageOps.createSingleBand(type, 30, 40);
				GrayU8 found = new GrayU8(30,40);
				GrayU8 expected = new GrayU8(30,40);

				GImageMiscOps.fillUniform(input, rand, 0, 200);

				GlobalBinaryFilter.Li alg =
						new GlobalBinaryFilter.Li<>(0,255,1.0,true, ImageType.single(type));

				alg.process(input,found);
				double threshold = GThresholdImageOps.computeLi(input, 0, 255);
				GThresholdImageOps.threshold(input,expected,threshold,true);

				BoofTesting.assertEquals(found, expected, 0);
			}
		}
	}

	@Nested
	class Otsu {
		@Test
		void compare() {
			for( Class type : imageTypes ) {

				ImageGray input = GeneralizedImageOps.createSingleBand(type, 30, 40);
				GrayU8 found = new GrayU8(30,40);
				GrayU8 expected = new GrayU8(30,40);

				GImageMiscOps.fillUniform(input, rand, 0, 200);

				GlobalBinaryFilter.Otsu alg =
						new GlobalBinaryFilter.Otsu<>(0,255,1.0,true, ImageType.single(type));

				alg.process(input,found);
				double threshold = GThresholdImageOps.computeOtsu(input, 0, 255);
				GThresholdImageOps.threshold(input,expected,threshold,true);

				BoofTesting.assertEquals(found, expected, 0);
			}
		}
	}
}
