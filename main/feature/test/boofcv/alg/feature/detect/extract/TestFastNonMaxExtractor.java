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
import georegression.struct.point.Point2D_I16;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestFastNonMaxExtractor {
	Random rand = new Random(0x334);


	/**
	 * See if it correctly ignores features which  are equal to MAX_VALUE
	 */
	@Test
	public void exclude_MAX_VALUE() {
		ImageFloat32 inten = new ImageFloat32(30, 40);

		inten.set(15,20,Float.MAX_VALUE);
		inten.set(20,21,Float.MAX_VALUE);
		inten.set(10,25,Float.MAX_VALUE);
		inten.set(11,24,10);
		inten.set(25,35,10);


		QueueCorner foundList = new QueueCorner(100);
		FastNonMaxExtractor alg = new FastNonMaxExtractor(2, 0.6F);
		// find corners the first time
		alg.process(inten,foundList);

		// only one feature should be found.  The rest should be MAX_VALUE or too close to MAX_VALUE
		assertEquals(1,foundList.size);
		assertEquals(25,foundList.data[0].x);
		assertEquals(35,foundList.data[0].y);
	}

	/**
	 * Checks to see if {@link FastNonMaxExtractor} produces exactly the same results as
	 * {@link NonMaxExtractorNaive}
	 */
	@Test
	public void compareToNaive() {

		ImageFloat32 inten = new ImageFloat32(30, 40);

		QueueCorner fastCorners = new QueueCorner(inten.getWidth() * inten.getHeight());
		QueueCorner regCorners = new QueueCorner(inten.getWidth() * inten.getHeight());

		for (int useSubImage = 0; useSubImage < 2; useSubImage++) {
			// make sure it handles sub images correctly
			if (useSubImage == 1) {
				ImageFloat32 larger = new ImageFloat32(inten.width + 10, inten.height + 8);
				inten = larger.subimage(0, 0, 30, 40);
			}

			for (int nonMaxWidth = 3; nonMaxWidth <= 9; nonMaxWidth += 2) {
				FastNonMaxExtractor fast = new FastNonMaxExtractor(nonMaxWidth / 2, 0.6F);
				NonMaxExtractorNaive reg = new NonMaxExtractorNaive(nonMaxWidth / 2, 0.6F);

				for (int i = 0; i < 10; i++) {
					ImageTestingOps.randomize(inten, rand, 0, 10);

					fast.process(inten, fastCorners);
					reg.process(inten, regCorners);

					assertTrue(fastCorners.size() > 0);

					assertEquals(regCorners.size(), fastCorners.size());

					for (int j = 0; j < regCorners.size(); j++) {
						Point2D_I16 a = fastCorners.get(j);
						Point2D_I16 b = regCorners.get(j);

						assertEquals(b.getX(), a.getX());
						assertEquals(b.getY(), a.getY());
					}
				}
			}
		}
	}
}