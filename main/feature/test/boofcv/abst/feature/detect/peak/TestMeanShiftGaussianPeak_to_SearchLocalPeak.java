/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.peak;

import boofcv.alg.feature.detect.peak.MeanShiftGaussianPeak;

/**
 * @author Peter Abeles
 */
public class TestMeanShiftGaussianPeak_to_SearchLocalPeak extends GeneralSearchLocalPeakChecks {
	@Override
	public SearchLocalPeak createSearch() {
		MeanShiftGaussianPeak alg = new MeanShiftGaussianPeak(50,1e-3f,-1);
		return new MeanShiftPeak_to_SearchLocalPeak(alg);
	}
}
