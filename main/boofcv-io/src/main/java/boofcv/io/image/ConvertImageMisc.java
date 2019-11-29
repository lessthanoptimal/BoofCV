/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.io.image;

import boofcv.concurrency.BoofConcurrency;
import boofcv.io.image.impl.ImplConvertImageMisc;
import boofcv.io.image.impl.ImplConvertImageMisc_MT;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU16;

/**
 * Functions for converting image formats that don't cleanly fit into any other location
 *
 * @author Peter Abeles
 */
public class ConvertImageMisc {
	/**
	 * <p>Converts a floating point image with all positive values into a 16-bit image using fixed-point math.
	 * The number of bits dedicated to the fractional component is configurable. If 8-bits are allocated
	 * then the max integer value is 256 and the fractional resolution is 1/256</pre>
	 * Limitations:
	 * <ul>
	 *     <li>Fractional bits specifies accuracy of fractional component. FR=2**fractional_bits</li>
	 *     <li>src must have values with in the range 0 &le; x &le; 2**(16-fractional_bits)</li>
	 *     <li>fractional component will only be saved to within 1/FR resolution</li>
	 *     <li>To reduce discretization bias rounding is done instead of flooring</li>
	 * </ul>
	 * @param src (Input) Input F32 image
	 * @param fractionBits (Input) Number of bits allocated to the fractional component
	 * @param dst (Output) Output U16 image
	 */
	public static void convert_F32_U16(GrayF32 src, int fractionBits, GrayU16 dst) {
		dst.reshape(src);
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertImageMisc_MT.convert_F32_U16(src,fractionBits,dst);
		} else {
			ImplConvertImageMisc.convert_F32_U16(src,fractionBits,dst);
		}
	}

	/**
	 * Inverse of {@link #convert_F32_U16(GrayF32, int, GrayU16)}
	 * @param src (Input) Input U16 image
	 * @param fractionBits (Input) Number of bits allocated to the fractional component
	 * @param dst (Output) Output F32 image
	 */
	public static void convert_U16_F32(GrayU16 src, int fractionBits, GrayF32 dst) {
		dst.reshape(src);
		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertImageMisc_MT.convert_U16_F32(src,fractionBits,dst);
		} else {
			ImplConvertImageMisc.convert_U16_F32(src,fractionBits,dst);
		}
	}
}
