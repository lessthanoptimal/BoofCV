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
import boofcv.struct.image.GrayU8;

/**
 * Concurrent version of {@link ThresholdLocalOtsu}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class ThresholdLocalOtsu_MT extends ThresholdLocalOtsu {

	public ThresholdLocalOtsu_MT( boolean otsu2, ConfigLength regionWidthLength,
								  double tuning, double scale, boolean down ) {
		super(otsu2, regionWidthLength, tuning, scale, down);
	}

	@Override
	protected void process( GrayU8 input, GrayU8 output, int x0, int y0, int x1, int y1, byte a, byte b ) {

		BoofConcurrency.loopBlocks(y0, y1, ( block0, block1 ) -> {
			// handle the inner portion first
			ApplyHelper h = helpers.pop();
			for (int y = block0; y < block1; y++) {
				int indexInput = input.startIndex + y*input.stride + x0;
				int indexOutput = output.startIndex + y*output.stride + x0;

				h.computeHistogram(0, y - y0, input);
				output.data[indexOutput++] = (input.data[indexInput++] & 0xFF) <= h.otsu.threshold ? a : b;

				for (int x = x0 + 1; x < x1; x++) {
					h.updateHistogramX(x - x0, y - y0, input);
					output.data[indexOutput++] = (input.data[indexInput++] & 0xFF) <= h.otsu.threshold ? a : b;
				}
			}

			// Now update the border
//			applyToBorder(input, output, y0, y1, x0, x1, h);
			helpers.recycle(h);
		});
		ApplyHelper h = helpers.pop();
		applyToBorder(input, output, y0, y1, x0, x1, h);
		helpers.recycle(h);
	}
}
