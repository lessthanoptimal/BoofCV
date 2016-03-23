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

package boofcv.alg.feature.detect.extract;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestNonMaxExtractorNaive {

	@Test
	public void standardTests() {
		standardTests(true);
		standardTests(false);
	}

	private void standardTests( boolean strict ) {
		new GenericNonMaxAlgorithmTests(strict,false,true) {

			@Override
			public void findMaximums(GrayF32 intensity, float threshold, int radius, int border,
									 QueueCorner foundMinimum, QueueCorner foundMaximum) {
				NonMaxExtractorNaive alg = new NonMaxExtractorNaive(strict);
				alg.setSearchRadius(radius);
				alg.setThreshold(threshold);
				alg.setBorder(border);
				alg.process(intensity,foundMaximum);
			}
		}.allStandard();
	}

}
