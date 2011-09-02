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
import jgrl.struct.point.Point2D_I16;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;


/**
 * @author Peter Abeles
 */
public class TestFastNonMaxExtractor {
	Random rand = new Random(0x334);

	/**
	 * Pass in a null list and see if it blows up
	 */
	@Test
	public void checkNullExcludeList() {
		ImageFloat32 inten = new ImageFloat32(30, 40);
		ImageTestingOps.randomize(inten, new Random(1231), 0, 10);

		QueueCorner foundList = new QueueCorner(inten.getWidth() * inten.getHeight());

		FastNonMaxExtractor alg = new FastNonMaxExtractor(2, 2,0.6F);
		alg.process(inten,null,foundList);
		// if it doesn't blow up it passed!
	}

	/**
	 * If a non-empty list of features is passed in it should not add them again to the list nor return
	 * any similar features.
	 */
	@Test
	public void excludePreExisting() {
		ImageFloat32 inten = new ImageFloat32(30, 40);
		ImageTestingOps.randomize(inten, new Random(1231), 0, 10);

		QueueCorner excludeList = new QueueCorner(inten.getWidth() * inten.getHeight());
		QueueCorner foundList = new QueueCorner(inten.getWidth() * inten.getHeight());


		FastNonMaxExtractor alg = new FastNonMaxExtractor(2, 2,0.6F);
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
				FastNonMaxExtractor fast = new FastNonMaxExtractor(nonMaxWidth / 2, 0, 0.6F);
				NonMaxExtractorNaive reg = new NonMaxExtractorNaive(nonMaxWidth / 2, 0.6F);

				for (int i = 0; i < 10; i++) {
					ImageTestingOps.randomize(inten, rand, 0, 10);

					fast.process(inten, null,fastCorners);
					reg.process(inten, null,regCorners);

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