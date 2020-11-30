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

package boofcv.alg.segmentation.cc;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked") public abstract class CommonConnectedTwoRowSpeckleFiller<T extends ImageGray<T>>
		extends CommonConnectedSpeckleFiller<T> {
	protected CommonConnectedTwoRowSpeckleFiller( ImageType<T> imageType ) {
		super(imageType);
	}

	@Override public ConnectedSpeckleFiller<T> createAlg() {
		return createTwoRow();
	}

	protected abstract ConnectedTwoRowSpeckleFiller<T> createTwoRow();

	/**
	 * Checks to see if internal checks get triggered. This caught some difficult bugs. Increase 'w'' to make it more
	 * rigorous. Try 100 or 1000
	 */
	@Test void random() {
		ConnectedTwoRowSpeckleFiller<T> alg = createTwoRow();

		int w = 20;
		int r = 5;
		int numTrials = 1000;

		for (int trial = 0; trial < numTrials; trial++) {
			float fillValue = 3.0f;
			var image = input.createNew(w + rand.nextInt(r), w + rand.nextInt(r));
			GImageMiscOps.fillUniform(image, rand, 0.0f, fillValue);
			alg.process(image, 20, 1.0f, fillValue);
		}
	}

	@Test void compareToNaive() {
		ConnectedSpeckleFiller<T> naive = input.imageType.getDataType().isInteger() ?
				new ConnectedNaiveSpeckleFiller_Int(ImageType.SB_U8) :
				(ConnectedSpeckleFiller)new ConnectedNaiveSpeckleFiller_F32();
		ConnectedTwoRowSpeckleFiller<T> alg = createTwoRow();

		double fillValue = 3.0f;
		T found = input.createNew(100, 90);
		GImageMiscOps.fillUniform(found, rand, 0.0f, fillValue);
		T expected = found.clone();
		naive.process(expected, 20, 1.0f, fillValue);
		alg.process(found, 20, 1.0f, fillValue);

		BoofTesting.assertEquals(expected, found, 0.0);
	}

	/** Give it a non-trivial situation where multiple hops are required to merge */
	@Test void mergeClustersInB() {
		ConnectedTwoRowSpeckleFiller<T> alg = createTwoRow();

		alg.labelsB.setTo(0, 0, -1, 1, 2, 3, 4);
		alg.countsB.setTo(1, 2, 3, 4, 5);
		alg.merge.setTo(-1, 0, -1, 4, 1);
		alg.mergeClustersInB();

		assertTrue(alg.countsB.isEquals(12, 0, 3, 0, 0));
		assertTrue(alg.labelsB.isEquals(0, 0, -1, 0, 2, 0, 0));
	}

	@Test void addCountsRowAIntoB() {
		ConnectedTwoRowSpeckleFiller<T> alg = createTwoRow();

		alg.connectAtoB.setTo(-1, 1, 0, -1);
		alg.countsA.setTo(1, 2, 3, 4);
		alg.labelsB.setTo(0, 0, -1, 1, 2, 3, 4);
		alg.countsB.setTo(6, 7, 8, 9, 0);
		alg.merge.setTo(-1, 0, -1, -1, -1);
		alg.addCountsRowAIntoB();

		assertTrue(alg.finished.isEquals(0, 3));
		assertTrue(alg.countsB.isEquals(11, 7, 8, 9, 0));
	}

	/** Nominal case with a simple graph */
	@Test void findConnectionsBetweenRows() {
		T image = expected.createNew(7, 4);
		image._setData(createArray(1,
				4, 3, 3, 8, 1, 2, 8,
				4, 9, 2, 8, 8, 8, 8));

		ConnectedTwoRowSpeckleFiller<T> alg = createTwoRow();
		alg.image = image;
		alg.initTypeSpecific(0.9, 10);
		alg.countsA.size = 6;
		alg.countsB.size = 4;
		alg.labelsA.setTo(0, 1, 1, 2, 3, 4, 5);
		alg.labelsB.setTo(0, 1, 2, 3, 3, 3, 3);
		alg.findConnectionsBetweenRows(1, 8);

		assertTrue(alg.connectAtoB.isEquals(0, -1, 3, -1, -1, 3));
		assertTrue(alg.merge.isEquals(-1, -1, -1, -1));
	}

	/** A fill value matches one of the element values */
	@Test void findConnectionsBetweenRows_fillValue() {
		T image = expected.createNew(7, 4);
		image._setData(createArray(1,
				4.1f, 3, 3, 4, 1, 2, 8,
				4, 9, 2, 4.1f, 8, 8, 8));

		ConnectedTwoRowSpeckleFiller<T> alg = createTwoRow();
		alg.image = image;
		alg.initTypeSpecific(0.9, 4);
		alg.countsA.size = 5;
		alg.countsB.size = 4;
		alg.labelsA.setTo(0, 1, 1, -1, 2, 3, 4);
		alg.labelsB.setTo(-1, 0, 1, 2, 3, 3, 3);
		alg.findConnectionsBetweenRows(1, 8);

		assertTrue(alg.connectAtoB.isEquals(-1, -1, -1, -1, 3));
		assertTrue(alg.merge.isEquals(-1, -1, -1, -1));
	}

	/** Scenario where labels wil need to be merged in B */
	@Test void findConnectionsBetweenRows_merge() {
		T image = expected.createNew(7, 4);
		image._setData(createArray(1,
				4, 3, 4, 8, 8, 8, 8,
				4, 4, 4, 8, 2, 1, 8));

		ConnectedTwoRowSpeckleFiller<T> alg = createTwoRow();
		alg.image = image;

		alg.initTypeSpecific(0.9, 10.0);
		alg.countsA.size = 4;
		alg.countsB.size = 5;
		alg.labelsA.setTo(0, 1, 2, 3, 3, 3, 3);
		alg.labelsB.setTo(0, 0, 0, 1, 2, 3, 4);
		alg.findConnectionsBetweenRows(1, 8);

		assertTrue(alg.connectAtoB.isEquals(0, -1, 0, 1));
		assertTrue(alg.merge.isEquals(-1, -1, -1, -1, 1));
	}

	protected abstract Object createArray( double... values );
}
