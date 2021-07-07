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

package boofcv.alg.feature.detect.line;

import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayU8;

/**
 * Concurrent version of {@link HoughTransformBinary}
 *
 * @author Peter Abeles
 */
public class HoughTransformBinary_MT extends HoughTransformBinary {
	/**
	 * Specifies parameters of transform. The minimum number of points specified in the extractor
	 * is an important tuning parameter.
	 *
	 * @param extractor Extracts local maxima from transform space.
	 */
	public HoughTransformBinary_MT( NonMaxSuppression extractor, HoughTransformParameters parameters ) {
		super(extractor, parameters);
	}

	@Override
	void computeParameters( GrayU8 binary ) {
		BoofConcurrency.loopFor(0, binary.height, y -> {
			int start = binary.startIndex + y*binary.stride;
			int stop = start + binary.width;

			for (int index = start; index < stop; index++) {
				if (binary.data[index] != 0) {
					parameters.parameterize(index - start, y, transform);
				}
			}
		});
	}
}
