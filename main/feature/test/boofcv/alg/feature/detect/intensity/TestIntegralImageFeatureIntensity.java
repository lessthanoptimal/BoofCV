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

package boofcv.alg.feature.detect.intensity;

import boofcv.alg.feature.detect.intensity.impl.ImplIntegralImageFeatureIntensity;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.ii.IntegralImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class TestIntegralImageFeatureIntensity {

	Random rand = new Random(234);
	int width = 60;
	int height = 70;

	/**
	 * Compares hessian intensity against a naive implementation
	 */
	@Test
	public void hessian_F32() {
		GrayF32 original = new GrayF32(width,height);
		GrayF32 integral = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);
		GrayF32 expected = new GrayF32(width,height);

		GImageMiscOps.fillUniform(original, rand, 0, 50);
		IntegralImageOps.transform(original,integral);

		int size = 9;

		for( int skip = 1; skip <= 4; skip++ ) {
			found.reshape(width/skip,height/skip);
			expected.reshape(width/skip,height/skip);
			ImplIntegralImageFeatureIntensity.hessianNaive(integral,skip,size,expected);
			IntegralImageFeatureIntensity.hessian(integral,skip,size,found);

			BoofTesting.assertEquals(expected,found, 1e-4f);
		}
	}

	/**
	 * Compares hessian intensity against a naive implementation
	 */
	@Test
	public void hessian_S32() {
		GrayS32 original = new GrayS32(width,height);
		GrayS32 integral = new GrayS32(width,height);
		GrayF32 found = new GrayF32(width,height);
		GrayF32 expected = new GrayF32(width,height);

		GImageMiscOps.fillUniform(original, rand, 0, 50);
		IntegralImageOps.transform(original,integral);

		int size = 9;

		for( int skip = 1; skip <= 4; skip++ ) {
			found.reshape(width/skip,height/skip);
			expected.reshape(width/skip,height/skip);
			ImplIntegralImageFeatureIntensity.hessianNaive(integral,skip,size,expected);
			IntegralImageFeatureIntensity.hessian(integral,skip,size,found);

			BoofTesting.assertEquals(expected,found, 1e-4f);
		}
	}
}
