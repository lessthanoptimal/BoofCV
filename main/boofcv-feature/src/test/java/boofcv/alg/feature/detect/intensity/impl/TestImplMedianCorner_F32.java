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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.alg.feature.detect.intensity.GenericCornerIntensityTests;
import boofcv.alg.feature.detect.intensity.MedianCornerIntensity;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */
public class TestImplMedianCorner_F32 extends GenericCornerIntensityTests
{
	GrayF32 median = new GrayF32(width,height);

	@Test void genericTests() {
		performAllTests();
	}

	@Override
	public void computeIntensity( GrayF32 intensity ) {
		MedianCornerIntensity.process(imageF, median, intensity);
	}

	@Override
	protected void computeDerivatives() {
		FactoryBlurFilter.median(ImageType.single(GrayF32.class),2).process(imageF,median);
	}
}
