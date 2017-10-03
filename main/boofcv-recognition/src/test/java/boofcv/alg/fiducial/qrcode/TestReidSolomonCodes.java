/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import org.ddogleg.struct.GrowQueue_I8;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestReidSolomonCodes {
	Random rand = new Random(234);
	int primitive2 = 0b111;
	int primitive8 = 0b100011101;

	@Test
	public void computeECC() {
		GrowQueue_I8 message = new GrowQueue_I8();
		for (int i = 0; i < 50; i++) {
			message.add(rand.nextInt(256));
		}
		GrowQueue_I8 ecc = new GrowQueue_I8();

		ReidSolomonCodes alg = new ReidSolomonCodes(8,primitive8);
		alg.generator(6);
		alg.computeECC(message,ecc);

		assertEquals(6,ecc.size);

		int numNotZero = 0;
		for (int i = 0; i < ecc.size; i++) {
			if( 0 != ecc.data[i])
				numNotZero++;
		}
		assertTrue(numNotZero>=5);

		// numerical properties are tested by computeSyndromes
	}

	@Test
	public void computeSyndromes() {
		GrowQueue_I8 message = new GrowQueue_I8();
		for (int i = 0; i < 50; i++) {
			message.add(rand.nextInt(256));
		}
		GrowQueue_I8 ecc = new GrowQueue_I8();

		ReidSolomonCodes alg = new ReidSolomonCodes(8,primitive8);
		alg.generator(6);
		alg.computeECC(message,ecc);

		int syndromes[] = new int[6];
		alg.computeSyndromes(message,ecc,syndromes);

		// no error. All values should be zero
		for (int i = 0; i < syndromes.length; i++) {
			assertEquals(0,syndromes[i]);
		}

		// introduce an error
		message.data[6] += 7;
		alg.computeSyndromes(message,ecc,syndromes);

		int notZero = 0;
		for (int i = 0; i < syndromes.length; i++) {
			if( syndromes[i] != 0 )
				notZero++;
		}
		assertTrue(notZero > 1);
	}

	@Test
	public void generator() {
		ReidSolomonCodes alg = new ReidSolomonCodes(8,primitive8);
		alg.generator(5);

		for (int i = 0; i < 6; i++) {
			int value = alg.math.power(2,i);
			int found = alg.math.polyEval(alg.generator,value);
			assertEquals(0,found);
		}

		assertTrue( 0 != alg.math.polyEval(alg.generator,5));
	}
}