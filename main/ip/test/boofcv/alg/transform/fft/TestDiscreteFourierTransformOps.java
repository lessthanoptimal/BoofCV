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

package boofcv.alg.transform.fft;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.*;
import boofcv.testing.BoofTesting;
import org.ejml.data.Complex64F;
import org.ejml.ops.ComplexMath64F;
import org.junit.Test;

import java.util.Random;

import static boofcv.core.image.GeneralizedImageOps.get;
import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestDiscreteFourierTransformOps {

	Random rand = new Random(234);

	Class imageTypes[] = new Class[]{InterleavedF32.class,InterleavedF64.class};
	Class imageTypesS[] = new Class[]{GrayF32.class,GrayF64.class};

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
		DiscreteFourierTransformOps.checkImageArguments(new GrayF64(10,12),new InterleavedF32(10,12,2));

		// test negative cases
		try {
			DiscreteFourierTransformOps.checkImageArguments(new GrayF64(10,12),new InterleavedF32(10,12,1));
			fail("Should have thrown an exception");
		} catch( IllegalArgumentException ignore ){}
		try {
			DiscreteFourierTransformOps.checkImageArguments(new GrayF64(10,12),new InterleavedF32(20,12,2));
			fail("Should have thrown an exception");
		} catch( IllegalArgumentException ignore ){}
		try {
			DiscreteFourierTransformOps.checkImageArguments(new GrayF64(10,12),new InterleavedF32(10,14,2));
			fail("Should have thrown an exception");
		} catch( IllegalArgumentException ignore ){}
	}

	@Test
	public void shiftZeroFrequency() {
		for( Class type : imageTypes ) {
			ImageInterleaved complex;

			// test even images first
			complex = GeneralizedImageOps.createInterleaved(type,4,6,2);
			shiftZeroFrequency(complex);
			BoofTesting.checkSubImage(this, "shiftZeroFrequency", false, complex);

			// test odd images now
			complex = GeneralizedImageOps.createInterleaved(type,5,7,2);
			shiftZeroFrequency(complex);
			BoofTesting.checkSubImage(this, "shiftZeroFrequency", false, complex);

			// check one side being odd or even
			complex = GeneralizedImageOps.createInterleaved(type,6,7,2);
			shiftZeroFrequency(complex);
			BoofTesting.checkSubImage(this, "shiftZeroFrequency", false, complex);

			complex = GeneralizedImageOps.createInterleaved(type,5,8,2);
			shiftZeroFrequency(complex);
			BoofTesting.checkSubImage(this, "shiftZeroFrequency", false, complex);
		}
	}

	public void shiftZeroFrequency( ImageInterleaved input ) {
		GImageMiscOps.fillUniform(input, rand, -5, 5);

		ImageInterleaved original = (ImageInterleaved)input.clone();

		// corners should be at zero now
		if( input instanceof InterleavedF32 )
			DiscreteFourierTransformOps.shiftZeroFrequency((InterleavedF32)input,true);
		else
			DiscreteFourierTransformOps.shiftZeroFrequency((InterleavedF64)input,true);

		int hw = input.width/2;
		int hh = input.height/2;
		int w = input.width;
		int h = input.height;

		assertEquals(get(original, 0, 0, 0), get(input, hw, hh, 0),1e-4);
		assertEquals(get(original, 0, 0, 1), get(input, hw, hh, 1),1e-4);

		assertEquals(get(original, w - 1, h - 1, 0),get(input, hw - 1, hh - 1, 0),1e-4);
		assertEquals(get(original, w - 1, h - 1, 1),get(input, hw - 1, hh - 1, 1),1e-4);

		assertEquals(get(original, w - 1, 0, 0),get(input, hw - 1, hh, 0),1e-4);
		assertEquals(get(original, w - 1, 0, 1),get(input, hw - 1, hh, 1),1e-4);

		assertEquals(get(original, 0, h - 1, 0),get(input, hw, hh - 1, 0),1e-4);
		assertEquals(get(original, 0, h - 1, 1),get(input, hw, hh - 1, 1),1e-4);

		// undo the transform
		if( input instanceof InterleavedF32 )
			DiscreteFourierTransformOps.shiftZeroFrequency((InterleavedF32)input,false);
		else
			DiscreteFourierTransformOps.shiftZeroFrequency((InterleavedF64)input,false);
		BoofTesting.assertEquals(original,input,1e-4);
	}

	@Test
	public void magnitude() {
		for( int i = 0; i < imageTypes.length; i++ ) {
			ImageInterleaved complex = GeneralizedImageOps.createInterleaved(imageTypes[i],10,20,2);
			ImageGray output = GeneralizedImageOps.createSingleBand(imageTypesS[i], 10, 20);

			magnitude(complex, output);
			BoofTesting.checkSubImage(this, "magnitude", false, complex, output);
		}
	}

	public void magnitude( ImageInterleaved transform , ImageGray output ) {
		GImageMiscOps.fillUniform(transform,rand,-5,5);
		GImageMiscOps.fillUniform(output,rand,-5,5);

		if( transform instanceof InterleavedF32 )
			DiscreteFourierTransformOps.magnitude((InterleavedF32)transform, (GrayF32)output);
		else
			DiscreteFourierTransformOps.magnitude((InterleavedF64)transform, (GrayF64)output);

		for( int y = 0; y < transform.height; y++ ) {
			for( int x = 0; x < transform.width; x++ ) {
				double r = get(transform,x,y,0);
				double i = get(transform, x, y, 1);

				double m = Math.sqrt(r*r + i*i);
				assertEquals(m,get(output, x, y),1e-4);
			}
		}
	}

	@Test
	public void phase() {
		for( int i = 0; i < imageTypes.length; i++ ) {
			ImageInterleaved complex = GeneralizedImageOps.createInterleaved(imageTypes[i],10,20,2);
			ImageGray output = GeneralizedImageOps.createSingleBand(imageTypesS[i],10,20);

			phase(complex,output);
			BoofTesting.checkSubImage(this, "phase", false, complex, output );
		}
	}

	public void phase( ImageInterleaved transform , ImageGray output ) {
		GImageMiscOps.fillUniform(transform,rand,-5,5);
		GImageMiscOps.fillUniform(output,rand,-5,5);

		if( transform instanceof InterleavedF32 )
			DiscreteFourierTransformOps.phase((InterleavedF32) transform, (GrayF32) output);
		else
			DiscreteFourierTransformOps.phase((InterleavedF64) transform, (GrayF64) output);


		for( int y = 0; y < transform.height; y++ ) {
			for( int x = 0; x < transform.width; x++ ) {
				double r = get(transform, x, y, 0);
				double i = get(transform, x, y, 1);

				double m = Math.atan2(i, r);
				assertEquals(m,get(output, x, y),1e-4);
			}
		}
	}

	@Test
	public void realToComplex() {
		for( int i = 0; i < imageTypes.length; i++ ) {
			ImageGray real = GeneralizedImageOps.createSingleBand(imageTypesS[i], 10, 20);
			ImageInterleaved complex = GeneralizedImageOps.createInterleaved(imageTypes[i],10,20,2);

			realToComplex(real,complex);
			BoofTesting.checkSubImage(this, "realToComplex", false, real, complex );
		}
	}

	public void realToComplex(ImageGray real , ImageInterleaved complex ) {
		GImageMiscOps.fillUniform(real,rand,-5,5);
		GImageMiscOps.fillUniform(complex,rand,-5,5);

		if( complex instanceof InterleavedF32 )
			DiscreteFourierTransformOps.realToComplex((GrayF32) real, (InterleavedF32) complex);
		else
			DiscreteFourierTransformOps.realToComplex((GrayF64) real, (InterleavedF64) complex);

		for( int y = 0; y < real.height; y++ ) {
			for( int x = 0; x < real.width; x++ ) {
				assertEquals(get(real,x,y),get(complex, x, y, 0),1e-4);
				assertEquals(0,get(complex,x,y,1),1e-4);
			}
		}
	}

	@Test
	public void multiplyRealComplex() {
		for( int i = 0; i < imageTypes.length; i++ ) {
			ImageGray realA = GeneralizedImageOps.createSingleBand(imageTypesS[i],10,20);
			ImageInterleaved complexB = GeneralizedImageOps.createInterleaved(imageTypes[i],10,20,2);
			ImageInterleaved complexC = GeneralizedImageOps.createInterleaved(imageTypes[i],10,20,2);

			GImageMiscOps.fillUniform(realA,rand,-5,5);
			GImageMiscOps.fillUniform(complexB,rand,-5,5);

			multiplyRealComplex(realA,complexB,complexC);

			BoofTesting.checkSubImage(this, "multiplyRealComplex", false, realA, complexB, complexC);
		}
	}

	public void multiplyRealComplex(ImageGray realA , ImageInterleaved complexB , ImageInterleaved complexC ) {
		if( complexB instanceof InterleavedF32 )
			DiscreteFourierTransformOps.multiplyRealComplex((GrayF32)realA, (InterleavedF32)complexB, (InterleavedF32)complexC);
		else
			DiscreteFourierTransformOps.multiplyRealComplex((GrayF64)realA, (InterleavedF64)complexB, (InterleavedF64)complexC);

		Complex64F expected = new Complex64F();

		for( int y = 0; y < realA.height; y++ ) {
			for( int x = 0; x < realA.width; x++ ) {
				Complex64F a = new Complex64F(get(realA,x, y),0);
				Complex64F b = new Complex64F(get(complexB,x,y,0),get(complexB, x, y, 1));

				ComplexMath64F.multiply(a, b, expected);

				assertEquals(expected.getReal(),get(complexC, x, y, 0),1e-4);
				assertEquals(expected.getImaginary(),get(complexC, x, y, 1),1e-4);
			}
		}
	}

	@Test
	public void multiplyComplex() {
		for( int i = 0; i < imageTypes.length; i++ ) {
			ImageInterleaved complexA = GeneralizedImageOps.createInterleaved(imageTypes[i], 10, 20, 2);
			ImageInterleaved complexB = GeneralizedImageOps.createInterleaved(imageTypes[i],10,20,2);
			ImageInterleaved complexC = GeneralizedImageOps.createInterleaved(imageTypes[i],10,20,2);

			GImageMiscOps.fillUniform(complexA,rand,-5,5);
			GImageMiscOps.fillUniform(complexB,rand,-5,5);

			multiplyComplex(complexA,complexB,complexC);

			BoofTesting.checkSubImage(this,"multiplyComplex",false,complexA,complexB,complexC);
		}
	}

	public void multiplyComplex( ImageInterleaved complexA , ImageInterleaved complexB , ImageInterleaved complexC ) {
		if( complexB instanceof InterleavedF32 )
			DiscreteFourierTransformOps.multiplyComplex((InterleavedF32) complexA, (InterleavedF32) complexB, (InterleavedF32) complexC);
		else
			DiscreteFourierTransformOps.multiplyComplex((InterleavedF64) complexA, (InterleavedF64) complexB, (InterleavedF64) complexC);

		Complex64F expected = new Complex64F();

		for( int y = 0; y < complexA.height; y++ ) {
			for( int x = 0; x < complexA.width; x++ ) {
				Complex64F a = new Complex64F(get(complexA, x, y, 0),get(complexA, x, y, 1));
				Complex64F b = new Complex64F(get(complexB, x, y, 0),get(complexB,x,y,1));

				ComplexMath64F.multiply(a, b, expected);

				assertEquals(expected.getReal(),get(complexC, x, y, 0),1e-4);
				assertEquals(expected.getImaginary(),get(complexC, x, y, 1),1e-4);
			}
		}
	}
}
