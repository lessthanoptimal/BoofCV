/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.feature.NccFeature;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.feature.TupleDesc_S8;
import boofcv.struct.feature.TupleDesc_U8;

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
	public static void positive( TupleDesc_F64 input , TupleDesc_U8 output ) {
		double max = 0;
		for( int i = 0; i < input.size(); i++ ) {
			double v = input.value[i];
			if( v > max )
				max = v;
		}

		if( max == 0 )
			max = 1.0;

		for( int i = 0; i < input.size(); i++ ) {
			output.value[i] = (byte)(255.0*input.value[i]/max);
		}
	}

	/**
	 * Converts a floating point description with real values into the 8-bit integer descriptor by
	 * dividing each element in the input by the element maximum absolute value and multiplying by 127.
	 *
	 * @param input Description with elements that are all positive
	 * @param output Unsigned 8-bit output
	 */
	public static void real( TupleDesc_F64 input , TupleDesc_S8 output ) {
		double max = 0;
		for( int i = 0; i < input.size(); i++ ) {
			double v = Math.abs(input.value[i]);
			if( v > max )
				max = v;
		}

		for( int i = 0; i < input.size(); i++ ) {
			output.value[i] = (byte)(127.0*input.value[i]/max);
		}
	}

	/**
	 * Converts a regular feature description into a NCC feature description
	 * @param input Tuple descriptor. (not modified)
	 * @param output The equivalent NCC feature. (modified)
	 */
	public static void convertNcc( TupleDesc_F64 input , NccFeature output ) {

		if( input.size() != output.size() )
			throw new IllegalArgumentException("Feature lengths do not match.");

		double mean = 0;
		for (int i = 0; i < input.value.length; i++) {
			mean += input.value[i];
		}
		mean /= input.value.length;

		double variance = 0;
		for( int i = 0; i < input.value.length; i++ ) {
			double d = output.value[i] = input.value[i] - mean;
			variance += d*d;
		}
		variance /= output.size();

		output.mean = mean;
		output.sigma = Math.sqrt(variance);
	}
}
