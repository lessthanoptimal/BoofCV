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

/**
 * TODO Summarize
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
	 * @param input Input message
	 * @param output error correction code
	 */
	public void computeErrorCorrection( GrowQueue_I8 input , GrowQueue_I8 output ) {
		math.polyDivide(input,generator,tmp0,output);
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
	 * Creates the generator function with the specified polynomial degree
	 */
	void generator( int degree ) {
		generator.resize(1);
		generator.data[0] = 1;

		tmp1.resize(2);
		tmp1.data[0 ] = 1;
		for (int i = 0; i < degree; i++) {
			tmp1.data[1] = (byte)math.power(2,i);
			math.polyMult(generator,tmp1,tmp0);
			generator.setTo(tmp0);
		}
	}
}
