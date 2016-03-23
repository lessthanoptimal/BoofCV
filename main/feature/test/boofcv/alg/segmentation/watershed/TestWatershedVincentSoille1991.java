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

package boofcv.alg.segmentation.watershed;

import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestWatershedVincentSoille1991 {

	@Test
	public void sortPixels() {
		WatershedVincentSoille1991 alg = new Dummy();

		GrayU8 image = new GrayU8(3,4);
		image.data = new byte[]
				{1,2,3,
				 2,2,2,
				 5,6,1,
				 3,3,(byte)255};

		alg.output = new GrayS32(4,5);

		alg.sortPixels(image);

		assertEquals(0,alg.histogram[0].size);
		assertEquals(2,alg.histogram[1].size);
		assertEquals(4,alg.histogram[2].size);
		assertEquals(3,alg.histogram[3].size);
		assertEquals(0,alg.histogram[4].size);
		assertEquals(1,alg.histogram[5].size);
		assertEquals(1,alg.histogram[6].size);

		for( int i = 7; i < 255; i++ )
			assertEquals(0,alg.histogram[i].size);

		assertEquals(1,alg.histogram[255].size);

		// check output coordinate for (0,2)
		int indexOut = 3*4 + 1;
		assertEquals(indexOut,alg.histogram[5].get(0));

	}

	private static class Dummy extends WatershedVincentSoille1991 {

		@Override
		protected void assignNewToNeighbors(int index) {}

		@Override
		protected void checkNeighborsAssign(int index) {}

		@Override
		protected void checkNeighborsMasks(int index) {}
	}
}
