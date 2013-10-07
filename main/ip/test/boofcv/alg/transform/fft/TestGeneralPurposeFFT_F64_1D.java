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

package boofcv.alg.transform.fft;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestGeneralPurposeFFT_F64_1D {

	Random rand = new Random(234);

	int sizes[] = new int[]{1,2,3,16,32,100,103};

	@Test
	public void real() {
		for( int i = 0; i < sizes.length; i++ ) {
			checkReal(sizes[i]);
		}
	}

	private void checkReal(int n) {
		double input[] = new double[n *2];
		double original[] = new double[n *2];

		for( int i = 0; i < n; i++ ) {
			double val = rand.nextGaussian();

			input[i*2] = val;
			input[i*2+1] = 0;

			original[i*2] = val;
			original[i*2+1] = 0;
		}

		GeneralPurposeFFT_F64_1D alg = new GeneralPurposeFFT_F64_1D(n);
		alg.realForward(input);

		// make sure the values have changed
		checkForChange(input, original);

		alg.realInverse(input, true);

		for( int i = 0; i < n; i++ ) {

			assertEquals(original[i*2],input[i*2  ],1e-8);
			assertEquals(original[i*2+1],input[i*2+1],1e-8);
		}
	}

	@Test
	public void realFull() {
		for( int i = 0; i < sizes.length; i++ ) {
			checkRealFull(sizes[i]);
		}
	}

	private void checkRealFull(int n) {
		double input[] = new double[n *2];
		double original[] = new double[n *2];

		for( int i = 0; i < n; i++ ) {
			double val = rand.nextGaussian();

			input[i] = val;

			original[i*2] = val;
			original[i*2+1] = 0;
		}

		GeneralPurposeFFT_F64_1D alg = new GeneralPurposeFFT_F64_1D(n);
		alg.realForwardFull(input);

		// make sure the values have changed
		checkForChange(input, original);

		alg.complexInverse(input, true);

		for( int i = 0; i < n; i++ ) {

			assertEquals(original[i*2],input[i*2  ],1e-8);
			assertEquals(original[i*2+1],input[i*2+1],1e-8);
		}
	}

	public static void checkForChange(double[] input, double[] original) {
		if( input.length/2 > 1 ) {
			boolean change = false;
			for( int i = 0; i < input.length; i++ ) {
				if(Math.abs(original[i] - input[i]) > 1e-8) {
					change = true;
					break;
				}
			}
			assertTrue(change);
		}
	}

	@Test
	public void complex() {
		for( int i = 0; i < sizes.length; i++ ) {
			checkComplex(sizes[i]);
		}
	}

	private void checkComplex(int n) {
		double input[] = new double[n *2];
		double original[] = new double[n *2];

		for( int i = 0; i < n; i++ ) {
			double val = rand.nextGaussian();

			input[i*2] = val;
			input[i*2+1] = 0;

			original[i*2] = val;
			original[i*2+1] = 0;
		}

		GeneralPurposeFFT_F64_1D alg = new GeneralPurposeFFT_F64_1D(n);
		alg.complexForward(input);

		// make sure the values have changed
		checkForChange(input, original);

		alg.complexInverse(input, true);

		for( int i = 0; i < n; i++ ) {

			assertEquals(original[i*2],input[i*2  ],1e-8);
			assertEquals(original[i*2+1],input[i*2+1],1e-8);
		}
	}
}
