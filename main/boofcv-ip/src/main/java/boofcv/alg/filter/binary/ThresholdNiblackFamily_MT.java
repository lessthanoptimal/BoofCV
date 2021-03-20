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

package boofcv.alg.filter.binary;

import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;

/**
 * Concurrent implementation of {@link ThresholdNiblackFamily}.
 *
 * @author Peter Abeles
 */
public class ThresholdNiblackFamily_MT extends ThresholdNiblackFamily {
	public ThresholdNiblackFamily_MT( ConfigLength width, float k, boolean down, Variant variant ) {
		super(width, k, down, variant);
	}

	@Override protected void applyThresholding( GrayF32 input, GrayU8 output ) {
		if (down) {
			BoofConcurrency.loopFor(0, input.height, y -> {
				int i = y*stdev.width;
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for (int x = 0; x < input.width; x++, i++) {
					float threshold = op.compute(inputMean.data[i], stdev.data[i]);
					output.data[indexOut++] = (byte)(input.data[indexIn++] <= threshold ? 1 : 0);
				}
			});
		} else {
			BoofConcurrency.loopFor(0, input.height, y -> {
				int i = y*stdev.width;
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for (int x = 0; x < input.width; x++, i++) {
					float threshold = op.compute(inputMean.data[i], stdev.data[i]);
					output.data[indexOut++] = (byte)(input.data[indexIn++] >= threshold ? 1 : 0);
				}
			});
		}
	}
}
