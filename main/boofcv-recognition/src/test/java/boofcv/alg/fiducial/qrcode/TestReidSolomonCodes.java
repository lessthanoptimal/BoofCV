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

		// no error. All syndromes values should be zero
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

		// Evaluate it at the zeros and see if they are zero
		for (int i = 0; i < 6; i++) {
			int value = alg.math.power(2,i);
			int found = alg.math.polyEval(alg.generator,value);
			assertEquals(0,found);
		}

		// Pass in a number which should not be a zero
		assertTrue( 0 != alg.math.polyEval(alg.generator,5));
	}

	/**
	 * Computed using a reference implementation found at [1].
	 */
	@Test
	public void findErrorLocatorBM_known() {
		GrowQueue_I8 message = GrowQueue_I8.parseHex(
				"[ 0x40, 0xd2, 0x75, 0x47, 0x76, 0x17, 0x32, 0x06, 0x27, 0x26, 0x96, 0xc6, 0xc6, 0x96, 0x70, 0xec ]");
		GrowQueue_I8 ecc = new GrowQueue_I8();
		int nsyn = 10;
		int syndromes[] = new int[nsyn];

		ReidSolomonCodes alg = new ReidSolomonCodes(8,primitive8);
		alg.generator(nsyn);
		alg.computeECC(message,ecc);

		message.data[0] = 0;
		alg.computeSyndromes(message,ecc,syndromes);
		GrowQueue_I8 errorLocator = new GrowQueue_I8();
		alg.findErrorLocatorBM(syndromes,nsyn,errorLocator);
		assertEquals(2,errorLocator.size);
		assertEquals(3,errorLocator.get(0));
		assertEquals(1,errorLocator.get(1));

		message.data[6] = 10;
		alg.computeSyndromes(message,ecc,syndromes);
		alg.findErrorLocatorBM(syndromes,nsyn,errorLocator);
		assertEquals(3,errorLocator.size);
		assertEquals(238,errorLocator.get(0)&0xFF);
		assertEquals(89,errorLocator.get(1));
		assertEquals(1,errorLocator.get(2));
	}
}