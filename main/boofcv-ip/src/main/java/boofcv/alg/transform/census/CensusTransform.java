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

package boofcv.alg.transform.census;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.transform.census.impl.ImplCensusTransformBorder;
import boofcv.alg.transform.census.impl.ImplCensusTransformInner;
import boofcv.alg.transform.census.impl.ImplCensusTransformInner_MT;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;

import javax.annotation.Nullable;

/**
 * The Census transform computes a bit mask for each pixel in the image. If a neighboring pixel is greater than the
 * center pixel in a region that bit is set to 1. A 3x3 region 9radius=1) is encoded in 8-bits and a 5x5 region
 * (radius=2) in 24-bits. To compute the error between two pixels simply compute the hamming distance. The
 * hamming distance for an input can be computed using DescriptorDistance.hamming()
 *
 * @author Peter Abeles
 */
public class CensusTransform {
	/**
	 * Census transform for local 3x3 region around each pixel.
	 *
	 * @param input Input image
	 * @param output Census transformed output image
	 * @param border (Nullable) How the border is handled
	 */
	public static void region3x3(final GrayU8 input , final GrayU8 output , @Nullable ImageBorder_S32<GrayU8> border ) {
		InputSanityCheck.checkReshape(input,output);

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplCensusTransformInner_MT.region3x3(input,output);
		} else {
			ImplCensusTransformInner.region3x3(input,output);
		}

		if( border != null ) {
			border.setImage(input);
			ImplCensusTransformBorder.region3x3(border,output);
		}
	}

	/**
	 * Census transform for local 5x5 region around each pixel.
	 *
	 * @param input Input image
	 * @param output Census transformed output image
	 * @param border (Nullable) How the border is handled
	 */
	public static void region5x5(final GrayU8 input , final GrayS32 output , @Nullable ImageBorder_S32<GrayU8> border ) {
		InputSanityCheck.checkReshape(input,output);

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplCensusTransformInner_MT.region5x5(input,output);
		} else {
			ImplCensusTransformInner.region5x5(input,output);
		}

		if( border != null ) {
			border.setImage(input);
			ImplCensusTransformBorder.region5x5(border,output);
		}
	}
}
