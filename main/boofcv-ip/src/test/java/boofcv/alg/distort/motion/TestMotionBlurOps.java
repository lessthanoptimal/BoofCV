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

package boofcv.alg.distort.motion;

import boofcv.alg.filter.kernel.KernelMath;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMotionBlurOps extends BoofStandardJUnit {
	/**
	 * Really basic tests. Does not guarantee the PSF is correct.
	 */
	@Test void linearMotionPsf() {
		Kernel2D_F32 a = MotionBlurOps.linearMotionPsf(10, 0);
		Kernel2D_F32 b = MotionBlurOps.linearMotionPsf(20, 0);

		// Basic property of the kernel
		assertEquals(a.offset, a.width/2);
		assertEquals(b.offset, b.width/2);

		// more motion should be more blur
		assertTrue(a.width < b.width);

		// blur should sum up to one
		assertEquals(1.0f, KernelMath.sum(a), UtilEjml.TEST_F32);
		assertEquals(1.0f, KernelMath.sum(b), UtilEjml.TEST_F32);

		// add rotation
		Kernel2D_F32 c = MotionBlurOps.linearMotionPsf(20, 0.2);
		assertEquals(b.width, c.width);
		assertEquals(1.0f, KernelMath.sum(c), UtilEjml.TEST_F32);
	}
}
