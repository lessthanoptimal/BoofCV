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

package boofcv.factory.filter.kernel;

import boofcv.alg.filter.kernel.SteerableKernel;
import boofcv.struct.convolve.Kernel2D_F32;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestFactorySteerable {

	/**
	 * Very basis tests to see if the algorithm explodes or not.
	 */
	@Test
	public void gaussian() {

		for( int totalOrder = 1; totalOrder <= 4; totalOrder++ ) {
			for( int orderX = 0; orderX<= totalOrder; orderX++ ) {
				int orderY = totalOrder-orderX;

				SteerableKernel<Kernel2D_F32> alg = FactorySteerable.gaussian(Kernel2D_F32.class,orderX,orderY, -1, 10);

				Kernel2D_F32 k = alg.compute(0.1);

				// make sure its not zero.
				boolean notZero = false;
				for( int y = 0; y < k.width; y++ ) {
					for( int x = 0; x < k.width; x++ ) {
						if( k.get(x,y) != 0 )
							notZero=true;
					}
				}
				assertTrue(notZero);
			}
		}
	}

	/**
	 * Compare the values found in the separable kernel against what was computed
	 * using the full 2D gaussian kernel.
	 */
	// todo this is going to be a long standing bug.
	@Test
	public void separable() {
		for( int totalOrder = 1; totalOrder <= 4; totalOrder++ ) {
			for( int orderX = 0; orderX<= totalOrder; orderX++ ) {
				int orderY = totalOrder-orderX;

				// todo note the Dy doesn't work either   only Dx
				System.out.println("x = "+orderX+" y = "+orderY);

				SteerableKernel<Kernel2D_F32> alg = FactorySteerable.separable(Kernel2D_F32.class,orderX,orderY,10);
				SteerableKernel<Kernel2D_F32> algValidate = FactorySteerable.gaussian(Kernel2D_F32.class,orderX,orderY, -1, 10);

				Kernel2D_F32 found = alg.compute(0.2);
				Kernel2D_F32 expected = algValidate.compute(0.2);

				float total = 0;
				float sum = 0;
				for( int y = 0; y < found.data.length; y++ ) {
					total += Math.abs(found.data[y]-expected.data[y]);
					sum += Math.abs(expected.data[y]);
				}
				assertTrue(total/sum < 0.01);
			}
		}
	}
}
