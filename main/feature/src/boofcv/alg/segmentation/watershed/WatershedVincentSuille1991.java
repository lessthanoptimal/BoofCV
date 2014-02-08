/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation.watershed;

import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import org.ddogleg.struct.GrowQueue_I32;

/**
 *
 * <p>
 *
 * </p>
 *
 * @author Peter Abeles
 */
public class WatershedVincentSuille1991 {

	// histogram for sorting the image.  8-bits so 256 possible values
	// each element refers to a pixel in the input image
	GrowQueue_I32 histogram[] = new GrowQueue_I32[256];

	public WatershedVincentSuille1991() {
		for( int i = 0; i < histogram.length; i++ ) {
			histogram[i] = new GrowQueue_I32();
		}
	}

	public void process( ImageUInt8 input , ImageSInt32 output ) {
		if( input.isSubimage() || output.isSubimage() )
			throw new IllegalArgumentException("No sub-images allowed");
		InputSanityCheck.checkSameShape(input,output);

		sortPixels(input);

	}

	private void sortPixels(ImageUInt8 input) {
		// initialize histogram
		for( int i = 0; i < histogram.length; i++ ) {
			histogram[i].reset();
		}
		// quickly sort by creating a histogram
		int N = input.width*input.height;
		for( int i = 0; i < N; i++ ) {
			int value = input.data[i] & 0xFF;
			histogram[value].add(i);
		}
	}
}
