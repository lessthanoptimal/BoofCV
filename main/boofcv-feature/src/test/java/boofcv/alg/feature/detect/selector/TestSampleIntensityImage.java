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

package boofcv.alg.feature.detect.selector;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestSampleIntensityImage extends BoofStandardJUnit {

	GrayF32 image = new GrayF32(30, 40);

	public TestSampleIntensityImage() {
		ImageMiscOps.fillUniform(image, rand, -1, 1);
	}

	@Nested
	public class I16 {
		@Test
		void sample() {
			var sampler = new SampleIntensityImage.I16();
			float expected = image.get(3, 5);
			float found = sampler.sample(image, -1, new Point2D_I16((short)3, (short)5));
			assertEquals(expected, found, 1e-8f);
		}

		@Test
		void get() {
			var sampler = new SampleIntensityImage.I16();
			var p = new Point2D_I16((short)3, (short)5);
			assertEquals(3, sampler.getX(p));
			assertEquals(5, sampler.getY(p));
		}
	}

	@Nested
	public class F32 {
		@Test
		void sample() {
			var sampler = new SampleIntensityImage.F32();
			float expected = image.get(3, 5);
			float found = sampler.sample(image, -1, new Point2D_F32(3.2f, 5.6f));
			assertEquals(expected, found, 1e-8f);
		}

		@Test
		void get() {
			var sampler = new SampleIntensityImage.F32();
			var p = new Point2D_F32(3.2f, 5.6f);
			assertEquals(3, sampler.getX(p));
			assertEquals(5, sampler.getY(p));
		}
	}

	@Nested
	public class F64 {
		@Test
		void sample() {
			var sampler = new SampleIntensityImage.F64();
			float expected = image.get(3, 5);
			float found = sampler.sample(image, -1, new Point2D_F64(3.2, 5.6));
			assertEquals(expected, found, 1e-8f);
		}

		@Test
		void get() {
			var sampler = new SampleIntensityImage.F64();
			var p = new Point2D_F64(3.2, 5.6);
			assertEquals(3, sampler.getX(p));
			assertEquals(5, sampler.getY(p));
		}
	}
}
