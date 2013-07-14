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

package boofcv.alg.transform.ii;

import boofcv.alg.transform.ii.impl.ImplIntegralImageOps;
import boofcv.struct.ImageRectangle;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;


/**
 * @author Peter Abeles
 */
public class TestIntegralImageOps {

	@Test
	public void transform() {
		int expected = countName("transform",IntegralImageOps.class);
		int found = countName("transform",ImplIntegralImageOps.class);

		assertTrue(found != 0 );
		assertEquals(expected, found);
	}

	@Test
	public void convolve() {
		int expected = countName("convolve",IntegralImageOps.class);
		int found = countName("convolve",ImplIntegralImageOps.class);

		assertTrue(found != 0 );
		assertEquals(expected,found);
	}

	@Test
	public void convolveBorder() {
		int expected = countName("convolveBorder", IntegralImageOps.class);
		int found = countName("convolveBorder",ImplIntegralImageOps.class);

		assertTrue(found != 0 );
		assertEquals(expected,found);
	}

	@Test
	public void convolveSparse() {
		int expected = countName("convolveSparse", IntegralImageOps.class);
		int found = countName("convolveSparse",ImplIntegralImageOps.class);

		assertTrue(found != 0 );
		assertEquals(expected,found);
	}

	@Test
	public void block_unsafe() {
		int expected = countName("block_unsafe", IntegralImageOps.class);
		int found = countName("block_unsafe",ImplIntegralImageOps.class);

		assertTrue(found != 0 );
		assertEquals(expected,found);
	}

	@Test
	public void block_zero() {
		int expected = countName("block_zero", IntegralImageOps.class);
		int found = countName("block_zero",ImplIntegralImageOps.class);

		assertTrue(found != 0 );
		assertEquals(expected,found);
	}
	
	@Test
	public void isInBounds() {
		IntegralKernel kernel = new IntegralKernel(2);
		kernel.blocks[0] = new ImageRectangle(-1,-2,1,2);
		kernel.blocks[1] = new ImageRectangle(-3,-2,1,2);

		// obvious cases
		assertTrue(IntegralImageOps.isInBounds(30,30,kernel,100,100));
		assertFalse(IntegralImageOps.isInBounds(-10, -20, kernel, 100, 100));
		// positive border cases
		assertTrue(IntegralImageOps.isInBounds(3,30,kernel,100,100));
		assertTrue(IntegralImageOps.isInBounds(98,30,kernel,100,100));
		assertTrue(IntegralImageOps.isInBounds(30,2,kernel,100,100));
		assertTrue(IntegralImageOps.isInBounds(30,97,kernel,100,100));
		// negative border cases
		assertFalse(IntegralImageOps.isInBounds(2, 30, kernel, 100, 100));
		assertFalse(IntegralImageOps.isInBounds(99, 30, kernel, 100, 100));
		assertFalse(IntegralImageOps.isInBounds(30, 1, kernel, 100, 100));
		assertFalse(IntegralImageOps.isInBounds(30, 98, kernel, 100, 100));
	}

	private static int countName( String name , Class c ) {
		int found = 0;
		for( Method m : c.getMethods() ) {
			if( m.getName().compareTo(name) == 0 )
				found++;
		}
		return found;
	}
}
