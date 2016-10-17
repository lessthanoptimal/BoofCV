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

import boofcv.alg.misc.ImageMiscOps;
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

	int pixelsPerCell = 4;
	int widthCellsX = 3;
	int widthCellsY = 4;

	@Test
	public void process() {
		// intentionally left blank.  This is handled by image type specific checks
	}

	/**
	 * Tests to see if the weight has the expected shape or at least some of the expected characteristics.
	 */
	@Test
	public void computeWeightBlockPixels() {
		int pixelsPerCell = 3;
		int cases[] = new int[]{3,4};

		for( int widthCells : cases ) {
			DescribeDenseHogAlg<GrayF32> helper = new DescribeDenseHogAlg<>(10,pixelsPerCell,widthCells,widthCells+1,1,imageType);

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

	/**
	 * Checks to see if the expected cells are modified in the descriptor
	 */
	@Test
	public void computeCellHistogram() {

		DescribeDenseHogAlg<GrayF32> helper = new DescribeDenseHogAlg<>(
				10,pixelsPerCell, widthCellsX, widthCellsY,1,imageType);

		helper.setInput(new GrayF32(imgWidth,imgHeight));
		ImageMiscOps.fill(helper.orientation,0);
		ImageMiscOps.fill(helper.magnitude,1);

		int cellX = 1;
		int cellY = 2;

		helper.histogram = new double[10* widthCellsX*widthCellsY];
		helper.computeCellHistogram(20,25,cellX,cellY);

		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				checkCellModified(helper.histogram,cellX+j,cellY+i, true);
			}
		}
		// sanity check.  Shouldn't be modified
		checkCellModified(helper.histogram,0,0, false);
	}

	private void checkCellModified( double histogram[] , int cellX , int cellY , boolean modified ) {
		int index = (cellY*widthCellsX + cellX)*10;

		for (int i = 0; i < 10; i++) {
			if( (histogram[i+index] != 0) == modified ) {
				return;
			}
		}
		fail("Not modified = "+modified);
	}

	@Test
	public void addToHistogram() {
		DescribeDenseHogAlg<GrayF32> helper = new DescribeDenseHogAlg<>(
				10,pixelsPerCell, widthCellsX, widthCellsX +1,1,imageType);

		helper.histogram = new double[10*widthCellsX*widthCellsY];

		// first try to add outside
		helper.addToHistogram(-1,2,3,1.0);
		assertEquals(-1,notZeroIndex(helper.histogram));
		helper.addToHistogram(10,2,3,1.0);
		assertEquals(-1,notZeroIndex(helper.histogram));
		helper.addToHistogram(1,-2,3,1.0);
		assertEquals(-1,notZeroIndex(helper.histogram));
		helper.addToHistogram(1,20,3,1.0);
		assertEquals(-1,notZeroIndex(helper.histogram));

		// set it inside
		helper.addToHistogram(1,2,3,1.0);
		assertEquals((2* widthCellsX +1)*10+3,notZeroIndex(helper.histogram));
	}

	private int notZeroIndex( double a[] ) {
		for (int i = 0; i < a.length; i++) {
			if( a[i] != 0 )
				return i;
		}
		return -1;
	}
}
