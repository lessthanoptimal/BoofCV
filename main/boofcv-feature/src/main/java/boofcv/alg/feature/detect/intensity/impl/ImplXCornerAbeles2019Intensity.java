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

package boofcv.alg.feature.detect.intensity.impl;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;

/**
 * X-Corner detector
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"UnnecessaryParentheses"})
public class ImplXCornerAbeles2019Intensity {
	public static void process( GrayF32 input, GrayF32 intensity ) {
		final int radius = 3;
		final int width = input.width;
		final float[] src = input.data;

		ImageMiscOps.fillBorder(intensity, 0, radius);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(radius,input.height-radius,y->{
		for (int y = radius; y < input.height - radius; y++) {
			// @formatter:off
			int inputIdx0 = input.startIndex + (y-3)*input.stride + radius;
			int inputIdx1 = input.startIndex + (y-2)*input.stride + radius;
			int inputIdx2 = input.startIndex + (y-1)*input.stride + radius;
			int inputIdx3 = input.startIndex + (y  )*input.stride + radius;
			int inputIdx4 = input.startIndex + (y+1)*input.stride + radius;
			int inputIdx5 = input.startIndex + (y+2)*input.stride + radius;
			int inputIdx6 = input.startIndex + (y+3)*input.stride + radius;

			int outputIdx = intensity.startIndex + y*intensity.stride + radius;
			for (int x = radius; x < width - radius; x++) {
				float v00 = src[inputIdx0  ];     // (x  , y-3 )
				float v01 = src[inputIdx0+1];     // (x+1, y-3 )
				float v02 = src[inputIdx1+2];     // (x+2, y-2 )
				float v03 = src[inputIdx2+3];     // (x+3, y-1 )
				float v04 = src[inputIdx3+3];     // (x+3, y   )
				float v05 = src[inputIdx4+3];     // (x+3, y+1 )
				float v06 = src[inputIdx5+2];     // (x+2, y+2 )
				float v07 = src[inputIdx6+1];     // (x+1, y+3 )
				float v08 = src[inputIdx6  ];     // (x  , y+3 )
				float v09 = src[inputIdx6-1];     // (x-1, y+3 )
				float v10 = src[inputIdx5-2];     // (x-2, y+2 )
				float v11 = src[inputIdx4-3];     // (x-3, y+1 )
				float v12 = src[inputIdx3-3];     // (x-3, y   )
				float v13 = src[inputIdx2-3];     // (x-3, y-1 )
				float v14 = src[inputIdx1-2];     // (x-2, y-2 )
				float v15 = src[inputIdx0-1];     // (x-1, y-3 )

				float a = (v15 + v00 + v01);
				float b = (v03 + v04 + v05);
				float c = (v07 + v08 + v09);
				float d = (v11 + v12 + v13);

				float e = (v01 + v02 + v03);
				float f = (v05 + v06 + v07);
				float g = (v09 + v10 + v11);
				float h = (v13 + v14 + v15);

				intensity.data[outputIdx++] = Math.max(score(a, b, c, d), score(e, f, g, h));
				inputIdx0++;
				inputIdx1++;
				inputIdx2++;
				inputIdx3++;
				inputIdx4++;
				inputIdx5++;
				inputIdx6++;
			}
			// @formatter:on
		}
		//CONCURRENT_ABOVE });
	}

	private static float score( float a, float b, float c, float d ) {
		float mean = (a + b + c + d)/4f;
		return (a - mean)*(c - mean) + (b - mean)*(d - mean);
	}
}
