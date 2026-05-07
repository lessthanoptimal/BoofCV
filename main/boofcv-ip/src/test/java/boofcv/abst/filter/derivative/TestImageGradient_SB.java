/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.derivative;

import boofcv.alg.filter.derivative.*;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestImageGradient_SB extends BoofStandardJUnit {

	int width = 20;
	int height = 30;

	/**
	 * See if it throws an exception or not
	 */
	@Test void testNoException() {
		GrayF32 input = new GrayF32(width, height);
		GrayF32 derivX = new GrayF32(width, height);
		GrayF32 derivY = new GrayF32(width, height);

		var alg = new ImageGradient_SB.Sobel<>(GrayF32.class, GrayF32.class);

		alg.process(input, derivX, derivY);
	}

	@Nested class Sobel {
		// make sure its set to one when floating point image and the actual divisor for others
		@Test void gradientDivisor() {
			assertEquals(1.0f, new ImageGradient_SB.Sobel<>(GrayF32.class, GrayF32.class).divisor());
			assertEquals(GradientSobel.divisor, new ImageGradient_SB.Sobel<>(GrayU8.class, GrayS16.class).divisor());
		}
	}

	@Nested class Prewitt {
		@Test void gradientDivisor() {
			assertEquals(1.0f, new ImageGradient_SB.Prewitt<>(GrayF32.class, GrayF32.class).divisor());
			assertEquals(GradientPrewitt.divisor, new ImageGradient_SB.Prewitt<>(GrayU8.class, GrayS16.class).divisor());
		}
	}

	@Nested class Scharr {
		@Test void gradientDivisor() {
			assertEquals(1.0f, new ImageGradient_SB.Scharr<>(GrayF32.class, GrayF32.class).divisor());
			assertEquals(GradientScharr.divisor, new ImageGradient_SB.Scharr<>(GrayU8.class, GrayS16.class).divisor());
		}
	}

	@Nested class Three {
		@Test void gradientDivisor() {
			assertEquals(1.0f, new ImageGradient_SB.Three<>(GrayF32.class, GrayF32.class).divisor());
			assertEquals(GradientThree.divisor, new ImageGradient_SB.Three<>(GrayU8.class, GrayS16.class).divisor());
		}
	}

	@Nested class Two0 {
		@Test void gradientDivisor() {
			assertEquals(1.0f, new ImageGradient_SB.Two0<>(GrayF32.class, GrayF32.class).divisor());
			assertEquals(GradientTwo0.divisor, new ImageGradient_SB.Two0<>(GrayU8.class, GrayS16.class).divisor());
		}
	}

	@Nested class Two1 {
		@Test void gradientDivisor() {
			assertEquals(1.0f, new ImageGradient_SB.Two1<>(GrayF32.class, GrayF32.class).divisor());
			assertEquals(GradientTwo1.divisor, new ImageGradient_SB.Two1<>(GrayU8.class, GrayS16.class).divisor());
		}
	}
}
