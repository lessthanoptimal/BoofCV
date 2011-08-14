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

package gecv.alg.filter.kernel;

import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import org.junit.Test;
import pja.stats.UtilGaussian;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestFactoryKernelGaussian {

	@Test
	public void gaussian() {
		// basic test to see if its creating kernels of the correct type
		assertTrue( FactoryKernelGaussian.gaussian(1,true,1,2) instanceof Kernel1D_F32 );
		assertTrue( FactoryKernelGaussian.gaussian(1,false,1,2) instanceof Kernel1D_I32 );
		assertTrue( FactoryKernelGaussian.gaussian(2,true,1,2) instanceof Kernel2D_F32 );
		assertTrue( FactoryKernelGaussian.gaussian(2,false,1,2) instanceof Kernel2D_I32);
	}

	@Test
	public void gaussian1D_F32() {
		// un-normalized it should be the same as the PDF
		Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian1D_F32(1.0, 2, false);

		float g[] = kernel.data;

		assertEquals(UtilGaussian.computePDF(0, 1, -2), g[0], 1e-4);
		assertEquals(UtilGaussian.computePDF(0, 1, -1), g[1], 1e-4);
		assertEquals(UtilGaussian.computePDF(0, 1, 0), g[2], 1e-4);
		assertEquals(UtilGaussian.computePDF(0, 1, 1), g[3], 1e-4);
		assertEquals(UtilGaussian.computePDF(0, 1, 2), g[4], 1e-4);

		// if normalized it should add up to one
		kernel = FactoryKernelGaussian.gaussian1D_F32(1.0, 2, true);

		g = kernel.data;
		double normalizer = 0;
		double total = 0;
		for (int i = 0; i < g.length; i++) {
			total += g[i];
			normalizer += UtilGaussian.computePDF(0, 1, i - 2);
		}
		assertEquals(1.0, total, 1e-8);

		assertEquals(UtilGaussian.computePDF(0, 1, -2) / normalizer, g[0], 1e-4);
		assertEquals(UtilGaussian.computePDF(0, 1, -1) / normalizer, g[1], 1e-4);
		assertEquals(UtilGaussian.computePDF(0, 1, 0) / normalizer, g[2], 1e-4);
		assertEquals(UtilGaussian.computePDF(0, 1, 1) / normalizer, g[3], 1e-4);
		assertEquals(UtilGaussian.computePDF(0, 1, 2) / normalizer, g[4], 1e-4);

	}

	@Test
	public void gaussian2D_F32() {
		// testing using the separable property
		Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian1D_F32(1.0, 2, false);
		Kernel2D_F32 kernel2 = FactoryKernelGaussian.gaussian2D_F32(1.0, 2, false);

		for( int i = 0; i < kernel2.width; i++ ) {
			for( int j = 0; j < kernel2.width; j++ ) {
				float expected = kernel.data[i]*kernel.data[j];
				assertEquals(expected,kernel2.get(j,i),1e-4f);
			}
		}

		// normalized it should add up to one
		kernel2 = FactoryKernelGaussian.gaussian2D_F32(1.0, 2, true);

		float total = 0;

		for( int i = 0; i < kernel2.width; i++ ) {
			for( int j = 0; j < kernel2.width; j++ ) {
				total += kernel2.get(j,i);
			}
		}

		assertEquals(1,total,1e-4);
	}

	@Test
	public void derivative1D_F32() {
		float sigma = 1.5f;
		int radius = 2;
		Kernel1D_F32 found = FactoryKernelGaussian.derivative1D_F32(1,sigma,radius);

		for( int i = -radius; i <= radius; i++ ) {
			int index = i+radius;
			assertTrue((float)-UtilGaussian.derivative1(0,sigma,i)==found.data[index]);
		}
	}
}
