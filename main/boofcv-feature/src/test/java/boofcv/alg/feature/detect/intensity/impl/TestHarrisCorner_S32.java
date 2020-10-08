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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.alg.feature.detect.intensity.GenericCornerIntensityGradientTests;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestHarrisCorner_S32 extends BoofStandardJUnit {
	@Test
	void checkScore() {
		float kappa = 0.04f;
		HarrisCorner_S32 alg = new HarrisCorner_S32(kappa);

		int XX = 50;
		int XY = 70;
		int YY = 80;

		float expected = XX*YY-XY*XY - kappa*(float)Math.pow(XX+YY,2.0);
		assertEquals(expected,alg.compute(XX,XY,YY), UtilEjml.TEST_F32);
	}

	@Nested
	public class SingleThread extends GenericCornerIntensityGradientTests {
		ImplSsdCorner_S16 detector = new ImplSsdCorner_S16(1,new HarrisCorner_S32(0.04f));

		@Test
		void genericTests() {
			performAllTests();
		}

		@Override
		public void computeIntensity( GrayF32 intensity ) {
			detector.process(derivX_I16,derivY_I16,intensity);
		}
	}

	@Nested
	public class MultiThread extends GenericCornerIntensityGradientTests {
		ImplSsdCorner_S16_MT detector = new ImplSsdCorner_S16_MT(1,new HarrisCorner_S32(0.04f));

		@Test
		void genericTests() {
			performAllTests();
		}

		@Override
		public void computeIntensity( GrayF32 intensity ) {
			detector.process(derivX_I16,derivY_I16,intensity);
		}
	}
}
