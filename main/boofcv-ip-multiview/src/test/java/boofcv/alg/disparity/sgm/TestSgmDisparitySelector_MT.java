/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.disparity.sgm;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */
class TestSgmDisparitySelector_MT extends BoofStandardJUnit {
	int width = 60;
	int height = 50;
	int rangeD = 30;

	/**
	 * Compte threaded to single threaded
	 */
	@Test
	void compareToSingle() {
		Planar<GrayU16> aggregatedYXD = new Planar<>(GrayU16.class, rangeD, width, height);
		GImageMiscOps.fillUniform(aggregatedYXD, rand, 0, SgmDisparityCost.MAX_COST);

		SgmDisparitySelector single = new SgmDisparitySelector();
		SgmDisparitySelector multi = new SgmDisparitySelector_MT();

		GrayU8 expected = new GrayU8(width, height);
		GrayU8 found = new GrayU8(width, height);

		for (int rightToLeft = 0; rightToLeft < 2; rightToLeft++) {
			single.setRightToLeftTolerance(rightToLeft);
			multi.setRightToLeftTolerance(rightToLeft);

			single.select(null, aggregatedYXD, expected);
			multi.select(null, aggregatedYXD, found);

			BoofTesting.assertEquals(expected, found, 0);
		}
	}
}