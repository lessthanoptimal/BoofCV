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
	 * Decodes the message and performs any neccisary error correction
	 * @param input (Input) Message
	 * @param ecc (Input) error correction code for the message
	 * @param output (Output) the error corrected message
	 * @return true if it was successful or false if it failed
	 */
	public boolean decode( GrowQueue_I8 input ,
						   GrowQueue_I8 ecc,
						   GrowQueue_I8 output )
	{

		return false;
	}

	/**
	 * Computes the syndromes for the message (input + ecc). If there's no error then the output will be zero.
	 * @param input Data portion of the message
	 * @param ecc ECC portion of the message
	 * @param syndromes (Output) results of the syndromes computations
	 */
	void computeSyndromes( GrowQueue_I8 input ,
						   GrowQueue_I8 ecc ,
						   int syndromes[])
	{
		for (int i = 0; i < generator.size-1; i++) {
			int val = math.power(2,i);
			syndromes[i] = math.polyEval(input,val);
			syndromes[i] = math.polyEvalContinue(syndromes[i],ecc,val);
		}
	}

	/**
	 * Creates the generator function with the specified polynomial degree. The generator function is composed
	 * of factors of (x-a_n) where a_n is a power of 2.<br>
	 *
	 * g<sub>4</sub>(x) = (x - α0) (x - α1) (x - α2) (x - α3) = 01 x4 + 0f x3 + 36 x2 + 78 x + 40
	 */
	void generator( int degree ) {
		// predeclare memory
		generator.resize(degree+1);
		// initialize to a polynomial = 1
		generator.size = 1;
		generator.data[0] = 1;

		// (1*x - a[i])
		tmp1.resize(2);
		tmp1.data[0] = 1;
		for (int i = 0; i < degree; i++) {
			tmp1.data[1] = (byte)math.power(2,i);
			math.polyMult(generator,tmp1,tmp0);
			generator.setTo(tmp0);
		}
	}
}
