/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.selector;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.FastQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Generic tests that applied to all {@link FeatureSelectLimitIntensity}.
 */
public abstract class ChecksFeatureSelectLimitIntensity<Point> {

	Random rand = BoofTesting.createRandom(0);

	int width=30;
	int height=20;
	GrayF32 intensity = new GrayF32(width,height);

	public abstract FeatureSelectLimitIntensity<Point> createAlgorithm();

	public abstract FastArray<Point> createArray();

	@BeforeEach
	public void setup() {
		GImageMiscOps.fillUniform(intensity,rand,-1,1);
	}

	/**
	 * Should just copy detected corners in this case. prior is null and less than max
	 */
	@Test
	void lessThanMax_and_SelectedCleared() {
		FastQueue<Point> detected = createRandom(15);

		FeatureSelectLimitIntensity<Point> alg = createAlgorithm();
		FastArray<Point> found = createArray();

		for (int count = 0; count < 2; count++) {
			alg.select(intensity,count==0,null,detected,30,found);

			// partial check to make sure the input wasn't modified
			assertEquals(15, detected.size);

			// see if there's the expected count in the output. The order should also be the same
			assertEquals(15, found.size);
			for (int i = 0; i < found.size; i++) {
				assertSame(found.get(i), detected.get(i));
			}
		}
	}

	/**
	 * Makes sure select is cleared when called multiple times and there are more featuers than requested
	 */
	@Test
	void multipleCalls_MoreThan_SelectCleared() {
		FastQueue<Point> prior = createRandom(20);

		FeatureSelectLimitIntensity<Point> alg = createAlgorithm();
		FastArray<Point> found = createArray();

		for (int count = 0; count < 2; count++) {
			FastQueue<Point> detected = createRandom(30);
			alg.select(intensity,count==0,prior,detected,22,found);

			// partial check to make sure the input wasn't modified
			assertEquals(20, prior.size);
			assertEquals(30, detected.size);

			// see if there's the expected count in the output. The order should also be the same
			assertEquals(22, found.size);
			// Make sure elements from previous calls were not saved and returned
			for (int i = 0; i < found.size; i++) {
				assertTrue(detected.contains(found.get(i)));
			}
		}
	}

	/**
	 * Shouldn't blow up
	 */
	@Test
	void priorIsBlowUp() {
		FastQueue<Point> prior = createRandom(20);
		FeatureSelectLimitIntensity<Point> alg = createAlgorithm();
		FastArray<Point> found = createArray();

		alg.select(intensity,true,prior,createRandom(15),30,found);
		alg.select(intensity,true,prior,createRandom(15),10,found);
		alg.select(intensity,true,null,createRandom(15),30,found);
		alg.select(intensity,true,null,createRandom(15),10,found);
	}

	protected abstract FastQueue<Point> createRandom(int i2);

	public static abstract class I16 extends ChecksFeatureSelectLimitIntensity<Point2D_I16> {
		@Override
		public FastArray<Point2D_I16> createArray() {
			return new FastArray<>(Point2D_I16.class);
		}

		@Override
		protected FastQueue<Point2D_I16> createRandom(int i2) {
			FastQueue<Point2D_I16> detected = new FastQueue<>(Point2D_I16::new);
			for (int i = 0; i < i2; i++) {
				detected.grow().set(rand.nextInt(width), rand.nextInt(height));
			}
			return detected;
		}
	}

}
