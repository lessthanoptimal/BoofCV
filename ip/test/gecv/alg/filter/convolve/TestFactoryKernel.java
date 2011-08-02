/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.convolve;

import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import org.junit.Test;

import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestFactoryKernel {

	Random rand = new Random(234);

	@Test
	public void table1D_I32() {
		Kernel1D_I32 kernel = FactoryKernel.table1D_I32(3);

		assertEquals(7,kernel.width);

		for( int i = 0; i < kernel.width; i++ ) {
			assertEquals(1,kernel.get(i));
		}
	}

	@Test
	public void table1D_F32_unnormalized() {
		Kernel1D_F32 kernel = FactoryKernel.table1D_F32(3,false);

		assertEquals(7,kernel.width);

		for( int i = 0; i < kernel.width; i++ ) {
			assertEquals(1.0,kernel.get(i),1e-8);
		}
	}

	@Test
	public void table1D_F32_normalized() {
		Kernel1D_F32 kernel = FactoryKernel.table1D_F32(3,true);

		assertEquals(7,kernel.width);

		double expected = 1.0/kernel.width;

		for( int i = 0; i < kernel.width; i++ ) {
			assertEquals(expected,kernel.get(i),1e-8);
		}
	}

	@Test
	public void random1D_I32() {
		Kernel1D_I32 kernel = FactoryKernel.random1D_I32(2, -2, 2, rand);
		int nonZero = 0;
		for (int i = 0; i < kernel.width; i++) {
			assertTrue(kernel.get(i) <= 2);
			assertTrue(kernel.get(i) >= -2);
			if (kernel.get(i) != 0)
				nonZero++;
		}
		assertTrue(nonZero != 0);
	}

	@Test
	public void random1D_F32() {
		Kernel1D_F32 kernel = FactoryKernel.random1D_F32(2, -2, 2, rand);
		int nonZero = 0;
		for (int i = 0; i < kernel.width; i++) {
			assertTrue(kernel.get(i) <= 2);
			assertTrue(kernel.get(i) >= -2);
			if (kernel.get(i) != 0)
				nonZero++;
		}
		assertTrue(nonZero != 0);
	}

	@Test
	public void random2D_I32() {
		Kernel2D_I32 kernel = FactoryKernel.random2D_I32(2, -2, 2, rand);
		int nonZero = 0;
		for (int i = 0; i < kernel.width; i++) {
			for (int j = 0; j < kernel.width; j++) {
				assertTrue(kernel.get(j, i) <= 2);
				assertTrue(kernel.get(j, i) >= -2);
				if (kernel.get(j, i) != 0)
					nonZero++;
			}
		}
		assertTrue(nonZero != 0);
	}

	@Test
	public void random2D_F32() {
		Kernel2D_F32 kernel = FactoryKernel.random2D_F32(2, -2, 2, rand);
		int nonZero = 0;
		for (int i = 0; i < kernel.width; i++) {
			for (int j = 0; j < kernel.width; j++) {
				assertTrue(kernel.get(j, i) <= 2);
				assertTrue(kernel.get(j, i) >= -2);
				if (kernel.get(j, i) != 0)
					nonZero++;
			}
		}
		assertTrue(nonZero != 0);
	}

	@Test
	public void transpose_F32() {
		Kernel2D_F32 a = FactoryKernel.random2D_F32(2, -2, 2, rand);
		Kernel2D_F32 b = FactoryKernel.transpose(a);

		for( int i = 0; i < a.width; i++ ) {
			for( int j = 0; j < a.width; j++ ) {
				assertEquals(a.get(i,j),b.get(j,i),1e-4);
			}
		}
	}

	@Test
	public void transpose_I32() {
		Kernel2D_I32 a = FactoryKernel.random2D_I32(2, -2, 2, rand);
		Kernel2D_I32 b = FactoryKernel.transpose(a);

		for( int i = 0; i < a.width; i++ ) {
			for( int j = 0; j < a.width; j++ ) {
				assertEquals(a.get(i,j),b.get(j,i),1e-4);
			}
		}
	}

	@Test
	public void normalizeSumToOne() {
		Kernel1D_F32 kernel = new Kernel1D_F32(3);
		for (int i = 0; i < 3; i++)
			kernel.data[i] = 2.0F;

		FactoryKernel.normalizeSumToOne(kernel);

		for (int i = 0; i < 3; i++)
			assertEquals(kernel.data[i], 2.0F / 6.0f, 1e-4);
	}

	@Test
	public void convolve_1D() {
		Kernel1D_F32 k1 = FactoryKernel.random1D_F32(2,-1,1,rand);
		Kernel1D_F32 k2 = FactoryKernel.random1D_F32(2,-1,1,rand);

		Kernel2D_F32 c = FactoryKernel.convolve(k1,k2);

		for( int i = 0; i < 5; i++ ) {
			for( int j = 0; j < 5; j++ ) {
				assertEquals(k1.data[i]*k2.data[j],c.get(j,i),1e-4);
			}
		}
	}
}
