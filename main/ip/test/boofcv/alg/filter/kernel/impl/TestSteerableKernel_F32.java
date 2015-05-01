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

package boofcv.alg.filter.kernel.impl;

import boofcv.alg.filter.kernel.KernelMath;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.convolve.Kernel2D_F32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestSteerableKernel_F32 {

	Random rand = new Random(234);
	int width = 21;

	/**
	 * Checks to see if the basis kernels are correctly combined together.
	 */
	@Test
	public void checkCombining() {
		double c[] = new double[]{0.1,0.2,0.8};
		DummySteerableCoefficients coef = new DummySteerableCoefficients(c);
		Kernel2D basis[] = new Kernel2D[3];
		basis[0] = FactoryKernel.random2D_F32(width,width/2,0,10,rand);
		basis[1] = FactoryKernel.random2D_F32(width,width/2,0,10,rand);
		basis[2] = FactoryKernel.random2D_F32(width,width/2,0,10,rand);

		Kernel2D_F32 expected = new Kernel2D_F32(width);

		for( int y = 0; y < width; y++ ) {
			for( int x = 0; x < width; x++ ) {
				float total = 0;
				for( int i = 0; i < c.length;i++ ) {
					total += c[i]*((Kernel2D_F32)basis[i]).get(x,y);
				}
				expected.set(x,y,total);
			}
		}

		SteerableKernel_F32 alg = new SteerableKernel_F32();
		alg.setBasis(coef,basis);
		Kernel2D_F32 found = alg.compute(60.0);

		assertTrue(KernelMath.isEquals(expected.data,found.data,width*width,1e-4f));
	}
}
