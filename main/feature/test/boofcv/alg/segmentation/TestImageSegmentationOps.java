/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImageSegmentationOps {

	@Test
	public void countRegionPixels_single() {
		ImageSInt32 output = new ImageSInt32(4,5);

		output.data = new int[]{
				0, 0, 0, 1,
				1, 1, 1, 1,
				0, 0, 3, 3,
				2, 2, 2, 2,
				2, 2, 2, 2};

		assertEquals(5,ImageSegmentationOps.countRegionPixels(output,0));
		assertEquals(5,ImageSegmentationOps.countRegionPixels(output,1));
		assertEquals(8,ImageSegmentationOps.countRegionPixels(output,2));
		assertEquals(2,ImageSegmentationOps.countRegionPixels(output,3));
	}

	@Test
	public void countRegionPixels_all() {
		ImageSInt32 output = new ImageSInt32(4,5);

		output.data = new int[]{
				0, 0, 0, 1,
				1, 1, 1, 1,
				0, 0, 3, 3,
				2, 2, 2, 2,
				2, 2, 2, 2};

		int counts[] = new int[10];
		ImageSegmentationOps.countRegionPixels(output,4,counts);

		assertEquals(5,counts[0]);
		assertEquals(5,counts[1]);
		assertEquals(8,counts[2]);
		assertEquals(2,counts[3]);
	}

	/**
	 * Manually construct input data and see if it has the expected output
	 */
	@Test
	public void regionPixelId_to_Compact() {
		ImageSInt32 graph = new ImageSInt32(4,5);
		ImageSInt32 output = new ImageSInt32(4,5);

		regionPixelId_to_Compact(graph, output);
		regionPixelId_to_Compact(BoofTesting.createSubImageOf(graph), output);
		regionPixelId_to_Compact(graph, BoofTesting.createSubImageOf(output));
	}

	private void regionPixelId_to_Compact(ImageSInt32 graph, ImageSInt32 output) {
		ImageSInt32 input = new ImageSInt32(4,5);
		input.data = new int[]{
				2, 2, 2, 5,
				5, 5, 5, 5,
				2, 2, 2, 2,
				15,15,15,15,
				15,15,15,15};

		// graph might be a sub-image
		for( int y = 0; y < graph.height; y++ ) {
			for( int x = 0; x < graph.width; x++ ) {
				graph.set(x,y,adjust(input.get(x, y), graph));
			}
		}

		GrowQueue_I32 rootNodes = new GrowQueue_I32();
		rootNodes.add(adjust(2,graph));
		rootNodes.add(adjust(5,graph));
		rootNodes.add(adjust(15,graph));

		ImageSegmentationOps.regionPixelId_to_Compact(graph, rootNodes, output);

		ImageSInt32 expected = new ImageSInt32(4,5);
		expected.data = new int[]{
				0, 0, 0, 1,
				1, 1, 1, 1,
				0, 0, 0, 0,
				2, 2, 2, 2,
				2, 2, 2, 2};

		BoofTesting.assertEquals(expected, output, 1e-4);
	}

	/**
	 * Change the image index from one image to another
	 */
	private int adjust( int index , ImageBase image ) {
		int x = index%image.width;
		int y = index/image.width;

		return image.getIndex(x,y);
	}

	@Test
	public void markRegionBorders1() {
		ImageSInt32 input = new ImageSInt32(4,5);
		input.data = new int[]{
				0, 0, 0, 1,
				0, 0, 0, 1,
				0, 2, 2, 2,
				0, 2, 2, 2,
				0, 2, 2, 2};

		ImageUInt8 expected = new ImageUInt8(4,5);
		expected.data = new byte[]{
				0, 0, 1, 1,
				0, 1, 1, 1,
				1, 1, 1, 1,
				1, 1, 0, 0,
				1, 1, 0, 0};

		ImageUInt8 found = new ImageUInt8(4,5);

		ImageSegmentationOps.markRegionBorders(input, found);
		BoofTesting.assertEquals(expected, found, 1e-4);
	}

	@Test
	public void markRegionBorders2() {
		ImageSInt32 input = new ImageSInt32(4,5);
		input.data = new int[]{
				0, 0, 0, 0,
				0, 0, 0, 0,
				0, 0, 0, 0,
				0, 0, 0, 0,
				0, 0, 0, 2};

		ImageUInt8 expected = new ImageUInt8(4,5);
		expected.data = new byte[]{
				0, 0, 0, 0,
				0, 0, 0, 0,
				0, 0, 0, 0,
				0, 0, 0, 1,
				0, 0, 1, 1};

		ImageUInt8 found = new ImageUInt8(4,5);

		ImageSegmentationOps.markRegionBorders(input, found);
		BoofTesting.assertEquals(expected, found, 1e-4);
	}

}
