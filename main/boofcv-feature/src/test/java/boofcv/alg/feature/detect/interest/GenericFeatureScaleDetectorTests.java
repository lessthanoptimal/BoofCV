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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.struct.image.GrayF32;

public abstract class GenericFeatureScaleDetectorTests extends GenericFeatureDetectorTests {

	int r = 2;

	private GeneralFeatureDetector<GrayF32, GrayF32> createBlobDetector(int maxFeatures) {
		return FactoryDetectPoint.createHessianDeriv(new ConfigGeneralDetector(maxFeatures,r,0,0,true,true,true), HessianBlobIntensity.Type.TRACE,
				GrayF32.class);
	}

	@Override
	protected Object createDetector(int maxFeatures) {
		return createDetector(createBlobDetector(maxFeatures));
	}

	protected abstract Object createDetector(GeneralFeatureDetector<GrayF32, GrayF32> detector);
}
