/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.struct.distort;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestPixelDistortMap {

	int width = 10;
	int height = 20;

	@Test
	public void set_FromDistort() {
		PixelTransformMap map = new PixelTransformMap(width,height);
		map.set(new TestTransform());

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				map.compute(x,y);
				assertEquals(x+2f,map.distX);
				assertEquals(y+2f,map.distY);
			}
		}
	}

	@Test
	public void set_Pixel() {
		PixelTransformMap map = new PixelTransformMap(width,height);

		map.set(1,2,3.1f,4.1f);

		map.compute(1,2);

		assertEquals(3.1f,map.distX,1e-4f);
		assertEquals(4.1f,map.distY,1e-4f);
	}

	private static class TestTransform extends PixelTransform
	{

		@Override
		public void compute(int x, int y) {
			distX = x + 2;
			distY = y + 2;
		}
	}
}
