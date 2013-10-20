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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageFloat64;
import boofcv.struct.image.InterleavedF32;
import boofcv.testing.BoofTesting;
import org.ddogleg.complex.ComplexMath64F;
import org.ejml.data.Complex64F;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestDiscreteFourierTransformOps {

	Random rand = new Random(234);

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

	@Test
	public void multiplyComplex() {
		InterleavedF32 complexA = new InterleavedF32(10,20,2);
		InterleavedF32 complexB = new InterleavedF32(10,20,2);
		InterleavedF32 complexC = new InterleavedF32(10,20,2);

		ImageMiscOps.fillUniform(complexA,rand,-5,5);
		ImageMiscOps.fillUniform(complexB,rand,-5,5);

		multiplyComplex(complexA,complexB,complexC);

		BoofTesting.checkSubImage(this,"multiplyComplex",false,complexA,complexB,complexC);
	}

	public void multiplyComplex( InterleavedF32 complexA , InterleavedF32 complexB , InterleavedF32 complexC ) {
		DiscreteFourierTransformOps.multiplyComplex(complexA,complexB,complexC);

		Complex64F expected = new Complex64F();

		for( int y = 0; y < complexA.height; y++ ) {
			for( int x = 0; x < complexA.width; x++ ) {
				Complex64F a = new Complex64F(complexA.getBand(x,y,0),complexA.getBand(x,y,1));
				Complex64F b = new Complex64F(complexB.getBand(x,y,0),complexB.getBand(x,y,1));

				ComplexMath64F.mult(a,b,expected);

				assertEquals(expected.getReal(),complexC.getBand(x,y,0),1e-4);
				assertEquals(expected.getImaginary(),complexC.getBand(x,y,1),1e-4);
			}
		}
	}
}
