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
import boofcv.struct.image.ImageFloat32;
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
	public void shiftZeroFrequency() {
		InterleavedF32 complex;

		// test even images first
		complex = new InterleavedF32(4,6,2);
		shiftZeroFrequency(complex);
		BoofTesting.checkSubImage(this, "shiftZeroFrequency", false, complex);

		// test odd images now
		complex = new InterleavedF32(5,7,2);
		shiftZeroFrequency(complex);
		BoofTesting.checkSubImage(this, "shiftZeroFrequency", false, complex);
	}

	public void shiftZeroFrequency( InterleavedF32 input ) {
		ImageMiscOps.fillUniform(input,rand,-5,5);

		InterleavedF32 original = input.clone();

		// corners should be at zero now
		DiscreteFourierTransformOps.shiftZeroFrequency(input,true);

		int hw = input.width/2;
		int hh = input.height/2;
		int w = input.width;
		int h = input.height;

		assertEquals(original.getBand(0,0,0),input.getBand(hw,hh,0),1e-4);
		assertEquals(original.getBand(0,0,1),input.getBand(hw,hh,1),1e-4);

		assertEquals(original.getBand(w-1,h-1,0),input.getBand(hw-1,hh-1,0),1e-4);
		assertEquals(original.getBand(w-1,h-1,1),input.getBand(hw-1,hh-1,1),1e-4);

		assertEquals(original.getBand(w-1,0,0),input.getBand(hw-1,hh,0),1e-4);
		assertEquals(original.getBand(w-1,0,1),input.getBand(hw-1,hh,1),1e-4);

		assertEquals(original.getBand(0,h-1,0),input.getBand(hw,hh-1,0),1e-4);
		assertEquals(original.getBand(0,h-1,1),input.getBand(hw,hh-1,1),1e-4);

		// undo the transform
		DiscreteFourierTransformOps.shiftZeroFrequency(input,false);
		BoofTesting.assertEquals(original,input,1e-4);
	}

	@Test
	public void magnitude() {
		InterleavedF32 complex = new InterleavedF32(10,20,2);
		ImageFloat32 output = new ImageFloat32(10,20);

		magnitude(complex, output);
		BoofTesting.checkSubImage(this, "magnitude", false, complex, output);
	}

	public void magnitude( InterleavedF32 transform , ImageFloat32 output ) {
		ImageMiscOps.fillUniform(transform,rand,-5,5);
		ImageMiscOps.fillUniform(output,rand,-5,5);

		DiscreteFourierTransformOps.magnitude(transform, output);

		for( int y = 0; y < transform.height; y++ ) {
			for( int x = 0; x < transform.width; x++ ) {
				float r = transform.getBand(x,y,0);
				float i = transform.getBand(x,y,1);

				double m = Math.sqrt(r*r + i*i);
				assertEquals(m,output.get(x,y),1e-4);
			}
		}
	}

	@Test
	public void phase() {
		InterleavedF32 complex = new InterleavedF32(10,20,2);
		ImageFloat32 output = new ImageFloat32(10,20);

		phase(complex,output);
		BoofTesting.checkSubImage(this, "phase", false, complex, output );
	}

	public void phase( InterleavedF32 transform , ImageFloat32 phase ) {
		ImageMiscOps.fillUniform(transform,rand,-5,5);
		ImageMiscOps.fillUniform(phase,rand,-5,5);

		DiscreteFourierTransformOps.phase(transform,phase);

		for( int y = 0; y < transform.height; y++ ) {
			for( int x = 0; x < transform.width; x++ ) {
				float r = transform.getBand(x,y,0);
				float i = transform.getBand(x,y,1);

				double theta = Math.atan2(i,r);
				assertEquals(theta,phase.get(x, y),1e-4);
			}
		}
	}

	@Test
	public void realToComplex() {
		ImageFloat32 real = new ImageFloat32(10,20);
		InterleavedF32 complex = new InterleavedF32(10,20,2);

		realToComplex(real,complex);
		BoofTesting.checkSubImage(this, "realToComplex", false, real, complex );
	}

	public void realToComplex( ImageFloat32 real , InterleavedF32 complex ) {
		ImageMiscOps.fillUniform(real,rand,-5,5);
		ImageMiscOps.fillUniform(complex,rand,-5,5);

		DiscreteFourierTransformOps.realToComplex(real,complex);

		for( int y = 0; y < real.height; y++ ) {
			for( int x = 0; x < real.width; x++ ) {
				assertEquals(real.get(x,y),complex.getBand(x,y,0),1e-4);
				assertEquals(0,complex.getBand(x,y,1),1e-4);
			}
		}
	}

	@Test
	public void multiplyRealComplex() {
		ImageFloat32 realA = new ImageFloat32(10,20);
		InterleavedF32 complexB = new InterleavedF32(10,20,2);
		InterleavedF32 complexC = new InterleavedF32(10,20,2);

		ImageMiscOps.fillUniform(realA,rand,-5,5);
		ImageMiscOps.fillUniform(complexB,rand,-5,5);

		multiplyRealComplex(realA,complexB,complexC);

		BoofTesting.checkSubImage(this, "multiplyRealComplex", false, realA, complexB, complexC);
	}

	public void multiplyRealComplex( ImageFloat32 realA , InterleavedF32 complexB , InterleavedF32 complexC ) {
		DiscreteFourierTransformOps.multiplyRealComplex(realA, complexB, complexC);

		Complex64F expected = new Complex64F();

		for( int y = 0; y < realA.height; y++ ) {
			for( int x = 0; x < realA.width; x++ ) {
				Complex64F a = new Complex64F(realA.get(x, y),0);
				Complex64F b = new Complex64F(complexB.getBand(x,y,0),complexB.getBand(x,y,1));

				ComplexMath64F.mult(a,b,expected);

				assertEquals(expected.getReal(),complexC.getBand(x,y,0),1e-4);
				assertEquals(expected.getImaginary(),complexC.getBand(x,y,1),1e-4);
			}
		}
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
