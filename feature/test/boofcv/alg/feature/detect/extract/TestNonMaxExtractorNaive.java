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

package boofcv.alg.feature.detect.extract;

import boofcv.alg.misc.ImageTestingOps;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I16;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Peter Abeles
 */
public class TestNonMaxExtractorNaive {

	/**
	 * Pass in a null list and see if it blows up
	 */
	@Test
	public void checkNullExcludeList() {
		ImageFloat32 inten = new ImageFloat32(30, 40);
		ImageTestingOps.randomize(inten, new Random(1231), 0, 10);

		QueueCorner foundList = new QueueCorner(inten.getWidth() * inten.getHeight());

		NonMaxExtractorNaive alg = new NonMaxExtractorNaive(2, 0.6F);
		alg.process(inten,null,foundList);
		// if it doesn't blow up it passed!
	}

	/**
	 *See if the exclude list is honored
	 */
	@Test
	public void excludePreExisting() {
		ImageFloat32 inten = new ImageFloat32(30, 40);
		ImageTestingOps.randomize(inten, new Random(1231), 0, 10);

		QueueCorner excludeList = new QueueCorner(inten.getWidth() * inten.getHeight());
		QueueCorner foundList = new QueueCorner(inten.getWidth() * inten.getHeight());


		NonMaxExtractorNaive alg = new NonMaxExtractorNaive(2, 0.6F);
		// find corners the first time
		alg.process(inten,excludeList,foundList);

		// add points which should be excluded
		QueueCorner cornersSecond = new QueueCorner(inten.getWidth() * inten.getHeight());
		for( int i = 0; i < 20; i++ ) {
			excludeList.add(foundList.get(i));
		}

		// recreate the same image
		ImageTestingOps.randomize(inten, new Random(1231), 0, 10);
		alg.process(inten,excludeList,cornersSecond);

		// make sure none of the features in the exclude list are in the second list
		for( int i = 0; i < excludeList.num; i++ ) {
			Point2D_I16 p = excludeList.get(i);

			for( int j = 0; j < cornersSecond.num; j++ ) {
				Point2D_I16 c = cornersSecond.get(i);

				assertFalse(c.x == p.x && p.y == c.y);
			}
		}

	}

	/**
	 * See if it produces the correct answers after adjusting the width
	 */
	@Test
	public void testRegionWidth() {
		float inten[] = new float[]{0, 1, 0, 0, 3, 4, 4, 0, 0,
				1, 0, 2, 0, 5, 0, 0, 0, 1,
				0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 9, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 1, 0,
				0, 0, 0, 4, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0};
		ImageFloat32 img = new ImageFloat32(9, 8);
		img.data = inten;

		BoofTesting.checkSubImage(this, "testRegionWidth", true, img);
	}

	public void testRegionWidth(ImageFloat32 img) {
		QueueCorner corners = new QueueCorner(100);

		NonMaxExtractorNaive extractor;
		extractor = new NonMaxExtractorNaive(1, 0);
		extractor.process(img, null , corners);
		assertEquals(5, corners.size());

		corners.reset();
		extractor.setMinSeparation(2);
		extractor.process(img, null , corners);
		assertEquals(1, corners.size());

		corners.reset();
		extractor.setMinSeparation(3);
		extractor.process(img, null , corners);
		assertEquals(1, corners.size());

		assertEquals(4, corners.get(0).x);
		assertEquals(3, corners.get(0).y);
	}

	/**
	 * Make sure it does the threshold thing correctly
	 */
	@Test
	public void testThreshold() {
		float inten[] = new float[]{0, 1, 0, 0, 3, 4, 4, 0, 0,
				1, 0, 2, 0, 5, 0, 0, 0, 1,
				0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 9, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 4, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0};
		ImageFloat32 img = new ImageFloat32(9, 8);
		img.data = inten;

		BoofTesting.checkSubImage(this, "testThreshold", true, img);
	}

	public void testThreshold(ImageFloat32 img) {
		QueueCorner corners = new QueueCorner(100);

		NonMaxExtractorNaive extractor;
		extractor = new NonMaxExtractorNaive(0, 0);
		extractor.process(img, null , corners);
		assertEquals(9 * 8, corners.size());

		corners.reset();
		extractor.setThresh(3);
		extractor.process(img, null , corners);
		assertEquals(6, corners.size());
	}
}
