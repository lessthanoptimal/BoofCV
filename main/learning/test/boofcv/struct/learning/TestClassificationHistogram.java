/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.learning;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestClassificationHistogram {
	@Test
	public void reset() {
		ClassificationHistogram alg = new ClassificationHistogram(3);
		batch(alg,0,0,4);
		batch(alg,0,1,8);

		alg.reset();

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				assertEquals(0,alg.get(i,j));
			}
		}
	}

	/**
	 * Tests create confusion and increment.
	 */
	@Test
	public void createConfusion() {

		ClassificationHistogram alg = new ClassificationHistogram(3);
		batch(alg,0,0,4);
		batch(alg,0,1,8);
		batch(alg,1,1,4);
		batch(alg,2,2,1);

		Confusion c = alg.createConfusion();

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				double v = c.matrix.get(i,j);

				if (i == 0 && j == 0) {
					assertEquals(1.0 / 3.0, v, 1e-8);
				} else if (i == 0 && j == 1) {
					assertEquals(2.0 / 3.0, v, 1e-8);
				} else if (i == 2 && j == 2) {
					assertEquals(1.0, v, 1e-8);
				} else if (i == 1 && j == 1) {
					assertEquals(1.0, v, 1e-8);
				} else {
					assertEquals(0,v,1e-8 );
				}
			}
		}
	}

	private void batch( ClassificationHistogram alg , int a , int b , int num ) {
		for (int i = 0; i < num; i++) {
			alg.increment(a,b);
		}
	}

	@Test
	public void get() {
		ClassificationHistogram alg = new ClassificationHistogram(3);
		batch(alg,0,0,4);
		batch(alg,0,1,8);
		batch(alg,2,0,4);

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				int v = alg.get(i,j);

				if (i == 0 && j == 0) {
					assertEquals(4,v);
				} else if (i == 0 && j == 1) {
					assertEquals(8,v);
				} else if (i == 2 && j == 0) {
					assertEquals(4,v);
				} else {
					assertEquals(0,v );
				}
			}
		}
	}
}
