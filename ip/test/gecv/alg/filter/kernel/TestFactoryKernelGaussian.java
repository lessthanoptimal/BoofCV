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

import org.junit.Test;

import static org.junit.Assert.fail;


/**
 * @author Peter Abeles
 */
public class TestFactoryKernelGaussian {

	@Test
	public void update() {
		fail("Update these tests");
	}

//	@Test
//	public void gaussian1D_I32_radius() {
//		Kernel1D_I32 g1 = FactoryKernelGaussian.gaussian(Kernel1D_I32.class,-1,2,true);
//		Kernel1D_I32 g2 = FactoryKernelGaussian.gaussian(Kernel1D_I32.class,-1, 2, true);
//
//		for (int i = 0; i < g1.data.length; i++) {
//			assertEquals(g1.data[i], g2.data[i], 1e-8);
//		}
//	}
//
//	@Test
//	public void gaussian1D_I32_radius_sigma() {
//		Kernel1D_I32 g = FactoryKernelGaussian.gaussian1D_I32(1.0, 2);
//
//		assertEquals(1, g.data[0]);
//		assertEquals(4, g.data[1]);
//		assertEquals(7, g.data[2]);
//		assertEquals(4, g.data[3]);
//		assertEquals(1, g.data[4]);
//	}
//
//	@Test
//	public void gaussian2D_I32_sigma_radius() {
//		Kernel2D_I32 g2 = FactoryKernelGaussian.gaussian2D_I32(1.0, 2);
//		Kernel1D_I32 g1 = FactoryKernelGaussian.gaussian1D_I32(1.0, 2);
//
//		for (int i = 0; i < 5; i++) {
//			for (int j = 0; j < 5; j++) {
//				int expected = g1.data[i] * g1.data[j];
//
//				assertEquals(g2.data[i * 5 + j], expected);
//			}
//		}
//	}
//
//	@Test
//	public void gaussian1D_F32_sigma_radius() {
//		// un-normalized it should be the same as the PDF
//		Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian1D_F32(1.0, 2, false);
//
//		float g[] = kernel.data;
//
//		assertEquals(UtilGaussian.computePDF(0, 1, -2), g[0], 1e-4);
//		assertEquals(UtilGaussian.computePDF(0, 1, -1), g[1], 1e-4);
//		assertEquals(UtilGaussian.computePDF(0, 1, 0), g[2], 1e-4);
//		assertEquals(UtilGaussian.computePDF(0, 1, 1), g[3], 1e-4);
//		assertEquals(UtilGaussian.computePDF(0, 1, 2), g[4], 1e-4);
//
//		// if normalized it should add up to one
//		kernel = FactoryKernelGaussian.gaussian1D_F32(1.0, 2, true);
//
//		g = kernel.data;
//		double normalizer = 0;
//		double total = 0;
//		for (int i = 0; i < g.length; i++) {
//			total += g[i];
//			normalizer += UtilGaussian.computePDF(0, 1, i - 2);
//		}
//		assertEquals(1.0, total, 1e-8);
//
//		assertEquals(UtilGaussian.computePDF(0, 1, -2) / normalizer, g[0], 1e-4);
//		assertEquals(UtilGaussian.computePDF(0, 1, -1) / normalizer, g[1], 1e-4);
//		assertEquals(UtilGaussian.computePDF(0, 1, 0) / normalizer, g[2], 1e-4);
//		assertEquals(UtilGaussian.computePDF(0, 1, 1) / normalizer, g[3], 1e-4);
//		assertEquals(UtilGaussian.computePDF(0, 1, 2) / normalizer, g[4], 1e-4);
//
//	}
//
//	@Test
//	public void gaussian1D_F32_radius() {
//		for (int toggle = 0; toggle < 2; toggle++) {
//			boolean normalize = toggle == 1;
//			Kernel1D_F32 g1 = FactoryKernelGaussian.gaussian1D_F32(FactoryKernelGaussian.sigmaForRadius(2), 2, normalize);
//			Kernel1D_F32 g2 = FactoryKernelGaussian.gaussian1D_F32(2, normalize);
//
//			for (int i = 0; i < g1.data.length; i++) {
//				assertEquals(g1.data[i], g2.data[i], 1e-8);
//			}
//		}
//	}
//
//	@Test
//	public void gaussian2D_F32_radius() {
//		for (int toggle = 0; toggle < 2; toggle++) {
//			boolean normalize = toggle == 1;
//			Kernel2D_F32 kernel = FactoryKernelGaussian.gaussian2D_F32(1.0, 2, normalize);
//
//			float g[] = kernel.data;
//
//			assertEquals(25, g.length);
//
//			float max = g[5 * 2 + 2];
//
//			float total = 0;
//			for (float a : g) {
//				total += a;
//				assertTrue(max >= a);
//			}
//
//			// see if it is normalized
//			if (normalize)
//				assertEquals(1.0, total, 1e-4);
//			else
//				assertTrue(Math.abs(1.0 - total) > 1e-4);
//		}
//	}
//
//	@Test
//	public void gaussian1D_F32_sigma_radius_min() {
//		// check to see if it has the expected length for a specific example
//		double thresh = 0.08;
//
//		Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian1D_F32(1.0, 10, thresh, false);
//		float g[] = kernel.data;
//
//		assertEquals(5, kernel.width);
//		assertTrue(g[0] <= thresh && g[0] > 0);
//
//		// see if it is within tolerance for a few values
//		for (thresh = 0.01; thresh < 0.4; thresh *= 2) {
//			kernel = FactoryKernelGaussian.gaussian1D_F32(1.0, 21, thresh, false);
//			g = kernel.data;
//
//			assertTrue(g[0] <= thresh && g[0] > 0);
//		}
//	}
//
//	@Test
//	public void gaussianDerivative1D_F32() {
//
//		for (int toggle = 0; toggle < 2; toggle++) {
//			boolean normalize = toggle == 1;
//
//			Kernel1D_F32 deriv = FactoryKernelGaussian.gaussianDerivative1D_F32(1.0, 2, normalize);
//
//			// the derivative should be symmetric
//			assertEquals(deriv.data[4], -deriv.data[0], 1e-5);
//			assertEquals(deriv.data[3], -deriv.data[1], 1e-5);
//			assertEquals(0f, deriv.data[2], 1e-5);
//		}
//	}
//
//	@Test
//	public void gaussianDerivative1D_I32()
//	{
//		Kernel1D_I32 deriv = FactoryKernelGaussian.gaussianDerivative1D_I32(1.0, 2);
//
//		// the derivative should be symmetric
//		assertEquals(deriv.data[4], -deriv.data[0], 1e-5);
//		assertEquals(deriv.data[3], -deriv.data[1], 1e-5);
//		assertEquals(0f, deriv.data[2], 1e-5);
//	}
}
