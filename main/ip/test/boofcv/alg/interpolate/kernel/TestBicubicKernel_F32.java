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

package boofcv.alg.interpolate.kernel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestBicubicKernel_F32 {
	@Test
	public void checkSumToOne() {
		BicubicKernel_F32 kernel = new BicubicKernel_F32(-0.5f);

		// should sum to one for different offsets
		for( int offset = 0; offset < 10; offset++ ) {
			float delta = offset*0.01f;

			float total = 0;
			for( int i = 0; i < kernel.getWidth(); i++ ) {
				float x = i - kernel.getRadius();
				total += kernel.compute(x+delta);
			}

			assertEquals(1,total,1e-4);
		}
	}
}
