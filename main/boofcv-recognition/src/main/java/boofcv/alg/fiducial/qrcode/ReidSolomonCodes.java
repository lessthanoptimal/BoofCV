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

import org.ddogleg.struct.GrowQueue_I32;
import org.ddogleg.struct.GrowQueue_I8;

import java.util.Arrays;

/**
 * TODO Summarize
 *
 * <p>Code and code comments based on the tutorial at [1].</p>
 *
 *  <p>[1] <a href="https://en.wikiversity.org/wiki/Reed–Solomon_codes_for_coders">Reed-Solomon Codes for Coders</a>
 *  Viewed on September 28, 2017</p>
 *
 * @author Peter Abeles
 */
public class ReidSolomonCodes {

	GaliosFieldTableOps math;

	GrowQueue_I8 generator = new GrowQueue_I8();

	GrowQueue_I8 tmp0 = new GrowQueue_I8();
	GrowQueue_I8 tmp1 = new GrowQueue_I8();

	GrowQueue_I32 errorLocations = new GrowQueue_I32();
	GrowQueue_I8 errorLocatorPoly = new GrowQueue_I8();
	GrowQueue_I8 syndromes = new GrowQueue_I8();

	public ReidSolomonCodes( int numBits , int primitive) {
		math = new GaliosFieldTableOps(numBits,primitive);
	}

	public void setDegree( int degree ) {
		generator(degree);
	}

	/**
	 * Given the input message compute the error correction code for it
	 * @param input Input message. Modified internally then returned to its initial state
	 * @param output error correction code
	 */
	public void computeECC( GrowQueue_I8 input , GrowQueue_I8 output ) {

		int N = generator.size-1;
		input.extend(input.size+N);
		Arrays.fill(input.data,input.size-N,input.size,(byte)0);

		math.polyDivide(input,generator,tmp0,output);

		input.size -= N;
	}

	/**
	 * Decodes the message and performs any necessary error correction
	 * @param input (Input) Corrupted Message (Output) corrected message
	 * @param ecc (Input) error correction code for the message
	 * @return true if it was successful or false if it failed
	 */
	public boolean correct(GrowQueue_I8 input , GrowQueue_I8 ecc )
	{
		computeSyndromes(input,ecc,syndromes);
		findErrorLocatorPolynomialBM(syndromes,errorLocatorPoly);
		if( !findErrorLocations_BruteForce(errorLocatorPoly,input.size+ecc.size,errorLocations))
			return false;

		correctErrors(input,input.size+ecc.size,syndromes,errorLocatorPoly,errorLocations);
		return true;
	}

	/**
	 * Computes the syndromes for the message (input + ecc). If there's no error then the output will be zero.
	 * @param input Data portion of the message
	 * @param ecc ECC portion of the message
	 * @param syndromes (Output) results of the syndromes computations
	 */
	void computeSyndromes( GrowQueue_I8 input ,
						   GrowQueue_I8 ecc ,
						   GrowQueue_I8 syndromes)
	{
		syndromes.resize(syndromeLength());
		for (int i = 0; i < syndromes.size; i++) {
			int val = math.power(2,i);
			syndromes.data[i] = (byte)math.polyEval(input,val);
			syndromes.data[i] = (byte)math.polyEvalContinue(syndromes.data[i]&0xFF,ecc,val);
		}
	}

