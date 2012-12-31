/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.interpolate.array;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestPolynomialNevilleFixed_F32 {

	Random rand = new Random(0xf342);

	int num = 30;

	@Test
	public void compareToGeneral() {
		// create some random data
		float x[] = new float[num];
		float y[] = new float[num];

		for (int i = 0; i < num; i++) {
			x[i] = i;
			y[i] = rand.nextFloat() * 10;
		}

		PolynomialNeville_F32 general = new PolynomialNeville_F32(num - 1, x, y, num);
		PolynomialNevilleFixed_F32 alg = new PolynomialNevilleFixed_F32(num, y, num);

		for (int i0 = 0; i0 < num; i0 += 2) {
			for (int M = 2; M < 20; M++) {
				if (i0 + M >= num) break;
//                System.out.println("i0 = "+i0);
				general.changeDegree(M);

				float sample = x[i0 + M / 2] + 0.1f;

				float expected = general.process(i0, sample);
				float found = alg.process(sample, i0, i0 + M);

				assertEquals(expected, found, 1e-5);
			}
		}
	}
}
