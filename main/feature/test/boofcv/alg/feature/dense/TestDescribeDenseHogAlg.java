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

package boofcv.alg.feature.dense;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestDescribeDenseHogAlg {
	ImageType<GrayF32> imageType = ImageType.single(GrayF32.class);

	int imgWidth = 60;
	int imgHeight = 80;

	/**
	 * Tests to see if the weight has the expected shape or at least some of the expected characteristics.
	 */
	@Test
	public void computeWeightBlockPixels() {
		int pixelsPerCell = 3;
		int cases[] = new int[]{3,4};

		for( int widthCells : cases ) {
			DescribeDenseHogAlg<GrayF32> helper = new DescribeDenseHogAlg<GrayF32>(10,pixelsPerCell,widthCells,widthCells+1,1,imageType);

			int widthPixelsX = widthCells*pixelsPerCell;
			int widthPixelsY = (widthCells+1)*pixelsPerCell;

			int rx = widthPixelsX/2 + widthPixelsX%2;
			int ry = widthPixelsY/2 + widthPixelsY%2;

			int totalOne = 0;
			double max = 0;
			for (int i = 0; i < ry; i++) {
				for (int j = 0; j < rx; j++) {
					// should be mirrored in all 4 quadrants
					double v0 = helper.weights[i * widthPixelsX + j];
					double v1 = helper.weights[i * widthPixelsX + (widthPixelsX - j - 1)];
					double v2 = helper.weights[(widthPixelsY - i - 1) * widthPixelsX + (widthPixelsX - j - 1)];
					double v3 = helper.weights[(widthPixelsY - i - 1) * widthPixelsX + j];

					assertEquals(v0, v1, 1e-8);
					assertEquals(v1, v2, 1e-8);
					assertEquals(v2, v3, 1e-8);

					max = Math.max(max, v0);

					if( v0 == 1.0)
						totalOne++;
				}
			}
			assertTrue(helper.weights[0] < helper.weights[1]);
			assertTrue(helper.weights[0] < helper.weights[widthCells]);
			assertEquals(1.0, max, 1e-8);
			if( widthPixelsX%2 == 1 )
				assertEquals(1,totalOne);
		}
	}

	@Test
	public void computePixelFeatures() {
		fail("Implement");
	}

	@Test
	public void computeCellHistogram() {
		fail("Implement");
	}
}
