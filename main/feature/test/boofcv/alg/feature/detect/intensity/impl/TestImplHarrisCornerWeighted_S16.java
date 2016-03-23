/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImplHarrisCornerWeighted_S16 extends GenericCornerIntensityGradientTests {
	ImplHarrisCornerWeighted_S16 detector = new ImplHarrisCornerWeighted_S16(1,0.04f);

	@Test
	public void genericTests() {
		performAllTests();
	}

	@Override
	public void computeIntensity( GrayF32 intensity ) {
		detector.process(derivX_I16, derivY_I16,intensity);
	}

	@Test
	public void checkOverflow() {
		detector.totalXX = (1<<18)+10;
		detector.totalYY = (1<<20)+50;
		detector.totalXY = (1<<16)+5;

		long totalXX = detector.totalXX;
		long totalYY = detector.totalYY;
		long totalXY = detector.totalXY;

		float trace = totalXX + totalYY;
		float expected = (totalXX * totalYY - totalXY * totalXY) - detector.kappa * trace*trace;

		float found = detector.computeResponse();
		assertEquals(expected,found,1e-4);
	}
}
