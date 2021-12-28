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

package boofcv.alg.feature.detect.extract;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;

/**
 * @author Peter Abeles
 */
public abstract class GenericNonMaxBlockTests extends GenericNonMaxTests {
	NonMaxBlock nonmax;

	protected GenericNonMaxBlockTests( boolean strict, NonMaxBlock.Search search ) {
		super(strict, search.isDetectMinimums(), search.isDetectMaximums());
		nonmax = new NonMaxBlock(search);
	}

	protected GenericNonMaxBlockTests( boolean strict, NonMaxBlock.Search search, boolean concurrent ) {
		super(strict, search.isDetectMinimums(), search.isDetectMaximums());
		if (concurrent)
			nonmax = new NonMaxBlock_MT(search);
		else
			nonmax = new NonMaxBlock(search);
	}

	@Override
	public void findPeaks( GrayF32 intensity, float threshold, int radius, int border,
						   QueueCorner foundMinimum, QueueCorner foundMaximum ) {
		nonmax.setSearchRadius(radius);
		nonmax.setBorder(border);
		nonmax.setThresholdMin(-threshold);
		nonmax.setThresholdMax(threshold);

		nonmax.process(intensity, foundMinimum, foundMaximum);
	}
}
