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

package boofcv.abst.feature.detect.peak;

import boofcv.factory.feature.detect.peak.ConfigMeanShiftSearch;
import boofcv.factory.feature.detect.peak.FactorySearchLocalPeak;
import boofcv.struct.image.GrayF32;

public class TestMeanShiftPeak_to_SearchLocalPeak extends GeneralSearchLocalPeakChecks {
	@Override
	public SearchLocalPeak createSearch( Class<GrayF32> imageType ) {
		return FactorySearchLocalPeak.meanShiftGaussian(new ConfigMeanShiftSearch(50,1e-3f),imageType);
	}
}
