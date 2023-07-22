/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.interest;

import boofcv.alg.feature.detect.intensity.FastCornerDetector;
import boofcv.alg.feature.detect.intensity.impl.ImplFastCorner11_U8;
import boofcv.alg.feature.detect.selector.FeatureSelectN;
import boofcv.struct.image.GrayU8;

class TestFastToInterestPoint extends GeneralInterestPointDetectorChecks<GrayU8> {
	public TestFastToInterestPoint() {
		super(new FastToInterestPoint<>(new FastCornerDetector<>(new ImplFastCorner11_U8(20)),
				new FeatureSelectN<>()), false, false, GrayU8.class);
	}
}