	/**
	 * Computes the error locator polynomial using  Berlekamp-Massey algorithm [1]
	 *
	 * <p>[1] Massey, J. L. (1969), "Shift-register synthesis and BCH decoding" (PDF), IEEE Trans.
	 * Information Theory, IT-15 (1): 122–127</p>
	 *
	 * @param syndromes (Input) The syndromes
	 * @param errorLocator (Output) Error locator polynomial. Coefficients are large to small.
	 */
	void findErrorLocatorPolynomialBM(GrowQueue_I8 syndromes , GrowQueue_I8 errorLocator ) {
		GrowQueue_I8 C = errorLocator; // error polynomial
		GrowQueue_I8 B = new GrowQueue_I8();  // previous error polynomial
		// TODO remove new from this function

		initToOne(C,syndromes.size+1);
		initToOne(B,syndromes.size+1);

		GrowQueue_I8 tmp = new GrowQueue_I8(syndromes.size);

//		int L = 0;
//		int m = 1; // stores how much B is 'shifted' by
		int b = 1;

		for (int n = 0; n < syndromes.size; n++) {

			// Compute discrepancy delta
			int delta = syndromes.data[n]&0xFF;

			for (int j = 1; j < C.size; j++) {
				delta ^= math.multiply(C.data[C.size-j-1]&0xFF, syndromes.data[n-j]&0xFF);
			}

			// B = D^m * B
			B.data[B.size++] = 0;

			// Step 3 is implicitly handled
			// m = m + 1

			if( delta != 0 ) {
				int scale = math.multiply(delta, math.inverse(b));
				math.polyAddScaleB(C, B, scale, tmp);

				if (B.size <= C.size) {
					// if 2*L > N ---- Step 4
//					m += 1;
				} else {
					// if 2*L <= N --- Step 5
					B.setTo(C);
//					L = n+1-L;
					b = delta;
//					m = 1;
				}
				C.setTo(tmp);
			}
		}

		removeLeadingZeros(C);
	}

	private void removeLeadingZeros(GrowQueue_I8 poly ) {
		int count = 0;
		for (; count < poly.size; count++) {
			if( poly.data[count] != 0 )
				break;
		}
		for (int i = count; i < poly.size; i++) {
			poly.data[i-count] = poly.data[i];
		}
		poly.size -= count;
	}

	/**
	 * Compute the error locator polynomial when given the error locations in the message.
	 *
	 * @param messageLength (Input) Length of the message
	 * @param errorLocations (Input) List of error locations in the byte
	 * @param errorLocator (Output) Error locator polynomial. Coefficients are large to small.
	 */
	void findErrorLocatorPolynomial( int messageLength , GrowQueue_I32 errorLocations , GrowQueue_I8 errorLocator ) {
		tmp1.resize(2);
		tmp1.data[1] = 1;
		errorLocator.resize(1);
		errorLocator.data[0] = 1;
		for (int i = 0; i < errorLocations.size; i++) {
			// Convert from positions in the message to coefficient degrees
			int where = messageLength - errorLocations.get(i) - 1;

			// tmp1 = [2**w,1]
			tmp1.data[0] = (byte)math.power(2,where);
//			tmp1.data[1] = 1;

			tmp0.setTo(errorLocator);
			math.polyMult(tmp0,tmp1,errorLocator);
		}
	}

	/**
	 * Creates a list of bytes that have errors in them
	 *
	 * @param errorLocator (Input) Error locator polynomial. Coefficients from small to large.
	 * @param messageLength (Input) Length of the message + ecc.
	 * @param locations (Output) locations of bytes in message with errors.
	 */
	public boolean findErrorLocations_BruteForce(GrowQueue_I8 errorLocator ,
												 int messageLength ,
												 GrowQueue_I32 locations )
	{
		locations.resize(0);
		for (int i = 0; i < messageLength; i++) {
			if( math.polyEval_S(errorLocator,math.power(2,i)) == 0 ) {
				locations.add(messageLength-i-1);
			}
		}

		// see if the expected number of errors were found
		return locations.size == errorLocator.size - 1;
	}

