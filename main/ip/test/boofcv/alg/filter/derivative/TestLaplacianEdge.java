/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.filter.derivative;

import boofcv.alg.misc.ImageTestingOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestLaplacianEdge {

	Random rand = new Random(0xfeed);

	private final int width = 4;
	private final int height = 5;

	@Test
	public void checkInputShape() {
		BoofTesting.checkImageDimensionValidation(new LaplacianEdge(), 2);
	}

	@Test
	public void process_I8() {
		ImageUInt8 img = new ImageUInt8(width, height);
		ImageTestingOps.randomize(img, rand, 0, 100);

		ImageSInt16 deriv = new ImageSInt16(width, height);
		BoofTesting.checkSubImage(this, "process_I8", true, img, deriv);
	}

	public void process_I8(ImageUInt8 img, ImageSInt16 deriv) {
		LaplacianEdge.process(img, deriv);

		int expected = -4 * img.get(1, 1) + img.get(0, 1) + img.get(1, 0)
				+ img.get(2, 1) + img.get(1, 2);

		assertEquals(expected, deriv.get(1, 1));
	}

	@Test
	public void process_F32() {
		ImageFloat32 img = new ImageFloat32(width, height);
		ImageTestingOps.randomize(img, rand, 0, 1);

		ImageFloat32 deriv = new ImageFloat32(width, height);
		BoofTesting.checkSubImage(this, "process_F32", true, img, deriv);
	}

	public void process_F32(ImageFloat32 img, ImageFloat32 deriv) {
		LaplacianEdge.process(img, deriv);

		float expected = -4*img.get(1, 1) + img.get(0, 1) + img.get(1, 0)
				+ img.get(2, 1) + img.get(1, 2);

		assertEquals(expected, deriv.get(1, 1), 1e-5);
	}
}
