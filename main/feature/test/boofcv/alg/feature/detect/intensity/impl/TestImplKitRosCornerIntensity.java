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

/**
 * @author Peter Abeles
 */
public class TestImplKitRosCornerIntensity
{
	@Test
	public void test_F32() {
		Test_F32 a = new Test_F32();
		a.genericTests();
	}

	@Test
	public void test_S16() {
		Test_S16 a = new Test_S16();
		a.genericTests();
	}

	public static class Test_F32 extends GenericCornerIntensityGradientTests
	{
		@Test
		public void genericTests() {
			performAllTests();
		}

		@Override
		public void computeIntensity( GrayF32 intensity ) {
			ImplKitRosCornerIntensity.process(intensity,derivX_F32,derivY_F32,derivXX_F32,derivYY_F32,derivXY_F32);
		}
	}

	public static class Test_S16 extends GenericCornerIntensityGradientTests
	{
		@Test
		public void genericTests() {
			performAllTests();
		}

		public void computeIntensity( GrayF32 intensity ) {
			ImplKitRosCornerIntensity.process(intensity,derivX_I16,derivY_I16,derivXX_I16,derivYY_I16,derivXY_I16);
		}
	}


}