	/**
	 * Use Forney algorithm to compute correction values.
	 *
	 * @param message (Input/Output) The message which is to be corrected. Just the message. ECC not required.
	 * @param length_msg_ecc (Input) length of message and ecc code
	 * @param errorLocations (Input) locations of bytes in message with errors.
	 */
	void correctErrors( GrowQueue_I8 message ,
						int length_msg_ecc,
						GrowQueue_I8 syndromes,
						GrowQueue_I8 errorLocator ,
						GrowQueue_I32 errorLocations)
	{
		GrowQueue_I8 err_eval = new GrowQueue_I8(); // TODO avoid new
		findErrorEvaluator(syndromes,errorLocator,err_eval);

		// Compute error positions
		GrowQueue_I8 X = GrowQueue_I8.zeros(errorLocations.size); // TODO avoid new
		for (int i = 0; i < errorLocations.size; i++) {
			int coef_pos = (length_msg_ecc-errorLocations.data[i]-1);
			X.data[i] = (byte)math.power(2,coef_pos);
			// The commented out code below replicates exactly how the reference code works. This code above
			// seems to work just as well and passes all the unit tests
//			int coef_pos = math.max_value-(length_msg_ecc-errorLocations.data[i]-1);
//			X.data[i] = (byte)math.power_n(2,-coef_pos);
		}

		GrowQueue_I8 err_loc_prime_tmp = new GrowQueue_I8(X.size);

		// storage for error magnitude polynomial
		for (int i = 0; i < X.size; i++) {
			int Xi = X.data[i]&0xFF;
			int Xi_inv = math.inverse(Xi);

			// Compute the polynomial derivative
			err_loc_prime_tmp.size = 0;
			for (int j = 0; j < X.size; j++) {
				if( i == j )
					continue;
				err_loc_prime_tmp.data[err_loc_prime_tmp.size++] =
						(byte)GaliosFieldOps.subtract(1,math.multiply(Xi_inv,X.data[j]&0xFF));
			}
			// compute the product, which is the denominator of Forney algorithm (errata locator derivative)
			int err_loc_prime = 1;
			for (int j = 0; j < err_loc_prime_tmp.size; j++) {
				err_loc_prime = math.multiply(err_loc_prime,err_loc_prime_tmp.data[j]&0xFF);
			}

			int y = math.polyEval_S(err_eval,Xi_inv);
			y = math.multiply(math.power(Xi,1),y);

			// Compute the magnitude
			int magnitude = math.divide(y,err_loc_prime);

			// only apply a correction if it's part of the message and not the ECC
			int loc = errorLocations.get(i);
			if( loc < message.size )
				message.data[loc] = (byte)((message.data[loc]&0xFF) ^ magnitude);
		}
	}

	/**
	 * Compute the error evaluator polynomial Omega.
	 *
	 * @param syndromes (Input) syndromes
	 * @param errorLocator (Input) error locator polynomial.
	 * @param evaluator (Output) error evaluator polynomial. large to small coef
	 */
	void findErrorEvaluator( GrowQueue_I8 syndromes , GrowQueue_I8 errorLocator ,
							 GrowQueue_I8 evaluator )
	{
		math.polyMult_flipA(syndromes,errorLocator,evaluator);
		int N = errorLocator.size-1;
		int offset = evaluator.size-N;
		for (int i = 0; i < N; i++) {
			evaluator.data[i] = evaluator.data[i+offset];
		}
		evaluator.data[N]=0;
		evaluator.size = errorLocator.size;

		// flip evaluator around // TODO remove this flip and do it in place
		for (int i = 0; i < evaluator.size / 2; i++) {
			int j = evaluator.size-i-1;
			int tmp = evaluator.data[i];
			evaluator.data[i] = evaluator.data[j];
			evaluator.data[j] = (byte)tmp;
		}
	}

	/**
	 * Creates the generator function with the specified polynomial degree. The generator function is composed
	 * of factors of (x-a_n) where a_n is a power of 2.<br>
	 *
	 * g<sub>4</sub>(x) = (x - α0) (x - α1) (x - α2) (x - α3) = 01 x4 + 0f x3 + 36 x2 + 78 x + 40
	 */
	void generator( int degree ) {
		// initialize to a polynomial = 1
		initToOne(generator,degree+1);

		// (1*x - a[i])
		tmp1.resize(2);
		tmp1.data[0] = 1;
		for (int i = 0; i < degree; i++) {
			tmp1.data[1] = (byte)math.power(2,i);
			math.polyMult(generator,tmp1,tmp0);
			generator.setTo(tmp0);
		}
	}

	void initToOne( GrowQueue_I8 poly , int length ) {
		poly.setMaxSize(length);
		poly.size = 1;
		poly.data[0] = 1;
	}

	private int syndromeLength() {
		return generator.size-1;
	}
}
