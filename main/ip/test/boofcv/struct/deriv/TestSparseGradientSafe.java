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

package boofcv.struct.deriv;

import boofcv.struct.image.GrayF32;
import boofcv.struct.sparse.GradientValue_F32;
import boofcv.struct.sparse.SparseGradientSafe;
import boofcv.struct.sparse.SparseImageGradient;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSparseGradientSafe {

	static int r = 2;
	static int width = 20;
	static int height = 30;

	/**
	 * Read in a border and see if it returns zeros
	 */
	@Test
	public void checkMakeBorderSafe() {
		Dummy d = new Dummy();
		SparseImageGradient<GrayF32,GradientValue_F32> safe =
				new SparseGradientSafe<>(d);

		// read the border case
		GradientValue_F32 v = safe.compute(0,0);
		assertTrue(v.x==0);
		assertTrue(v.y==0);

		// read inside and see if it has the expected results
		assertTrue(safe.compute(width/2,height/2) == null);
	}
	
	
	private static class Dummy implements SparseImageGradient<GrayF32,GradientValue_F32>
	{

		@Override
		public GradientValue_F32 compute(int x, int y) {
			if( !isInBounds(x,y) )
				throw new RuntimeException("Bad stuff");
			
			return null;
		}

		@Override
		public Class<GradientValue_F32> getGradientType() {
			return GradientValue_F32.class;
		}

		@Override
		public void setImage(GrayF32 input) {

		}

		@Override
		public boolean isInBounds(int x, int y) {
			return !( x <= r || x >= width-r || y <= r || y >= height-r );
		}
	}
}
