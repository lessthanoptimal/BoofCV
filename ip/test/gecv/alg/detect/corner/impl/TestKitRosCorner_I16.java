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

package gecv.alg.detect.corner.impl;

import gecv.alg.detect.corner.GenericCornerIntensityGradientTests;
import gecv.struct.image.ImageFloat32;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestKitRosCorner_I16 extends GenericCornerIntensityGradientTests {

	KitRosCorner_I16 detector = new KitRosCorner_I16();

	@Test
	public void genericTests() {
		performAllTests();
	}

	@Override
	public ImageFloat32 computeIntensity() {
		detector.process(derivX_I16,derivY_I16,derivXX_I16,derivYY_I16,derivXY_I16);
		return detector.getIntensity();
	}

}
