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

package boofcv.alg.feature.detect.line.gridline;

import boofcv.alg.feature.detect.line.CommonGridRansacLineDetectorChecks;
import boofcv.alg.feature.detect.line.GridRansacLineDetector;
import boofcv.struct.image.GrayS16;
import georegression.struct.line.LinePolar2D_F32;
import org.ddogleg.fitting.modelset.ModelMatcher;

/**
 * @author Peter Abeles
 */
public class TestImplGridRansacLineDetector_S16 extends CommonGridRansacLineDetectorChecks<GrayS16> {

	public TestImplGridRansacLineDetector_S16() {
		super(GrayS16.class);
	}

	@Override
	public GridRansacLineDetector<GrayS16> createDetector(int regionSize, int maxDetectLines,
														  ModelMatcher<LinePolar2D_F32, Edgel> robustMatcher) {
		return new ImplGridRansacLineDetector_S16(regionSize,maxDetectLines,robustMatcher);
	}
}
