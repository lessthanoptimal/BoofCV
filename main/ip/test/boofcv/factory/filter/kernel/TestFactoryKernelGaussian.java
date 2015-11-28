/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.filter.kernel;

import boofcv.alg.filter.kernel.KernelMath;
import boofcv.struct.convolve.*;
import org.ddogleg.stats.UtilGaussian;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestFactoryKernelGaussian {

	@Test
	public void gaussian() {
		// basic test to see if its creating kernels of the correct type
		assertTrue( FactoryKernelGaussian.gaussian(1,true, 32, 1,2) instanceof Kernel1D_F32 );
		assertTrue( FactoryKernelGaussian.gaussian(1,false, 32, 1,2) instanceof Kernel1D_I32 );
		assertTrue( FactoryKernelGaussian.gaussian(2,true, 32, 1,2) instanceof Kernel2D_F32 );
		assertTrue( FactoryKernelGaussian.gaussian(2,false, 32, 1,2) instanceof Kernel2D_I32);
	}

	@Test
	public void gaussian1D_F32() {
		for( boolean odd : new boolean[]{true,false}) {
			double adj = odd ? 0 : 0.5;
			// un-normalized it should be the same as the PDF
			Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian1D_F32(1.0, 2, odd, false);

			if( odd )
				assertEquals(5,kernel.getWidth());
			else
				assertEquals(4,kernel.getWidth());

			float g[] = kernel.data;

			assertEquals(UtilGaussian.computePDF(0, 1, -2+adj), g[0], 1e-4);
			assertEquals(UtilGaussian.computePDF(0, 1, -1+adj), g[1], 1e-4);
			assertEquals(UtilGaussian.computePDF(0, 1, 0+adj), g[2], 1e-4);
			assertEquals(UtilGaussian.computePDF(0, 1, 1+adj), g[3], 1e-4);
			if( odd )
				assertEquals(UtilGaussian.computePDF(0, 1, 2+adj), g[4], 1e-4);

			// if normalized it should add up to one
			kernel = FactoryKernelGaussian.gaussian1D_F32(1.0, 2, odd, true);

			g = kernel.data;
			double normalizer = 0;
			double total = 0;
			for (int i = 0; i < g.length; i++) {
				total += g[i];
				normalizer += UtilGaussian.computePDF(0, 1, i - 2 + adj);
			}
			assertEquals(1.0, total, 1e-4);

			assertEquals(UtilGaussian.computePDF(0, 1, -2+adj) / normalizer, g[0], 1e-4);
			assertEquals(UtilGaussian.computePDF(0, 1, -1+adj) / normalizer, g[1], 1e-4);
			assertEquals(UtilGaussian.computePDF(0, 1, 0+adj) / normalizer, g[2], 1e-4);
			assertEquals(UtilGaussian.computePDF(0, 1, 1+adj) / normalizer, g[3], 1e-4);
			if( odd )
				assertEquals(UtilGaussian.computePDF(0, 1, 2+adj) / normalizer, g[4], 1e-4);
		}

	}

	@Test
	public void gaussian2D_F32() {
		for( boolean odd : new boolean[]{true,false}) {
			// testing using the separable property
			Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian1D_F32(1.0, 2, odd, false);
			Kernel2D_F32 kernel2 = FactoryKernelGaussian.gaussian2D_F32(1.0, 2, odd, false);

			for (int i = 0; i < kernel2.width; i++) {
				for (int j = 0; j < kernel2.width; j++) {
					float expected = kernel.data[i] * kernel.data[j];
					assertEquals(expected, kernel2.get(j, i), 1e-4f);
				}
			}

			// normalized it should add up to one
			kernel2 = FactoryKernelGaussian.gaussian2D_F32(1.0, 2, odd, true);

			float total = 0;

			for (int i = 0; i < kernel2.width; i++) {
				for (int j = 0; j < kernel2.width; j++) {
					total += kernel2.get(j, i);
				}
			}

			assertEquals(1, total, 1e-4);
		}
	}

	@Test
	public void derivative1D_F32() {
		float sigma = 1.5f;
		int radius = 2;
		Kernel1D_F32 found = FactoryKernelGaussian.derivative1D_F32(1,sigma,radius, false);

		int index = 0;
		for( int i = radius; i >= -radius; i-- ) {
			assertTrue((float)UtilGaussian.derivative1(0,sigma,i)==found.data[index++]);
		}

		// todo check normalized version
	}

	/**
	 * Create a kernel with an even width
	 */
	@Test
	public void gaussianWidth_even() {
		Kernel2D_F64 a = FactoryKernelGaussian.gaussianWidth(2,4);

		assertEquals(4, a.width);
		checkForSymmetry(a);
		assertEquals(1,KernelMath.sum(a),1e-8);

		// don't specify sigma
		a = FactoryKernelGaussian.gaussianWidth(-1,4);
		assertEquals(4, a.width);
		checkForSymmetry(a);
		assertEquals(1,KernelMath.sum(a),1e-8);
	}

	/**
	 * Create a kernel with an odd width
	 */
	@Test
	public void gaussianWidth_odd() {
		Kernel2D_F64 a = FactoryKernelGaussian.gaussianWidth(2,5);
		assertEquals(5, a.width);
		checkForSymmetry(a);
		assertEquals(1,KernelMath.sum(a),1e-8);

		// don't specify sigma
		a = FactoryKernelGaussian.gaussianWidth(-1,5);
		assertEquals(5, a.width);
		checkForSymmetry(a);
		assertEquals(1,KernelMath.sum(a),1e-8);
	}

	public static void checkForSymmetry( Kernel2D_F64 a ) {
		boolean even = a.getWidth()%2==0;
		int r = a.getRadius();
		int w = a.getWidth()-1;

		if( even )
			r--;

		for( int i = 0; i <= r; i++ ) {
			for( int j = 0; j <= r; j++ ) {
				assertEquals(a.get(i,j),a.get(w-i,j),1e-8);
				assertEquals(a.get(i,j),a.get(i,w-j),1e-8);
			}
		}

		if( !even ) {
			assertTrue(Math.abs(a.get(r,r)-a.get(r+1,r))>1e-8);
			assertTrue(Math.abs(a.get(r,r)-a.get(r,r+1))>1e-8);
		}

	}
}
