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

package boofcv.alg.fiducial.qrcode;

import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.DogArray_I8;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestReidSolomonCodes extends BoofStandardJUnit {

	int primitive8 = 0b100011101;

	@Test void computeECC() {
		DogArray_I8 message = randomMessage(50);
		DogArray_I8 ecc = new DogArray_I8();

		ReidSolomonCodes alg = new ReidSolomonCodes(8, primitive8);
		alg.generator(6);
		alg.computeECC(message, ecc);

		assertEquals(6, ecc.size);

		int numNotZero = 0;
		for (int i = 0; i < ecc.size; i++) {
			if (0 != ecc.data[i])
				numNotZero++;
		}
		assertTrue(numNotZero >= 5);

		// numerical properties are tested by computeSyndromes
	}

	/**
	 * Compare against results from python tutorial
	 */
	@Test void computeECC_python() {
		byte[] a = new byte[]{0x40, (byte)0xd2, 0x75, 0x47, 0x76, 0x17, 0x32,
				0x06, 0x27, 0x26, (byte)0x96, (byte)0xc6, (byte)0xc6, (byte)0x96, 0x70, (byte)0xec};
		byte[] b = new byte[]{(byte)0xbc, 0x2a, (byte)0x90, 0x13, 0x6b,
				(byte)0xaf, (byte)0xef, (byte)0xfd, 0x4b, (byte)0xe0};

		DogArray_I8 message = new DogArray_I8();
		DogArray_I8 ecc = new DogArray_I8();
		message.data = a;
		message.size = a.length;

		ReidSolomonCodes alg = new ReidSolomonCodes(8, 0x11d);
		alg.generator(10);
		alg.computeECC(message, ecc);

		assertEquals(10, ecc.size);
		for (int i = 0; i < b.length; i++) {
			assertEquals(b[i], ecc.data[i]);
		}
	}

	@Test void computeSyndromes() {
		DogArray_I8 message = randomMessage(50);
		DogArray_I8 ecc = new DogArray_I8();

		ReidSolomonCodes alg = new ReidSolomonCodes(8, primitive8);
		alg.generator(6);
		alg.computeECC(message, ecc);

		DogArray_I8 syndromes = DogArray_I8.zeros(6);
		alg.computeSyndromes(message, ecc, syndromes);

		// no error. All syndromes values should be zero
		for (int i = 0; i < syndromes.size; i++) {
			assertEquals(0, syndromes.data[i]);
		}

		// introduce an error
		message.data[6] += (byte)7;
		alg.computeSyndromes(message, ecc, syndromes);

		int notZero = 0;
		for (int i = 0; i < syndromes.size; i++) {
			if (syndromes.data[i] != 0)
				notZero++;
		}
		assertTrue(notZero > 1);
	}

	private DogArray_I8 randomMessage( int N ) {
		DogArray_I8 message = new DogArray_I8();
		for (int i = 0; i < N; i++) {
			message.add(rand.nextInt(256));
		}
		return message;
	}

	@Test void generator() {
		ReidSolomonCodes alg = new ReidSolomonCodes(8, primitive8);
		alg.generator(5);

		// Evaluate it at the zeros and see if they are zero
		for (int i = 0; i < 5; i++) {
			int value = alg.math.power(2, i);
			int found = alg.math.polyEval(alg.generator, value);
			assertEquals(0, found);
		}

		// Pass in a number which should not be a zero
		assertTrue(0 != alg.math.polyEval(alg.generator, 5));
	}

	/**
	 * Computed using a reference implementation found at [1].
	 */
	@Test void findErrorLocatorPolynomialBM() {
		DogArray_I8 message = DogArray_I8.parseHex(
				"[ 0x40, 0xd2, 0x75, 0x47, 0x76, 0x17, 0x32, 0x06, 0x27, 0x26, 0x96, 0xc6, 0xc6, 0x96, 0x70, 0xec ]");
		DogArray_I8 ecc = new DogArray_I8();
		int nsyn = 10;
		DogArray_I8 syndromes = DogArray_I8.zeros(nsyn);

		ReidSolomonCodes alg = new ReidSolomonCodes(8, primitive8);
		alg.generator(nsyn);
		alg.computeECC(message, ecc);

		message.data[0] = 0;
		alg.computeSyndromes(message, ecc, syndromes);
		DogArray_I8 errorLocator = new DogArray_I8();
		alg.findErrorLocatorPolynomialBM(syndromes, errorLocator);
		assertEquals(2, errorLocator.size);
		assertEquals(3, errorLocator.get(0));
		assertEquals(1, errorLocator.get(1));

		message.data[6] = 10;
		alg.computeSyndromes(message, ecc, syndromes);
		alg.findErrorLocatorPolynomialBM(syndromes, errorLocator);
		assertEquals(3, errorLocator.size);
		assertEquals(238, errorLocator.get(0) & 0xFF);
		assertEquals(89, errorLocator.get(1));
		assertEquals(1, errorLocator.get(2));
	}

	/**
	 * Compares the results from BM against an error locator polynomial computed directly given the known
	 * error locations
	 */
	@Test void findErrorLocatorPolynomialBM_compareToDirect() {

		DogArray_I8 found = new DogArray_I8();
		DogArray_I8 expected = new DogArray_I8();

		for (int i = 0; i < 30; i++) {
			int N = 50;
			DogArray_I8 message = randomMessage(N);

			DogArray_I8 ecc = new DogArray_I8();
			int nsyn = 10;
			DogArray_I8 syndromes = DogArray_I8.zeros(nsyn);

			ReidSolomonCodes alg = new ReidSolomonCodes(8, primitive8);
			alg.generator(nsyn);
			alg.computeECC(message, ecc);

			int where = rand.nextInt(N);
			message.data[where] ^= (byte)0x12;
			alg.computeSyndromes(message, ecc, syndromes);

			DogArray_I32 whereList = new DogArray_I32(1);
			whereList.add(where);

			alg.findErrorLocatorPolynomialBM(syndromes, found);
			alg.findErrorLocatorPolynomial(N + ecc.size, whereList, expected);

			assertEquals(found.size, expected.size);
			for (int j = 0; j < found.size; j++) {
				assertEquals(found.get(j), expected.get(j));
			}
		}
	}

	/**
	 * Test positive cases
	 */
	@Test void findErrors_BruteForce() {
		DogArray_I8 message = randomMessage(50);
		for (int i = 0; i < 200; i++) {
			findErrors_BruteForce(message, rand.nextInt(5), false);
		}
	}

	public void findErrors_BruteForce( DogArray_I8 message, int numErrors, boolean expectedFail ) {
		DogArray_I8 ecc = new DogArray_I8();
		int nsyn = 10;
		DogArray_I8 syndromes = DogArray_I8.zeros(nsyn);
		DogArray_I8 errorLocator = new DogArray_I8();
		DogArray_I32 locations = new DogArray_I32();

		ReidSolomonCodes alg = new ReidSolomonCodes(8, primitive8);
		alg.generator(nsyn);
		alg.computeECC(message, ecc);

		DogArray_I8 cmessage = message.copy();

		// corrupt the message and ecc
		int N = message.size + ecc.size;
		int[] corrupted = selectN(numErrors, N);
		for (int i = 0; i < corrupted.length; i++) {
			int w = corrupted[i];
			if (w < message.size)
				cmessage.data[w] ^= (byte)0x45;
			else {
				ecc.data[w - message.size] ^= (byte)0x45;
			}
		}
		// compute needed info
		alg.computeSyndromes(cmessage, ecc, syndromes);
		alg.findErrorLocatorPolynomialBM(syndromes, errorLocator);

		if (expectedFail) {
			assertFalse(alg.findErrorLocations_BruteForce(errorLocator, N, locations));
		} else {

			// find the error locations
			assertTrue(alg.findErrorLocations_BruteForce(errorLocator, N, locations));

			// see if it found the expected number of errors and that the locations match
			assertEquals(numErrors, locations.size);

			for (int i = 0; i < locations.size; i++) {
				int num = 0;
				for (int j = 0; j < corrupted.length; j++) {
					if (corrupted[j] == locations.data[i]) {
						num++;
					}
				}
				assertEquals(1, num);
			}

			DogArray_I8 hack = new DogArray_I8();
			alg.findErrorLocatorPolynomial(N, locations, hack);
		}
	}

	/**
	 * Test a case where there are too many errors.
	 */
	@Test void findErrors_BruteForce_TooMany() {
		DogArray_I8 message = randomMessage(50);
		findErrors_BruteForce(message, 6, true);
		findErrors_BruteForce(message, 8, true);
	}

	public int[] selectN( int setSize, int maxValue ) {
		int[] a = new int[maxValue];

		for (int i = 0; i < maxValue; i++) {
			a[i] = i;
		}
		for (int i = 0; i < setSize; i++) {
			int selected = rand.nextInt(maxValue - i) + i;
			int tmp = a[selected];
			a[selected] = a[i];
			a[i] = tmp;
		}

		int[] out = new int[setSize];
		System.arraycopy(a, 0, out, 0, setSize);
		return out;
	}

	/**
	 * Compare solution to reference code
	 */
	@Test void findErrorEvaluator() {

		// one error
		findErrorEvaluator(array(64, 192, 93, 231, 52, 92, 228, 49, 83, 2455),
				array(3, 1),
				array(0, 64));

		// two errors
		findErrorEvaluator(array(62, 101, 255, 19, 236, 196, 112, 227, 174, 215),
				array(159, 118, 1),
				array(0, 62, 142));

		// three errors
		findErrorEvaluator(array(32, 188, 7, 92, 8, 39, 184, 32, 101, 213),
				array(97, 138, 194, 1),
				array(0, 32, 217, 182));
	}

	private void findErrorEvaluator( DogArray_I8 syndromes,
									 DogArray_I8 errorLocator,
									 DogArray_I8 expected ) {
		DogArray_I8 found = new DogArray_I8();
		ReidSolomonCodes alg = new ReidSolomonCodes(8, primitive8);
		alg.findErrorEvaluator(syndromes, errorLocator, found);

		assertEquals(found.size, expected.size);
		for (int j = 0; j < found.size; j++) {
			assertEquals(found.get(j), expected.get(j));
		}
	}

	/**
	 * Compare against a hand computed scenario
	 */
	@Test void correctErrors_hand() {
		DogArray_I8 message = DogArray_I8.parseHex(
				"[ 0x40, 0xd2, 0x75, 0x47, 0x76, 0x17, 0x32, 0x06, 0x27, 0x26, 0x96, 0xc6, 0xc6, 0x96, 0x70, 0xec ]");
		DogArray_I8 ecc = new DogArray_I8();
		DogArray_I8 syndromes = new DogArray_I8();
		DogArray_I8 errorLocator = new DogArray_I8();
		int nsyn = 10;

		ReidSolomonCodes alg = new ReidSolomonCodes(8, primitive8);
		alg.generator(nsyn);
		alg.computeECC(message, ecc);

		DogArray_I8 corrupted = message.copy();
		corrupted.data[0] = 0;
		corrupted.data[4] = 8;
		corrupted.data[5] = 9;
		alg.computeSyndromes(corrupted, ecc, syndromes);
		alg.findErrorLocatorPolynomialBM(syndromes, errorLocator);

		DogArray_I32 errorLocations = new DogArray_I32(3);
		errorLocations.data[0] = 0;
		errorLocations.data[1] = 4;
		errorLocations.data[2] = 5;
		errorLocations.size = 3;

		alg.correctErrors(corrupted, message.size + ecc.size, syndromes, errorLocator, errorLocations);

		assertEquals(corrupted.size, message.size);
		for (int j = 0; j < corrupted.size; j++) {
			assertEquals(corrupted.get(j), message.get(j));
		}
	}

	/**
	 * Randomly correct the message and ECC. See if the message is correctly reconstructed.
	 */
	@Test void correct_random() {
		DogArray_I8 ecc = new DogArray_I8();
		int nsyn = 10; // should be able to recover from 4 errors

		ReidSolomonCodes alg = new ReidSolomonCodes(8, primitive8);
		alg.generator(nsyn);

		for (int i = 0; i < 20000; i++) {
			DogArray_I8 message = randomMessage(100);
			DogArray_I8 corrupted = message.copy();

			alg.computeECC(message, ecc);

			// apply noise to the message
			int numErrors = rand.nextInt(6);

			for (int j = 0; j < numErrors; j++) {
				int selected = rand.nextInt(message.size);
				corrupted.data[selected] ^= (byte)(0x12 + j); // make sure it changes even if the same number is selected twice
			}

			// corrupt the ecc code
			if (numErrors < 5 && rand.nextInt(5) < 1) {
				ecc.data[rand.nextInt(ecc.size)] ^= (byte)0x13;
			}

			alg.correct(corrupted, ecc);

			assertEquals(corrupted.size, message.size);
			for (int j = 0; j < corrupted.size; j++) {
				assertEquals(corrupted.get(j), message.get(j));
			}
		}
	}

	private static DogArray_I8 array( int... values ) {
		DogArray_I8 out = DogArray_I8.zeros(values.length);
		for (int i = 0; i < values.length; i++) {
			out.data[i] = (byte)values[i];
		}
		return out;
	}
}
