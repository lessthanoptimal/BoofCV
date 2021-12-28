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

package boofcv.alg.descriptor;

import boofcv.struct.feature.*;

/**
 * Converts between different types of descriptions
 *
 * @author Peter Abeles
 */
public class ConvertDescriptors {
	/**
	 * Converts a floating point description with all positive values into the 8-bit integer descriptor by
	 * dividing each element in the input by the element maximum value and multiplying by 255.
	 *
	 * @param input Description with elements that are all positive
	 * @param output Unsigned 8-bit output
	 */
	public static void positive( TupleDesc_F64 input, TupleDesc_U8 output ) {
		double max = 0;
		for (int i = 0; i < input.size(); i++) {
			double v = input.data[i];
			if (v > max)
				max = v;
		}

		if (max == 0)
			max = 1.0;

		for (int i = 0; i < input.size(); i++) {
			output.data[i] = (byte)(255.0*input.data[i]/max);
		}
	}

	/**
	 * Converts a floating point description with signed real values into the 8-bit integer descriptor by
	 * dividing each element in the input by the element maximum absolute value and multiplying by 127.
	 *
	 * @param input Description with elements that are all positive
	 * @param output Unsigned 8-bit output
	 */
	public static void signed( TupleDesc_F64 input, TupleDesc_S8 output ) {
		double max = 0;
		for (int i = 0; i < input.size(); i++) {
			double v = Math.abs(input.data[i]);
			if (v > max)
				max = v;
		}

		for (int i = 0; i < input.size(); i++) {
			output.data[i] = (byte)(127.0*input.data[i]/max);
		}
	}

	/**
	 * Converts double into float by type casting
	 */
	public static void float_F64_F32( TupleDesc_F64 input, TupleDesc_F32 output ) {
		for (int i = 0; i < input.size(); i++) {
			output.data[i] = (float)input.data[i];
		}
	}

	/**
	 * Converts a regular feature description into a NCC feature description
	 *
	 * @param input Tuple descriptor. (not modified)
	 * @param output The equivalent NCC feature. (modified)
	 */
	public static void convertNcc( TupleDesc_F64 input, NccFeature output ) {
		if (input.size() != output.size())
			throw new IllegalArgumentException("Feature lengths do not match.");

		double mean = 0;
		for (int i = 0; i < input.data.length; i++) {
			mean += input.data[i];
		}
		mean /= input.data.length;

		double variance = 0;
		for (int i = 0; i < input.data.length; i++) {
			double d = output.data[i] = input.data[i] - mean;
			variance += d*d;
		}
		variance /= output.size();

		output.mean = mean;
		output.sigma = Math.sqrt(variance);
	}
}
