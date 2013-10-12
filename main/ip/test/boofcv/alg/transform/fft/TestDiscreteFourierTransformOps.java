/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.fft;

import boofcv.struct.image.ImageFloat64;
import boofcv.struct.image.InterleavedF32;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestDiscreteFourierTransformOps {

	@Test
	public void isPowerOf2() {
		assertFalse(DiscreteFourierTransformOps.isPowerOf2(1));
		assertTrue(DiscreteFourierTransformOps.isPowerOf2(2));
		assertFalse(DiscreteFourierTransformOps.isPowerOf2(3));
		assertTrue(DiscreteFourierTransformOps.isPowerOf2(4));
		assertFalse(DiscreteFourierTransformOps.isPowerOf2(5));
		assertFalse(DiscreteFourierTransformOps.isPowerOf2(6));
		assertTrue(DiscreteFourierTransformOps.isPowerOf2(8));
		assertFalse(DiscreteFourierTransformOps.isPowerOf2(31));
		assertTrue(DiscreteFourierTransformOps.isPowerOf2(32));
		assertTrue(DiscreteFourierTransformOps.isPowerOf2(1024));
	}

	@Test
	public void nextPow2() {
		assertEquals(2,DiscreteFourierTransformOps.nextPow2(1));
		assertEquals(2,DiscreteFourierTransformOps.nextPow2(2));
		assertEquals(4,DiscreteFourierTransformOps.nextPow2(3));
		assertEquals(4,DiscreteFourierTransformOps.nextPow2(4));
		assertEquals(1024,DiscreteFourierTransformOps.nextPow2(767));
		assertEquals(1024,DiscreteFourierTransformOps.nextPow2(1024));
	}

	@Test
	public void checkImageArguments() {
		DiscreteFourierTransformOps.checkImageArguments(new ImageFloat64(10,12),new InterleavedF32(10,12,2));

		// test negative cases
		try {
			DiscreteFourierTransformOps.checkImageArguments(new ImageFloat64(10,12),new InterleavedF32(10,12,1));
			fail("Should have thrown an exception");
		} catch( IllegalArgumentException ignore ){}
		try {
			DiscreteFourierTransformOps.checkImageArguments(new ImageFloat64(10,12),new InterleavedF32(20,12,2));
			fail("Should have thrown an exception");
		} catch( IllegalArgumentException ignore ){}
		try {
			DiscreteFourierTransformOps.checkImageArguments(new ImageFloat64(10,12),new InterleavedF32(10,14,2));
			fail("Should have thrown an exception");
		} catch( IllegalArgumentException ignore ){}
	}

	@Test
	public void centerZeroFrequency() {
		fail("implement");
	}

	@Test
	public void magnitude() {
		fail("implement");
	}

	@Test
	public void phase() {
		fail("implement");
	}

	@Test
	public void realToComplex() {
		fail("implement");
	}

	@Test
	public void multiplyRealComplex() {
		fail("implement");
	}
}
