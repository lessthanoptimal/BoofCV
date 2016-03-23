/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.core.image;

import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.InterleavedF32;

/**
 * Functions related to interleaved images.
 *
 * @author Peter Abeles
 */
public class InterleavedImageOps {

	/**
	 * Splits the 2-band interleaved into into two {@link ImageGray}.
	 *
	 * @param interleaved (Input) Interleaved image with 2 bands
	 * @param band0 (Output) band 0
	 * @param band1 (Output) band 1
	 */
	public static void split2(InterleavedF32 interleaved , GrayF32 band0 , GrayF32 band1 ) {
		if( interleaved.numBands != 2 )
			throw new IllegalArgumentException("Input interleaved image must have 2 bands");
		InputSanityCheck.checkSameShape(band0, interleaved);
		InputSanityCheck.checkSameShape(band1, interleaved);

		for( int y = 0; y < interleaved.height; y++ ) {

			int indexTran = interleaved.startIndex + y*interleaved.stride;
			int indexReal = band0.startIndex + y*band0.stride;
			int indexImg = band1.startIndex + y*band1.stride;

			for( int x = 0; x < interleaved.width; x++, indexTran += 2 ) {

				band0.data[indexReal++] = interleaved.data[indexTran];
				band1.data[indexImg++] = interleaved.data[indexTran+1];
			}
		}
	}

	/**
	 * Combines two {@link ImageGray} into a single {@link boofcv.struct.image.ImageInterleaved}.
	 *
	 * @param band0 (Input) band 0
	 * @param band1 (Input) band 1
	 * @param interleaved (Output) Interleaved image with 2 bands
	 */
	public static void merge2(GrayF32 band0 , GrayF32 band1 , InterleavedF32 interleaved ) {
		if( interleaved.numBands != 2 )
			throw new IllegalArgumentException("Output interleaved image must have 2 bands");

		InputSanityCheck.checkSameShape(band0,interleaved);
		InputSanityCheck.checkSameShape(band1,interleaved);

		for( int y = 0; y < interleaved.height; y++ ) {

			int indexTran = interleaved.startIndex + y*interleaved.stride;
			int indexReal = band0.startIndex + y*band0.stride;
			int indexImg = band1.startIndex + y*band1.stride;

			for( int x = 0; x < interleaved.width; x++, indexTran += 2 ) {

				interleaved.data[indexTran] = band0.data[indexReal++];
				interleaved.data[indexTran+1] = band1.data[indexImg++];
			}
		}
	}
}
