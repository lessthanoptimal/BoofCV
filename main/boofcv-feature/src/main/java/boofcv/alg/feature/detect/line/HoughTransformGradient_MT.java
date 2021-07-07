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
import boofcv.struct.ListIntPoint2D;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I32;
import pabeles.concurrency.GrowArray;

/**
 * Concurrent version of {@link HoughTransformGradient}
 *
 * @author Peter Abeles
 */
public class HoughTransformGradient_MT<D extends ImageGray<D>>
		extends HoughTransformGradient<D> {

	// storage for candidates in each thread's block
	private final GrowArray<ListIntPoint2D> blockCandidates = new GrowArray<>(ListIntPoint2D::new);

	Point2D_I32 p = new Point2D_I32();

	/**
	 * Specifies parameters of transform.
	 *
	 * @param extractor Extracts local maxima from transform space. A set of candidates is provided, but can be ignored.
	 */
	public HoughTransformGradient_MT( NonMaxSuppression extractor, HoughTransformParameters parameters, Class<D> derivType ) {
		super(extractor, parameters, derivType);
	}

	@Override
	void transform( GrayU8 binary ) {
		candidates.configure(transform.width, transform.height);
		blockCandidates.reset();
		BoofConcurrency.loopBlocks(0, binary.height, blockCandidates, ( storage, y0, y1 ) -> {
			storage.configure(transform.width, transform.height);
			for (int y = y0; y < y1; y++) {
				int start = binary.startIndex + y*binary.stride;
				int end = start + binary.width;

				for (int index = start; index < end; index++) {
					if (binary.data[index] != 0) {
						int x = index - start;
						parameterize(storage, x, y, _derivX.unsafe_getF(x, y), _derivY.unsafe_getF(x, y));
					}
				}
			}
		});

		// Combine results found in each thread together
		for (int i = 0; i < blockCandidates.size(); i++) {
			ListIntPoint2D s = blockCandidates.get(i);
			for (int j = 0; j < s.size(); j++) {
				s.get(j, p);
				candidates.add(p.x, p.y);
			}
		}
	}
}
