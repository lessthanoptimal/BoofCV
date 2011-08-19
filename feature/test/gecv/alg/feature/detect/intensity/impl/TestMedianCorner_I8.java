/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.feature.detect.intensity.impl;

import gecv.abst.filter.blur.FactoryBlurFilter;
import gecv.alg.feature.detect.intensity.GenericCornerIntensityTests;
import gecv.alg.feature.detect.intensity.MedianCornerIntensity;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageUInt8;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestMedianCorner_I8 extends GenericCornerIntensityTests {

	ImageFloat32 intensity = new ImageFloat32(width,height);
	ImageUInt8 median = new ImageUInt8(width,height);

	@Test
	public void genericTests() {
		performAllTests();
	}

	@Override
	public ImageFloat32 computeIntensity() {
		MedianCornerIntensity.process(intensity,imageI,median);
		return intensity;
	}

	@Override
	protected void computeDerivatives() {
		FactoryBlurFilter.median(ImageUInt8.class,2).process(imageI,median);
	}
}
