/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.describe;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
abstract class GenericDescribePointChecks<T extends ImageBase<T>, TD extends TupleDesc<TD>>
		extends BoofStandardJUnit {
	protected ImageType<T> imageType;

	int width = 50;
	int height = 40;

	protected abstract DescribePoint<T, TD> createAlg();

	protected GenericDescribePointChecks( ImageType<T> imageType ) {
		this.imageType = imageType;
	}

	/**
	 * The image has texture and the descriptor should not be all zeros
	 */
	@Test void notFilledWithZeros() {
		T image = imageType.createImage(width, height);
		GImageMiscOps.fillUniform(image, rand, 0, 100);

		DescribePoint<T, TD> describe = createAlg();
		TD desc = describe.createDescription();

		describe.setImage(image);
		describe.process(20, 30, desc);

		int numZeros = 0;
		for (int i = 0; i < desc.size(); i++) {
			if (0.0 == desc.getDouble(i))
				numZeros++;
		}
		assertTrue(numZeros != desc.size());
	}

	/**
	 * Tell it to process the same locations multiple times and sanity check the response
	 */
	@Test void multipleCallsSameResponse() {
		T image = imageType.createImage(width, height);
		GImageMiscOps.fillUniform(image, rand, 0, 100);

		DescribePoint<T, TD> describe = createAlg();
		TD desc0 = describe.createDescription();
		TD desc1 = describe.createDescription();

		// sample points inside the image since those are unlikely to have border issues
		describe.setImage(image);
		describe.process(20, 30, desc0);
		describe.process(25, 26, desc1);

		TD tmp = describe.createDescription();
		describe.process(20, 30, tmp);
		checkEquals(desc0, tmp);
		describe.process(25, 26, tmp);
		checkEquals(desc1, tmp);
	}

	/**
	 * Process at the image borders and see if it blows up
	 */
	@Test void blowUpAtImageBorder() {
		T image = imageType.createImage(width, height);

		DescribePoint<T, TD> describe = createAlg();
		describe.setImage(image);

		TD description = describe.createDescription();

		// Check the corners
		describe.setImage(image);
		describe.process(0, 0, description);
		describe.process(width-1, 0, description);
		describe.process(0, height-1, description);
		describe.process(width-1, height-1, description);
	}

	void checkEquals( TupleDesc<?> a, TupleDesc<?> b ) {
		assertEquals(a.size(), b.size());

		int totalMissMatches = 0;
		for (int i = 0; i < a.size(); i++) {
			if (Math.abs(a.getDouble(i) - b.getDouble(i)) > 0.1) {
				totalMissMatches++;
			}
		}
		assertTrue(totalMissMatches <= a.size()/32);
	}
}
