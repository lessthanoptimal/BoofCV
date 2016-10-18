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

import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.alg.feature.describe.DescribeSiftCommon;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDescribeDenseHogFastAlg {

	ImageType<GrayF32> imageType = ImageType.single(GrayF32.class);

	int imgWidth = 60;
	int imgHeight = 80;

	@Test
	public void process() {
		// intentionally left blank.  This is handled by image type specific checks
	}

	@Test
	public void growCellArray() {
		DescribeDenseHogFastAlg<GrayF32> helper = new DescribeDenseHogFastAlg<>(10,8,2,2,1,imageType);

		helper.growCellArray(64,32);
		assertEquals(8*4,helper.cells.length);
		assertEquals(8,helper.cellCols);
		assertEquals(4,helper.cellRows);

		helper.growCellArray(32,16);
		assertEquals(8*4,helper.cells.length);
		assertEquals(4,helper.cellCols);
		assertEquals(2,helper.cellRows);

		helper.growCellArray(64,40);
		assertEquals(8*5,helper.cells.length);
		assertEquals(8,helper.cellCols);
		assertEquals(5,helper.cellRows);
	}

	@Test
	public void getDescriptorsInRegion() {

		int x0 = 5, x1 = 67;
		int y0 = 9, y1 = 89;

		DescribeDenseHogFastAlg<GrayF32> helper = new DescribeDenseHogFastAlg<>(10,8,2,2,1,imageType);

		GrayF32 input = new GrayF32(120,110);
		helper.setInput(input);

		helper.process();

		List<TupleDesc_F64> expected = new ArrayList<>();

		// use a different more brute force technique to find all the descriptors contained inside the region
		// take advantage of the descriptors being computed in a row major order
		int c = 8;
		int w = 2*c;
		for (int y = 0; y < input.height-w; y += c) {
			int i = (y/c)*helper.cellCols;
			for (int x = 0; x < input.width-w; x += c, i++) {
				if( x >= x0 && x+w < x1 && y >= y0 && y+w < y1) {
					expected.add( helper.getDescriptions().get(i));
				}
			}
		}

		List<TupleDesc_F64> found = new ArrayList<>();
		helper.getDescriptorsInRegion(x0,y0,x1,y1,found);

		assertEquals(expected.size(),found.size());

		for (int j = 0; j < expected.size(); j++) {
			assertTrue(found.contains(expected.get(j)));
		}
	}

	@Test
	public void computeDescriptor() {
		DescribeDenseHogFastAlg<GrayF32> helper = new DescribeDenseHogFastAlg<>(10,8,2,2,1,imageType);

		helper.growCellArray(imgWidth, imgHeight);
		int stride = helper.cellCols;

		// manually build a simple histogram for input and manually construct the expected resulting descriptor
		TupleDesc_F64 expected = new TupleDesc_F64(40);

		setHistogram(helper.cells[2].histogram,2,3 , expected.value,0);
		setHistogram(helper.cells[3].histogram,2,3 , expected.value,10);
		setHistogram(helper.cells[stride + 2].histogram,5,0 , expected.value,20);
		setHistogram(helper.cells[stride + 3].histogram,7,8 , expected.value,30);

		DescribeSiftCommon.normalizeDescriptor(expected,0.2);

		helper.computeDescriptor(0,2);

		Point2D_I32 where = helper.locations.get(0);
		TupleDesc_F64 found = helper.descriptions.get(0);

		assertEquals(8*2,where.x);
		assertEquals(0,where.y);

		assertEquals(40,found.size());
		assertTrue(DescriptorDistance.euclidean(expected,found) < 1e-8 );
	}

	private void setHistogram( float histogram[] , int a , int b , double expected[], int index0 ) {
		Arrays.fill(histogram,0);
		histogram[a] = 2.4f;
		histogram[b] = 1.2f;

		// construct the expected descriptor
		expected[index0+a] = 2.4f;
		expected[index0+b] = 1.2f;
	}

	@Test
	public void computeCells() {

		int cellWidth = 8;

		DescribeDenseHogFastAlg<GrayF32> helper = new DescribeDenseHogFastAlg<>(10,cellWidth,3,3,1,imageType);

		helper.setInput(new GrayF32(imgWidth,imgHeight));
		helper.growCellArray(imgWidth, imgHeight);

		int N = cellWidth*cellWidth;

		for (int degree = 0; degree < 360; degree++) {
			double theta = (degree*Math.PI/180.0);
			ImageMiscOps.fill(helper._getDerivX(), (float)Math.cos(theta));
			ImageMiscOps.fill(helper._getDerivY(), (float)Math.sin(theta));

			double floatBin = ((degree+90.0)*10.0/180);
			int targetBin = (int)floatBin;
			float expected = (float)(1.0-(floatBin-targetBin));

			helper.computeCellHistograms();

			targetBin %= 10;

			for (int i = 0; i < helper.cells.length; i++) {
				DescribeDenseHogFastAlg.Cell c = helper.cells[i];

				for (int j = 0; j < c.histogram.length; j++) {
					if( j == targetBin ) {
						assertEquals(N*expected,c.histogram[j],1e-3f);
					} else if( j == (targetBin+1)%10 ) {
						assertEquals(N*(1.0f-expected),c.histogram[j],1e-3f);
					} else {
						assertEquals(0.0f,c.histogram[j],1e-3f);
					}
				}
			}
		}
	}

	@Test
	public void getRegionWidthPixel() {
		DescribeDenseHogFastAlg helper = new DescribeDenseHogFastAlg(10,8,3,2,1,imageType);
		assertEquals(3*8,helper.getRegionWidthPixelX());
		assertEquals(2*8,helper.getRegionWidthPixelY());
	}


}