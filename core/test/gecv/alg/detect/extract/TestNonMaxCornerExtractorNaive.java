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

package gecv.alg.detect.extract;

import gecv.core.image.UtilImageFloat32;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageFloat32;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestNonMaxCornerExtractorNaive {

	/**
	 * If a non-empty list of features is passed in it should not add them again to the list nor return
	 * any similar features.
	 */
	@Test
	public void excludePreExisting() {
		ImageFloat32 inten = new ImageFloat32(30, 40);
		UtilImageFloat32.randomize(inten, new Random(1231), 0, 10);

		QueueCorner cornersFirst = new QueueCorner(inten.getWidth() * inten.getHeight());

		NonMaxCornerExtractorNaive alg = new NonMaxCornerExtractorNaive(2, 0.6F);
		// find corners the first time
		alg.process(inten,cornersFirst);

		// add points which should be excluded
		QueueCorner cornersSecond = new QueueCorner(inten.getWidth() * inten.getHeight());
		for( int i = 0; i < 20; i++ ) {
			cornersSecond.add(cornersFirst.get(i));
		}

		// recreate the same image
		UtilImageFloat32.randomize(inten, new Random(1231), 0, 10);
		alg.process(inten,cornersSecond);
		assertEquals(cornersSecond.size(),cornersFirst.size());

		//make sure it isn't just clearing the list and finding the same corners again
		UtilImageFloat32.fill(inten,0);
		alg.process(inten,cornersSecond);
		assertEquals(cornersSecond.size(),cornersFirst.size());
		cornersSecond.reset();
		alg.process(inten,cornersSecond);
		assertEquals(cornersSecond.size(),0);
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

		GecvTesting.checkSubImage(this, "testRegionWidth", true, img);
	}

	public void testRegionWidth(ImageFloat32 img) {
		QueueCorner corners = new QueueCorner(100);

		NonMaxCornerExtractorNaive extractor;
		extractor = new NonMaxCornerExtractorNaive(1, 0);
		extractor.process(img, corners);
		assertEquals(5, corners.size());

		corners.reset();
		extractor.setMinSeparation(2);
		extractor.process(img, corners);
		assertEquals(1, corners.size());

		corners.reset();
		extractor.setMinSeparation(3);
		extractor.process(img, corners);
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

		GecvTesting.checkSubImage(this, "testThreshold", true, img);
	}

	public void testThreshold(ImageFloat32 img) {
		QueueCorner corners = new QueueCorner(100);

		NonMaxCornerExtractorNaive extractor;
		extractor = new NonMaxCornerExtractorNaive(0, 0);
		extractor.process(img, corners);
		assertEquals(9 * 8, corners.size());

		corners.reset();
		extractor.setThresh(3);
		extractor.process(img, corners);
		assertEquals(6, corners.size());
	}
}
