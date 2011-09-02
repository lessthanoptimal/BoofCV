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

package boofcv.alg.feature.detect.extract;

import boofcv.alg.misc.ImageTestingOps;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestNonMaxCandidateExtractor {

	/**
	 * Pass in a null list and see if it blows up
	 */
	@Test
	public void checkNullExcludeList() {
		ImageFloat32 inten = new ImageFloat32(30, 40);
		ImageTestingOps.randomize(inten, new Random(1231), 0, 10);

		QueueCorner foundList = new QueueCorner(inten.getWidth() * inten.getHeight());
		QueueCorner candidates = new QueueCorner(100);

		NonMaxCandidateExtractor alg = new NonMaxCandidateExtractor(2, 0.6F);
		alg.process(inten,candidates,null,foundList);
		// if it doesn't blow up it passed!
	}

	/**
	 * If a list of pre-existing corners is added they should not be added again to the found list
	 */
	@Test
	public void excludePreExisting() {
		ImageFloat32 img = createTestImage();

		QueueCorner corners = new QueueCorner(100);
		QueueCorner candidates = new QueueCorner(100);
		QueueCorner exclude = new QueueCorner(100);

		candidates.add(4,3);
		candidates.add(3,5);

		// see if it detects everything
		NonMaxCandidateExtractor extractor;
		extractor = new NonMaxCandidateExtractor(1, 0);
		extractor.process(img, candidates, exclude, corners);
		assertEquals(2, corners.size());

		// now exclude one of them by adding a near by exclude point
		corners.reset();
		exclude.add(2,5);
		extractor.process(img, candidates, exclude,corners);
		assertEquals(1, corners.size());

	}

	/**
	 * See if it produces the correct answers after adjusting the width
	 */
	@Test
	public void testRegionWidth() {
		ImageFloat32 img = createTestImage();

		GecvTesting.checkSubImage(this, "testRegionWidth", true, img);
	}

	public void testRegionWidth(ImageFloat32 img) {
		QueueCorner corners = new QueueCorner(100);
		QueueCorner candidates = new QueueCorner(100);

		candidates.add(4,3);
		candidates.add(3,5);

		NonMaxCandidateExtractor extractor;
		extractor = new NonMaxCandidateExtractor(1, 0);
		extractor.process(img, candidates, null,corners);
		assertEquals(2, corners.size());

		corners.reset();
		extractor.setMinSeparation(3);
		extractor.process(img, candidates, null,corners);
		assertEquals(1, corners.size());
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
		QueueCorner candidates = new QueueCorner(100);

		candidates.add(4,3);
		candidates.add(3,5);

		NonMaxCandidateExtractor extractor;
		extractor = new NonMaxCandidateExtractor(0, 0);
		extractor.process(img, candidates, null,corners);
		assertEquals(2, corners.size());

		corners.reset();
		extractor.setThresh(5);
		extractor.process(img, candidates, null,corners);
		assertEquals(1, corners.size());
	}

	private ImageFloat32 createTestImage() {
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
		return img;
	}
}
