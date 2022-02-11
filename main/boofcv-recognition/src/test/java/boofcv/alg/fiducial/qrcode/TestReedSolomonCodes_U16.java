/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import org.ddogleg.struct.DogArray_I16;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestReedSolomonCodes_U16 extends BoofStandardJUnit {

	int primitive12 = 0b1_0000_0110_1001;
	int primitive8 = 0b1_0001_1101;
	int primitive4 = 0b1_0011;

	@Test void computeECC() {
		DogArray_I16 message = randomMessage(0xFF, 50);
		var ecc = new DogArray_I16();

		var alg = new ReedSolomonCodes_U16(12, primitive12);
		alg.generatorQR(6);
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
		short[] a = new short[]{0x40, (short)0xd2, 0x75, 0x47, 0x76, 0x17, 0x32,
				0x06, 0x27, 0x26, (short)0x96, (short)0xc6, (short)0xc6, (short)0x96, 0x70, (short)0xec};
		short[] b = new short[]{(short)0xbc, 0x2a, (short)0x90, 0x13, 0x6b,
				(short)0xaf, (short)0xef, (short)0xfd, 0x4b, (short)0xe0};

		var message = new DogArray_I16();
		var ecc = new DogArray_I16();
		message.data = a;
		message.size = a.length;

		var alg = new ReedSolomonCodes_U16(8, 0x11d);
		alg.generatorQR(10);
		alg.computeECC(message, ecc);

		assertEquals(10, ecc.size);
		for (int i = 0; i < b.length; i++) {
			assertEquals(b[i], ecc.data[i]);
		}
	}

	@Test void computeSyndromes() {
		DogArray_I16 message = randomMessage(0xFF, 50);
		var ecc = new DogArray_I16();

		var alg = new ReedSolomonCodes_U16(12, primitive12);
		alg.generatorQR(6);
		alg.computeECC(message, ecc);

		DogArray_I16 syndromes = DogArray_I16.zeros(6);
		alg.computeSyndromes(message, ecc, syndromes);

		// no error. All syndromes values should be zero
		for (int i = 0; i < syndromes.size; i++) {
			assertEquals(0, syndromes.data[i]);
		}

		// introduce an error
		message.data[6] += (short)7;
		alg.computeSyndromes(message, ecc, syndromes);

		int notZero = 0;
		for (int i = 0; i < syndromes.size; i++) {
			if (syndromes.data[i] != 0)
				notZero++;
		}
		assertTrue(notZero > 1);
	}

	private DogArray_I16 randomMessage( int maxValue, int N ) {
		var message = new DogArray_I16();
		for (int i = 0; i < N; i++) {
			message.add(rand.nextInt(maxValue + 1));
		}
		return message;
	}

	@Test void generatorQR() {
		var alg = new ReedSolomonCodes_U16(12, primitive12);
		alg.generatorQR(5);

		// Evaluate it at the zeros and see if they are zero
		for (int i = 0; i < 5; i++) {
			int value = alg.math.power(2, i);
			int found = alg.math.polyEval(alg.generator, value);
			assertEquals(0, found);
		}

		// Pass in a number which should not be a zero
		assertTrue(0 != alg.math.polyEval(alg.generator, 5));
	}

	@Test void generatorAztec() {
		var alg = new ReedSolomonCodes_U16(12, primitive12);
		alg.generatorAztec(5);

		// Evaluate it at the zeros and see if they are zero
		for (int i = 0; i < 5; i++) {
			int value = alg.math.power(2, i + 1);
			int found = alg.math.polyEval(alg.generator, value);
			assertEquals(0, found);
		}

		// Pass in a number which should not be a zero
		assertTrue(0 != alg.math.polyEval(alg.generator, 5));
	}

	/** Compare to a known solution */
	@Test void generatorAztecKnown1() {
		var alg = new ReedSolomonCodes_U8(4, 19);
		alg.generatorAztec(5);

		// From ISO section 7.2.3
		assertEquals(6, alg.generator.size);
		assertEquals(1, alg.generator.data[0] & 0xFF);  // x**5
		assertEquals(11, alg.generator.data[1] & 0xFF); // x**4
		assertEquals(4, alg.generator.data[2] & 0xFF);
		assertEquals(6, alg.generator.data[3] & 0xFF);
		assertEquals(2, alg.generator.data[4] & 0xFF);
		assertEquals(1, alg.generator.data[5] & 0xFF);  // x**0
	}

	/** Compare to a known solution */
	@Test void generatorAztecKnown0() {
		var alg = new ReedSolomonCodes_U8(4, 19);
		alg.generatorAztec(6);

		// From ISO section 7.2.3
		assertEquals(7, alg.generator.size);
		assertEquals(1, alg.generator.data[0] & 0xFF);  // x**6
		assertEquals(7, alg.generator.data[1] & 0xFF);  // x**5
		assertEquals(9, alg.generator.data[2] & 0xFF);
		assertEquals(3, alg.generator.data[3] & 0xFF);
		assertEquals(12, alg.generator.data[4] & 0xFF);
		assertEquals(10, alg.generator.data[5] & 0xFF);
		assertEquals(12, alg.generator.data[6] & 0xFF); // x**0
	}

	/**
	 * Computed using a reference implementation found at [1].
	 */
	@Test void findErrorLocatorPolynomialBM() {
		DogArray_I16 message = DogArray_I16.parseHex(
				"[ 0x40, 0xd2, 0x75, 0x47, 0x76, 0x17, 0x32, 0x06, 0x27, 0x26, 0x96, 0xc6, 0xc6, 0x96, 0x70, 0xec ]");
		var ecc = new DogArray_I16();
		int nsyn = 10;
		DogArray_I16 syndromes = DogArray_I16.zeros(nsyn);

		var alg = new ReedSolomonCodes_U16(8, primitive8);
		alg.generatorQR(nsyn);
		alg.computeECC(message, ecc);

		message.data[0] = 0;
		alg.computeSyndromes(message, ecc, syndromes);
		DogArray_I16 errorLocator = new DogArray_I16();
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

		DogArray_I16 found = new DogArray_I16();
		DogArray_I16 expected = new DogArray_I16();

		for (int i = 0; i < 30; i++) {
			int N = 50;
			DogArray_I16 message = randomMessage(0xFF, N);

			var ecc = new DogArray_I16();
			int nsyn = 10;
			DogArray_I16 syndromes = DogArray_I16.zeros(nsyn);

			var alg = new ReedSolomonCodes_U16(12, primitive12);
			alg.generatorQR(nsyn);
			alg.computeECC(message, ecc);

			int where = rand.nextInt(N);
			message.data[where] ^= (short)0x12;
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
		DogArray_I16 message = randomMessage(0xFF, 50);
		for (int i = 0; i < 200; i++) {
			findErrors_BruteForce(message, rand.nextInt(5), false);
		}
	}

	public void findErrors_BruteForce( DogArray_I16 message, int numErrors, boolean expectedFail ) {
		var ecc = new DogArray_I16();
		int nsyn = 10;
		DogArray_I16 syndromes = DogArray_I16.zeros(nsyn);
		DogArray_I16 errorLocator = new DogArray_I16();
		DogArray_I32 locations = new DogArray_I32();

		var alg = new ReedSolomonCodes_U16(12, primitive12);
		alg.generatorQR(nsyn);
		alg.computeECC(message, ecc);

		DogArray_I16 cmessage = message.copy();

		// corrupt the message and ecc
		int N = message.size + ecc.size;
		int[] corrupted = selectN(numErrors, N);
		for (int i = 0; i < corrupted.length; i++) {
			int w = corrupted[i];
			if (w < message.size)
				cmessage.data[w] ^= (short)0x45;
			else {
				ecc.data[w - message.size] ^= (short)0x45;
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

			DogArray_I16 hack = new DogArray_I16();
			alg.findErrorLocatorPolynomial(N, locations, hack);
		}
	}

	/**
	 * Test a case where there are too many errors.
	 */
	@Test void findErrors_BruteForce_TooMany() {
		DogArray_I16 message = randomMessage(0xFF, 50);
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
		findErrorEvaluator(array(64, 192, 93, 231, 52, 92, 228, 49, 83, 5),
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

	private void findErrorEvaluator( DogArray_I16 syndromes,
									 DogArray_I16 errorLocator,
									 DogArray_I16 expected ) {
		var found = new DogArray_I16();
		var alg = new ReedSolomonCodes_U16(8, primitive8);
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
		DogArray_I16 message = DogArray_I16.parseHex(
				"[ 0x40, 0xd2, 0x75, 0x47, 0x76, 0x17, 0x32, 0x06, 0x27, 0x26, 0x96, 0xc6, 0xc6, 0x96, 0x70, 0xec ]");
		var ecc = new DogArray_I16();
		DogArray_I16 syndromes = new DogArray_I16();
		DogArray_I16 errorLocator = new DogArray_I16();
		int nsyn = 10;

		var alg = new ReedSolomonCodes_U16(12, primitive12);
		alg.generatorQR(nsyn);
		alg.computeECC(message, ecc);

		DogArray_I16 corrupted = message.copy();
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
		correct_random(4, primitive4);
//		correct_random(8, primitive8);
//		correct_random(12, primitive12);
	}

	void correct_random( int numBits, int primitive ) {
		var ecc = new DogArray_I16();
		int nsyn = 10; // should be able to recover from 4 errors

		int mask = 0;
		for (int i = 0; i < numBits; i++) {
			mask |= 1 << i;
		}
		var alg = new ReedSolomonCodes_U16(numBits, primitive);
		alg.generatorQR(nsyn);


		for (int i = 0; i < 20000; i++) {
			DogArray_I16 message = randomMessage(mask, 100);
			DogArray_I16 corrupted = message.copy();

			alg.computeECC(message, ecc);

			// apply noise to the message
			int numErrors = rand.nextInt(4);

			for (int j = 0; j < numErrors; j++) {
				int selected = rand.nextInt(message.size);
				corrupted.data[selected] ^= (short)((0x12 + j) & mask); // make sure it changes even if the same number is selected twice
			}

			// corrupt the ecc code
			if (numErrors == 0 || rand.nextInt(5) < 1) {
				ecc.data[rand.nextInt(ecc.size)] ^= (short)(0x13 & mask);
			}

			alg.correct(corrupted, ecc);

			assertEquals(message.size, corrupted.size);
			for (int j = 0; j < corrupted.size; j++) {
				assertEquals(message.get(j), corrupted.get(j));
			}
		}
	}

	private static DogArray_I16 array( int... values ) {
		DogArray_I16 out = DogArray_I16.zeros(values.length);
		for (int i = 0; i < values.length; i++) {
			out.data[i] = (short)values[i];
		}
		return out;
	}
}
